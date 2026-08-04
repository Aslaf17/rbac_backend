package com.rbac.dto.recording;

import com.rbac.model.recording.RecordingStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateRecordingStatusRequest {

    @NotNull(message = "status is required")
    private RecordingStatus status;

    private String failureReason;
}
