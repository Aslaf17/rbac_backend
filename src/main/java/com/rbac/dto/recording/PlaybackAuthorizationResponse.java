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
public class PlaybackAuthorizationResponse {
    private String recordingId;
    private String playbackToken;
    private Instant expiresAt;
    private boolean downloadAllowed;
}
