package com.queueless.ai.service;

import com.queueless.ai.entity.Counter;
import com.queueless.ai.exception.BadRequestException;
import com.queueless.ai.repository.CounterRepository;
import com.queueless.ai.repository.TokenRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.queueless.ai.security.SecurityUtils;
import com.queueless.ai.entity.Role;
import com.queueless.ai.entity.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final CounterRepository counterRepository;
    private final TokenRepository tokenRepository;

    /**
     * Admin sets available future dates and the daily capacity for a counter.
     */
    @Transactional
    public void setSchedule(Long counterId, List<LocalDate> dates, Integer dailyCapacity) {
        Counter counter = counterRepository.findById(counterId)
                .orElseThrow(() -> new BadRequestException("Counter not found: " + counterId));

        User currentUser = SecurityUtils.currentUser().getUser();
        if (currentUser.getRole() == Role.ORG_ADMIN) {
            if (currentUser.getOrganization() == null || !currentUser.getOrganization().getId().equals(counter.getOrganization().getId())) {
                throw new AccessDeniedException("Not authorized to modify this organization's schedule");
            }
        }

        if (dates == null || dates.isEmpty()) {
            throw new BadRequestException("At least one available date is required");
        }

        LocalDate today = LocalDate.now();
        boolean hasPastDate = dates.stream().anyMatch(d -> d.isBefore(today));
        if (hasPastDate) {
            throw new BadRequestException("All scheduled dates must be today or in the future");
        }

        counter.setAvailableDates(dates);
        if (dailyCapacity != null && dailyCapacity > 0) {
            counter.setDailyCapacity(dailyCapacity);
        }
        counterRepository.save(counter);
        log.info("ScheduleService: Set {} available dates for counter {}", dates.size(), counterId);
    }

    /**
     * Returns dates that still have capacity available.
     * A date is "open" if the booked count < dailyCapacity.
     */
    @Transactional(readOnly = true)
    public List<LocalDate> availableDates(Long counterId) {
        Counter counter = counterRepository.findById(counterId)
                .orElseThrow(() -> new BadRequestException("Counter not found: " + counterId));

        LocalDate today = LocalDate.now();
        return counter.getAvailableDates().stream()
                .filter(date -> !date.isBefore(today))
                .filter(date -> tokenRepository.countByCounterIdAndScheduledDate(counterId, date)
                        < counter.getDailyCapacity())
                .sorted()
                .toList();
    }
}
