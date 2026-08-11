package com.rbac.dto.batch;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AssignTrainersRequest {

    @NotEmpty(message = "trainerIds must contain at least one id")
    private List<String> trainerIds;
}