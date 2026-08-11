package com.rbac.controller;

import com.rbac.dto.admin.AdminLiveSessionResponse;
import com.rbac.dto.admin.AdminSessionWatchResponse;
import com.rbac.dto.admin.AttendanceSummaryResponse;
import com.rbac.dto.classroom.ParticipantResponse;
import com.rbac.dto.session.SessionResponse;
import com.rbac.security.chat.AuthenticatedUser;
import com.rbac.security.chat.CurrentUserProvider;
import com.rbac.service.admin.AdminLiveSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/admin/live-sessions")
@RequiredArgsConstructor
public class AdminLiveSessionController {

    private final AdminLiveSessionService adminLiveSessionService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/live")
    public ResponseEntity<List<AdminLiveSessionResponse>> getLiveSessions() {
        return ResponseEntity.ok(adminLiveSessionService.getLiveSessions());
    }

    @GetMapping
    public ResponseEntity<List<AdminLiveSessionResponse>> getAllSessions() {
        return ResponseEntity.ok(adminLiveSessionService.getAllSessions());
    }

    @PutMapping("/{sessionId}/end")
    public ResponseEntity<AdminLiveSessionResponse> endSession(@PathVariable String sessionId) {
        AuthenticatedUser admin = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(adminLiveSessionService.forceEndSession(sessionId, admin));
    }

    @GetMapping("/{sessionId}/statistics")
    public ResponseEntity<SessionResponse.SessionStatisticsResponse> getStatistics(@PathVariable String sessionId) {
        return ResponseEntity.ok(adminLiveSessionService.getStatistics(sessionId));
    }

    @GetMapping("/{sessionId}/attendance-summary")
    public ResponseEntity<AttendanceSummaryResponse> getAttendanceSummary(@PathVariable String sessionId) {
        return ResponseEntity.ok(adminLiveSessionService.getAttendanceSummary(sessionId));
    }

    @PostMapping("/{sessionId}/watch")
    public ResponseEntity<AdminSessionWatchResponse> watchSession(@PathVariable String sessionId) {
        AuthenticatedUser admin = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(adminLiveSessionService.watchSession(sessionId, admin));
    }

    @GetMapping("/{sessionId}/participants")
    public ResponseEntity<List<ParticipantResponse>> getParticipants(@PathVariable String sessionId) {
        return ResponseEntity.ok(adminLiveSessionService.getParticipants(sessionId));
    }
}