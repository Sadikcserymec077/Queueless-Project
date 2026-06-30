package com.queueless.ai.repository;

import com.queueless.ai.entity.Token;
import com.queueless.ai.entity.TokenStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TokenRepository extends JpaRepository<Token, Long> {
    Optional<Token> findByTokenNumber(String tokenNumber);

    Page<Token> findByUserIdOrderByBookingTimeDesc(Long userId, Pageable pageable);

    List<Token> findTop20ByCounterIdAndStatusOrderByBookingTimeAsc(Long counterId, TokenStatus status);

    Optional<Token> findTopByUserIdAndStatusInOrderByBookingTimeDesc(Long userId, Collection<TokenStatus> statuses);

    Optional<Token> findTopByUserIdAndCounterIdAndStatusInOrderByBookingTimeDesc(Long userId, Long counterId, Collection<TokenStatus> statuses);

    Optional<Token> findTopByCounterIdAndStatusOrderByBookingTimeAsc(Long counterId, TokenStatus status);

    Optional<Token> findTopByCounterIdAndStatusOrderByCalledAtDesc(Long counterId, TokenStatus status);

    boolean existsByUserIdAndCounterIdAndStatusIn(Long userId, Long counterId, Collection<TokenStatus> statuses);

    long countByCounterIdAndStatus(Long counterId, TokenStatus status);

    long countByCounterIdAndStatusAndBookingTimeBefore(Long counterId, TokenStatus status, Instant bookingTime);

    long countByCounterIdAndBookingTimeBetween(Long counterId, Instant from, Instant to);

    long countByUserIdAndBookingTimeBetweenAndStatusNot(Long userId, Instant from, Instant to, TokenStatus status);

    long countByStatus(TokenStatus status);

    List<Token> findByBookingTimeBetween(Instant from, Instant to);

    // ── Feature 1: Future Date Booking ────────────────────────────────────────

    /** Count tokens already booked for a specific counter on a specific future date */
    long countByCounterIdAndScheduledDate(Long counterId, LocalDate scheduledDate);

    /**
     * Find all WAITING tokens for a specific scheduled date
     * (used by the nightly cron job to assign queue positions).
     */
    @Query("""
            select t from Token t
            where t.scheduledDate = :date
              and t.status = com.queueless.ai.entity.TokenStatus.WAITING
            order by t.bookingTime asc
            """)
    List<Token> findWaitingByScheduledDate(@Param("date") LocalDate date);

    /**
     * Live queue for admins: only tokens that are active TODAY.
     * Future-dated tokens (scheduledDate > today) stay invisible until their day.
     */
    @Query("""
            select t from Token t
            where t.counter.id = :counterId
              and t.status = com.queueless.ai.entity.TokenStatus.WAITING
              and (t.scheduledDate is null or t.scheduledDate = :today)
            order by t.bookingTime asc
            """)
    List<Token> findLiveWaitingForCounter(@Param("counterId") Long counterId, @Param("today") LocalDate today);

    /** User's upcoming tokens (future-scheduled, not yet active) */
    @Query("""
            select t from Token t
            where t.user.id = :userId
              and t.status = com.queueless.ai.entity.TokenStatus.WAITING
              and t.scheduledDate is not null
              and t.scheduledDate > :today
            order by t.scheduledDate asc
            """)
    List<Token> findUpcomingForUser(@Param("userId") Long userId, @Param("today") LocalDate today);

    // ── Existing complex JPQL queries ─────────────────────────────────────────

    @Query("""
            select t from Token t
            where t.counter.id = :counterId
              and t.status = com.queueless.ai.entity.TokenStatus.COMPLETED
              and t.calledAt is not null
              and t.completedAt is not null
            order by t.completedAt desc
            """)
    List<Token> findCompletedTokensForCounter(@Param("counterId") Long counterId);

    @Query("""
            select count(t) from Token t
            where t.counter.organization.id = :organizationId
              and t.status in (
                    com.queueless.ai.entity.TokenStatus.WAITING,
                    com.queueless.ai.entity.TokenStatus.CALLED
              )
            """)
    long countActiveByOrganizationId(@Param("organizationId") Long organizationId);

    @Query("""
            select count(distinct t.counter.id) from Token t
            where t.status in (
                    com.queueless.ai.entity.TokenStatus.WAITING,
                    com.queueless.ai.entity.TokenStatus.CALLED
              )
            """)
    long countActiveQueues();

    @Query("""
            select t from Token t
            where (:status is null or t.status = :status)
              and (:from is null or t.bookingTime >= :from)
              and (:to is null or t.bookingTime <= :to)
              and (
                    cast(:q as String) is null
                    or lower(t.tokenNumber) like lower(concat('%', cast(:q as String), '%'))
                    or lower(t.user.name) like lower(concat('%', cast(:q as String), '%'))
                    or lower(t.counter.organization.name) like lower(concat('%', cast(:q as String), '%'))
              )
            """)
    Page<Token> searchAdmin(
            @Param("status") TokenStatus status,
            @Param("q") String q,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable
    );
}
