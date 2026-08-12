package com.rbac.dto.exam;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamAnalyticsResponse {

    private String examId;
    private String examName;

    private long totalStudents;
    private long attemptedCount;
    private long passedCount;
    private long failedCount;

    private double averageScore;
    private double highestScore;
    private double lowestScore;

    private double passPercentage;
    private double failPercentage;
    private double completionRate;
}
