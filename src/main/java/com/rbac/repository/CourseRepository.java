package com.rbac.repository;

import com.rbac.model.course.Course;
import com.rbac.model.course.CourseStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CourseRepository extends MongoRepository<Course, String> {

    List<Course> findByStatus(CourseStatus status);

    List<Course> findByTrainerId(String trainerId);

    long countByStatus(CourseStatus status);

    long countByTrainerIdIsNotNull();

    long countByTrainerIdIsNull();
}