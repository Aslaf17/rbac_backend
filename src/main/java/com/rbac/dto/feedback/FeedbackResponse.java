package com.rbac.dto.feedback;

import com.rbac.model.feedback.Feedback;
import com.rbac.model.feedback.FeedbackTag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackResponse {

    private String id;
    private String sessionId;
    private String sessionTitle;
    private String studentId;
    private String studentName;
    private String trainerId;
    private String trainerName;
    private int rating;
    private String review;
    private FeedbackTag tag;
    private Instant createdAt;

    public static FeedbackResponse fromEntity(Feedback f) {
        return FeedbackResponse.builder()
                .id(f.getId())
                .sessionId(f.getSessionId())
                .sessionTitle(f.getSessionTitle())
                .studentId(f.getStudentId())
                .studentName(f.getStudentName())
                .trainerId(f.getTrainerId())
                .trainerName(f.getTrainerName())
                .rating(f.getRating())
                .review(f.getReview())
                .tag(f.getTag())
                .createdAt(f.getCreatedAt())
                .build();
    }
}