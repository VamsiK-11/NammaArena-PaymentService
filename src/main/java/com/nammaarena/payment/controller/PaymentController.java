package com.nammaarena.payment.controller;

import com.nammaarena.payment.dto.*;
import com.nammaarena.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class PaymentController {
    
    private final PaymentService paymentService;

    @PostMapping("/create")
    public ResponseEntity<PaymentResponse> createPaymentIntent(@RequestBody PaymentRequest request) {
        System.out.println("Payment request received: " + request);
        return ResponseEntity.ok(paymentService.createPaymentIntent(request));
    }
    
    @GetMapping("/config")
    public ResponseEntity<Map<String, String>> getStripeConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("publishableKey", "pk_test_51S5gKzPARYumdThXYxUt4LS6N6vnUkbCfvBHVS33DhrAISCzkdyDdZnkn4FkXlmlh69usUmAGoKVqsgYGGJkBiBT00njNZxGJI");
        return ResponseEntity.ok(config);
    }
    
    @GetMapping("/test-stripe")
    public ResponseEntity<Map<String, String>> testStripe() {
        return ResponseEntity.ok(paymentService.testStripeConnection());
    }
    
    @PostMapping("/test")
    public ResponseEntity<Map<String, String>> testEndpoint(@RequestBody Map<String, Object> request) {
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Payment service is working");
        response.put("received", request.toString());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/confirm")
    public ResponseEntity<PaymentResponse> confirmPayment(@RequestBody ConfirmPaymentRequest request) {
        return ResponseEntity.ok(paymentService.confirmPayment(request));
    }
    
    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            paymentService.handleWebhook(payload, sigHeader);
            return ResponseEntity.ok("Success");
        } catch (Exception e) {
            System.err.println("Webhook error: " + e.getMessage());
            return ResponseEntity.badRequest().body("Webhook failed");
        }
    }
    
    @GetMapping("/success")
    public ResponseEntity<Map<String, String>> paymentSuccess(@RequestParam String payment_intent) {
        try {
            PaymentResponse response = paymentService.handlePaymentSuccess(payment_intent);
            Map<String, String> result = new HashMap<>();
            result.put("status", "success");
            result.put("message", "Payment confirmed successfully");
            result.put("bookingId", response.getBookingId().toString());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @PostMapping("/mock-confirm/{paymentId}")
    public ResponseEntity<PaymentResponse> mockConfirmPayment(@PathVariable String paymentId) {
        return ResponseEntity.ok(paymentService.mockConfirmPayment(paymentId));
    }
}