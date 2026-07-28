package com.rbac.exception.feedback;

public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) { super(message); }
}