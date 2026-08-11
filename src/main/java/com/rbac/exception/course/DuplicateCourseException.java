package com.rbac.exception.course;

public class DuplicateCourseException extends RuntimeException {
    public DuplicateCourseException(String message) { super(message); }
}