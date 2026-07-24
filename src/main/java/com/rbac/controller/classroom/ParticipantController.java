package com.rbac.controller.classroom;

import com.rbac.dto.classroom.ParticipantResponse;
import com.rbac.security.chat.AuthenticatedUser;
import com.rbac.security.chat.CurrentUserProvider;
import com.rbac.service.classroom.ParticipantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/session/{sessionId}/participants")
@RequiredArgsConstructor
public class ParticipantController {

    private final ParticipantService participantService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping("/join")
    public ResponseEntity<?> join(@PathVariable String sessionId) {

        try {

            AuthenticatedUser user = currentUserProvider.getCurrentUser();

            return ResponseEntity.ok(
                    participantService.requestJoin(sessionId, user)
            );

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.internalServerError()
                    .body(e.getClass().getSimpleName() + " : " + e.getMessage());
        }
    }

    @PostMapping("/leave")
    public ResponseEntity<?> leave(@PathVariable String sessionId) {

        try {

            AuthenticatedUser user = currentUserProvider.getCurrentUser();

            return ResponseEntity.ok(
                    participantService.leave(sessionId, user)
            );

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.internalServerError()
                    .body(e.getClass().getSimpleName() + " : " + e.getMessage());
        }
    }

    @PostMapping("/rejoin")
    public ResponseEntity<ParticipantResponse> rejoin(
            @PathVariable String sessionId) {

        AuthenticatedUser user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(participantService.rejoin(sessionId, user));
    }

    @GetMapping
    public ResponseEntity<List<ParticipantResponse>> getParticipants(
            @PathVariable String sessionId,
            @RequestParam(required = false) String search) {

        AuthenticatedUser user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(
                participantService.getParticipants(sessionId, search, user));
    }

    @GetMapping("/count")
    public ResponseEntity<Long> count(
            @PathVariable String sessionId) {

        return ResponseEntity.ok(
                participantService.getLiveCount(sessionId));
    }

    @DeleteMapping("/{participantId}")
    public ResponseEntity<ParticipantResponse> remove(
            @PathVariable String sessionId,
            @PathVariable String participantId,
            @RequestParam(defaultValue = "false") boolean allowRejoin) {

        AuthenticatedUser user = currentUserProvider.getCurrentUser();

        return ResponseEntity.ok(
                participantService.removeParticipant(
                        sessionId,
                        participantId,
                        allowRejoin,
                        user));
    }
}