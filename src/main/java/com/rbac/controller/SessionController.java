package com.rbac.controller;

import com.rbac.dto.attendance.AttendanceResponse;
import com.rbac.dto.session.SessionResponse;
import com.rbac.dto.session.StartSessionRequest;
import com.rbac.security.chat.AuthenticatedUser;
import com.rbac.security.chat.CurrentUserProvider;
import com.rbac.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/session")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping("/start")
    public ResponseEntity<SessionResponse> startSession(@Valid @RequestBody StartSessionRequest request) {
        AuthenticatedUser trainer = currentUserProvider.getCurrentUser();

        if (!trainer.isTrainerOrAdmin()) {
            throw new AccessDeniedException("Only ADMIN or TEACHER can start a session");
        }

        SessionResponse response = sessionService.startSession(request, trainer);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{sessionId}/end")
    public ResponseEntity<SessionResponse> endSession(@PathVariable String sessionId) {
        AuthenticatedUser requester = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(sessionService.endSession(sessionId, requester));
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<SessionResponse> getSession(@PathVariable String sessionId) {
        return ResponseEntity.ok(sessionService.getSession(sessionId));
    }

    @PostMapping("/{sessionId}/join")
    public ResponseEntity<AttendanceResponse> joinSession(@PathVariable String sessionId) {
        AuthenticatedUser user = currentUserProvider.getCurrentUser();
        AttendanceResponse response = sessionService.joinSession(sessionId, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{sessionId}/lock")
    public ResponseEntity<SessionResponse> lockSession(@PathVariable String sessionId) {
        return ResponseEntity.ok(sessionService.lockSession(sessionId, currentUserProvider.getCurrentUser()));
    }

    @PutMapping("/{sessionId}/unlock")
    public ResponseEntity<SessionResponse> unlockSession(@PathVariable String sessionId) {
        return ResponseEntity.ok(sessionService.unlockSession(sessionId, currentUserProvider.getCurrentUser()));
    }

    @GetMapping("/all")
    public ResponseEntity<List<SessionResponse>> getAllSessions() {
        return ResponseEntity.ok(sessionService.getAllSessions());
    }

    @GetMapping("/live")
    public ResponseEntity<List<SessionResponse>> getLiveSessions() {
        return ResponseEntity.ok(sessionService.getLiveSessions());
    }

    @GetMapping("/{sessionId}/statistics")
    public ResponseEntity<SessionResponse.SessionStatisticsResponse> getStatistics(@PathVariable String sessionId) {
        return ResponseEntity.ok(sessionService.getStatistics(sessionId));
    }
}