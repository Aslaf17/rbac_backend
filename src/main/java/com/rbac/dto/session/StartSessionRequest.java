package com.rbac.dto.session;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StartSessionRequest {

    @NotBlank(message = "title is required")
    private String title;

    @NotBlank(message = "batchId is required")
    private String batchId;

    private String userId;

    private String sessionId;

    private Integer reconnectTimeoutSeconds;
}