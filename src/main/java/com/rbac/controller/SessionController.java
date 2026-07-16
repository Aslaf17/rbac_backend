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

@RestController
@RequestMapping("/api/session")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping("/start")
    public ResponseEntity<SessionResponse> startSession(@Valid @RequestBody StartSessionRequest request) {
        AuthenticatedUser trainer = currentUserProvider.getCurrentUser();

        // Only ADMIN and TEACHER may create a session.
        // AuthenticatedUser already exposes this check (roles contains
        // ROLE_TEACHER or ROLE_ADMIN), so we just reuse it.
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
}