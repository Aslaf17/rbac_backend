package com.rbac.controller.classroom;

import com.rbac.dto.classroom.ParticipantResponse;
import com.rbac.security.chat.CurrentUserProvider;
import com.rbac.service.classroom.MediaControlService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/session/{sessionId}/media")
@RequiredArgsConstructor
public class MediaControlController {

    private final MediaControlService mediaControlService;
    private final CurrentUserProvider currentUserProvider;

    @PutMapping("/mic")
    public ResponseEntity<ParticipantResponse> toggleOwnMic(@PathVariable String sessionId, @RequestParam boolean unmute) {
        return ResponseEntity.ok(mediaControlService.toggleOwnMic(sessionId, currentUserProvider.getCurrentUser(), unmute));
    }

    @PutMapping("/camera")
    public ResponseEntity<ParticipantResponse> toggleOwnCamera(@PathVariable String sessionId, @RequestParam boolean on) {
        return ResponseEntity.ok(mediaControlService.toggleOwnCamera(sessionId, currentUserProvider.getCurrentUser(), on));
    }

    @PutMapping("/{participantId}/mute")
    public ResponseEntity<ParticipantResponse> trainerMute(@PathVariable String sessionId, @PathVariable String participantId) {
        return ResponseEntity.ok(mediaControlService.trainerMute(sessionId, participantId, currentUserProvider.getCurrentUser()));
    }

    @PutMapping("/mute-all")
    public ResponseEntity<List<ParticipantResponse>> muteAll(@PathVariable String sessionId) {
        return ResponseEntity.ok(mediaControlService.muteAll(sessionId, currentUserProvider.getCurrentUser()));
    }

    @PutMapping("/{participantId}/self-unmute")
    public ResponseEntity<ParticipantResponse> setSelfUnmuteAllowed(
            @PathVariable String sessionId, @PathVariable String participantId, @RequestParam boolean allowed) {
        return ResponseEntity.ok(mediaControlService.setSelfUnmuteAllowed(sessionId, participantId, allowed, currentUserProvider.getCurrentUser()));
    }

    @PostMapping("/{participantId}/request-camera")
    public ResponseEntity<ParticipantResponse> requestCameraOn(@PathVariable String sessionId, @PathVariable String participantId) {
        return ResponseEntity.ok(mediaControlService.requestCameraOn(sessionId, participantId, currentUserProvider.getCurrentUser()));
    }
}