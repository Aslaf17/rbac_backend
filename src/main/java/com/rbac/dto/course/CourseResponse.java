package com.rbac.dto.course;

import com.rbac.model.course.Course;
import com.rbac.model.course.CourseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseResponse {

    private String courseId;
    private String title;
    private String description;
    private String category;
    private Integer durationWeeks;
    private CourseStatus status;
    private String trainerId;
    private String trainerName;
    private String createdBy;
    private String createdByName;
    private Instant createdAt;
    private Instant updatedAt;

    public static CourseResponse fromEntity(Course course) {
        return CourseResponse.builder()
                .courseId(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .category(course.getCategory())
                .durationWeeks(course.getDurationWeeks())
                .status(course.getStatus())
                .trainerId(course.getTrainerId())
                .trainerName(course.getTrainerName())
                .createdBy(course.getCreatedBy())
                .createdByName(course.getCreatedByName())
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .build();
    }
}