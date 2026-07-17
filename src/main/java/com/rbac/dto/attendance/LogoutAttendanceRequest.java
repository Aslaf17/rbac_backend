package com.rbac.dto.attendance;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LogoutAttendanceRequest {

    @NotBlank(message = "userId is required")
    private String userId;

    @NotBlank(message = "sessionId is required")
    private String sessionId;
}