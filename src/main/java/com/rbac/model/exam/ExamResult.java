package com.rbac.model.exam;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "exam_results")
public class ExamResult {

    @Id
    private String id;

    @Indexed
    private String examId;

    @Indexed
    private String studentId;

    private String studentName;

    // questionId -> selected option index
    private Map<String, Integer> answers = new HashMap<>();

    private double obtainedMarks;

    private double totalMarks;

    private double percentage;

    private String grade;

    private boolean passed;

    private boolean completed;

    private Instant startedAt = Instant.now();

    private Instant submittedAt;
}
