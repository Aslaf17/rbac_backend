package com.rbac.service.exam;

import com.rbac.dto.exam.*;
import com.rbac.exception.exam.InvalidRequestException;
import com.rbac.exception.exam.ResourceNotFoundException;
import com.rbac.exception.exam.UnauthorizedActionException;
import com.rbac.model.batch.Batch;
import com.rbac.model.exam.Exam;
import com.rbac.model.exam.ExamResult;
import com.rbac.model.exam.ExamStatus;
import com.rbac.model.exam.Question;
import com.rbac.model.login.Role;
import com.rbac.repository.BatchRepository;
import com.rbac.repository.exam.ExamRepository;
import com.rbac.repository.exam.ExamResultRepository;
import com.rbac.repository.UserRepository;
import com.rbac.security.chat.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ExamService {

    ExamResponse createExam(CreateExamRequest request, AuthenticatedUser user);

    ExamResponse updateExam(String examId, UpdateExamRequest request, AuthenticatedUser user);

    void deleteExam(String examId, AuthenticatedUser user);

    ExamResponse getExam(String examId, AuthenticatedUser user);

    List<ExamResponse> getAllExams(String course, String batchId, ExamStatus status, AuthenticatedUser user);

    List<ExamResponse> getAvailableExamsForStudent(AuthenticatedUser user);

    ExamResponse publishExam(String examId, AuthenticatedUser user);

    ExamResponse unpublishExam(String examId, AuthenticatedUser user);

    ExamAnalyticsResponse getAnalytics(String examId, AuthenticatedUser user);

    List<ExamResultSummaryResponse> getResultSummary(String examId, AuthenticatedUser user);

    ExamResultSummaryResponse submitExam(String examId, SubmitExamRequest request, AuthenticatedUser user);
}

@Slf4j
@Service
@RequiredArgsConstructor
class ExamServiceImpl implements ExamService {

    private final ExamRepository examRepository;
    private final ExamResultRepository examResultRepository;
    private final BatchRepository batchRepository;
    private final UserRepository userRepository;

    @Override
    public ExamResponse createExam(CreateExamRequest request, AuthenticatedUser user) {
        requireTrainerOrAdmin(user, "create an exam");

        Batch batch = batchRepository.findById(request.getBatchId())
                .orElseThrow(() -> new InvalidRequestException("Batch not found: " + request.getBatchId()));

        if (request.getPassingMarks() > request.getTotalMarks()) {
            throw new InvalidRequestException("passingMarks cannot exceed totalMarks");
        }

        Exam exam = new Exam();
        exam.setExamName(request.getExamName());
        exam.setCourse(request.getCourse());
        exam.setBatchId(batch.getId());
        exam.setBatchName(batch.getName());
        exam.setDurationMinutes(request.getDurationMinutes());
        exam.setTotalMarks(request.getTotalMarks());
        exam.setPassingMarks(request.getPassingMarks());
        exam.setExamDate(request.getExamDate());
        exam.setQuestions(toQuestions(request.getQuestions()));
        exam.setStatus(ExamStatus.DRAFT);
        exam.setCreatedBy(user.getUserId());
        exam.setCreatedByName(user.getUserName());
        exam.setCreatedAt(Instant.now());
        exam.setUpdatedAt(Instant.now());

        Exam saved = examRepository.save(exam);
        log.info("Exam {} created by {}", saved.getId(), user.getUserId());
        return toResponse(saved, false);
    }

    @Override
    public ExamResponse updateExam(String examId, UpdateExamRequest request, AuthenticatedUser user) {
        requireTrainerOrAdmin(user, "update an exam");
        Exam exam = findOrThrow(examId);

        if (exam.getStatus() == ExamStatus.COMPLETED) {
            throw new InvalidRequestException("Cannot update an exam that has already been completed");
        }

        Batch batch = batchRepository.findById(request.getBatchId())
                .orElseThrow(() -> new InvalidRequestException("Batch not found: " + request.getBatchId()));

        if (request.getPassingMarks() > request.getTotalMarks()) {
            throw new InvalidRequestException("passingMarks cannot exceed totalMarks");
        }

        exam.setExamName(request.getExamName());
        exam.setCourse(request.getCourse());
        exam.setBatchId(batch.getId());
        exam.setBatchName(batch.getName());
        exam.setDurationMinutes(request.getDurationMinutes());
        exam.setTotalMarks(request.getTotalMarks());
        exam.setPassingMarks(request.getPassingMarks());
        exam.setExamDate(request.getExamDate());
        exam.setQuestions(toQuestions(request.getQuestions()));
        exam.setUpdatedAt(Instant.now());

        Exam saved = examRepository.save(exam);
        log.info("Exam {} updated by {}", examId, user.getUserId());
        return toResponse(saved, false);
    }

    @Override
    public void deleteExam(String examId, AuthenticatedUser user) {
        requireTrainerOrAdmin(user, "delete an exam");
        Exam exam = findOrThrow(examId);

        examResultRepository.deleteByExamId(examId);
        examRepository.deleteById(exam.getId());
        log.info("Exam {} deleted by {}", examId, user.getUserId());
    }

    @Override
    public ExamResponse getExam(String examId, AuthenticatedUser user) {
        Exam exam = findOrThrow(examId);

        if (isStudent(user)) {
            assertStudentCanAccess(exam, user);
            return toResponse(exam, true);
        }

        return toResponse(exam, false);
    }

    @Override
    public List<ExamResponse> getAllExams(String course, String batchId, ExamStatus status, AuthenticatedUser user) {
        requireTrainerOrAdmin(user, "view exams");

        List<Exam> exams = examRepository.findAll();
        return exams.stream()
                .filter(e -> !StringUtils.hasText(course) || course.equalsIgnoreCase(e.getCourse()))
                .filter(e -> !StringUtils.hasText(batchId) || batchId.equals(e.getBatchId()))
                .filter(e -> status == null || ExamStatusResolver.resolve(e) == status)
                .sorted(Comparator.comparing(Exam::getExamDate).reversed())
                .map(e -> toResponse(e, false))
                .toList();
    }

    @Override
    public List<ExamResponse> getAvailableExamsForStudent(AuthenticatedUser user) {
        com.rbac.model.login.User student = userRepository.findById(user.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        List<String> batchIds = student.getBatchIds() == null ? List.of() : student.getBatchIds().stream().toList();
        if (batchIds.isEmpty()) {
            return List.of();
        }

        List<Exam> exams = examRepository.findByBatchIdIn(batchIds);
        return exams.stream()
                .filter(ExamStatusResolver::isAvailableToStudents)
                .sorted(Comparator.comparing(Exam::getExamDate))
                .map(e -> toResponse(e, true))
                .toList();
    }

    @Override
    public ExamResponse publishExam(String examId, AuthenticatedUser user) {
        requireTrainerOrAdmin(user, "publish an exam");
        Exam exam = findOrThrow(examId);

        if (exam.getQuestions() == null || exam.getQuestions().isEmpty()) {
            throw new InvalidRequestException("Cannot publish an exam that has no questions");
        }

        exam.setStatus(ExamStatus.PUBLISHED);
        exam.setUpdatedAt(Instant.now());
        Exam saved = examRepository.save(exam);
        log.info("Exam {} published by {}", examId, user.getUserId());
        return toResponse(saved, false);
    }

    @Override
    public ExamResponse unpublishExam(String examId, AuthenticatedUser user) {
        requireTrainerOrAdmin(user, "unpublish an exam");
        Exam exam = findOrThrow(examId);

        exam.setStatus(ExamStatus.UNPUBLISHED);
        exam.setUpdatedAt(Instant.now());
        Exam saved = examRepository.save(exam);
        log.info("Exam {} unpublished by {}", examId, user.getUserId());
        return toResponse(saved, false);
    }

    @Override
    public ExamAnalyticsResponse getAnalytics(String examId, AuthenticatedUser user) {
        requireTrainerOrAdmin(user, "view exam analytics");
        Exam exam = findOrThrow(examId);

        long totalStudents = userRepository.countByBatchIdsContaining(exam.getBatchId());
        List<ExamResult> results = examResultRepository.findByExamId(examId);
        List<ExamResult> completed = results.stream().filter(ExamResult::isCompleted).toList();

        long attemptedCount = completed.size();
        long passedCount = completed.stream().filter(ExamResult::isPassed).count();
        long failedCount = attemptedCount - passedCount;

        double averageScore = completed.stream().mapToDouble(ExamResult::getObtainedMarks).average().orElse(0.0);
        double highestScore = completed.stream().mapToDouble(ExamResult::getObtainedMarks).max().orElse(0.0);
        double lowestScore = completed.stream().mapToDouble(ExamResult::getObtainedMarks).min().orElse(0.0);

        double passPercentage = attemptedCount == 0 ? 0.0 : round((passedCount * 100.0) / attemptedCount);
        double failPercentage = attemptedCount == 0 ? 0.0 : round((failedCount * 100.0) / attemptedCount);
        double completionRate = totalStudents == 0 ? 0.0 : round((attemptedCount * 100.0) / totalStudents);

        return ExamAnalyticsResponse.builder()
                .examId(exam.getId())
                .examName(exam.getExamName())
                .totalStudents(totalStudents)
                .attemptedCount(attemptedCount)
                .passedCount(passedCount)
                .failedCount(failedCount)
                .averageScore(round(averageScore))
                .highestScore(round(highestScore))
                .lowestScore(round(lowestScore))
                .passPercentage(passPercentage)
                .failPercentage(failPercentage)
                .completionRate(completionRate)
                .build();
    }

    @Override
    public List<ExamResultSummaryResponse> getResultSummary(String examId, AuthenticatedUser user) {
        requireTrainerOrAdmin(user, "view exam results");
        findOrThrow(examId);

        return examResultRepository.findByExamId(examId).stream()
                .sorted(Comparator.comparing(ExamResult::getObtainedMarks).reversed())
                .map(r -> ExamResultSummaryResponse.builder()
                        .studentId(r.getStudentId())
                        .studentName(r.getStudentName())
                        .totalMarks(r.getTotalMarks())
                        .obtainedMarks(r.getObtainedMarks())
                        .percentage(round(r.getPercentage()))
                        .grade(r.getGrade())
                        .passed(r.isPassed())
                        .completed(r.isCompleted())
                        .submittedAt(r.getSubmittedAt())
                        .build())
                .toList();
    }

    @Override
    public ExamResultSummaryResponse submitExam(String examId, SubmitExamRequest request, AuthenticatedUser user) {
        if (!isStudent(user)) {
            throw new UnauthorizedActionException("Only students can submit exam answers");
        }

        Exam exam = findOrThrow(examId);
        assertStudentCanAccess(exam, user);

        if (ExamStatusResolver.resolve(exam) != ExamStatus.ONGOING
                && ExamStatusResolver.resolve(exam) != ExamStatus.PUBLISHED) {
            throw new InvalidRequestException("This exam is not currently open for submission");
        }

        examResultRepository.findByExamIdAndStudentId(examId, user.getUserId()).ifPresent(existing -> {
            throw new InvalidRequestException("You have already submitted this exam");
        });

        double obtained = 0.0;
        Map<String, Integer> answers = request.getAnswers();
        for (Question q : exam.getQuestions()) {
            Integer selected = answers.get(q.getId());
            if (selected != null && selected == q.getCorrectOptionIndex()) {
                obtained += q.getMarks();
            }
        }

        double percentage = exam.getTotalMarks() == 0 ? 0.0 : (obtained / exam.getTotalMarks()) * 100.0;
        boolean passed = obtained >= exam.getPassingMarks();

        ExamResult result = new ExamResult();
        result.setId(UUID.randomUUID().toString());
        result.setExamId(examId);
        result.setStudentId(user.getUserId());
        result.setStudentName(user.getUserName());
        result.setAnswers(answers);
        result.setObtainedMarks(obtained);
        result.setTotalMarks(exam.getTotalMarks());
        result.setPercentage(percentage);
        result.setGrade(computeGrade(percentage));
        result.setPassed(passed);
        result.setCompleted(true);
        result.setSubmittedAt(Instant.now());

        ExamResult saved = examResultRepository.save(result);
        log.info("Student {} submitted exam {} (score {}/{})", user.getUserId(), examId, obtained, exam.getTotalMarks());

        return ExamResultSummaryResponse.builder()
                .studentId(saved.getStudentId())
                .studentName(saved.getStudentName())
                .totalMarks(saved.getTotalMarks())
                .obtainedMarks(saved.getObtainedMarks())
                .percentage(round(saved.getPercentage()))
                .grade(saved.getGrade())
                .passed(saved.isPassed())
                .completed(saved.isCompleted())
                .submittedAt(saved.getSubmittedAt())
                .build();
    }

    // ---------- helpers ----------

    private Exam findOrThrow(String examId) {
        return examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found: " + examId));
    }

    private void requireTrainerOrAdmin(AuthenticatedUser user, String action) {
        if (!user.hasRole(Role.ADMIN) && !user.hasRole(Role.TEACHER)) {
            throw new UnauthorizedActionException("Only an admin or trainer can " + action);
        }
    }

    private boolean isStudent(AuthenticatedUser user) {
        return user.hasRole(Role.STUDENT) && !user.hasRole(Role.ADMIN) && !user.hasRole(Role.TEACHER);
    }

    private void assertStudentCanAccess(Exam exam, AuthenticatedUser user) {
        com.rbac.model.login.User student = userRepository.findById(user.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        boolean inBatch = student.getBatchIds() != null && student.getBatchIds().contains(exam.getBatchId());
        if (!inBatch) {
            throw new UnauthorizedActionException("You are not enrolled in the batch for this exam");
        }

        if (!ExamStatusResolver.isAvailableToStudents(exam)) {
            throw new UnauthorizedActionException("This exam is not currently available");
        }
    }

    private List<Question> toQuestions(List<QuestionDto> dtos) {
        if (dtos == null) {
            return List.of();
        }
        return dtos.stream().map(QuestionDto::toEntity).toList();
    }

    private ExamResponse toResponse(Exam exam, boolean forStudent) {
        long totalStudents = userRepository.countByBatchIdsContaining(exam.getBatchId());
        return ExamResponse.fromEntity(exam, totalStudents, forStudent);
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
