package com.nammaarena.payment.service;

import com.nammaarena.payment.client.BookingServiceClient;
import com.nammaarena.payment.dto.*;
import com.nammaarena.payment.entity.Payment;
import com.nammaarena.payment.enums.PaymentStatus;
import com.nammaarena.payment.repository.PaymentRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;

@Service
@RequiredArgsConstructor
public class PaymentService {
    
    private final PaymentRepository paymentRepository;
    private final BookingServiceClient bookingServiceClient;
    
    @Value("${stripe.secret.key}")
    private String stripeSecretKey;
    
    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    @Transactional
    public PaymentResponse createPaymentIntent(PaymentRequest request) {
        try {
            System.out.println("Creating Stripe payment for booking: " + request.getBookingId() + ", amount: " + request.getAmount());
            
            // Create Stripe PaymentIntent
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(request.getAmount().multiply(new java.math.BigDecimal("100")).longValue())
                .setCurrency("inr")
                .setAutomaticPaymentMethods(
                    PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                        .setEnabled(true)
                        .build()
                )
                .build();
                
            System.out.println("Creating PaymentIntent with params: amount=" + params.getAmount() + ", currency=" + params.getCurrency());
            PaymentIntent intent = PaymentIntent.create(params);
            
            System.out.println("PaymentIntent created successfully:");
            System.out.println("- ID: " + intent.getId());
            System.out.println("- Status: " + intent.getStatus());
            System.out.println("- Amount: " + intent.getAmount());
            System.out.println("- Currency: " + intent.getCurrency());
            System.out.println("- Client Secret: " + intent.getClientSecret());
            
            // Save payment record
            Payment payment = new Payment();
            payment.setBookingId(request.getBookingId());
            payment.setAmount(request.getAmount());
            payment.setStripePaymentId(intent.getId());
            payment.setStatus(PaymentStatus.PENDING);
            
            payment = paymentRepository.save(payment);
            System.out.println("Payment saved to database with ID: " + payment.getId());
            
            // Map to response
            PaymentResponse response = mapToResponse(payment);
            response.setClientSecret(intent.getClientSecret());
            
            System.out.println("Returning response with client secret: " + response.getClientSecret());
            return response;
            
        } catch (StripeException e) {
            System.err.println("Stripe error: " + e.getMessage());
            throw new RuntimeException("Stripe payment failed: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Payment error: " + e.getMessage());
            throw new RuntimeException("Payment creation failed: " + e.getMessage());
        }
    }

    @Transactional
    public PaymentResponse confirmPayment(ConfirmPaymentRequest request) {
        try {
            // Retrieve PaymentIntent from Stripe
            PaymentIntent intent = PaymentIntent.retrieve(request.getPaymentIntentId());
            
            // Find payment record
            Payment payment = paymentRepository.findByStripePaymentId(request.getPaymentIntentId())
                .orElseThrow(() -> new RuntimeException("Payment not found"));
            
            if ("succeeded".equals(intent.getStatus())) {
                // Update payment status
                payment.setStatus(PaymentStatus.COMPLETED);
                payment = paymentRepository.save(payment);
                
                // Update booking status
                try {
                    bookingServiceClient.updateBookingStatus(
                        payment.getBookingId(), 
                        "CONFIRMED", 
                        payment.getStripePaymentId()
                    );
                } catch (Exception e) {
                    System.err.println("Failed to update booking: " + e.getMessage());
                }
                
                return mapToResponse(payment);
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                payment = paymentRepository.save(payment);
                throw new RuntimeException("Payment failed");
            }
            
        } catch (StripeException e) {
            throw new RuntimeException("Stripe confirmation failed: " + e.getMessage());
        }
    }
    
    @Transactional
    public PaymentResponse mockConfirmPayment(String paymentId) {
        // Find payment record
        Payment payment = paymentRepository.findByStripePaymentId(paymentId)
            .orElseThrow(() -> new RuntimeException("Payment not found"));
        
        // Update payment status to completed
        payment.setStatus(PaymentStatus.COMPLETED);
        payment = paymentRepository.save(payment);
        
        // Update booking status via Feign client
        try {
            bookingServiceClient.updateBookingStatus(
                payment.getBookingId(), 
                "CONFIRMED", 
                payment.getStripePaymentId()
            );
            System.out.println("Booking status updated to CONFIRMED for booking: " + payment.getBookingId());
        } catch (Exception e) {
            System.err.println("Failed to update booking status: " + e.getMessage());
        }
        
        return mapToResponse(payment);
    }
    
    public void handleWebhook(String payload, String sigHeader) {
        try {
            // For now, just log the webhook - you can add signature verification later
            System.out.println("Received Stripe webhook: " + payload);
            
            // Parse the event (simplified)
            if (payload.contains("payment_intent.succeeded")) {
                // Extract payment intent ID from payload
                String paymentIntentId = extractPaymentIntentId(payload);
                if (paymentIntentId != null) {
                    // Find and update payment
                    Payment payment = paymentRepository.findByStripePaymentId(paymentIntentId)
                        .orElse(null);
                    
                    if (payment != null) {
                        payment.setStatus(PaymentStatus.COMPLETED);
                        paymentRepository.save(payment);
                        
                        // Update booking status
                        try {
                            bookingServiceClient.updateBookingStatus(
                                payment.getBookingId(), 
                                "CONFIRMED", 
                                payment.getStripePaymentId()
                            );
                            System.out.println("Booking confirmed via webhook for booking: " + payment.getBookingId());
                        } catch (Exception e) {
                            System.err.println("Failed to update booking via webhook: " + e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Webhook processing error: " + e.getMessage());
        }
    }
    
    private String extractPaymentIntentId(String payload) {
        try {
            // Simple extraction - in production use proper JSON parsing
            String search = "\"id\":\"pi_";
            int start = payload.indexOf(search);
            if (start != -1) {
                start += search.length() - 3; // Include "pi_"
                int end = payload.indexOf("\"", start);
                if (end != -1) {
                    return payload.substring(start, end);
                }
            }
        } catch (Exception e) {
            System.err.println("Error extracting payment intent ID: " + e.getMessage());
        }
        return null;
    }
    
    @Transactional
    public PaymentResponse handlePaymentSuccess(String paymentIntentId) {
        try {
            // Retrieve PaymentIntent from Stripe to verify status
            PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
            
            // Find payment record
            Payment payment = paymentRepository.findByStripePaymentId(paymentIntentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
            
            if ("succeeded".equals(intent.getStatus())) {
                // Update payment status
                payment.setStatus(PaymentStatus.COMPLETED);
                payment = paymentRepository.save(payment);
                
                // Update booking status
                try {
                    bookingServiceClient.updateBookingStatus(
                        payment.getBookingId(), 
                        "CONFIRMED", 
                        payment.getStripePaymentId()
                    );
                    System.out.println("Booking confirmed for booking: " + payment.getBookingId());
                } catch (Exception e) {
                    System.err.println("Failed to update booking: " + e.getMessage());
                }
                
                return mapToResponse(payment);
            } else {
                throw new RuntimeException("Payment not successful. Status: " + intent.getStatus());
            }
            
        } catch (StripeException e) {
            throw new RuntimeException("Failed to verify payment: " + e.getMessage());
        }
    }

    public java.util.Map<String, String> testStripeConnection() {
        java.util.Map<String, String> result = new java.util.HashMap<>();
        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(100L)
                .setCurrency("inr")
                .build();
                
            PaymentIntent intent = PaymentIntent.create(params);
            
            result.put("status", "success");
            result.put("message", "Stripe connection working");
            result.put("testPaymentId", intent.getId());
            
        } catch (StripeException e) {
            result.put("status", "error");
            result.put("message", "Stripe error: " + e.getMessage());
        }
        return result;
    }
    
    private PaymentResponse mapToResponse(Payment payment) {
        PaymentResponse response = new PaymentResponse();
        response.setId(payment.getId());
        response.setBookingId(payment.getBookingId());
        response.setAmount(payment.getAmount());
        response.setStripePaymentId(payment.getStripePaymentId());
        response.setStatus(payment.getStatus());
        response.setTimestamp(payment.getTimestamp());
        return response;
    }
}