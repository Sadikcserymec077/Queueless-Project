package com.queueless.ai.service;

import com.queueless.ai.dto.AnalyticsDtos.ChartPoint;
import com.queueless.ai.dto.AnalyticsDtos.DashboardStatsResponse;
import com.queueless.ai.entity.Token;
import com.queueless.ai.entity.TokenStatus;
import com.queueless.ai.repository.TokenRepository;
import com.queueless.ai.repository.UserRepository;
import com.queueless.ai.security.UserPrincipal;
import com.queueless.ai.entity.Role;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.WeekFields;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;

    @Transactional(readOnly = true)
    public DashboardStatsResponse dashboard(UserPrincipal principal) {
        Instant now = Instant.now();
        Instant from = now.minus(Duration.ofDays(90));
        ZoneId zone = ZoneId.systemDefault();
        
        boolean isSuperAdmin = principal.getUser().getRole() == Role.SUPER_ADMIN;
        Long orgId = principal.getUser().getOrganization() != null ? principal.getUser().getOrganization().getId() : null;

        List<Token> tokens;
        long totalUsers, totalTokens, activeQueues, completed, cancelled;
        
        if (isSuperAdmin) {
            tokens = tokenRepository.findByBookingTimeBetween(from, now);
            totalUsers = userRepository.count();
            totalTokens = tokenRepository.count();
            activeQueues = tokenRepository.countActiveQueues();
            completed = tokenRepository.countByStatus(TokenStatus.COMPLETED);
            cancelled = tokenRepository.countByStatus(TokenStatus.CANCELLED);
        } else {
            // ORG_ADMIN or SUB_ADMIN
            if (orgId == null) {
                tokens = List.of();
                totalUsers = 0; totalTokens = 0; activeQueues = 0; completed = 0; cancelled = 0;
            } else {
                tokens = tokenRepository.findByBookingTimeBetween(from, now).stream()
                        .filter(t -> t.getCounter().getOrganization().getId().equals(orgId))
                        .toList();
                totalUsers = userRepository.countByOrganizationId(orgId);
                totalTokens = tokenRepository.findAll().stream()
                        .filter(t -> t.getCounter().getOrganization().getId().equals(orgId))
                        .count();
                activeQueues = tokenRepository.countActiveByOrganizationId(orgId);
                completed = tokenRepository.findAll().stream()
                        .filter(t -> t.getCounter().getOrganization().getId().equals(orgId) && t.getStatus() == TokenStatus.COMPLETED)
                        .count();
                cancelled = tokenRepository.findAll().stream()
                        .filter(t -> t.getCounter().getOrganization().getId().equals(orgId) && t.getStatus() == TokenStatus.CANCELLED)
                        .count();
            }
        }

        return new DashboardStatsResponse(
                totalUsers,
                totalTokens,
                activeQueues,
                completed,
                cancelled,
                dailyVisitors(tokens, zone),
                weeklyVisitors(tokens, zone),
                monthlyVisitors(tokens, zone),
                peakHours(tokens, zone),
                averageWaitingTime(tokens, zone),
                serviceEfficiency(completed, cancelled, isSuperAdmin ? tokenRepository.countByStatus(TokenStatus.SKIPPED) : tokenRepository.findAll().stream().filter(t -> t.getCounter().getOrganization().getId().equals(orgId) && t.getStatus() == TokenStatus.SKIPPED).count())
        );
    }

    private List<ChartPoint> dailyVisitors(List<Token> tokens, ZoneId zone) {
        LocalDate today = LocalDate.now(zone);
        Map<LocalDate, Long> counts = tokens.stream()
                .collect(Collectors.groupingBy(token -> LocalDate.ofInstant(token.getBookingTime(), zone), TreeMap::new, Collectors.counting()));
        return IntStream.rangeClosed(0, 6)
                .mapToObj(today.minusDays(6)::plusDays)
                .map(date -> new ChartPoint(date.toString(), counts.getOrDefault(date, 0L).doubleValue()))
                .toList();
    }

    private List<ChartPoint> weeklyVisitors(List<Token> tokens, ZoneId zone) {
        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        Map<String, Long> counts = tokens.stream()
                .collect(Collectors.groupingBy(token -> {
                    LocalDate date = LocalDate.ofInstant(token.getBookingTime(), zone);
                    return date.getYear() + "-W" + date.get(weekFields.weekOfWeekBasedYear());
                }, TreeMap::new, Collectors.counting()));
        return counts.entrySet().stream()
                .skip(Math.max(0, counts.size() - 8))
                .map(entry -> new ChartPoint(entry.getKey(), entry.getValue().doubleValue()))
                .toList();
    }

    private List<ChartPoint> monthlyVisitors(List<Token> tokens, ZoneId zone) {
        Map<YearMonth, Long> counts = tokens.stream()
                .collect(Collectors.groupingBy(token -> YearMonth.from(token.getBookingTime().atZone(zone)), TreeMap::new, Collectors.counting()));
        return counts.entrySet().stream()
                .skip(Math.max(0, counts.size() - 6))
                .map(entry -> new ChartPoint(entry.getKey().toString(), entry.getValue().doubleValue()))
                .toList();
    }

    private List<ChartPoint> peakHours(List<Token> tokens, ZoneId zone) {
        Map<Integer, Long> counts = tokens.stream()
                .collect(Collectors.groupingBy(token -> token.getBookingTime().atZone(zone).getHour(), Collectors.counting()));
        return IntStream.range(0, 24)
                .mapToObj(hour -> new ChartPoint(String.format("%02d:00", hour), counts.getOrDefault(hour, 0L).doubleValue()))
                .sorted(Comparator.comparing(ChartPoint::label))
                .toList();
    }

    private List<ChartPoint> averageWaitingTime(List<Token> tokens, ZoneId zone) {
        LocalDate today = LocalDate.now(zone);
        return IntStream.rangeClosed(0, 6)
                .mapToObj(today.minusDays(6)::plusDays)
                .map(date -> {
                    double average = tokens.stream()
                            .filter(token -> token.getCalledAt() != null)
                            .filter(token -> LocalDate.ofInstant(token.getBookingTime(), zone).equals(date))
                            .mapToLong(token -> Duration.between(token.getBookingTime(), token.getCalledAt()).toMinutes())
                            .average()
                            .orElse(0);
                    return new ChartPoint(date.toString(), average);
                })
                .toList();
    }

    private List<ChartPoint> serviceEfficiency(long completed, long cancelled, long skipped) {
        return List.of(
                new ChartPoint("Completed", (double) completed),
                new ChartPoint("Cancelled", (double) cancelled),
                new ChartPoint("Skipped", (double) skipped)
        );
    }
}
