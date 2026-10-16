package com.nammaarena.payment.service;

import com.nammaarena.payment.client.BookingServiceClient;
import com.nammaarena.payment.dto.PaymentRequest;
import com.nammaarena.payment.dto.ConfirmPaymentRequest;
import com.nammaarena.payment.entity.Payment;
import com.nammaarena.payment.enums.PaymentStatus;
import com.nammaarena.payment.repository.PaymentRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BookingServiceClient bookingServiceClient;

    @InjectMocks
    private PaymentService paymentService;

    private Payment testPayment;
    private PaymentRequest paymentRequest;

    @BeforeEach
    void setUp() {
        testPayment = new Payment();
        testPayment.setId(1L);
        testPayment.setBookingId(1L);
        testPayment.setAmount(new BigDecimal("100"));
        testPayment.setStatus(PaymentStatus.PENDING);
        testPayment.setStripePaymentId("pi_test123");

        paymentRequest = new PaymentRequest();
        paymentRequest.setBookingId(1L);
        paymentRequest.setAmount(new BigDecimal("100"));
    }

    @Test
    void createPaymentIntent_Success() {
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        try (MockedStatic<PaymentIntent> mockedPaymentIntent = mockStatic(PaymentIntent.class)) {
            PaymentIntent mockIntent = mock(PaymentIntent.class);
            when(mockIntent.getId()).thenReturn("pi_test123");
            when(mockIntent.getClientSecret()).thenReturn("pi_test123_secret");
            mockedPaymentIntent.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class))).thenReturn(mockIntent);

            var response = paymentService.createPaymentIntent(paymentRequest);

            assertNotNull(response);
            assertEquals("pi_test123", response.getStripePaymentId());
        }
    }

    @Test
    void confirmPayment_Success() {
        when(paymentRepository.findByStripePaymentId("pi_test123")).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        try (MockedStatic<PaymentIntent> mockedPaymentIntent = mockStatic(PaymentIntent.class)) {
            PaymentIntent mockIntent = mock(PaymentIntent.class);
            when(mockIntent.getStatus()).thenReturn("succeeded");
            mockedPaymentIntent.when(() -> PaymentIntent.retrieve("pi_test123")).thenReturn(mockIntent);

            ConfirmPaymentRequest request = new ConfirmPaymentRequest();
            request.setPaymentIntentId("pi_test123");

            var response = paymentService.confirmPayment(request);

            assertNotNull(response);
            assertEquals(PaymentStatus.COMPLETED, testPayment.getStatus());
        }
    }

    @Test
    void mockConfirmPayment_Success() {
        when(paymentRepository.findByStripePaymentId("pi_test123")).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        var response = paymentService.mockConfirmPayment("pi_test123");

        assertNotNull(response);
        assertEquals(PaymentStatus.COMPLETED, testPayment.getStatus());
    }

    @Test
    void testStripeConnection_Success() {
        try (MockedStatic<PaymentIntent> mockedPaymentIntent = mockStatic(PaymentIntent.class)) {
            PaymentIntent mockIntent = mock(PaymentIntent.class);
            when(mockIntent.getId()).thenReturn("pi_test123");
            mockedPaymentIntent.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class))).thenReturn(mockIntent);

            var result = paymentService.testStripeConnection();

            assertEquals("success", result.get("status"));
        }
    }

    @Test
    void testStripeConnection_Error() {
        try (MockedStatic<PaymentIntent> mockedPaymentIntent = mockStatic(PaymentIntent.class)) {
            mockedPaymentIntent.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
                .thenThrow(new StripeException("Connection error", "request_id", "code", 400) {});

            var result = paymentService.testStripeConnection();

            assertEquals("error", result.get("status"));
        }
    }

    @Test
    void confirmPayment_NotFound_ThrowsException() {
        when(paymentRepository.findByStripePaymentId("pi_test123")).thenReturn(Optional.empty());

        try (MockedStatic<PaymentIntent> mockedPaymentIntent = mockStatic(PaymentIntent.class)) {
            PaymentIntent mockIntent = mock(PaymentIntent.class);
            mockedPaymentIntent.when(() -> PaymentIntent.retrieve("pi_test123")).thenReturn(mockIntent);

            ConfirmPaymentRequest request = new ConfirmPaymentRequest();
            request.setPaymentIntentId("pi_test123");

            assertThrows(RuntimeException.class, () -> paymentService.confirmPayment(request));
        }
    }

    @Test
    void confirmPayment_Failed_ThrowsException() {
        when(paymentRepository.findByStripePaymentId("pi_test123")).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        try (MockedStatic<PaymentIntent> mockedPaymentIntent = mockStatic(PaymentIntent.class)) {
            PaymentIntent mockIntent = mock(PaymentIntent.class);
            when(mockIntent.getStatus()).thenReturn("failed");
            mockedPaymentIntent.when(() -> PaymentIntent.retrieve("pi_test123")).thenReturn(mockIntent);

            ConfirmPaymentRequest request = new ConfirmPaymentRequest();
            request.setPaymentIntentId("pi_test123");

            assertThrows(RuntimeException.class, () -> paymentService.confirmPayment(request));
        }
    }

    @Test
    void mockConfirmPayment_NotFound_ThrowsException() {
        when(paymentRepository.findByStripePaymentId("pi_test123")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> paymentService.mockConfirmPayment("pi_test123"));
    }

    @Test
    void createPaymentIntent_StripeException_ThrowsRuntimeException() {
        try (MockedStatic<PaymentIntent> mockedPaymentIntent = mockStatic(PaymentIntent.class)) {
            mockedPaymentIntent.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
                .thenThrow(new StripeException("Stripe error", "request_id", "code", 400) {});

            assertThrows(RuntimeException.class, () -> paymentService.createPaymentIntent(paymentRequest));
        }
    }
}