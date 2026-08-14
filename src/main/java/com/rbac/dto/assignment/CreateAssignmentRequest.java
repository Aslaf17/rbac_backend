package com.rbac.dto.assignment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class CreateAssignmentRequest {

    @NotBlank(message = "title is required")
    private String title;

    @NotBlank(message = "course is required")
    private String course;

    @NotBlank(message = "batchId is required")
    private String batchId;

    private String description;

    private String instructions;

    @Positive(message = "totalMarks must be greater than 0")
    private double totalMarks;

    @NotNull(message = "dueDate is required")
    @Future(message = "dueDate must be in the future")
    private Instant dueDate;

    @Valid
    private List<AttachmentDto> attachments;
}