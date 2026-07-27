package com.rbac.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSummaryResponse {
    private long total;
    private long unread;
    private long highPriority;
    private List<NotificationResponse> recent;
}