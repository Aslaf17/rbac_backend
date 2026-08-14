package com.rbac.controller;

import com.rbac.dto.assignment.*;
import com.rbac.model.assignment.AssignmentStatus;
import com.rbac.security.chat.AuthenticatedUser;
import com.rbac.security.chat.CurrentUserProvider;
import com.rbac.service.assignment.AssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assignments")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;
    private final CurrentUserProvider currentUserProvider;

    // ---- Admin/Trainer: CRUD ----

    @PostMapping
    public ResponseEntity<AssignmentResponse> createAssignment(@Valid @RequestBody CreateAssignmentRequest request) {
        AuthenticatedUser user = currentUserProvider.getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED).body(assignmentService.createAssignment(request, user));
    }

    @GetMapping
    public ResponseEntity<List<AssignmentResponse>> getAllAssignments(
            @RequestParam(required = false) String course,
            @RequestParam(required = false) String batchId,
            @RequestParam(required = false) AssignmentStatus status) {
        AuthenticatedUser user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(assignmentService.getAllAssignments(course, batchId, status, user));
    }

    @GetMapping("/{assignmentId}")
    public ResponseEntity<AssignmentResponse> getAssignment(@PathVariable String assignmentId) {
        AuthenticatedUser user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(assignmentService.getAssignment(assignmentId, user));
    }

    @PutMapping("/{assignmentId}")
    public ResponseEntity<AssignmentResponse> updateAssignment(
            @PathVariable String assignmentId, @Valid @RequestBody UpdateAssignmentRequest request) {
        AuthenticatedUser user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(assignmentService.updateAssignment(assignmentId, request, user));
    }

    @DeleteMapping("/{assignmentId}")
    public ResponseEntity<Void> deleteAssignment(@PathVariable String assignmentId) {
        AuthenticatedUser user = currentUserProvider.getCurrentUser();
        assignmentService.deleteAssignment(assignmentId, user);
        return ResponseEntity.noContent().build();
    }

    // ---- Status controls ----

    @PatchMapping("/{assignmentId}/publish")
    public ResponseEntity<AssignmentResponse> publishAssignment(@PathVariable String assignmentId) {
        AuthenticatedUser user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(assignmentService.publishAssignment(assignmentId, user));
    }

    @PatchMapping("/{assignmentId}/unpublish")
    public ResponseEntity<AssignmentResponse> unpublishAssignment(@PathVariable String assignmentId) {
        AuthenticatedUser user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(assignmentService.unpublishAssignment(assignmentId, user));
    }

    @PatchMapping("/{assignmentId}/close")
    public ResponseEntity<AssignmentResponse> closeAssignment(@PathVariable String assignmentId) {
        AuthenticatedUser user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(assignmentService.closeAssignment(assignmentId, user));
    }

    // ---- Submissions ----

    @PostMapping("/{assignmentId}/submit")
    public ResponseEntity<SubmissionResponse> submitAssignment(
            @PathVariable String assignmentId, @Valid @RequestBody SubmitAssignmentRequest request) {
        AuthenticatedUser user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(assignmentService.submitAssignment(assignmentId, request, user));
    }

    @GetMapping("/{assignmentId}/my-submission")
    public ResponseEntity<SubmissionResponse> getMySubmission(@PathVariable String assignmentId) {
        AuthenticatedUser user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(assignmentService.getMySubmission(assignmentId, user));
    }

    @GetMapping("/{assignmentId}/submissions")
    public ResponseEntity<List<SubmissionResponse>> getSubmissions(@PathVariable String assignmentId) {
        AuthenticatedUser user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(assignmentService.getSubmissions(assignmentId, user));
    }

    @PatchMapping("/{assignmentId}/submissions/{studentId}/grade")
    public ResponseEntity<SubmissionResponse> gradeSubmission(
            @PathVariable String assignmentId, @PathVariable String studentId,
            @Valid @RequestBody GradeSubmissionRequest request) {
        AuthenticatedUser user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(assignmentService.gradeSubmission(assignmentId, studentId, request, user));
    }

    // ---- Analytics ----

    @GetMapping("/{assignmentId}/analytics")
    public ResponseEntity<AssignmentAnalyticsResponse> getAnalytics(@PathVariable String assignmentId) {
        AuthenticatedUser user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(assignmentService.getAnalytics(assignmentId, user));
    }

    // ---- Student access ----

    @GetMapping("/student/available")
    public ResponseEntity<List<AssignmentResponse>> getAvailableAssignments() {
        AuthenticatedUser user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(assignmentService.getAvailableAssignmentsForStudent(user));
    }
}