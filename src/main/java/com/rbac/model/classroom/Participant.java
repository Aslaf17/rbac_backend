package com.rbac.model.classroom;

import com.rbac.model.login.Role;
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
@Document(collection = "classroom_participants")
@CompoundIndex(name = "session_user_idx", def = "{'sessionId': 1, 'userId': 1}", unique = true)
public class Participant {

    @Id
    private String id;

    private String sessionId;
    private String userId;
    private String userName;
    private String email;
    private Role role;

    @Builder.Default
    private ParticipantStatus status = ParticipantStatus.WAITING;

    @Builder.Default
    private MicStatus micStatus = MicStatus.MUTED;

    @Builder.Default
    private CameraStatus cameraStatus = CameraStatus.OFF;

    @Builder.Default
    private HandStatus handStatus = HandStatus.NONE;

    private Instant handRaisedAt;

    @Builder.Default
    private boolean canSpeak = false;

    @Builder.Default
    private boolean canChat = true;

    @Builder.Default
    private boolean canScreenShare = false;

    @Builder.Default
    private boolean canUnmuteSelf = false;

    @Builder.Default
    private boolean canRejoin = true;

    private Instant joinedAt;
    private Instant leftAt;

    @Builder.Default
    private Instant createdAt = Instant.now();

    @Builder.Default
    private Instant updatedAt = Instant.now();
}