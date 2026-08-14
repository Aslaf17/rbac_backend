package com.rbac.exception.certificate;

public class UnauthorizedActionException extends RuntimeException {
    public UnauthorizedActionException(String message) { super(message); }
}
