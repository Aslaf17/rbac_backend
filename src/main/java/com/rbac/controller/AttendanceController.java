package com.rbac.controller;

import com.rbac.dto.attendance.MarkAttendanceRequest;
import com.rbac.dto.attendance.UpdateAttendanceRequest;
import com.rbac.model.attendance.Attendance;
import com.rbac.service.AttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/mark")
    public ResponseEntity<Attendance> mark(@Valid @RequestBody MarkAttendanceRequest request) {
        Attendance attendance = attendanceService.markAttendance(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(attendance);
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<Attendance>> getBySession(@PathVariable String sessionId) {
        return ResponseEntity.ok(attendanceService.getBySession(sessionId));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<Attendance>> getByStudent(@PathVariable String studentId) {
        return ResponseEntity.ok(attendanceService.getByStudent(studentId));
    }

    @PutMapping("/update")
    public ResponseEntity<Attendance> update(@Valid @RequestBody UpdateAttendanceRequest request) {
        return ResponseEntity.ok(attendanceService.updateAttendance(request));
    }

    @GetMapping("/session/{sessionId}/summary")
    public ResponseEntity<java.util.Map<String, Object>> getSummary(@PathVariable String sessionId) {
        return ResponseEntity.ok(attendanceService.getSummaryBySession(sessionId));
    }
}
