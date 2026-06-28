package com.queueless.ai.controller;

import com.queueless.ai.dto.ApiResponse;
import com.queueless.ai.dto.CounterDtos.CounterRequest;
import com.queueless.ai.dto.CounterDtos.CounterResponse;
import com.queueless.ai.service.CounterService;
import com.queueless.ai.service.ScheduleService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/counters")
@RequiredArgsConstructor
public class CounterController {

    private final CounterService counterService;
    private final ScheduleService scheduleService;

    @GetMapping("/organization/{organizationId}")
    public ApiResponse<List<CounterResponse>> byOrganization(@PathVariable Long organizationId) {
        return ApiResponse.success("Counters retrieved", counterService.findByOrganization(organizationId));
    }

    @GetMapping("/{id}")
    public ApiResponse<CounterResponse> get(@PathVariable Long id) {
        return ApiResponse.success("Counter retrieved", counterService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN')")
    public ApiResponse<CounterResponse> create(@Valid @RequestBody CounterRequest request) {
        return ApiResponse.success("Counter created", counterService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN')")
    public ApiResponse<CounterResponse> update(@PathVariable Long id, @Valid @RequestBody CounterRequest request) {
        return ApiResponse.success("Counter updated", counterService.update(id, request));
    }

    @PatchMapping("/{id}/disable")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN')")
    public ApiResponse<CounterResponse> disable(@PathVariable Long id) {
        return ApiResponse.success("Counter disabled", counterService.disable(id));
    }

    // --- Feature 1: Future Date Booking endpoints ---

    @PostMapping("/{id}/schedule")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN')")
    public ApiResponse<Void> setSchedule(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload
    ) {
        Object datesObj = payload.get("dates");
        if (datesObj == null) {
            throw new IllegalArgumentException("dates list is required");
        }
        List<String> dateStrings = (List<String>) datesObj;
        List<LocalDate> dates = dateStrings.stream()
                .filter(d -> d != null && !d.trim().isEmpty())
                .map(LocalDate::parse)
                .collect(java.util.stream.Collectors.toList());
        
        Integer dailyCapacity = 100;
        if (payload.containsKey("dailyCapacity") && payload.get("dailyCapacity") != null) {
            Object dcObj = payload.get("dailyCapacity");
            if (dcObj instanceof Number) {
                dailyCapacity = ((Number) dcObj).intValue();
            } else if (dcObj instanceof String) {
                dailyCapacity = Integer.parseInt((String) dcObj);
            }
        }
        
        scheduleService.setSchedule(id, dates, dailyCapacity);
        return ApiResponse.success("Schedule updated", null);
    }

    @GetMapping("/{id}/available-dates")
    public ApiResponse<List<LocalDate>> availableDates(@PathVariable Long id) {
        return ApiResponse.success("Available dates retrieved", scheduleService.availableDates(id));
    }
}
