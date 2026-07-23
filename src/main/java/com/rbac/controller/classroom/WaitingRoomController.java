package com.rbac.controller.classroom;

import com.rbac.dto.classroom.ParticipantResponse;
import com.rbac.security.chat.CurrentUserProvider;
import com.rbac.service.classroom.WaitingRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/session/{sessionId}/waiting-room")
@RequiredArgsConstructor
public class WaitingRoomController {

    private final WaitingRoomService waitingRoomService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public ResponseEntity<List<ParticipantResponse>> getWaitingList(@PathVariable String sessionId) {
        return ResponseEntity.ok(waitingRoomService.getWaitingList(sessionId, currentUserProvider.getCurrentUser()));
    }

    @PutMapping("/{participantId}/approve")
    public ResponseEntity<ParticipantResponse> approve(@PathVariable String sessionId, @PathVariable String participantId) {
        return ResponseEntity.ok(waitingRoomService.approve(sessionId, participantId, currentUserProvider.getCurrentUser()));
    }

    @PutMapping("/{participantId}/reject")
    public ResponseEntity<ParticipantResponse> reject(@PathVariable String sessionId, @PathVariable String participantId) {
        return ResponseEntity.ok(waitingRoomService.reject(sessionId, participantId, currentUserProvider.getCurrentUser()));
    }
}