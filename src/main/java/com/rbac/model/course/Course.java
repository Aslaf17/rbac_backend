package com.rbac.model.course;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "courses")
public class Course {

    @Id
    private String id; // course code, e.g. "CSE-101"

    @Indexed
    private String title;

    private String description;

    @Indexed
    private String category;

    private Integer durationWeeks;

    @Indexed
    private CourseStatus status = CourseStatus.ACTIVE;

    private String trainerId;
    private String trainerName;

    private String createdBy;
    private String createdByName;

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
}