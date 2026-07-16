package com.rbac.controller;

import com.rbac.dto.whiteboard.SaveWhiteboardRequest;
import com.rbac.dto.whiteboard.UpdateWhiteboardRequest;
import com.rbac.dto.whiteboard.WhiteboardResponse;
import com.rbac.security.chat.AuthenticatedUser;
import com.rbac.security.chat.CurrentUserProvider;
import com.rbac.service.whiteboard.WhiteboardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/whiteboard")
@RequiredArgsConstructor
public class WhiteboardController {

    private final WhiteboardService whiteboardService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping("/save")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WhiteboardResponse> saveDrawing(@Valid @RequestBody SaveWhiteboardRequest request) {
        AuthenticatedUser user = currentUserProvider.getCurrentUser();
        WhiteboardResponse response = whiteboardService.saveDrawing(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{sessionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<WhiteboardResponse>> getSessionWhiteboard(@PathVariable String sessionId) {
        AuthenticatedUser user = currentUserProvider.getCurrentUser();
        List<WhiteboardResponse> response = whiteboardService.getSessionWhiteboard(sessionId, user);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{sessionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WhiteboardResponse> updateDrawing(
            @PathVariable String sessionId,
            @Valid @RequestBody UpdateWhiteboardRequest request) {
        AuthenticatedUser user = currentUserProvider.getCurrentUser();
        WhiteboardResponse response = whiteboardService.updateDrawing(sessionId, request, user);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{sessionId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<Void> clearWhiteboard(@PathVariable String sessionId) {
        AuthenticatedUser user = currentUserProvider.getCurrentUser();
        whiteboardService.clearWhiteboard(sessionId, user);
        return ResponseEntity.noContent().build();
    }
}
