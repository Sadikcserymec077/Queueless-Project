package com.queueless.ai.repository;

import com.queueless.ai.entity.QueueAnalytics;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QueueAnalyticsRepository extends JpaRepository<QueueAnalytics, Long> {
    List<QueueAnalytics> findByDateBetweenOrderByDateAsc(LocalDate from, LocalDate to);
}
