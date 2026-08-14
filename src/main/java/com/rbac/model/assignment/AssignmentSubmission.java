package com.rbac.model.assignment;

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
@Document(collection = "assignment_submissions")
public class AssignmentSubmission {

    @Id
    private String id;

    @Indexed
    private String assignmentId;

    @Indexed
    private String studentId;

    private String studentName;

    private String answerText;

    private List<AttachmentRef> files = new ArrayList<>();

    private Instant submittedAt;

    private boolean late;

    private SubmissionStatus status = SubmissionStatus.NOT_SUBMITTED;

    // grading
    private Double obtainedMarks;
    private Double percentage;
    private String grade;
    private String feedback;
    private String gradedBy;
    private Instant gradedAt;
}