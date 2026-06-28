package com.queueless.ai.service;

import org.springframework.stereotype.Service;

@Service
public class PricingService {

    /**
     * Returns the discount percentage based on the patient count.
     * 1 patient  → 0% discount
     * 2 patients → 30% discount
     * 3+ patients → 40% discount
     */
    public double discountPercent(int patientCount) {
        if (patientCount >= 3) return 0.40;
        if (patientCount == 2) return 0.30;
        return 0.0;
    }

    public double originalAmount(int patientCount, double bookingFee) {
        return patientCount * bookingFee;
    }

    public double finalAmount(int patientCount, double bookingFee) {
        double original = originalAmount(patientCount, bookingFee);
        return original * (1.0 - discountPercent(patientCount));
    }

    /** Convert rupees to paise (Razorpay uses smallest currency unit) */
    public long toPaise(double rupees) {
        return Math.round(rupees * 100);
    }
}
