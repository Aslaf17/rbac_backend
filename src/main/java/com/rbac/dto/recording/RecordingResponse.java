package com.rbac.dto.recording;

import com.rbac.model.recording.Recording;
import com.rbac.model.recording.RecordingStatus;
import com.rbac.model.recording.Visibility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordingResponse {

    private String id;
    private String sessionId;
    private String batchId;
    private String trainerId;
    private String trainerName;

    private String title;
    private String description;

    private String videoUrl;
    private String thumbnailUrl;

    private long durationSeconds;
    private long fileSizeBytes;

    private RecordingStatus status;
    private String failureReason;

    private Instant recordingStartTime;
    private Instant recordingEndTime;
    private LocalDate recordingDate;

    private long playbackCount;
    private boolean downloadEnabled;
    private Visibility visibility;

    private Instant createdAt;
    private Instant updatedAt;

    public static RecordingResponse fromEntity(Recording r) {
        return RecordingResponse.builder()
                .id(r.getId())
                .sessionId(r.getSessionId())
                .batchId(r.getBatchId())
                .trainerId(r.getTrainerId())
                .trainerName(r.getTrainerName())
                .title(r.getTitle())
                .description(r.getDescription())
                .videoUrl(r.getVideoUrl())
                .thumbnailUrl(r.getThumbnailUrl())
                .durationSeconds(r.getDurationSeconds())
                .fileSizeBytes(r.getFileSizeBytes())
                .status(r.getStatus())
                .failureReason(r.getFailureReason())
                .recordingStartTime(r.getRecordingStartTime())
                .recordingEndTime(r.getRecordingEndTime())
                .recordingDate(r.getRecordingDate())
                .playbackCount(r.getPlaybackCount())
                .downloadEnabled(r.isDownloadEnabled())
                .visibility(r.getVisibility())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
