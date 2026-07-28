package com.rbac.dto;

import com.rbac.model.attendance.AttendanceStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.Instant;

@Data
public class MarkAttendanceRequest {

    @NotBlank(message = "userId is required")
    private String userId;

    @NotBlank(message = "sessionId is required")
    private String sessionId;

    private Instant joinTime;

    private Instant leaveTime;

    private AttendanceStatus status;
}
