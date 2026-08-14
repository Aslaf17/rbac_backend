package com.rbac.dto.assignment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class GradeSubmissionRequest {

    @NotNull(message = "obtainedMarks is required")
    @PositiveOrZero(message = "obtainedMarks must be >= 0")
    private Double obtainedMarks;

    private String feedback;
}