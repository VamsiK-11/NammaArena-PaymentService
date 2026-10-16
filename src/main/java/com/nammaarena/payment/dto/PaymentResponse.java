package com.nammaarena.payment.dto;

import com.nammaarena.payment.enums.PaymentStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentResponse {
    private Long id;
    private Long bookingId;
    private BigDecimal amount;
    private String stripePaymentId;
    private PaymentStatus status;
    private LocalDateTime timestamp;
    private String clientSecret;
}