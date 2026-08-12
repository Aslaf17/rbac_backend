package com.rbac.dto.course;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCourseRequest {

    @Pattern(regexp = "^[A-Za-z0-9_-]{3,40}$", message = "courseId may only contain letters, digits, '-' and '_' (3-40 chars)")
    private String courseId;

    @NotBlank(message = "title is required")
    @Size(max = 120, message = "title must be at most 120 characters")
    private String title;

    @Size(max = 1000, message = "description must be at most 1000 characters")
    private String description;

    @Size(max = 60, message = "category must be at most 60 characters")
    private String category;

    @Positive(message = "durationWeeks must be a positive number")
    private Integer durationWeeks;
}