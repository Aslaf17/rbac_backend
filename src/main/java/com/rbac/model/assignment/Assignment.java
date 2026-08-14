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
@Document(collection = "assignments")
public class Assignment {

    @Id
    private String id;

    @Indexed
    private String title;

    private String course;

    @Indexed
    private String batchId;

    private String batchName;

    private String description;

    private String instructions;

    private double totalMarks;

    private Instant dueDate;

    private List<AttachmentRef> attachments = new ArrayList<>();

    @Indexed
    private AssignmentStatus status = AssignmentStatus.DRAFT;

    private String createdBy;
    private String createdByName;

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
}