package com.rbac.controller;

import com.rbac.dto.batch.AssignStudentsRequest;
import com.rbac.dto.batch.AssignTrainersRequest;
import com.rbac.dto.batch.BatchAnalyticsResponse;
import com.rbac.dto.batch.BatchResponse;
import com.rbac.dto.batch.BatchStudentResponse;
import com.rbac.dto.batch.BatchTrainerResponse;
import com.rbac.dto.batch.CreateBatchRequest;
import com.rbac.dto.batch.UpdateBatchRequest;
import com.rbac.model.batch.BatchStatus;
import com.rbac.security.chat.AuthenticatedUser;
import com.rbac.security.chat.CurrentUserProvider;
import com.rbac.service.batch.BatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/batches")
@RequiredArgsConstructor
public class BatchController {

    private final BatchService batchService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    public ResponseEntity<BatchResponse> createBatch(@Valid @RequestBody CreateBatchRequest request) {
        AuthenticatedUser admin = currentUserProvider.getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED).body(batchService.createBatch(request, admin));
    }

    @GetMapping
    public ResponseEntity<List<BatchResponse>> getAllBatches(
            @RequestParam(required = false) BatchStatus status) {
        return ResponseEntity.ok(batchService.getAllBatches(status));
    }

    @GetMapping("/students/search")
    public ResponseEntity<List<BatchStudentResponse>> searchStudents(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String excludeBatchId) {
        return ResponseEntity.ok(batchService.searchStudents(query, excludeBatchId));
    }

    @GetMapping("/trainers/search")
    public ResponseEntity<List<BatchTrainerResponse>> searchTrainers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String excludeBatchId) {
        return ResponseEntity.ok(batchService.searchTrainers(query, excludeBatchId));
    }


    @GetMapping("/{batchId}")
    public ResponseEntity<BatchResponse> getBatch(@PathVariable String batchId) {
        return ResponseEntity.ok(batchService.getBatch(batchId));
    }

    @PutMapping("/{batchId}")
    public ResponseEntity<BatchResponse> updateBatch(
            @PathVariable String batchId, @Valid @RequestBody UpdateBatchRequest request) {
        AuthenticatedUser admin = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(batchService.updateBatch(batchId, request, admin));
    }

    @DeleteMapping("/{batchId}")
    public ResponseEntity<Void> deleteBatch(@PathVariable String batchId) {
        AuthenticatedUser admin = currentUserProvider.getCurrentUser();
        batchService.deleteBatch(batchId, admin);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{batchId}/students")
    public ResponseEntity<BatchResponse> assignStudents(
            @PathVariable String batchId, @Valid @RequestBody AssignStudentsRequest request) {
        AuthenticatedUser admin = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(batchService.assignStudents(batchId, request, admin));
    }

    @DeleteMapping("/{batchId}/students")
    public ResponseEntity<BatchResponse> removeStudents(
            @PathVariable String batchId, @Valid @RequestBody AssignStudentsRequest request) {
        AuthenticatedUser admin = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(batchService.removeStudents(batchId, request, admin));
    }

    @GetMapping("/{batchId}/students")
    public ResponseEntity<List<BatchStudentResponse>> getStudents(@PathVariable String batchId) {
        return ResponseEntity.ok(batchService.getStudents(batchId));
    }

    @PostMapping("/{batchId}/trainers")
    public ResponseEntity<BatchResponse> assignTrainers(
            @PathVariable String batchId, @Valid @RequestBody AssignTrainersRequest request) {
        AuthenticatedUser admin = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(batchService.assignTrainers(batchId, request, admin));
    }

    @DeleteMapping("/{batchId}/trainers")
    public ResponseEntity<BatchResponse> removeTrainers(
            @PathVariable String batchId, @Valid @RequestBody AssignTrainersRequest request) {
        AuthenticatedUser admin = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(batchService.removeTrainers(batchId, request, admin));
    }

    @GetMapping("/{batchId}/trainers")
    public ResponseEntity<List<BatchTrainerResponse>> getTrainers(@PathVariable String batchId) {
        return ResponseEntity.ok(batchService.getTrainers(batchId));
    }

    @GetMapping("/{batchId}/analytics")
    public ResponseEntity<BatchAnalyticsResponse> getBatchAnalytics(@PathVariable String batchId) {
        return ResponseEntity.ok(batchService.getBatchAnalytics(batchId));
    }
}

