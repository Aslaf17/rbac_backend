package com.rbac.dto.classroom;

import com.rbac.model.classroom.ActivityLog;
import com.rbac.model.classroom.ActivityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLogResponse {
    private String id;
    private String sessionId;
    private String userId;
    private String userName;
    private ActivityType eventType;
    private String description;
    private Instant timestamp;

    public static ActivityLogResponse fromEntity(ActivityLog log) {
        return ActivityLogResponse.builder()
                .id(log.getId())
                .sessionId(log.getSessionId())
                .userId(log.getUserId())
                .userName(log.getUserName())
                .eventType(log.getEventType())
                .description(log.getDescription())
                .timestamp(log.getTimestamp())
                .build();
    }
}