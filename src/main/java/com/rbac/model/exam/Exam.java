package com.rbac.model.exam;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "exams")
public class Exam {

    @Id
    private String id;

    @Indexed
    private String examName;

    private String course;

    @Indexed
    private String batchId;

    private String batchName;

    private int durationMinutes;

    private double totalMarks;

    private double passingMarks;

    private Instant examDate;

    private List<Question> questions = new ArrayList<>();

    @Indexed
    private ExamStatus status = ExamStatus.DRAFT;

    private String createdBy;
    private String createdByName;

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
}
