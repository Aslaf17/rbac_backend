package com.rbac.dto.assignment;

import jakarta.validation.Valid;
import lombok.Data;

import java.util.List;

@Data
public class SubmitAssignmentRequest {
    private String answerText;

    @Valid
    private List<AttachmentDto> files;
}