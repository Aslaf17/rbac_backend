package com.rbac.repository.exam;

import com.rbac.model.exam.Exam;
import com.rbac.model.exam.ExamStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ExamRepository extends MongoRepository<Exam, String> {

    List<Exam> findByBatchId(String batchId);

    List<Exam> findByStatus(ExamStatus status);

    List<Exam> findByBatchIdIn(List<String> batchIds);

    List<Exam> findByBatchIdInAndStatus(List<String> batchIds, ExamStatus status);
}
