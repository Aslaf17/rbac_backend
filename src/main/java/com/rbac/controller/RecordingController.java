package com.rbac.controller;

import com.rbac.dto.recording.PageResponse;
import com.rbac.dto.recording.PlaybackAuthorizationResponse;
import com.rbac.dto.recording.RecordingAnalyticsResponse;
import com.rbac.dto.recording.RecordingResponse;
import com.rbac.dto.recording.StreamResponse;
import com.rbac.dto.recording.UpdateRecordingRequest;
import com.rbac.dto.recording.UpdateRecordingStatusRequest;
import com.rbac.dto.recording.UploadRecordingRequest;
import com.rbac.dto.recording.WatchProgressRequest;
import com.rbac.security.chat.AuthenticatedUser;
import com.rbac.security.chat.CurrentUserProvider;
import com.rbac.service.recording.RecordingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recordings")
@RequiredArgsConstructor
public class RecordingController {

    private final RecordingService recordingService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<RecordingResponse> upload(@Valid @RequestBody UploadRecordingRequest request) {
        AuthenticatedUser requester = currentUserProvider.getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED).body(recordingService.upload(request, requester));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<RecordingResponse>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String batchId,
            @RequestParam(required = false) String trainerId,
            @RequestParam(required = false) String sessionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        AuthenticatedUser requester = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(recordingService.getAll(search, batchId, trainerId, sessionId, page, size, requester));
    }

    @GetMapping("/analytics/most-viewed")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<List<RecordingResponse>> getMostViewed(
            @RequestParam(defaultValue = "10") int limit) {
        AuthenticatedUser requester = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(recordingService.getMostViewed(limit, requester));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RecordingResponse> getById(@PathVariable String id) {
        AuthenticatedUser requester = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(recordingService.getById(id, requester));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<RecordingResponse> update(@PathVariable String id,
                                                    @Valid @RequestBody UpdateRecordingRequest request) {
        AuthenticatedUser requester = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(recordingService.update(id, request, requester));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<RecordingResponse> updateStatus(@PathVariable String id,
                                                          @Valid @RequestBody UpdateRecordingStatusRequest request) {
        AuthenticatedUser requester = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(recordingService.updateStatus(id, request, requester));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<Void> softDelete(@PathVariable String id) {
        AuthenticatedUser requester = currentUserProvider.getCurrentUser();
        recordingService.softDelete(id, requester);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/permanent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> permanentDelete(@PathVariable String id) {
        AuthenticatedUser requester = currentUserProvider.getCurrentUser();
        recordingService.permanentDelete(id, requester);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/playback-token")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PlaybackAuthorizationResponse> authorizePlayback(@PathVariable String id) {
        AuthenticatedUser requester = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(recordingService.authorizePlayback(id, requester));
    }

    @GetMapping("/{id}/stream")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StreamResponse> stream(@PathVariable String id,
                                                 @RequestParam String playbackToken) {
        AuthenticatedUser requester = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(recordingService.stream(id, playbackToken, requester));
    }

    @PostMapping("/{id}/download")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StreamResponse> download(@PathVariable String id,
                                                   @RequestParam String playbackToken) {
        AuthenticatedUser requester = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(recordingService.download(id, playbackToken, requester));
    }

    @PostMapping("/{id}/watch-progress")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> trackWatchProgress(@PathVariable String id,
                                                   @Valid @RequestBody WatchProgressRequest request) {
        AuthenticatedUser requester = currentUserProvider.getCurrentUser();
        recordingService.trackWatchProgress(id, request, requester);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/analytics")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<RecordingAnalyticsResponse> getAnalytics(@PathVariable String id) {
        AuthenticatedUser requester = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(recordingService.getAnalytics(id, requester));
    }
}
