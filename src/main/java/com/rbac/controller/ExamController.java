package com.rbac.controller;

import com.rbac.dto.exam.*;
import com.rbac.model.exam.ExamStatus;
import com.rbac.security.chat.AuthenticatedUser;
import com.rbac.security.chat.CurrentUserProvider;
import com.rbac.service.exam.ExamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exams")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;
    private final CurrentUserProvider currentUserProvider;

    // ---- Admin/Trainer: CRUD ----

    @PostMapping
    public ResponseEntity<ExamResponse> createExam(@Valid @RequestBody CreateExamRequest request) {
        AuthenticatedUser user = currentUserProvider.getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED).body(examService.createExam(request, user));
    }

    @GetMapping
    public ResponseEntity<List<ExamResponse>> getAllExams(
            @RequestParam(required = false) String course,
            @RequestParam(required = false) String batchId,
            @RequestParam(required = false) ExamStatus status) {
        AuthenticatedUser user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(examService.getAllExams(course, batchId, status, user));
    }

    @GetMapping("/{examId}")
    public ResponseEntity<ExamResponse> getExam(@PathVariable String examId) {
        AuthenticatedUser user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(examService.getExam(examId, user));
    }

    @PutMapping("/{examId}")
    public ResponseEntity<ExamResponse> updateExam(
            @PathVariable String examId, @Valid @RequestBody UpdateExamRequest request) {
        AuthenticatedUser user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(examService.updateExam(examId, request, user));
    }

    @DeleteMapping("/{examId}")
    public ResponseEntity<Void> deleteExam(@PathVariable String examId) {
        AuthenticatedUser user = currentUserProvider.getCurrentUser();
        examService.deleteExam(examId, user);
        return ResponseEntity.noContent().build();
    }

    // ---- Publish / Unpublish ----

    @PatchMapping("/{examId}/publish")
    public ResponseEntity<ExamResponse> publishExam(@PathVariable String examId) {
        AuthenticatedUser user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(examService.publishExam(examId, user));
    }

    @PatchMapping("/{examId}/unpublish")
    public ResponseEntity<ExamResponse> unpublishExam(@PathVariable String examId) {
        AuthenticatedUser user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(examService.unpublishExam(examId, user));
    }

    // ---- Analytics & Results ----

    @GetMapping("/{examId}/analytics")
    public ResponseEntity<ExamAnalyticsResponse> getAnalytics(@PathVariable String examId) {
        AuthenticatedUser user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(examService.getAnalytics(examId, user));
    }

    @GetMapping("/{examId}/results")
    public ResponseEntity<List<ExamResultSummaryResponse>> getResultSummary(@PathVariable String examId) {
        AuthenticatedUser user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(examService.getResultSummary(examId, user));
    }

    // ---- Student access ----

    @GetMapping("/student/available")
    public ResponseEntity<List<ExamResponse>> getAvailableExams() {
        AuthenticatedUser user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(examService.getAvailableExamsForStudent(user));
    }

    @PostMapping("/{examId}/submit")
    public ResponseEntity<ExamResultSummaryResponse> submitExam(
            @PathVariable String examId, @Valid @RequestBody SubmitExamRequest request) {
        AuthenticatedUser user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(examService.submitExam(examId, request, user));
    }
}
