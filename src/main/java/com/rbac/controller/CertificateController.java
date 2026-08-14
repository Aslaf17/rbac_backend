package com.rbac.controller;

import com.rbac.dto.certificate.CertificateResponse;
import com.rbac.dto.certificate.GenerateCertificateRequest;
import com.rbac.dto.certificate.VerifyCertificateResponse;
import com.rbac.security.chat.AuthenticatedUser;
import com.rbac.security.chat.CurrentUserProvider;
import com.rbac.service.certificate.CertificateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/certificates")
@RequiredArgsConstructor
public class CertificateController {

    private final CertificateService certificateService;
    private final CurrentUserProvider currentUserProvider;

    // ---- Admin/Trainer: Generate ----

    @PostMapping
    public ResponseEntity<CertificateResponse> generateCertificate(
            @Valid @RequestBody GenerateCertificateRequest request) {
        AuthenticatedUser user = currentUserProvider.getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(certificateService.generateCertificate(request, user));
    }

    // ---- List / view ----

    @GetMapping
    public ResponseEntity<List<CertificateResponse>> getAllCertificates(
            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) String courseId) {
        AuthenticatedUser user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(certificateService.getAllCertificates(studentId, courseId, user));
    }

    @GetMapping("/{certificateId}")
    public ResponseEntity<CertificateResponse> getCertificate(@PathVariable String certificateId) {
        AuthenticatedUser user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(certificateService.getCertificate(certificateId, user));
    }

    // ---- Verify (public, no auth required — see SecurityConfig) ----

    @GetMapping("/verify")
    public ResponseEntity<VerifyCertificateResponse> verifyCertificate(
            @RequestParam("code") String certificateIdOrCode) {
        return ResponseEntity.ok(certificateService.verifyCertificate(certificateIdOrCode));
    }

    // ---- Download PDF ----

    @GetMapping("/{certificateId}/download")
    public ResponseEntity<byte[]> downloadCertificate(@PathVariable String certificateId) {
        AuthenticatedUser user = currentUserProvider.getCurrentUser();
        byte[] pdfBytes = certificateService.downloadCertificatePdf(certificateId, user);
        String fileName = certificateService.buildDownloadFileName(certificateId);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(pdfBytes);
    }

    // ---- Admin: Revoke ----

    @PatchMapping("/{certificateId}/revoke")
    public ResponseEntity<CertificateResponse> revokeCertificate(@PathVariable String certificateId) {
        AuthenticatedUser user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(certificateService.revokeCertificate(certificateId, user));
    }
}
