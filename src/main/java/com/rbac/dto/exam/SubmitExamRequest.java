package com.rbac.dto.exam;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Map;

@Data
public class SubmitExamRequest {

    @NotEmpty(message = "answers must contain at least one response")
    private Map<String, Integer> answers;
}
