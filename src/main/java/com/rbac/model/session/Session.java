package com.rbac.model.session;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "sessions")
public class Session {

    @Id
    private String id;

    private String title;

    private String trainerId;

    private String trainerName;

    private String batchId;

    private SessionStatus status;

    private Instant startedAt;

    private Instant endedAt;

    private Instant createdAt = Instant.now();

    private boolean locked = false;


    private boolean trainerConnected = true;

    private Instant trainerDisconnectedAt;

    private int reconnectTimeoutSeconds = 300;
}