package com.rbac.model.recording;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "recordings")
public class Recording {

    @Id
    private String id;

    @Indexed
    private String sessionId;

    @Indexed
    private String batchId;

    @Indexed
    private String trainerId;
    private String trainerName;

    private String title;
    private String description;

    private String videoUrl;
    private String thumbnailUrl;

    private long durationSeconds;
    private long fileSizeBytes;

    @Indexed
    private RecordingStatus status;
    private String failureReason;

    private Instant recordingStartTime;
    private Instant recordingEndTime;
    private LocalDate recordingDate;

    @Builder.Default
    private long playbackCount = 0;

    private boolean downloadEnabled;

    private Visibility visibility;

    @Builder.Default
    private boolean deleted = false;
    private Instant deletedAt;

    @Builder.Default
    private Instant createdAt = Instant.now();
    private Instant updatedAt;
}
