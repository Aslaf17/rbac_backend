package com.rbac.service.classroom;

import com.rbac.dto.classroom.ActivityLogResponse;
import com.rbac.model.classroom.ActivityLog;
import com.rbac.model.classroom.ActivityType;
import com.rbac.repository.ActivityLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    public void record(String sessionId, String userId, String userName, ActivityType type, String description) {
        ActivityLog log = ActivityLog.builder()
                .sessionId(sessionId)
                .userId(userId)
                .userName(userName)
                .eventType(type)
                .description(description)
                .build();
        activityLogRepository.save(log);
    }

    public List<ActivityLogResponse> getLogs(String sessionId) {
        return activityLogRepository.findBySessionIdOrderByTimestampDesc(sessionId)
                .stream()
                .map(ActivityLogResponse::fromEntity)
                .toList();
    }
}