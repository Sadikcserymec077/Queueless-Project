package com.queueless.ai.repository;

import com.queueless.ai.entity.Counter;
import com.queueless.ai.entity.CounterStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CounterRepository extends JpaRepository<Counter, Long> {
    List<Counter> findByOrganizationIdOrderByCounterNumberAsc(Long organizationId);

    long countByOrganizationIdAndStatus(Long organizationId, CounterStatus status);

    @jakarta.persistence.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT c FROM Counter c WHERE c.id = :id")
    java.util.Optional<Counter> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);
}
