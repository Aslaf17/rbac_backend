package com.rbac.repository;

import com.rbac.model.feedback.Feedback;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface FeedbackRepository extends MongoRepository<Feedback, String> {

    Optional<Feedback> findBySessionIdAndStudentId(String sessionId, String studentId);

    boolean existsBySessionIdAndStudentId(String sessionId, String studentId);
}