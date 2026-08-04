package com.rbac.dto.notification;

import com.rbac.model.notification.NotificationPriority;
import com.rbac.model.notification.RecipientType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateNotificationRequest {

    @NotBlank(message = "title is required")
    @Size(max = 150, message = "title cannot exceed 150 characters")
    private String title;

    @NotBlank(message = "message is required")
    @Size(max = 2000, message = "message cannot exceed 2000 characters")
    private String message;

    @NotNull(message = "recipientType is required")
    private RecipientType recipientType;

    private String recipientId;

    private String batchId;

    private String sessionId;

    @NotNull(message = "priority is required")
    private NotificationPriority priority;
}