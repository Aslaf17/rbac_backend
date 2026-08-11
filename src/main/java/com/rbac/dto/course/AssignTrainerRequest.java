package com.rbac.dto.course;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AssignTrainerRequest {

    @NotBlank(message = "trainerId is required")
    private String trainerId;
}