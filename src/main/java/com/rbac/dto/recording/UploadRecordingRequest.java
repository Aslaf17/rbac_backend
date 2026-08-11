package com.rbac.dto.recording;

import com.rbac.model.recording.Visibility;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

@Data
public class UploadRecordingRequest {

    @NotBlank(message = "sessionId is required")
    private String sessionId;

    @NotBlank(message = "batchId is required")
    private String batchId;

    /** Optional when the requester is the trainer themselves; required/validated when an admin uploads on behalf of a trainer. */
    private String trainerId;

    @NotBlank(message = "title is required")
    private String title;

    private String description;

    @NotBlank(message = "videoUrl is required")
    private String videoUrl;

    private String thumbnailUrl;

    @Min(value = 0, message = "durationSeconds cannot be negative")
    private long durationSeconds;

    @Min(value = 0, message = "fileSizeBytes cannot be negative")
    private long fileSizeBytes;

    @NotNull(message = "recordingStartTime is required")
    private Instant recordingStartTime;

    private Instant recordingEndTime;

    private boolean downloadEnabled;

    private Visibility visibility;
}
