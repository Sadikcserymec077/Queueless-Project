package com.queueless.ai.dto;

import java.time.Instant;

public class NotificationDtos {
    public record NotificationResponse(
            Long id,
            String title,
            String message,
            Instant sentAt,
            Instant readAt
    ) {
    }
}
