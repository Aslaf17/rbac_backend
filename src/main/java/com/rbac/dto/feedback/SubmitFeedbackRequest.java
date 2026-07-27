package com.rbac.dto.feedback;

import com.rbac.model.feedback.FeedbackTag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SubmitFeedbackRequest {

    @NotBlank(message = "sessionId is required")
    private String sessionId;

    @NotBlank(message = "trainerId is required")
    private String trainerId;

    @NotNull(message = "rating is required")
    @Min(value = 1, message = "rating must be between 1 and 5")
    @Max(value = 5, message = "rating must be between 1 and 5")
    private Integer rating;

    @Size(max = 2000, message = "review must be at most 2000 characters")
    private String review;

    @NotNull(message = "tag is required")
    private FeedbackTag tag;
}