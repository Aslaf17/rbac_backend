package com.rbac.dto.certificate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerifyCertificateResponse {

    private boolean valid;
    private String message;

    private String certificateId;
    private String studentName;
    private String courseName;
    private LocalDate completionDate;
    private LocalDate issueDate;
    private String status;

    public static VerifyCertificateResponse invalid(String message) {
        return new VerifyCertificateResponse(false, message, null, null, null, null, null, null);
    }
}
