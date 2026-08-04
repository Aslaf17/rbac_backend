package com.rbac.service.notification;

import com.rbac.dto.notification.NotificationResponse;
import com.rbac.model.login.User;
import com.rbac.model.notification.Notification;
import com.rbac.model.notification.RecipientType;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationSocketService {

    private static final String EVENT_TYPE = "NEW_NOTIFICATION";

    private final SimpMessagingTemplate messagingTemplate;


    public void push(Notification notification, NotificationResponse response, List<User> recipients) {
        Map<String, Object> payload = Map.of(
                "type", EVENT_TYPE,
                "timestamp", Instant.now().toString(),
                "data", response
        );

        RecipientType type = notification.getRecipientType();

        switch (type) {
            case ALL -> messagingTemplate.convertAndSend("/topic/notifications", payload);

            case BATCH -> messagingTemplate.convertAndSend(
                    "/topic/notifications/batch/" + notification.getBatchId(), payload);

            case LIVE_CLASSROOM -> messagingTemplate.convertAndSend(
                    "/topic/notifications/classroom/" + notification.getSessionId(), payload);

            case USER -> {
                for (User recipient : recipients) {
                    messagingTemplate.convertAndSendToUser(recipient.getUsername(), "/queue/general-notifications", payload);
                }
            }
        }
    }
}
