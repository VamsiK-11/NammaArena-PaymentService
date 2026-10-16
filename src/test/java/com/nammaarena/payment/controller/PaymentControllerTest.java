package com.nammaarena.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nammaarena.payment.dto.*;
import com.nammaarena.payment.enums.PaymentStatus;
import com.nammaarena.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void createPaymentIntent_Success() throws Exception {
        PaymentRequest request = new PaymentRequest();
        request.setBookingId(1L);
        request.setAmount(new BigDecimal("100"));

        PaymentResponse response = new PaymentResponse();
        response.setStatus(PaymentStatus.PENDING);

        when(paymentService.createPaymentIntent(any(PaymentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/payments/create-intent")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void confirmPayment_Success() throws Exception {
        ConfirmPaymentRequest request = new ConfirmPaymentRequest();
        request.setPaymentIntentId("pi_test123");

        PaymentResponse response = new PaymentResponse();
        response.setStatus(PaymentStatus.COMPLETED);

        when(paymentService.confirmPayment(any(ConfirmPaymentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/payments/confirm")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void testStripeConnection_Success() throws Exception {
        Map<String, String> result = new HashMap<>();
        result.put("status", "success");

        when(paymentService.testStripeConnection()).thenReturn(result);

        mockMvc.perform(get("/api/payments/test-stripe"))
                .andExpect(status().isOk());
    }

    @Test
    void handleWebhook_Success() throws Exception {
        String payload = "{\"type\":\"payment_intent.succeeded\"}";
        doNothing().when(paymentService).handleWebhook(payload, "sig_header");

        mockMvc.perform(post("/api/payments/webhook")
                .header("Stripe-Signature", "sig_header")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isOk());
    }
}