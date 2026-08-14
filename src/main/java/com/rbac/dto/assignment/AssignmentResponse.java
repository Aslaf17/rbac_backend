package com.rbac.dto.assignment;

import com.rbac.model.assignment.Assignment;
import com.rbac.model.assignment.AssignmentStatus;
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
public class AssignmentResponse {

    private String assignmentId;
    private String title;
    private String course;
    private String batchId;
    private String batchName;
    private String description;
    private String instructions;
    private double totalMarks;
    private Instant dueDate;
    private List<AttachmentDto> attachments;
    private AssignmentStatus status;
    private long totalStudents;
    private long submittedCount;
    private long gradedCount;
    private long lateCount;
    private String createdBy;
    private String createdByName;
    private Instant createdAt;
    private Instant updatedAt;

    public static AssignmentResponse fromEntity(Assignment a, long totalStudents,
                                                long submittedCount, long gradedCount, long lateCount) {
        List<AttachmentDto> attachmentDtos = a.getAttachments() == null ? List.of() :
                a.getAttachments().stream().map(AttachmentDto::fromEntity).toList();

        return AssignmentResponse.builder()
                .assignmentId(a.getId())
                .title(a.getTitle())
                .course(a.getCourse())
                .batchId(a.getBatchId())
                .batchName(a.getBatchName())
                .description(a.getDescription())
                .instructions(a.getInstructions())
                .totalMarks(a.getTotalMarks())
                .dueDate(a.getDueDate())
                .attachments(attachmentDtos)
                .status(AssignmentStatusResolver.resolve(a))
                .totalStudents(totalStudents)
                .submittedCount(submittedCount)
                .gradedCount(gradedCount)
                .lateCount(lateCount)
                .createdBy(a.getCreatedBy())
                .createdByName(a.getCreatedByName())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}