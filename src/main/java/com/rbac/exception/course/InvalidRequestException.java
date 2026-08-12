package com.rbac.exception.course;

public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) { super(message); }
}