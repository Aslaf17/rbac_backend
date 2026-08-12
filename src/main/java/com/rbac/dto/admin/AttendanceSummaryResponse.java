package com.rbac.dto.admin;

import com.rbac.model.attendance.AttendanceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSummaryResponse {

    private String sessionId;
    private String sessionTitle;
    private String batchId;
    private String batchName;

    private long totalStudents;
    private long presentCount;
    private long absentCount;
    private long lateCount;
    private long leftEarlyCount;

    private double attendancePercentage;

    private List<StudentAttendanceRow> students;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentAttendanceRow {
        private String userId;
        private String studentName;
        private String email;
        private AttendanceStatus status;
        private Instant joinTime;
        private Instant leaveTime;
        private Long durationSeconds;
    }
}