package com.rbac.dto;

import com.rbac.model.attendance.AttendanceStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.Instant;

@Data
public class UpdateAttendanceRequest {

    @NotBlank(message = "id is required")
    private String id;

    private Instant joinTime;

    private Instant leaveTime;

    private AttendanceStatus status;
}