package com.nammaarena.payment.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class PaymentAspect {

    @Before("execution(* com.nammaarena.payment.service.PaymentService.createPaymentIntent(..))")
    public void auditPaymentCreation(JoinPoint joinPoint) {
        log.info("PAYMENT_AUDIT: Payment intent creation initiated");
    }

    @Before("execution(* com.nammaarena.payment.service.PaymentService.confirmPayment(..))")
    public void auditPaymentConfirmation(JoinPoint joinPoint) {
        log.info("PAYMENT_AUDIT: Payment confirmation attempt");
    }

    @Before("execution(* com.nammaarena.payment.service.PaymentService.handleWebhook(..))")
    public void auditWebhook(JoinPoint joinPoint) {
        log.info("PAYMENT_SECURITY: Webhook received");
    }

    @Around("execution(* com.nammaarena.payment.service.PaymentService.*(..))")
    public Object measurePerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;
            log.info("PAYMENT_PERFORMANCE: {} completed in {} ms", joinPoint.getSignature().toShortString(), executionTime);
            return result;
        } catch (Throwable throwable) {
            log.error("PAYMENT_ERROR: {} failed - {}", joinPoint.getSignature().toShortString(), throwable.getMessage());
            throw throwable;
        }
    }
}