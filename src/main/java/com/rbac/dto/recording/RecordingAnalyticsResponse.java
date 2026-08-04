package com.rbac.dto.recording;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordingAnalyticsResponse {
    private String recordingId;
    private long totalViews;
    private long uniqueViewers;
    private long totalWatchDurationSeconds;
    private Instant lastViewedAt;
    private long downloadCount;
}
