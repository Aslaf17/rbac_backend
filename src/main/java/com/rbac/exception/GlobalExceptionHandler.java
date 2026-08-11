package com.rbac.exception;

import com.rbac.dto.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(new ApiError(400, ex.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiError(401, ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiError(403, "You do not have permission to access this resource"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(new ApiError(400, message));
    }

    // Catches malformed JSON and invalid enum values (e.g. bad "priority" or "recipientType")
    // so they surface as 400 instead of falling through to the generic 500 handler.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleMalformedRequest(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(new ApiError(400, "Malformed request body or invalid field value"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError(500, "Something went wrong: " + ex.getMessage()));
    }

    @ExceptionHandler(com.rbac.exception.notification.ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotificationNotFound(com.rbac.exception.notification.ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(404, ex.getMessage()));
    }

    @ExceptionHandler(com.rbac.exception.notification.InvalidRequestException.class)
    public ResponseEntity<ApiError> handleNotificationInvalidRequest(com.rbac.exception.notification.InvalidRequestException ex) {
        return ResponseEntity.badRequest().body(new ApiError(400, ex.getMessage()));
    }

    @ExceptionHandler(com.rbac.exception.notification.UnauthorizedActionException.class)
    public ResponseEntity<ApiError> handleNotificationUnauthorized(com.rbac.exception.notification.UnauthorizedActionException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiError(403, ex.getMessage()));
    }

    @ExceptionHandler(com.rbac.exception.feedback.ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleFeedbackNotFound(com.rbac.exception.feedback.ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(404, ex.getMessage()));
    }

    @ExceptionHandler(com.rbac.exception.feedback.InvalidRequestException.class)
    public ResponseEntity<ApiError> handleFeedbackInvalidRequest(com.rbac.exception.feedback.InvalidRequestException ex) {
        return ResponseEntity.badRequest().body(new ApiError(400, ex.getMessage()));
    }

    @ExceptionHandler(com.rbac.exception.feedback.UnauthorizedActionException.class)
    public ResponseEntity<ApiError> handleFeedbackUnauthorized(com.rbac.exception.feedback.UnauthorizedActionException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiError(403, ex.getMessage()));
    }

    @ExceptionHandler(com.rbac.exception.feedback.DuplicateFeedbackException.class)
    public ResponseEntity<ApiError> handleFeedbackDuplicate(com.rbac.exception.feedback.DuplicateFeedbackException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(409, ex.getMessage()));
    }

    @ExceptionHandler(com.rbac.exception.session.ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleSessionNotFound(com.rbac.exception.session.ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(404, ex.getMessage()));
    }

    @ExceptionHandler(com.rbac.exception.session.InvalidRequestException.class)
    public ResponseEntity<ApiError> handleSessionInvalidRequest(com.rbac.exception.session.InvalidRequestException ex) {
        return ResponseEntity.badRequest().body(new ApiError(400, ex.getMessage()));
    }

    @ExceptionHandler(com.rbac.exception.recording.ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleRecordingNotFound(com.rbac.exception.recording.ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(404, ex.getMessage()));
    }

    @ExceptionHandler(com.rbac.exception.recording.InvalidRequestException.class)
    public ResponseEntity<ApiError> handleRecordingInvalidRequest(com.rbac.exception.recording.InvalidRequestException ex) {
        return ResponseEntity.badRequest().body(new ApiError(400, ex.getMessage()));
    }

    @ExceptionHandler(com.rbac.exception.recording.UnauthorizedActionException.class)
    public ResponseEntity<ApiError> handleRecordingUnauthorized(com.rbac.exception.recording.UnauthorizedActionException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiError(403, ex.getMessage()));
    }

    @ExceptionHandler(com.rbac.exception.recording.DuplicateRecordingException.class)
    public ResponseEntity<ApiError> handleRecordingDuplicate(com.rbac.exception.recording.DuplicateRecordingException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(409, ex.getMessage()));
    }

    @ExceptionHandler(com.rbac.exception.batch.ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleBatchNotFound(com.rbac.exception.batch.ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(404, ex.getMessage()));
    }

    @ExceptionHandler(com.rbac.exception.batch.InvalidRequestException.class)
    public ResponseEntity<ApiError> handleBatchInvalidRequest(com.rbac.exception.batch.InvalidRequestException ex) {
        return ResponseEntity.badRequest().body(new ApiError(400, ex.getMessage()));
    }

    @ExceptionHandler(com.rbac.exception.batch.UnauthorizedActionException.class)
    public ResponseEntity<ApiError> handleBatchUnauthorized(com.rbac.exception.batch.UnauthorizedActionException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiError(403, ex.getMessage()));
    }

    @ExceptionHandler(com.rbac.exception.batch.DuplicateBatchException.class)
    public ResponseEntity<ApiError> handleBatchDuplicate(com.rbac.exception.batch.DuplicateBatchException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(409, ex.getMessage()));
    }

    @ExceptionHandler(com.rbac.exception.course.ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleCourseNotFound(com.rbac.exception.course.ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(404, ex.getMessage()));
    }

    @ExceptionHandler(com.rbac.exception.course.InvalidRequestException.class)
    public ResponseEntity<ApiError> handleCourseInvalidRequest(com.rbac.exception.course.InvalidRequestException ex) {
        return ResponseEntity.badRequest().body(new ApiError(400, ex.getMessage()));
    }

    @ExceptionHandler(com.rbac.exception.course.UnauthorizedActionException.class)
    public ResponseEntity<ApiError> handleCourseUnauthorized(com.rbac.exception.course.UnauthorizedActionException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiError(403, ex.getMessage()));
    }

    @ExceptionHandler(com.rbac.exception.course.DuplicateCourseException.class)
    public ResponseEntity<ApiError> handleCourseDuplicate(com.rbac.exception.course.DuplicateCourseException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(409, ex.getMessage()));
    }

    @ExceptionHandler(com.rbac.exception.exam.ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleExamNotFound(com.rbac.exception.exam.ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(404, ex.getMessage()));
    }

    @ExceptionHandler(com.rbac.exception.exam.InvalidRequestException.class)
    public ResponseEntity<ApiError> handleExamInvalidRequest(com.rbac.exception.exam.InvalidRequestException ex) {
        return ResponseEntity.badRequest().body(new ApiError(400, ex.getMessage()));
    }

    @ExceptionHandler(com.rbac.exception.exam.UnauthorizedActionException.class)
    public ResponseEntity<ApiError> handleExamUnauthorized(com.rbac.exception.exam.UnauthorizedActionException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiError(403, ex.getMessage()));
    }
}
