package com.rbac.dto.exam;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamResultSummaryResponse {

    private String studentId;
    private String studentName;
    private double totalMarks;
    private double obtainedMarks;
    private double percentage;
    private String grade;
    private boolean passed;
    private boolean completed;
    private Instant submittedAt;
}
