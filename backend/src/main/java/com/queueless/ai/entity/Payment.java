package com.queueless.ai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 60)
    private String paymentId;

    @Column(nullable = false, unique = true, length = 100)
    private String razorpayOrderId;

    @Column(length = 100)
    private String razorpayPaymentId;

    @Column(length = 500)
    private String razorpaySignature;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long counterId;

    @Builder.Default
    private Integer patientCount = 1;

    private Double originalAmount;

    private Double discountPercent;

    private Double finalAmount;

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
