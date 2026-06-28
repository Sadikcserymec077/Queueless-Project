package com.queueless.ai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
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
@Table(name = "tokens")
public class Token {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String tokenNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "counter_id", nullable = false)
    private Counter counter;

    @Column(nullable = false)
    private Instant bookingTime;

    private Instant calledAt;

    private Instant completedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TokenStatus status;

    @Column(nullable = false)
    private Integer estimatedWaitTime;

    @Column(length = 1000)
    private String qrPayload;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String qrCodeData;

    // --- Feature 1: Future Date Booking ---
    private LocalDate scheduledDate;

    private Integer queuePosition;

    // --- Feature 2: Payment ---
    @Builder.Default
    private Integer patientCount = 1;

    @Builder.Default
    @Column(length = 20)
    private String paymentStatus = "SUCCESS";
}
