package com.rbac.dto.admin;

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
public class AdminLiveSessionResponse {

    private String sessionId;
    private String title;

    private String trainerId;
    private String trainerName;

    private String batchId;
    private String batchName;

    private SessionStatus status;

    private Instant startedAt;
    private Instant endedAt;

    private long durationSeconds;

    private long totalParticipants;
    private long activeParticipants;

    private boolean locked;
    private boolean trainerConnected;
}