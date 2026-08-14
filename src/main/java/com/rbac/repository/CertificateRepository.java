package com.rbac.repository;

import com.rbac.model.certificate.Certificate;
import com.rbac.model.certificate.CertificateStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface CertificateRepository extends MongoRepository<Certificate, String> {

    Optional<Certificate> findByCertificateId(String certificateId);

    Optional<Certificate> findByVerificationCode(String verificationCode);

    boolean existsByCertificateId(String certificateId);

    boolean existsByVerificationCode(String verificationCode);

    Optional<Certificate> findByStudentIdAndCourseId(String studentId, String courseId);

    boolean existsByStudentIdAndCourseId(String studentId, String courseId);

    List<Certificate> findByStudentId(String studentId);

    List<Certificate> findByCourseId(String courseId);

    List<Certificate> findByStatus(CertificateStatus status);

    List<Certificate> findByBatchId(String batchId);
}
