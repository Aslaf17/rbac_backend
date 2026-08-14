package com.rbac.dto.assignment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentAnalyticsResponse {
    private String assignmentId;
    private String title;
    private long totalStudents;
    private long submittedCount;
    private long pendingCount;
    private long lateCount;
    private long gradedCount;
    private double averageMarks;
    private double highestMarks;
    private double lowestMarks;
    private double submissionPercentage;
}