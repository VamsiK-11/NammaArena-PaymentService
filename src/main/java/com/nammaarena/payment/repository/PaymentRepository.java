package com.nammaarena.payment.repository;

import com.nammaarena.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByStripePaymentId(String stripePaymentId);
    Optional<Payment> findByBookingId(Long bookingId);
}