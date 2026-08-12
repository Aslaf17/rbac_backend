package com.rbac.dto.exam;

import com.rbac.model.exam.Exam;
import com.rbac.model.exam.ExamStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamResponse {

    private String examId;
    private String examName;
    private String course;
    private String batchId;
    private String batchName;
    private int durationMinutes;
    private double totalMarks;
    private double passingMarks;
    private Instant examDate;
    private List<QuestionDto> questions;
    private ExamStatus status;
    private long totalStudents;
    private String createdBy;
    private String createdByName;
    private Instant createdAt;
    private Instant updatedAt;

    public static ExamResponse fromEntity(Exam exam, long totalStudents, boolean forStudent) {
        List<QuestionDto> questionDtos = exam.getQuestions() == null ? List.of() :
                exam.getQuestions().stream()
                        .map(forStudent ? QuestionDto::fromEntityForStudent : QuestionDto::fromEntity)
                        .toList();

        return ExamResponse.builder()
                .examId(exam.getId())
                .examName(exam.getExamName())
                .course(exam.getCourse())
                .batchId(exam.getBatchId())
                .batchName(exam.getBatchName())
                .durationMinutes(exam.getDurationMinutes())
                .totalMarks(exam.getTotalMarks())
                .passingMarks(exam.getPassingMarks())
                .examDate(exam.getExamDate())
                .questions(questionDtos)
                .status(ExamStatusResolver.resolve(exam))
                .totalStudents(totalStudents)
                .createdBy(exam.getCreatedBy())
                .createdByName(exam.getCreatedByName())
                .createdAt(exam.getCreatedAt())
                .updatedAt(exam.getUpdatedAt())
                .build();
    }
}
