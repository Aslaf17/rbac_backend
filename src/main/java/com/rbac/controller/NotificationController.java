package com.rbac.controller;

import com.rbac.dto.notification.PageResponse;
import com.rbac.dto.notification.CreateNotificationRequest;
import com.rbac.dto.notification.NotificationResponse;
import com.rbac.dto.notification.NotificationSummaryResponse;
import com.rbac.dto.notification.UpdateNotificationRequest;
import com.rbac.security.chat.AuthenticatedUser;
import com.rbac.security.chat.CurrentUserProvider;
import com.rbac.service.notification.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<NotificationResponse> create(@Valid @RequestBody CreateNotificationRequest request) {
        AuthenticatedUser sender = currentUserProvider.getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED).body(notificationService.create(request, sender));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<PageResponse<NotificationResponse>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String recipientType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        AuthenticatedUser requester = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(notificationService.getAll(search, priority, recipientType, status, from, to, page, size, requester));
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<NotificationResponse>> getMy(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Boolean read,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        AuthenticatedUser requester = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(notificationService.getMyNotifications(search, priority, read, page, size, requester));
    }

    @GetMapping("/summary")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NotificationSummaryResponse> getSummary() {
        AuthenticatedUser requester = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(notificationService.getSummary(requester));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NotificationResponse> getById(@PathVariable String id) {
        AuthenticatedUser requester = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(notificationService.getById(id, requester));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<NotificationResponse> update(@PathVariable String id, @Valid @RequestBody UpdateNotificationRequest request) {
        AuthenticatedUser requester = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(notificationService.update(id, request, requester));
    }

    @PutMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markAsRead(@PathVariable String id) {
        AuthenticatedUser requester = currentUserProvider.getCurrentUser();
        notificationService.markAsRead(id, requester);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        AuthenticatedUser requester = currentUserProvider.getCurrentUser();
        notificationService.softDelete(id, requester);
        return ResponseEntity.noContent().build();
    }
}