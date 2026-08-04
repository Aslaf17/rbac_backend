package com.rbac.dto.batch;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AssignStudentsRequest {

    @NotEmpty(message = "studentIds must contain at least one id")
    private List<String> studentIds;
}
