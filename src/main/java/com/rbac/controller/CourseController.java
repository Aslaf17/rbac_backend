package com.rbac.controller;

import com.rbac.dto.course.*;
import com.rbac.model.course.CourseStatus;
import com.rbac.security.chat.AuthenticatedUser;
import com.rbac.security.chat.CurrentUserProvider;
import com.rbac.service.course.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    public ResponseEntity<CourseResponse> createCourse(@Valid @RequestBody CreateCourseRequest request) {
        AuthenticatedUser admin = currentUserProvider.getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED).body(courseService.createCourse(request, admin));
    }

    @GetMapping
    public ResponseEntity<List<CourseResponse>> getAllCourses(
            @RequestParam(required = false) CourseStatus status) {
        return ResponseEntity.ok(courseService.getAllCourses(status));
    }

    @GetMapping("/statistics")
    public ResponseEntity<CourseStatisticsResponse> getStatistics() {
        return ResponseEntity.ok(courseService.getStatistics());
    }

    @GetMapping("/{courseId}")
    public ResponseEntity<CourseResponse> getCourse(@PathVariable String courseId) {
        return ResponseEntity.ok(courseService.getCourse(courseId));
    }

    @PutMapping("/{courseId}")
    public ResponseEntity<CourseResponse> updateCourse(
            @PathVariable String courseId, @Valid @RequestBody UpdateCourseRequest request) {
        AuthenticatedUser admin = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(courseService.updateCourse(courseId, request, admin));
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<Void> deleteCourse(@PathVariable String courseId) {
        AuthenticatedUser admin = currentUserProvider.getCurrentUser();
        courseService.deleteCourse(courseId, admin);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{courseId}/trainer")
    public ResponseEntity<CourseResponse> assignTrainer(
            @PathVariable String courseId, @Valid @RequestBody AssignTrainerRequest request) {
        AuthenticatedUser admin = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(courseService.assignTrainer(courseId, request, admin));
    }

    @PutMapping("/{courseId}/archive")
    public ResponseEntity<CourseResponse> archiveCourse(@PathVariable String courseId) {
        AuthenticatedUser admin = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(courseService.archiveCourse(courseId, admin));
    }
}