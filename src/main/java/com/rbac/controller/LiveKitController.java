package com.rbac.controller;

import com.rbac.dto.session.LiveKitTokenResponse;
import com.rbac.exception.session.InvalidRequestException;
import com.rbac.exception.session.ResourceNotFoundException;
import com.rbac.model.session.Session;
import com.rbac.model.session.SessionStatus;
import com.rbac.repository.SessionRepository;
import com.rbac.security.chat.AuthenticatedUser;
import com.rbac.security.chat.CurrentUserProvider;
import io.livekit.server.AccessToken;
import io.livekit.server.RoomJoin;
import io.livekit.server.RoomName;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/session")
@RequiredArgsConstructor
public class LiveKitController {

    private final SessionRepository sessionRepository;
    private final CurrentUserProvider currentUserProvider;

    @Value("${livekit.api.key}")
    private String apiKey;

    @Value("${livekit.api.secret}")
    private String apiSecret;

    @Value("${livekit.url}")
    private String livekitUrl;

    @GetMapping("/{sessionId}/livekit-token")
    public ResponseEntity<LiveKitTokenResponse> getToken(@PathVariable String sessionId) {
        // This endpoint sits behind the standard JWT filter chain (JwtAuthFilter + SecurityConfig
        // already requires auth on GET /api/session/**), so by the time we get here the caller
        // is a verified, authenticated user - we just resolve who they are.
        AuthenticatedUser user = currentUserProvider.getCurrentUser();

        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));

        if (session.getStatus() != SessionStatus.LIVE) {
            throw new InvalidRequestException("Session " + sessionId + " is not live");
        }

        AccessToken token = new AccessToken(apiKey, apiSecret);
        token.setName(user.getUserName());
        token.setIdentity(user.getUserId());
        token.addGrants(new RoomJoin(true), new RoomName(sessionId));

        LiveKitTokenResponse response = LiveKitTokenResponse.builder()
                .token(token.toJwt())
                .url(livekitUrl)
                .roomName(sessionId)
                .identity(user.getUserId())
                .build();

        return ResponseEntity.ok(response);
    }
}
