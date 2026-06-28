package com.queueless.ai.repository;

import com.queueless.ai.entity.Payment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, String> {
    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);
}
