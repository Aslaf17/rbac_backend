package com.rbac.dto.exam;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class UpdateExamRequest {

    @NotBlank(message = "examName is required")
    @Size(max = 150, message = "examName must be at most 150 characters")
    private String examName;

    @NotBlank(message = "course is required")
    private String course;

    @NotBlank(message = "batchId is required")
    private String batchId;

    @Positive(message = "durationMinutes must be greater than 0")
    private int durationMinutes;

    @Positive(message = "totalMarks must be greater than 0")
    private double totalMarks;

    @PositiveOrZero(message = "passingMarks must be >= 0")
    private double passingMarks;

    @NotNull(message = "examDate is required")
    private Instant examDate;

    @Valid
    private List<QuestionDto> questions;
}
