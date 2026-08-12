package com.rbac.dto.batch;

import com.rbac.model.batch.BatchStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchAnalyticsResponse {

    private String batchId;
    private String name;
    private BatchStatus status;

    private long studentCount;
    private long trainerCount;

    private long totalSessions;
    private long liveSessions;
    private long completedSessions;

    private Instant createdAt;
    private Instant updatedAt;
}