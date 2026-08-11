package com.rbac.dto.session;

import com.rbac.model.session.Session;
import com.rbac.model.session.SessionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionResponse {

    private String sessionId;
    private String title;
    private String trainerId;
    private String trainerName;
    private String batchId;
    private SessionStatus status;
    private Instant startedAt;
    private Instant endedAt;
    private String batchName;
    private long activeParticipantCount;

    private boolean trainerConnected;
    private Instant trainerDisconnectedAt;
    private int reconnectTimeoutSeconds;

    private Instant scheduledAt;
    private int numberOfDays;

    public static SessionResponse fromEntity(Session session) {
        return fromEntity(session, null, 0L);
    }

    public static SessionResponse fromEntity(
            Session session,
            String batchName,
            long activeParticipantCount) {

        return SessionResponse.builder()
                .sessionId(session.getId())
                .title(session.getTitle())
                .trainerId(session.getTrainerId())
                .trainerName(session.getTrainerName())
                .batchId(session.getBatchId())
                .batchName(batchName)
                .status(session.getStatus())
                .startedAt(session.getStartedAt())
                .endedAt(session.getEndedAt())
                .trainerConnected(session.isTrainerConnected())
                .trainerDisconnectedAt(session.getTrainerDisconnectedAt())
                .reconnectTimeoutSeconds(session.getReconnectTimeoutSeconds())
                .activeParticipantCount(activeParticipantCount)
                .scheduledAt(session.getScheduledAt())
                .numberOfDays(session.getNumberOfDays())
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionStatisticsResponse {

        private String sessionId;
        private long durationSeconds;
        private long totalParticipants;
        private long activeParticipants;
        private long presentCount;
        private long absentCount;
        private long lateCount;
    }
}