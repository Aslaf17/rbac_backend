package com.rbac.service;

import com.rbac.dto.attendance.AttendanceResponse;
import com.rbac.dto.attendance.LogoutAttendanceRequest;
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

import com.rbac.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;

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
            throw new IllegalArgumentException(
                    "Attendance is already recorded for this user in this session");
        }
    }

    public List<AttendanceResponse> getBySession(String sessionId) {

        return attendanceRepository.findBySessionId(sessionId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }
    public List<AttendanceResponse> getByStudent(String studentId) {

        return attendanceRepository.findByUserId(studentId)
                .stream()
                .map(this::mapToResponse)
                .toList();
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

    public Attendance logoutAttendance(LogoutAttendanceRequest request) {
        Attendance attendance = attendanceRepository
                .findBySessionIdAndUserIdAndLeaveTimeIsNull(request.getSessionId(), request.getUserId())
                .orElseThrow(() -> new RuntimeException(
                        "No active (logged-in) attendance record found for this student and session."));

        attendance.setLeaveTime(java.time.Instant.now());
        return attendanceRepository.save(attendance);
    }

    private AttendanceResponse mapToResponse(Attendance attendance) {

        AttendanceResponse response = new AttendanceResponse();

        response.setId(attendance.getId());
        response.setUserId(attendance.getUserId());

        userRepository.findById(attendance.getUserId())
                .ifPresent(user -> response.setStudentName(user.getUsername()));

        response.setSessionId(attendance.getSessionId());

        response.setSessionName(attendance.getSessionId());

        response.setJoinTime(attendance.getJoinTime());
        response.setLeaveTime(attendance.getLeaveTime());
        response.setDurationSeconds(attendance.getDurationSeconds());
        response.setStatus(attendance.getStatus());

        return response;
    }
}
