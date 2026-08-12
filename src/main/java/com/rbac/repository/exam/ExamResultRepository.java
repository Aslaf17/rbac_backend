package com.rbac.repository.exam;

import com.rbac.model.exam.ExamResult;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ExamResultRepository extends MongoRepository<ExamResult, String> {

    List<ExamResult> findByExamId(String examId);

    Optional<ExamResult> findByExamIdAndStudentId(String examId, String studentId);

    void deleteByExamId(String examId);
}
