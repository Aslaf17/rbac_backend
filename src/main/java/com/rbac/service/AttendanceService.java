package com.rbac.service;

import com.rbac.dto.attendance.MarkAttendanceRequest;
import com.rbac.dto.attendance.UpdateAttendanceRequest;
import com.rbac.model.attendance.Attendance;
import com.rbac.model.attendance.AttendanceStatus;
import com.rbac.repository.AttendanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;

    public Attendance markAttendance(MarkAttendanceRequest request) {
        if (attendanceRepository.existsBySessionIdAndUserId(request.getSessionId(), request.getUserId())) {
            throw new IllegalArgumentException(
                    "Attendance is already recorded for this user in this session");
        }

        Instant joinTime = request.getJoinTime() != null ? request.getJoinTime() : Instant.now();
        Instant leaveTime = request.getLeaveTime();

        Attendance attendance = new Attendance();
        attendance.setUserId(request.getUserId());
        attendance.setSessionId(request.getSessionId());
        attendance.setJoinTime(joinTime);
        attendance.setLeaveTime(leaveTime);
        attendance.setDurationSeconds(calculateDurationSeconds(joinTime, leaveTime));
        attendance.setStatus(request.getStatus() != null ? request.getStatus() : AttendanceStatus.PRESENT);
        attendance.setCreatedAt(Instant.now());
        attendance.setUpdatedAt(Instant.now());

        try {
            return attendanceRepository.save(attendance);
        } catch (DuplicateKeyException ex) {
            // Guards against a race between the existsBy check above and the save
            throw new IllegalArgumentException(
                    "Attendance is already recorded for this user in this session");
        }
    }

    public List<Attendance> getBySession(String sessionId) {
        return attendanceRepository.findBySessionId(sessionId);
    }

    public List<Attendance> getByStudent(String studentId) {
        return attendanceRepository.findByUserId(studentId);
    }

    public Attendance updateAttendance(UpdateAttendanceRequest request) {
        Attendance attendance = attendanceRepository.findById(request.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No attendance record found with id: " + request.getId()));

        if (request.getJoinTime() != null) {
            attendance.setJoinTime(request.getJoinTime());
        }
        if (request.getLeaveTime() != null) {
            attendance.setLeaveTime(request.getLeaveTime());
        }
        if (request.getStatus() != null) {
            attendance.setStatus(request.getStatus());
        }

        attendance.setDurationSeconds(
                calculateDurationSeconds(attendance.getJoinTime(), attendance.getLeaveTime()));
        attendance.setUpdatedAt(Instant.now());

        return attendanceRepository.save(attendance);
    }

    private Long calculateDurationSeconds(Instant joinTime, Instant leaveTime) {
        if (joinTime == null || leaveTime == null) {
            return null;
        }
        long seconds = Duration.between(joinTime, leaveTime).getSeconds();
        return Math.max(seconds, 0L);
    }

    public java.util.Map<String, Object> getSummaryBySession(String sessionId) {
        List<Attendance> records = attendanceRepository.findBySessionId(sessionId);
        long present = records.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count();
        long absent = records.stream().filter(a -> a.getStatus() == AttendanceStatus.ABSENT).count();
        long late = records.stream().filter(a -> a.getStatus() == AttendanceStatus.LATE).count();
        long leftEarly = records.stream().filter(a -> a.getStatus() == AttendanceStatus.LEFT_EARLY).count();

        return java.util.Map.of(
                "sessionId", sessionId,
                "totalMarked", records.size(),
                "present", present,
                "absent", absent,
                "late", late,
                "leftEarly", leftEarly,
                "records", records
        );
    }
}
