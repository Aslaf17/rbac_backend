package com.rbac.model.recording;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "recording_views")
@CompoundIndexes({
        @CompoundIndex(name = "recording_viewer_unique", def = "{'recordingId': 1, 'userId': 1}", unique = true)
})
public class RecordingView {

    @Id
    private String id;

    private String recordingId;
    private String userId;
    private String userName;

    @Builder.Default
    private long viewCount = 0;

    @Builder.Default
    private long totalWatchDurationSeconds = 0;

    @Builder.Default
    private long downloadCount = 0;

    private Instant firstViewedAt;
    private Instant lastViewedAt;
}
