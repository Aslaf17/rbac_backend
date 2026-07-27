package com.rbac.dto.notification;

import com.rbac.model.notification.Notification;
import com.rbac.model.notification.NotificationPriority;
import com.rbac.model.notification.NotificationStatus;
import com.rbac.model.notification.RecipientType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private String id;
    private String title;
    private String message;
    private String senderId;
    private String senderName;
    private String senderRole;
    private RecipientType recipientType;
    private String recipientId;
    private String batchId;
    private NotificationPriority priority;
    private NotificationStatus status;
    private boolean read;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static NotificationResponse fromEntity(Notification entity, boolean read) {
        return NotificationResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .message(entity.getMessage())
                .senderId(entity.getSenderId())
                .senderName(entity.getSenderName())
                .senderRole(entity.getSenderRole())
                .recipientType(entity.getRecipientType())
                .recipientId(entity.getRecipientId())
                .batchId(entity.getBatchId())
                .priority(entity.getPriority())
                .status(entity.getStatus())
                .read(read)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}