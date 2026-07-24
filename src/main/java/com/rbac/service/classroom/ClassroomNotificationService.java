package com.rbac.service.classroom;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ClassroomNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcast(String sessionId, String eventType, Object payload) {
        messagingTemplate.convertAndSend(
                "/topic/session/" + sessionId + "/events",
                Map.of(
                        "type", eventType,
                        "timestamp", Instant.now().toString(),
                        "data", payload
                )
        );
    }

    public void notifyUser(String userId, String eventType, Object payload) {
        messagingTemplate.convertAndSendToUser(
                userId,
                "/queue/notifications",
                Map.of(
                        "type", eventType,
                        "timestamp", Instant.now().toString(),
                        "data", payload
                )
        );
    }
}