package com.rbac.exception.batch;

public class DuplicateBatchException extends RuntimeException {
    public DuplicateBatchException(String message) { super(message); }
}
