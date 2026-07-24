package com.rbac.model.classroom;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "classroom_activity_logs")
@CompoundIndex(name = "session_time_idx", def = "{'sessionId': 1, 'timestamp': 1}")
public class ActivityLog {

    @Id
    private String id;

    private String sessionId;
    private String userId;
    private String userName;
    private ActivityType eventType;
    private String description;

    @Builder.Default
    private Instant timestamp = Instant.now();
}