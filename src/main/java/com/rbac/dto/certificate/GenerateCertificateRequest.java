package com.rbac.dto.certificate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class GenerateCertificateRequest {

    @NotBlank(message = "studentId is required")
    private String studentId;

    @NotBlank(message = "courseId is required")
    private String courseId;

    @NotNull(message = "completionDate is required")
    private LocalDate completionDate;

    private LocalDate issueDate;

    private String batchId;

    private String remarks;
}
