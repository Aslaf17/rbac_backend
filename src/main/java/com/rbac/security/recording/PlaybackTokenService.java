package com.rbac.security.recording;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Component
public class PlaybackTokenService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${recording.playback.token-expiration-ms:900000}")
    private long expirationMs;

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generate(String recordingId, String userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(userId)
                .claim("recordingId", recordingId)
                .claim("purpose", "recording_playback")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey())
                .compact();
    }

    public Instant expiresAt() {
        return Instant.now().plusMillis(expirationMs);
    }

    public void validate(String token, String recordingId, String userId) {
        Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(signingKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException ex) {
            throw new JwtException("Playback token has expired");
        }

        String tokenRecordingId = claims.get("recordingId", String.class);
        String tokenUserId = claims.getSubject();
        String purpose = claims.get("purpose", String.class);

        if (!"recording_playback".equals(purpose)
                || tokenRecordingId == null || !tokenRecordingId.equals(recordingId)
                || tokenUserId == null || !tokenUserId.equals(userId)) {
            throw new IllegalArgumentException("Playback token is not valid for this recording/user");
        }
    }
}
