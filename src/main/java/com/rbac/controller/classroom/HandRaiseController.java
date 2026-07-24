package com.rbac.controller.classroom;

import com.rbac.dto.classroom.ParticipantResponse;
import com.rbac.security.chat.CurrentUserProvider;
import com.rbac.service.classroom.HandRaiseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/session/{sessionId}/hand")
@RequiredArgsConstructor
public class HandRaiseController {

    private final HandRaiseService handRaiseService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping("/raise")
    public ResponseEntity<ParticipantResponse> raiseHand(@PathVariable String sessionId) {
        return ResponseEntity.ok(handRaiseService.raiseHand(sessionId, currentUserProvider.getCurrentUser()));
    }

    @PostMapping("/lower")
    public ResponseEntity<ParticipantResponse> lowerHand(@PathVariable String sessionId) {
        return ResponseEntity.ok(handRaiseService.lowerHand(sessionId, currentUserProvider.getCurrentUser()));
    }

    @GetMapping
    public ResponseEntity<List<ParticipantResponse>> getRaisedHands(@PathVariable String sessionId) {
        return ResponseEntity.ok(handRaiseService.getRaisedHands(sessionId, currentUserProvider.getCurrentUser()));
    }

    @PutMapping("/{participantId}/approve")
    public ResponseEntity<ParticipantResponse> approve(@PathVariable String sessionId, @PathVariable String participantId) {
        return ResponseEntity.ok(handRaiseService.approve(sessionId, participantId, currentUserProvider.getCurrentUser()));
    }

    @PutMapping("/{participantId}/dismiss")
    public ResponseEntity<ParticipantResponse> dismiss(@PathVariable String sessionId, @PathVariable String participantId) {
        return ResponseEntity.ok(handRaiseService.dismiss(sessionId, participantId, currentUserProvider.getCurrentUser()));
    }
}