package com.rbac.dto.recording;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class WatchProgressRequest {

    @NotBlank(message = "playbackToken is required")
    private String playbackToken;

    @Min(value = 0, message = "watchedSeconds cannot be negative")
    private long watchedSeconds;
}
