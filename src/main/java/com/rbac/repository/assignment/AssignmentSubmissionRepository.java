package com.rbac.repository.assignment;

import com.rbac.model.assignment.AssignmentSubmission;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface AssignmentSubmissionRepository extends MongoRepository<AssignmentSubmission, String> {

    List<AssignmentSubmission> findByAssignmentId(String assignmentId);

    Optional<AssignmentSubmission> findByAssignmentIdAndStudentId(String assignmentId, String studentId);

    List<AssignmentSubmission> findByStudentId(String studentId);

    void deleteByAssignmentId(String assignmentId);
}