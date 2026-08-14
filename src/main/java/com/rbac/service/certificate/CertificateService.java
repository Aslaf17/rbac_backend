package com.rbac.service.certificate;

import com.rbac.dto.certificate.CertificateResponse;
import com.rbac.dto.certificate.GenerateCertificateRequest;
import com.rbac.dto.certificate.VerifyCertificateResponse;
import com.rbac.exception.certificate.InvalidRequestException;
import com.rbac.exception.certificate.ResourceNotFoundException;
import com.rbac.exception.certificate.UnauthorizedActionException;
import com.rbac.model.certificate.Certificate;
import com.rbac.model.certificate.CertificateStatus;
import com.rbac.model.course.Course;
import com.rbac.model.login.Role;
import com.rbac.model.login.User;
import com.rbac.repository.CourseRepository;
import com.rbac.repository.UserRepository;
import com.rbac.repository.CertificateRepository;
import com.rbac.security.chat.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CertificateService {

    private final CertificateRepository certificateRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final CertificatePdfGenerator pdfGenerator;

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no O/0/I/1 ambiguity
    private static final SecureRandom RANDOM = new SecureRandom();

    // ---------------- Generate ----------------

    public CertificateResponse generateCertificate(GenerateCertificateRequest request, AuthenticatedUser currentUser) {
        if (!currentUser.isTrainerOrAdmin()) {
            throw new UnauthorizedActionException("Only Admin/Trainer can generate certificates");
        }

        User student = userRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + request.getStudentId()));

        if (student.getRole() != Role.STUDENT) {
            throw new InvalidRequestException("Certificates can only be generated for students");
        }

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + request.getCourseId()));

        if (certificateRepository.existsByStudentIdAndCourseId(request.getStudentId(), request.getCourseId())) {
            throw new InvalidRequestException("A certificate already exists for this student and course");
        }

        if (request.getCompletionDate() == null) {
            throw new InvalidRequestException("completionDate is required");
        }

        LocalDate issueDate = request.getIssueDate() != null ? request.getIssueDate() : LocalDate.now();
        if (issueDate.isBefore(request.getCompletionDate())) {
            throw new InvalidRequestException("issueDate cannot be before completionDate");
        }

        Certificate certificate = new Certificate();
        certificate.setCertificateId(generateCertificateId());
        certificate.setVerificationCode(generateVerificationCode());
        certificate.setStudentId(student.getId());
        certificate.setStudentName(student.getDisplayName() != null ? student.getDisplayName() : student.getUsername());
        certificate.setCourseId(course.getId());
        certificate.setCourseName(course.getTitle());
        certificate.setCompletionDate(request.getCompletionDate());
        certificate.setIssueDate(issueDate);
        certificate.setStatus(CertificateStatus.ACTIVE);
        certificate.setBatchId(request.getBatchId());
        certificate.setIssuedBy(currentUser.getUserId());
        certificate.setIssuedByName(currentUser.getUserName());
        certificate.setRemarks(request.getRemarks());
        certificate.setCreatedAt(Instant.now());
        certificate.setUpdatedAt(Instant.now());

        certificate = certificateRepository.save(certificate);
        return CertificateResponse.fromEntity(certificate);
    }

    // ---------------- Read / list ----------------

    public List<CertificateResponse> getAllCertificates(String studentId, String courseId, AuthenticatedUser currentUser) {
        List<Certificate> certificates;

        if (currentUser.hasRole(Role.STUDENT)) {
            // students may only ever see their own certificates
            certificates = certificateRepository.findByStudentId(currentUser.getUserId());
        } else if (studentId != null) {
            certificates = certificateRepository.findByStudentId(studentId);
        } else if (courseId != null) {
            certificates = certificateRepository.findByCourseId(courseId);
        } else {
            certificates = certificateRepository.findAll();
        }

        return certificates.stream()
                .map(CertificateResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public CertificateResponse getCertificate(String certificateId, AuthenticatedUser currentUser) {
        Certificate certificate = findByCertificateIdOrThrow(certificateId);
        assertViewAccess(certificate, currentUser);
        return CertificateResponse.fromEntity(certificate);
    }

    // ---------------- Verify (public-safe, read only) ----------------

    public VerifyCertificateResponse verifyCertificate(String idOrCode) {
        if (idOrCode == null || idOrCode.isBlank()) {
            return VerifyCertificateResponse.invalid("Certificate ID or verification code is required");
        }

        Certificate certificate = certificateRepository.findByCertificateId(idOrCode)
                .or(() -> certificateRepository.findByVerificationCode(idOrCode))
                .orElse(null);

        if (certificate == null) {
            return VerifyCertificateResponse.invalid("No certificate found for the given ID/code");
        }

        if (certificate.getStatus() == CertificateStatus.REVOKED) {
            return new VerifyCertificateResponse(
                    false,
                    "This certificate has been revoked",
                    certificate.getCertificateId(),
                    certificate.getStudentName(),
                    certificate.getCourseName(),
                    certificate.getCompletionDate(),
                    certificate.getIssueDate(),
                    certificate.getStatus().name());
        }

        return new VerifyCertificateResponse(
                true,
                "Certificate is valid",
                certificate.getCertificateId(),
                certificate.getStudentName(),
                certificate.getCourseName(),
                certificate.getCompletionDate(),
                certificate.getIssueDate(),
                certificate.getStatus().name());
    }

    // ---------------- Download PDF ----------------

    public byte[] downloadCertificatePdf(String certificateId, AuthenticatedUser currentUser) {
        Certificate certificate = findByCertificateIdOrThrow(certificateId);
        assertViewAccess(certificate, currentUser);
        return pdfGenerator.generate(certificate);
    }

    public String buildDownloadFileName(String certificateId) {
        return "Certificate-" + certificateId + ".pdf";
    }

    // ---------------- Revoke (bonus admin action) ----------------

    public CertificateResponse revokeCertificate(String certificateId, AuthenticatedUser currentUser) {
        if (!currentUser.hasRole(Role.ADMIN)) {
            throw new UnauthorizedActionException("Only Admin can revoke certificates");
        }
        Certificate certificate = findByCertificateIdOrThrow(certificateId);
        certificate.setStatus(CertificateStatus.REVOKED);
        certificate.setUpdatedAt(Instant.now());
        certificate = certificateRepository.save(certificate);
        return CertificateResponse.fromEntity(certificate);
    }

    // ---------------- Helpers ----------------

    private Certificate findByCertificateIdOrThrow(String certificateId) {
        return certificateRepository.findByCertificateId(certificateId)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate not found: " + certificateId));
    }

    private void assertViewAccess(Certificate certificate, AuthenticatedUser currentUser) {
        if (currentUser.isTrainerOrAdmin()) {
            return;
        }
        if (currentUser.hasRole(Role.STUDENT) && certificate.getStudentId().equals(currentUser.getUserId())) {
            return;
        }
        throw new UnauthorizedActionException("You are not allowed to access this certificate");
    }

    private String generateCertificateId() {
        String candidate;
        do {
            int year = Year.now().getValue();
            String randomPart = String.format("%06d", RANDOM.nextInt(1_000_000));
            candidate = "CERT-" + year + "-" + randomPart;
        } while (certificateRepository.existsByCertificateId(candidate));
        return candidate;
    }

    private String generateVerificationCode() {
        String candidate;
        do {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 10; i++) {
                sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
            }
            candidate = sb.toString();
        } while (certificateRepository.existsByVerificationCode(candidate));
        return candidate;
    }
}
