package com.queueless.ai.dto;

import java.util.List;

public class AnalyticsDtos {
    public record ChartPoint(String label, Double value) {
    }

    public record DashboardStatsResponse(
            long totalUsers,
            long totalTokens,
            long activeQueues,
            long completedServices,
            long cancelledTokens,
            List<ChartPoint> dailyVisitors,
            List<ChartPoint> weeklyVisitors,
            List<ChartPoint> monthlyVisitors,
            List<ChartPoint> peakHours,
            List<ChartPoint> averageWaitingTime,
            List<ChartPoint> serviceEfficiency
    ) {
    }
}
