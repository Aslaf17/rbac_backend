package com.rbac.controller.classroom;

import com.rbac.dto.classroom.ActivityLogResponse;
import com.rbac.service.classroom.ActivityLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/session/{sessionId}/logs")
@RequiredArgsConstructor
public class ActivityLogController {

    private final ActivityLogService activityLogService;

    @GetMapping
    public ResponseEntity<List<ActivityLogResponse>> getLogs(@PathVariable String sessionId) {
        return ResponseEntity.ok(activityLogService.getLogs(sessionId));
    }
}