package com.rbac.controller.classroom;

import com.rbac.dto.classroom.ParticipantResponse;
import com.rbac.dto.classroom.UpdatePermissionsRequest;
import com.rbac.security.chat.CurrentUserProvider;
import com.rbac.service.classroom.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/session/{sessionId}/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;
    private final CurrentUserProvider currentUserProvider;

    @PutMapping("/{participantId}")
    public ResponseEntity<ParticipantResponse> updatePermissions(
            @PathVariable String sessionId,
            @PathVariable String participantId,
            @RequestBody UpdatePermissionsRequest request) {
        return ResponseEntity.ok(permissionService.updatePermissions(
                sessionId, participantId, request, currentUserProvider.getCurrentUser()));
    }
}