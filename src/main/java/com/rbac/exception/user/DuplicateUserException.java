package com.rbac.exception.user;

public class DuplicateUserException extends RuntimeException {
    public DuplicateUserException(String message) { super(message); }
}
