package com.rbac.dto.recording;

import com.rbac.model.recording.Visibility;
import lombok.Data;

@Data
public class UpdateRecordingRequest {
    private String title;
    private String description;
    private Visibility visibility;
    private String thumbnailUrl;
    private Boolean downloadEnabled;
}
