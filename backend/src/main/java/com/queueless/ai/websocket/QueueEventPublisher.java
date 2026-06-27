package com.queueless.ai.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QueueEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publishCounterUpdate(Long counterId, Object payload) {
        messagingTemplate.convertAndSend("/topic/counters/" + counterId, payload);
        messagingTemplate.convertAndSend("/topic/queues", payload);
    }

    public void publishUserUpdate(Long userId, Object payload) {
        messagingTemplate.convertAndSend("/topic/users/" + userId, payload);
    }
}
