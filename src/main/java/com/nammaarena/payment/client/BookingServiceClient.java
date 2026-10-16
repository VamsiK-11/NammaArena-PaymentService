package com.nammaarena.payment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "booking-service")
public interface BookingServiceClient {
    
    @PutMapping("/api/bookings/{bookingId}/status")
    void updateBookingStatus(@PathVariable Long bookingId, 
                           @RequestParam String status, 
                           @RequestParam String paymentId);
}