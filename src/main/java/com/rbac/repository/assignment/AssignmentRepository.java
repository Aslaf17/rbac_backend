package com.rbac.repository.assignment;

import com.rbac.model.assignment.Assignment;
import com.rbac.model.assignment.AssignmentStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AssignmentRepository extends MongoRepository<Assignment, String> {

    List<Assignment> findByBatchId(String batchId);

    List<Assignment> findByStatus(AssignmentStatus status);

    List<Assignment> findByBatchIdIn(List<String> batchIds);
}