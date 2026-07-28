package com.rbac.exception.feedback;

public class DuplicateFeedbackException extends RuntimeException {
    public DuplicateFeedbackException(String message) { super(message); }
}