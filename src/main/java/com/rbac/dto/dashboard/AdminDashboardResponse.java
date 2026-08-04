package com.rbac.dto.dashboard;

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
public class AdminDashboardResponse {

    private StatCards stats;
    private List<RecentRegistration> recentRegistrations;
    private Instant generatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatCards {
        private long totalStudents;
        private long totalTrainers;
        private long totalCourses;
        private long totalBatches;
        private long activeLiveSessions;
        private long completedSessions;
        private long totalExams;
        private long totalCertificates;
        private long pendingAssignments;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentRegistration {
        private String userId;
        private String username;
        private String email;
        private String role;
        private Instant registeredAt;
    }
}
