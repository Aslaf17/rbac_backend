package com.rbac.model.attendance;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "attendance")
@CompoundIndex(name = "session_user_unique", def = "{'sessionId': 1, 'userId': 1}", unique = true)
public class Attendance {

    @Id
    private String id;

    private String userId;

    private String sessionId;

    private Instant joinTime;

    private Instant leaveTime;

    private Long durationSeconds;

    private AttendanceStatus status;

    private Instant createdAt = Instant.now();

    private Instant updatedAt = Instant.now();
}
