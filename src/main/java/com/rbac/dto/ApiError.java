package com.rbac.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class ApiError {
    private Instant timestamp = Instant.now();
    private int status;
    private String message;

    public ApiError(int status, String message) {
        this.status = status;
        this.message = message;
    }
}
