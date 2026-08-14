package com.rbac.service.assignment;

import com.rbac.dto.assignment.*;
import com.rbac.exception.assignment.InvalidRequestException;
import com.rbac.exception.assignment.ResourceNotFoundException;
import com.rbac.exception.assignment.UnauthorizedActionException;
import com.rbac.model.assignment.Assignment;
import com.rbac.model.assignment.AssignmentStatus;
import com.rbac.model.assignment.AssignmentSubmission;
import com.rbac.model.assignment.SubmissionStatus;
import com.rbac.model.batch.Batch;
import com.rbac.model.login.Role;
import com.rbac.model.login.User;
import com.rbac.repository.BatchRepository;
import com.rbac.repository.UserRepository;
import com.rbac.repository.assignment.AssignmentRepository;
import com.rbac.repository.assignment.AssignmentSubmissionRepository;
import com.rbac.security.chat.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public interface AssignmentService {

    AssignmentResponse createAssignment(CreateAssignmentRequest request, AuthenticatedUser user);

    AssignmentResponse updateAssignment(String assignmentId, UpdateAssignmentRequest request, AuthenticatedUser user);

    void deleteAssignment(String assignmentId, AuthenticatedUser user);

    AssignmentResponse getAssignment(String assignmentId, AuthenticatedUser user);

    List<AssignmentResponse> getAllAssignments(String course, String batchId, AssignmentStatus status, AuthenticatedUser user);

    List<AssignmentResponse> getAvailableAssignmentsForStudent(AuthenticatedUser user);

    AssignmentResponse publishAssignment(String assignmentId, AuthenticatedUser user);

    AssignmentResponse unpublishAssignment(String assignmentId, AuthenticatedUser user);

    AssignmentResponse closeAssignment(String assignmentId, AuthenticatedUser user);

    SubmissionResponse submitAssignment(String assignmentId, SubmitAssignmentRequest request, AuthenticatedUser user);

    SubmissionResponse getMySubmission(String assignmentId, AuthenticatedUser user);

    List<SubmissionResponse> getSubmissions(String assignmentId, AuthenticatedUser user);

    SubmissionResponse gradeSubmission(String assignmentId, String studentId, GradeSubmissionRequest request, AuthenticatedUser user);

    AssignmentAnalyticsResponse getAnalytics(String assignmentId, AuthenticatedUser user);
}

@Slf4j
@Service
@RequiredArgsConstructor
class AssignmentServiceImpl implements AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final BatchRepository batchRepository;
    private final UserRepository userRepository;

    @Override
    public AssignmentResponse createAssignment(CreateAssignmentRequest request, AuthenticatedUser user) {
        requireTrainerOrAdmin(user, "create an assignment");

        Batch batch = batchRepository.findById(request.getBatchId())
                .orElseThrow(() -> new InvalidRequestException("Batch not found: " + request.getBatchId()));

        Assignment assignment = new Assignment();
        assignment.setTitle(request.getTitle());
        assignment.setCourse(request.getCourse());
        assignment.setBatchId(batch.getId());
        assignment.setBatchName(batch.getName());
        assignment.setDescription(request.getDescription());
        assignment.setInstructions(request.getInstructions());
        assignment.setTotalMarks(request.getTotalMarks());
        assignment.setDueDate(request.getDueDate());
        assignment.setAttachments(toAttachments(request.getAttachments()));
        assignment.setStatus(AssignmentStatus.DRAFT);
        assignment.setCreatedBy(user.getUserId());
        assignment.setCreatedByName(user.getUserName());
        assignment.setCreatedAt(Instant.now());
        assignment.setUpdatedAt(Instant.now());

        Assignment saved = assignmentRepository.save(assignment);
        log.info("Assignment {} created by {}", saved.getId(), user.getUserId());
        return toResponse(saved);
    }

    @Override
    public AssignmentResponse updateAssignment(String assignmentId, UpdateAssignmentRequest request, AuthenticatedUser user) {
        requireTrainerOrAdmin(user, "update an assignment");
        Assignment assignment = findOrThrow(assignmentId);

        if (assignment.getStatus() == AssignmentStatus.COMPLETED) {
            throw new InvalidRequestException("Cannot update an assignment that has already been completed");
        }

        Batch batch = batchRepository.findById(request.getBatchId())
                .orElseThrow(() -> new InvalidRequestException("Batch not found: " + request.getBatchId()));

        assignment.setTitle(request.getTitle());
        assignment.setCourse(request.getCourse());
        assignment.setBatchId(batch.getId());
        assignment.setBatchName(batch.getName());
        assignment.setDescription(request.getDescription());
        assignment.setInstructions(request.getInstructions());
        assignment.setTotalMarks(request.getTotalMarks());
        assignment.setDueDate(request.getDueDate());
        assignment.setAttachments(toAttachments(request.getAttachments()));
        assignment.setUpdatedAt(Instant.now());

        Assignment saved = assignmentRepository.save(assignment);
        log.info("Assignment {} updated by {}", assignmentId, user.getUserId());
        return toResponse(saved);
    }

    @Override
    public void deleteAssignment(String assignmentId, AuthenticatedUser user) {
        requireTrainerOrAdmin(user, "delete an assignment");
        Assignment assignment = findOrThrow(assignmentId);

        submissionRepository.deleteByAssignmentId(assignmentId);
        assignmentRepository.deleteById(assignment.getId());
        log.info("Assignment {} deleted by {}", assignmentId, user.getUserId());
    }

    @Override
    public AssignmentResponse getAssignment(String assignmentId, AuthenticatedUser user) {
        Assignment assignment = findOrThrow(assignmentId);

        if (isStudent(user)) {
            assertStudentCanView(assignment, user);
        }
        return toResponse(assignment);
    }

    @Override
    public List<AssignmentResponse> getAllAssignments(String course, String batchId, AssignmentStatus status, AuthenticatedUser user) {
        requireTrainerOrAdmin(user, "view assignments");

        List<Assignment> assignments = assignmentRepository.findAll();
        return assignments.stream()
                .filter(a -> course == null || course.isBlank() || course.equalsIgnoreCase(a.getCourse()))
                .filter(a -> batchId == null || batchId.isBlank() || batchId.equals(a.getBatchId()))
                .filter(a -> status == null || AssignmentStatusResolver.resolve(a) == status)
                .sorted(Comparator.comparing(Assignment::getDueDate).reversed())
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<AssignmentResponse> getAvailableAssignmentsForStudent(AuthenticatedUser user) {
        User student = userRepository.findById(user.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        List<String> batchIds = student.getBatchIds() == null ? List.of() : student.getBatchIds().stream().toList();
        if (batchIds.isEmpty()) {
            return List.of();
        }

        List<Assignment> assignments = assignmentRepository.findByBatchIdIn(batchIds);
        return assignments.stream()
                .filter(a -> {
                    AssignmentStatus effective = AssignmentStatusResolver.resolve(a);
                    return effective == AssignmentStatus.OPEN || effective == AssignmentStatus.COMPLETED;
                })
                .sorted(Comparator.comparing(Assignment::getDueDate))
                .map(this::toResponse)
                .toList();
    }

    @Override
    public AssignmentResponse publishAssignment(String assignmentId, AuthenticatedUser user) {
        requireTrainerOrAdmin(user, "publish an assignment");
        Assignment assignment = findOrThrow(assignmentId);

        assignment.setStatus(AssignmentStatus.PUBLISHED);
        assignment.setUpdatedAt(Instant.now());
        Assignment saved = assignmentRepository.save(assignment);
        log.info("Assignment {} published by {}", assignmentId, user.getUserId());
        return toResponse(saved);
    }

    @Override
    public AssignmentResponse unpublishAssignment(String assignmentId, AuthenticatedUser user) {
        requireTrainerOrAdmin(user, "unpublish an assignment");
        Assignment assignment = findOrThrow(assignmentId);

        assignment.setStatus(AssignmentStatus.DRAFT);
        assignment.setUpdatedAt(Instant.now());
        Assignment saved = assignmentRepository.save(assignment);
        log.info("Assignment {} unpublished by {}", assignmentId, user.getUserId());
        return toResponse(saved);
    }

    @Override
    public AssignmentResponse closeAssignment(String assignmentId, AuthenticatedUser user) {
        requireTrainerOrAdmin(user, "close an assignment");
        Assignment assignment = findOrThrow(assignmentId);

        assignment.setStatus(AssignmentStatus.CLOSED);
        assignment.setUpdatedAt(Instant.now());
        Assignment saved = assignmentRepository.save(assignment);
        log.info("Assignment {} closed by {}", assignmentId, user.getUserId());
        return toResponse(saved);
    }

    @Override
    public SubmissionResponse submitAssignment(String assignmentId, SubmitAssignmentRequest request, AuthenticatedUser user) {
        if (!isStudent(user)) {
            throw new UnauthorizedActionException("Only students can submit assignments");
        }

        Assignment assignment = findOrThrow(assignmentId);
        assertStudentCanView(assignment, user);

        if (assignment.getStatus() == AssignmentStatus.CLOSED || assignment.getStatus() == AssignmentStatus.DRAFT) {
            throw new InvalidRequestException("This assignment is not currently open for submission");
        }

        if ((request.getAnswerText() == null || request.getAnswerText().isBlank())
                && (request.getFiles() == null || request.getFiles().isEmpty())) {
            throw new InvalidRequestException("Submission must include answer text or at least one file");
        }

        submissionRepository.findByAssignmentIdAndStudentId(assignmentId, user.getUserId())
                .ifPresent(existing -> {
                    throw new InvalidRequestException("You have already submitted this assignment");
                });

        boolean late = AssignmentStatusResolver.isPastDue(assignment);

        AssignmentSubmission submission = new AssignmentSubmission();
        submission.setId(UUID.randomUUID().toString());
        submission.setAssignmentId(assignmentId);
        submission.setStudentId(user.getUserId());
        submission.setStudentName(user.getUserName());
        submission.setAnswerText(request.getAnswerText());
        submission.setFiles(toAttachments(request.getFiles()));
        submission.setSubmittedAt(Instant.now());
        submission.setLate(late);
        submission.setStatus(late ? SubmissionStatus.LATE : SubmissionStatus.SUBMITTED);

        AssignmentSubmission saved = submissionRepository.save(submission);
        log.info("Student {} submitted assignment {} (late={})", user.getUserId(), assignmentId, late);
        return SubmissionResponse.fromEntity(saved);
    }

    @Override
    public SubmissionResponse getMySubmission(String assignmentId, AuthenticatedUser user) {
        if (!isStudent(user)) {
            throw new UnauthorizedActionException("Only students can view their own submission");
        }
        findOrThrow(assignmentId);

        return submissionRepository.findByAssignmentIdAndStudentId(assignmentId, user.getUserId())
                .map(SubmissionResponse::fromEntity)
                .orElseGet(() -> SubmissionResponse.notSubmitted(user.getUserId(), user.getUserName()));
    }

    @Override
    public List<SubmissionResponse> getSubmissions(String assignmentId, AuthenticatedUser user) {
        requireTrainerOrAdmin(user, "view assignment submissions");
        Assignment assignment = findOrThrow(assignmentId);

        List<AssignmentSubmission> submissions = submissionRepository.findByAssignmentId(assignmentId);
        Map<String, AssignmentSubmission> byStudent = submissions.stream()
                .collect(Collectors.toMap(AssignmentSubmission::getStudentId, s -> s));

        List<User> enrolled = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.STUDENT)
                .filter(u -> u.getBatchIds() != null && u.getBatchIds().contains(assignment.getBatchId()))
                .toList();

        return enrolled.stream()
                .map(stu -> {
                    AssignmentSubmission s = byStudent.get(stu.getId());
                    return s != null
                            ? SubmissionResponse.fromEntity(s)
                            : SubmissionResponse.notSubmitted(stu.getId(), stu.getDisplayName());
                })
                .sorted(Comparator.comparing(SubmissionResponse::getStudentName,
                        Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    @Override
    public SubmissionResponse gradeSubmission(String assignmentId, String studentId, GradeSubmissionRequest request, AuthenticatedUser user) {
        requireTrainerOrAdmin(user, "grade a submission");
        Assignment assignment = findOrThrow(assignmentId);

        AssignmentSubmission submission = submissionRepository.findByAssignmentIdAndStudentId(assignmentId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("No submission found for this student"));

        if (request.getObtainedMarks() > assignment.getTotalMarks()) {
            throw new InvalidRequestException("obtainedMarks cannot exceed the assignment's totalMarks");
        }

        double percentage = assignment.getTotalMarks() == 0 ? 0.0
                : (request.getObtainedMarks() / assignment.getTotalMarks()) * 100.0;

        submission.setObtainedMarks(request.getObtainedMarks());
        submission.setPercentage(round(percentage));
        submission.setGrade(computeGrade(percentage));
        submission.setFeedback(request.getFeedback());
        submission.setStatus(SubmissionStatus.GRADED);
        submission.setGradedBy(user.getUserId());
        submission.setGradedAt(Instant.now());

        AssignmentSubmission saved = submissionRepository.save(submission);
        log.info("Assignment {} submission for student {} graded by {}", assignmentId, studentId, user.getUserId());
        return SubmissionResponse.fromEntity(saved);
    }

    @Override
    public AssignmentAnalyticsResponse getAnalytics(String assignmentId, AuthenticatedUser user) {
        requireTrainerOrAdmin(user, "view assignment analytics");
        Assignment assignment = findOrThrow(assignmentId);

        long totalStudents = userRepository.countByBatchIdsContaining(assignment.getBatchId());
        List<AssignmentSubmission> submissions = submissionRepository.findByAssignmentId(assignmentId);

        long submittedCount = submissions.size();
        long lateCount = submissions.stream().filter(AssignmentSubmission::isLate).count();
        long gradedCount = submissions.stream().filter(s -> s.getStatus() == SubmissionStatus.GRADED).count();
        long pendingCount = submittedCount - gradedCount;

        List<AssignmentSubmission> graded = submissions.stream()
                .filter(s -> s.getObtainedMarks() != null)
                .toList();

        double averageMarks = graded.stream().mapToDouble(AssignmentSubmission::getObtainedMarks).average().orElse(0.0);
        double highestMarks = graded.stream().mapToDouble(AssignmentSubmission::getObtainedMarks).max().orElse(0.0);
        double lowestMarks = graded.stream().mapToDouble(AssignmentSubmission::getObtainedMarks).min().orElse(0.0);
        double submissionPercentage = totalStudents == 0 ? 0.0 : round((submittedCount * 100.0) / totalStudents);

        return AssignmentAnalyticsResponse.builder()
                .assignmentId(assignment.getId())
                .title(assignment.getTitle())
                .totalStudents(totalStudents)
                .submittedCount(submittedCount)
                .pendingCount(pendingCount)
                .lateCount(lateCount)
                .gradedCount(gradedCount)
                .averageMarks(round(averageMarks))
                .highestMarks(round(highestMarks))
                .lowestMarks(round(lowestMarks))
                .submissionPercentage(submissionPercentage)
                .build();
    }

    // ---------- helpers ----------

    private Assignment findOrThrow(String assignmentId) {
        return assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + assignmentId));
    }

    private void requireTrainerOrAdmin(AuthenticatedUser user, String action) {
        if (!user.hasRole(Role.ADMIN) && !user.hasRole(Role.TEACHER)) {
            throw new UnauthorizedActionException("Only an admin or trainer can " + action);
        }
    }

    private boolean isStudent(AuthenticatedUser user) {
        return user.hasRole(Role.STUDENT) && !user.hasRole(Role.ADMIN) && !user.hasRole(Role.TEACHER);
    }

    private void assertStudentCanView(Assignment assignment, AuthenticatedUser user) {
        User student = userRepository.findById(user.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        boolean inBatch = student.getBatchIds() != null && student.getBatchIds().contains(assignment.getBatchId());
        if (!inBatch) {
            throw new UnauthorizedActionException("You are not enrolled in the batch for this assignment");
        }

        AssignmentStatus effective = AssignmentStatusResolver.resolve(assignment);
        if (effective == AssignmentStatus.DRAFT) {
            throw new UnauthorizedActionException("This assignment is not currently available");
        }
    }

    private List<com.rbac.model.assignment.AttachmentRef> toAttachments(List<AttachmentDto> dtos) {
        if (dtos == null) {
            return List.of();
        }
        return dtos.stream().map(AttachmentDto::toEntity).toList();
    }

    private AssignmentResponse toResponse(Assignment assignment) {
        long totalStudents = userRepository.countByBatchIdsContaining(assignment.getBatchId());
        List<AssignmentSubmission> submissions = submissionRepository.findByAssignmentId(assignment.getId());
        long submittedCount = submissions.size();
        long gradedCount = submissions.stream().filter(s -> s.getStatus() == SubmissionStatus.GRADED).count();
        long lateCount = submissions.stream().filter(AssignmentSubmission::isLate).count();
        return AssignmentResponse.fromEntity(assignment, totalStudents, submittedCount, gradedCount, lateCount);
    }

    private String computeGrade(double percentage) {
        if (percentage >= 90) return "A+";
        if (percentage >= 80) return "A";
        if (percentage >= 70) return "B+";
        if (percentage >= 60) return "B";
        if (percentage >= 50) return "C";
        if (percentage >= 40) return "D";
        return "F";
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}