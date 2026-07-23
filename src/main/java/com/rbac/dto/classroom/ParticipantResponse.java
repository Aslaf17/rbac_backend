package com.rbac.dto.classroom;

import com.rbac.model.classroom.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantResponse {
    private String id;
    private String sessionId;
    private String userId;
    private String userName;
    private String email;
    private ParticipantStatus status;
    private MicStatus micStatus;
    private CameraStatus cameraStatus;
    private HandStatus handStatus;
    private Instant handRaisedAt;
    private boolean canSpeak;
    private boolean canChat;
    private boolean canScreenShare;
    private boolean canUnmuteSelf;
    private boolean canRejoin;
    private Instant joinedAt;
    private Instant leftAt;

    public static ParticipantResponse fromEntity(Participant p) {
        return ParticipantResponse.builder()
                .id(p.getId())
                .sessionId(p.getSessionId())
                .userId(p.getUserId())
                .userName(p.getUserName())
                .email(p.getEmail())
                .status(p.getStatus())
                .micStatus(p.getMicStatus())
                .cameraStatus(p.getCameraStatus())
                .handStatus(p.getHandStatus())
                .handRaisedAt(p.getHandRaisedAt())
                .canSpeak(p.isCanSpeak())
                .canChat(p.isCanChat())
                .canScreenShare(p.isCanScreenShare())
                .canUnmuteSelf(p.isCanUnmuteSelf())
                .canRejoin(p.isCanRejoin())
                .joinedAt(p.getJoinedAt())
                .leftAt(p.getLeftAt())
                .build();
    }
}