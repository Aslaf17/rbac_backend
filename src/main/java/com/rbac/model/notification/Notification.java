package com.rbac.model.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "notifications")
public class Notification {

    @Id
    private String id;

    private String title;

    private String message;

    private String senderId;

    private String senderName;

    private String senderRole;

    @Indexed
    private RecipientType recipientType;

    @Indexed
    private String recipientId; // set when recipientType == USER

    @Indexed
    private String batchId; // set when recipientType == BATCH

    @Indexed
    private String sessionId; // set when recipientType == LIVE_CLASSROOM

    private NotificationPriority priority;

    @Builder.Default
    private NotificationStatus status = NotificationStatus.ACTIVE;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}