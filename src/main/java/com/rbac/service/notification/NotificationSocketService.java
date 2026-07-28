package com.rbac.service.notification;

import com.rbac.dto.notification.NotificationResponse;
import com.rbac.model.login.User;
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

    private final SimpMessagingTemplate messagingTemplate;

    public void push(RecipientType type, NotificationResponse response, List<User> recipients) {
        Map<String, Object> payload = Map.of(
                "type", "NEW_NOTIFICATION",
                "timestamp", Instant.now().toString(),
                "data", response
        );

        if (type == RecipientType.ALL) {
            messagingTemplate.convertAndSend("/topic/notifications", payload);
            return;
        }

        for (User recipient : recipients) {
            messagingTemplate.convertAndSendToUser(recipient.getUsername(), "/queue/general-notifications", payload);
        }
    }
}