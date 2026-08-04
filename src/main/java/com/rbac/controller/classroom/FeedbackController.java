package com.rbac.controller.classroom;

import com.rbac.dto.feedback.FeedbackListResponse;
import com.rbac.dto.feedback.FeedbackResponse;
import com.rbac.dto.feedback.SubmitFeedbackRequest;
import com.rbac.security.chat.AuthenticatedUser;
import com.rbac.security.chat.CurrentUserProvider;
import com.rbac.service.feedback.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<FeedbackResponse> submit(@Valid @RequestBody SubmitFeedbackRequest request) {
        AuthenticatedUser requester = currentUserProvider.getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED).body(feedbackService.submit(request, requester));
    }

    @GetMapping("/session/{sessionId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<FeedbackListResponse> getBySession(
            @PathVariable String sessionId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        AuthenticatedUser requester = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(feedbackService.getBySession(sessionId, search, page, size, requester));
    }

    @GetMapping("/trainer/{trainerId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<FeedbackListResponse> getByTrainer(
            @PathVariable String trainerId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        AuthenticatedUser requester = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(feedbackService.getByTrainer(trainerId, search, page, size, requester));
    }
}