package com.rbac.dto.assignment;

import com.rbac.model.assignment.AssignmentSubmission;
import com.rbac.model.assignment.SubmissionStatus;
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
public class SubmissionResponse {

    private String submissionId;
    private String assignmentId;
    private String studentId;
    private String studentName;
    private String answerText;
    private List<AttachmentDto> files;
    private Instant submittedAt;
    private boolean late;
    private SubmissionStatus status;
    private Double obtainedMarks;
    private Double percentage;
    private String grade;
    private String feedback;
    private Instant gradedAt;

    public static SubmissionResponse fromEntity(AssignmentSubmission s) {
        List<AttachmentDto> fileDtos = s.getFiles() == null ? List.of() :
                s.getFiles().stream().map(AttachmentDto::fromEntity).toList();

        return SubmissionResponse.builder()
                .submissionId(s.getId())
                .assignmentId(s.getAssignmentId())
                .studentId(s.getStudentId())
                .studentName(s.getStudentName())
                .answerText(s.getAnswerText())
                .files(fileDtos)
                .submittedAt(s.getSubmittedAt())
                .late(s.isLate())
                .status(s.getStatus())
                .obtainedMarks(s.getObtainedMarks())
                .percentage(s.getPercentage())
                .grade(s.getGrade())
                .feedback(s.getFeedback())
                .gradedAt(s.getGradedAt())
                .build();
    }

    public static SubmissionResponse notSubmitted(String studentId, String studentName) {
        return SubmissionResponse.builder()
                .studentId(studentId)
                .studentName(studentName)
                .status(SubmissionStatus.NOT_SUBMITTED)
                .late(false)
                .build();
    }
}