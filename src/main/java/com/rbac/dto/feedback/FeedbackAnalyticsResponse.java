package com.rbac.dto.feedback;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackAnalyticsResponse {

    private long totalFeedback;
    private double averageRating;

    private Map<Integer, Long> ratingDistribution;

    private Map<String, Long> tagDistribution;
}