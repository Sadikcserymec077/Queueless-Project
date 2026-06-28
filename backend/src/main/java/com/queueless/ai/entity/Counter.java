package com.queueless.ai.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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
@Table(name = "counters")
public class Counter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String counterName;

    @Column(nullable = false)
    private Integer counterNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CounterStatus status;

    @Column(nullable = false, length = 120)
    private String serviceType;

    // --- Feature 1: Future Date Booking ---
    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "counter_available_dates", joinColumns = @JoinColumn(name = "counter_id"))
    @Column(name = "available_date")
    private List<LocalDate> availableDates = new ArrayList<>();

    @Builder.Default
    @Column(name = "daily_capacity")
    private Integer dailyCapacity = 100;

    // --- Feature 2: Payment ---
    @Builder.Default
    @Column(name = "booking_fee")
    private Double bookingFee = 0.0;
    
    public Integer getDailyCapacity() {
        return dailyCapacity != null ? dailyCapacity : 100;
    }
    
    public Double getBookingFee() {
        return bookingFee != null ? bookingFee : 0.0;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Builder.Default
    @OneToMany(mappedBy = "counter", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Token> tokens = new ArrayList<>();
}
