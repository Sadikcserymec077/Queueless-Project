package com.queueless.ai.repository;

import com.queueless.ai.entity.Counter;
import com.queueless.ai.entity.CounterStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CounterRepository extends JpaRepository<Counter, Long> {
    List<Counter> findByOrganizationIdOrderByCounterNumberAsc(Long organizationId);

    long countByOrganizationIdAndStatus(Long organizationId, CounterStatus status);
}
