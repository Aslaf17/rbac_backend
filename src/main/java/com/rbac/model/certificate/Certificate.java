package com.rbac.model.certificate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "certificates")
public class Certificate {

    @Id
    private String id;

    @Indexed(unique = true)
    private String certificateId;

    @Indexed(unique = true)
    private String verificationCode;

    @Indexed
    private String studentId;
    private String studentName;

    @Indexed
    private String courseId;
    private String courseName;

    private LocalDate completionDate;
    private LocalDate issueDate;

    @Indexed
    private CertificateStatus status = CertificateStatus.ACTIVE;

    private String batchId;

    private String issuedBy;
    private String issuedByName;

    private String remarks;

    private String pdfStoragePath;

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
}
