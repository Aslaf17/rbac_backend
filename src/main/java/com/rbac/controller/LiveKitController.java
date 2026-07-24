package com.rbac.controller;

import io.livekit.server.AccessToken;
import io.livekit.server.RoomJoin;
import io.livekit.server.RoomName;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/session")
public class LiveKitController {

    @Value("${livekit.api.key}")
    private String apiKey;

    @Value("${livekit.api.secret}")
    private String apiSecret;

    @GetMapping("/{sessionId}/livekit-token")
    public ResponseEntity<Map<String, String>> getToken(
            @PathVariable String sessionId,
            Authentication authentication) {

        String username = authentication.getName();

        AccessToken token = new AccessToken(apiKey, apiSecret);
        token.setName(username);
        token.setIdentity(username);
        token.addGrants(new RoomJoin(true), new RoomName(sessionId));

        return ResponseEntity.ok(Map.of("token", token.toJwt()));
    }
}