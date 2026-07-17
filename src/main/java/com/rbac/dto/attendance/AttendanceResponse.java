package com.rbac.dto.attendance;

import com.rbac.model.attendance.AttendanceStatus;
import lombok.Data;

import java.time.Instant;

@Data
public class AttendanceResponse {

    private String id;
    private String userId;
    private String studentName;
    private String sessionId;
    private String sessionName;
    private Instant joinTime;
    private Instant leaveTime;
    private Long durationSeconds;
    private AttendanceStatus status;
}