package com.rbac.dto.course;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseStatisticsResponse {

    private long totalCourses;
    private long activeCourses;
    private long inactiveCourses;
    private long archivedCourses;
    private long coursesWithTrainer;
    private long coursesWithoutTrainer;
}