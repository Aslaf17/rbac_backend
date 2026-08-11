package com.rbac.controller;

import com.rbac.dto.dashboard.AdminDashboardResponse;
import com.rbac.service.dashboard.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/api/user/profile")
    public Map<String, Object> profile(Authentication authentication) {
        return Map.of(
                "username", authentication.getName(),
                "authorities", authentication.getAuthorities().toString(),
                "message", "Any authenticated user can see this, regardless of role."
        );
    }

    @GetMapping("/api/student/dashboard")
    public Map<String, Object> studentDashboard(Authentication authentication) {
        return response(authentication, "Student Dashboard",
                "Course schedule, assignments, and grades would appear here.");
    }

    @GetMapping("/api/teacher/dashboard")
    public Map<String, Object> teacherDashboard(Authentication authentication) {
        return response(authentication, "Teacher Dashboard",
                "Class rosters, grading tools, and lesson plans would appear here.");
    }

    @GetMapping("/api/employer/dashboard")
    public Map<String, Object> employerDashboard(Authentication authentication) {
        return response(authentication, "Employer Dashboard",
                "Job postings, applicant tracking, and hiring analytics would appear here.");
    }

    @GetMapping("/api/employee/dashboard")
    public Map<String, Object> employeeDashboard(Authentication authentication) {
        return response(authentication, "Employee Dashboard",
                "Payroll, timesheets, and company announcements would appear here.");
    }

    // Secured to ADMIN role only via SecurityConfig ("/api/admin/**" -> hasRole("ADMIN")).
    @GetMapping("/api/admin/dashboard")
    public ResponseEntity<AdminDashboardResponse> adminDashboard() {
        return ResponseEntity.ok(dashboardService.getAdminDashboard());
    }

    private Map<String, Object> response(Authentication authentication, String title, String detail) {
        return Map.of(
                "title", title,
                "detail", detail,
                "requestedBy", authentication.getName(),
                "timestamp", Instant.now().toString()
        );
    }
}