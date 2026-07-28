package com.rbac.exception;

import com.rbac.dto.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
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
}
