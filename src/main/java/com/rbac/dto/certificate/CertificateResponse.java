package com.rbac.dto.certificate;

import com.rbac.model.certificate.Certificate;
import com.rbac.model.certificate.CertificateStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CertificateResponse {

    private String id;
    private String certificateId;
    private String verificationCode;

    private String studentId;
    private String studentName;

    private String courseId;
    private String courseName;

    private LocalDate completionDate;
    private LocalDate issueDate;

    private CertificateStatus status;

    private String batchId;
    private String issuedBy;
    private String issuedByName;
    private String remarks;

    private Instant createdAt;
    private Instant updatedAt;

    public static CertificateResponse fromEntity(Certificate c) {
        return new CertificateResponse(
                c.getId(),
                c.getCertificateId(),
                c.getVerificationCode(),
                c.getStudentId(),
                c.getStudentName(),
                c.getCourseId(),
                c.getCourseName(),
                c.getCompletionDate(),
                c.getIssueDate(),
                c.getStatus(),
                c.getBatchId(),
                c.getIssuedBy(),
                c.getIssuedByName(),
                c.getRemarks(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}
