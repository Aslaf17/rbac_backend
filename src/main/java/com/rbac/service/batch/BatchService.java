package com.rbac.service.batch;

import com.rbac.dto.batch.AssignStudentsRequest;
import com.rbac.dto.batch.AssignTrainersRequest;
import com.rbac.dto.batch.BatchAnalyticsResponse;
import com.rbac.dto.batch.BatchResponse;
import com.rbac.dto.batch.BatchStudentResponse;
import com.rbac.dto.batch.BatchTrainerResponse;
import com.rbac.dto.batch.CreateBatchRequest;
import com.rbac.dto.batch.UpdateBatchRequest;
import com.rbac.repository.SessionRepository;
import com.rbac.model.session.Session;
import com.rbac.model.session.SessionStatus;
import com.rbac.exception.batch.DuplicateBatchException;
import com.rbac.exception.batch.InvalidRequestException;
import com.rbac.exception.batch.ResourceNotFoundException;
import com.rbac.exception.batch.UnauthorizedActionException;
import com.rbac.model.batch.Batch;
import com.rbac.model.batch.BatchStatus;
import com.rbac.model.login.Role;
import com.rbac.model.login.User;
import com.rbac.repository.BatchRepository;
import com.rbac.repository.UserRepository;
import com.rbac.security.chat.AuthenticatedUser;
import com.rbac.service.batch.BatchIdGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public interface BatchService {

    BatchResponse createBatch(CreateBatchRequest request, AuthenticatedUser admin);

    BatchResponse updateBatch(String batchId, UpdateBatchRequest request, AuthenticatedUser admin);

    BatchResponse getBatch(String batchId);

    List<BatchResponse> getAllBatches(BatchStatus status);

    void deleteBatch(String batchId, AuthenticatedUser admin);

    BatchResponse assignStudents(String batchId, AssignStudentsRequest request, AuthenticatedUser admin);

    BatchResponse removeStudents(String batchId, AssignStudentsRequest request, AuthenticatedUser admin);

    List<BatchStudentResponse> getStudents(String batchId);

    List<BatchStudentResponse> searchStudents(String query, String excludeBatchId);

    BatchResponse assignTrainers(String batchId, AssignTrainersRequest request, AuthenticatedUser admin);

    BatchResponse removeTrainers(String batchId, AssignTrainersRequest request, AuthenticatedUser admin);

    List<BatchTrainerResponse> getTrainers(String batchId);

    List<BatchTrainerResponse> searchTrainers(String query, String excludeBatchId);

    BatchAnalyticsResponse getBatchAnalytics(String batchId);
}

@Slf4j
@Service
@RequiredArgsConstructor
class BatchServiceImpl implements BatchService {

    private final BatchRepository batchRepository;
    private final UserRepository userRepository;
    private final BatchIdGeneratorService batchIdGeneratorService;
    private final SessionRepository sessionRepository;

    @Override
    public BatchResponse createBatch(CreateBatchRequest request, AuthenticatedUser admin) {
        requireAdmin(admin, "create a batch");

        String batchId = StringUtils.hasText(request.getBatchId())
                ? request.getBatchId().trim()
                : batchIdGeneratorService.generate(request.getPrefix(), request.getName());

        Batch batch = new Batch();
        batch.setId(batchId);
        batch.setName(request.getName());
        batch.setDescription(request.getDescription());
        batch.setStatus(BatchStatus.ACTIVE);
        batch.setCreatedBy(admin.getUserId());
        batch.setCreatedByName(admin.getUserName());
        batch.setCreatedAt(Instant.now());
        batch.setUpdatedAt(Instant.now());

        Batch saved;
        try {
            saved = batchRepository.insert(batch);
        } catch (DuplicateKeyException ex) {
            throw new DuplicateBatchException("A batch with id '" + batchId + "' already exists");
        }

        log.info("Batch {} created by {}", saved.getId(), admin.getUserId());
        return BatchResponse.fromEntity(saved, 0);
    }

    @Override
    public BatchResponse updateBatch(String batchId, UpdateBatchRequest request, AuthenticatedUser admin) {
        requireAdmin(admin, "update a batch");
        Batch batch = findOrThrow(batchId);

        batch.setName(request.getName());
        batch.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            batch.setStatus(request.getStatus());
        }
        batch.setUpdatedAt(Instant.now());

        Batch saved = batchRepository.save(batch);
        log.info("Batch {} updated by {}", batchId, admin.getUserId());

        return BatchResponse.fromEntity(saved, studentCountOf(batchId), trainerCountOf(batchId));
    }

    @Override
    public BatchResponse getBatch(String batchId) {
        Batch batch = findOrThrow(batchId);
        return BatchResponse.fromEntity(batch, studentCountOf(batchId), trainerCountOf(batchId));
    }

    @Override
    public List<BatchResponse> getAllBatches(BatchStatus status) {
        List<Batch> batches = status != null ? batchRepository.findByStatus(status) : batchRepository.findAll();
        return batches.stream()
                .map(b -> BatchResponse.fromEntity(b, studentCountOf(b.getId()), trainerCountOf(b.getId())))
                .toList();
    }

    @Override
    public void deleteBatch(String batchId, AuthenticatedUser admin) {
        requireAdmin(admin, "delete a batch");
        Batch batch = findOrThrow(batchId);

        long studentCount = studentCountOf(batchId);
        long trainerCount = trainerCountOf(batchId);
        if (studentCount > 0 || trainerCount > 0) {
            throw new InvalidRequestException(
                    "Cannot delete batch '" + batchId + "': " + studentCount +
                            " student(s) and " + trainerCount + " trainer(s) are still assigned. " +
                            "Reassign or remove them first, or set the batch status to ARCHIVED instead.");
        }

        batchRepository.deleteById(batchId);
        log.info("Batch {} deleted by {}", batchId, admin.getUserId());
    }

    @Override
    public BatchResponse assignStudents(String batchId, AssignStudentsRequest request, AuthenticatedUser admin) {
        requireAdmin(admin, "assign students to a batch");
        Batch batch = findOrThrow(batchId);

        if (batch.getStatus() != BatchStatus.ACTIVE) {
            throw new InvalidRequestException("Cannot assign students to a batch that is not ACTIVE");
        }

        List<String> requestedIds = request.getStudentIds().stream().distinct().toList();
        List<User> students = userRepository.findByIdInAndRole(requestedIds, Role.STUDENT);

        Set<String> foundIds = students.stream().map(User::getId).collect(Collectors.toSet());
        List<String> missing = requestedIds.stream().filter(id -> !foundIds.contains(id)).toList();
        if (!missing.isEmpty()) {
            throw new InvalidRequestException(
                    "The following ids are not valid students: " + String.join(", ", missing));
        }

        students.forEach(s -> {
            if (s.getBatchIds() == null) {
                s.setBatchIds(new java.util.HashSet<>());
            }
            s.getBatchIds().add(batchId);
        });
        userRepository.saveAll(students);

        log.info("{} student(s) assigned to batch {} by {}", students.size(), batchId, admin.getUserId());
        return BatchResponse.fromEntity(batch, studentCountOf(batchId), trainerCountOf(batchId));
    }

    @Override
    public BatchResponse removeStudents(String batchId, AssignStudentsRequest request, AuthenticatedUser admin) {
        requireAdmin(admin, "remove students from a batch");
        Batch batch = findOrThrow(batchId);

        List<String> requestedIds = request.getStudentIds().stream().distinct().toList();
        List<User> students = userRepository.findByIdInAndRole(requestedIds, Role.STUDENT);

        List<User> toUpdate = students.stream()
                .filter(s -> s.getBatchIds() != null && s.getBatchIds().contains(batchId))
                .toList();

        toUpdate.forEach(s -> s.getBatchIds().remove(batchId));
        userRepository.saveAll(toUpdate);

        log.info("{} student(s) removed from batch {} by {}", toUpdate.size(), batchId, admin.getUserId());
        return BatchResponse.fromEntity(batch, studentCountOf(batchId), trainerCountOf(batchId));
    }

    @Override
    public List<BatchStudentResponse> getStudents(String batchId) {
        findOrThrow(batchId); // 404 if batch itself doesn't exist
        return userRepository.findByBatchIdsContainingAndRole(batchId, Role.STUDENT).stream()
                .map(BatchStudentResponse::fromEntity)
                .toList();
    }

    @Override
    public List<BatchStudentResponse> searchStudents(String query, String excludeBatchId) {
        String q = StringUtils.hasText(query) ? query.trim().toLowerCase() : "";

        return userRepository.findByRole(Role.STUDENT).stream()
                .filter(u -> !StringUtils.hasText(excludeBatchId)
                        || u.getBatchIds() == null
                        || !u.getBatchIds().contains(excludeBatchId))
                .filter(u -> q.isEmpty()
                        || (u.getUsername() != null && u.getUsername().toLowerCase().contains(q))
                        || (u.getEmail() != null && u.getEmail().toLowerCase().contains(q)))
                .sorted(java.util.Comparator.comparing(User::getUsername, String.CASE_INSENSITIVE_ORDER))
                .limit(20)
                .map(BatchStudentResponse::fromEntity)
                .toList();
    }

    @Override
    public BatchResponse assignTrainers(String batchId, AssignTrainersRequest request, AuthenticatedUser admin) {
        requireAdmin(admin, "assign trainers to a batch");
        Batch batch = findOrThrow(batchId);

        if (batch.getStatus() != BatchStatus.ACTIVE) {
            throw new InvalidRequestException("Cannot assign trainers to a batch that is not ACTIVE");
        }

        List<String> requestedIds = request.getTrainerIds().stream().distinct().toList();
        List<User> trainers = userRepository.findByIdInAndRole(requestedIds, Role.TEACHER);

        Set<String> foundIds = trainers.stream().map(User::getId).collect(Collectors.toSet());
        List<String> missing = requestedIds.stream().filter(id -> !foundIds.contains(id)).toList();
        if (!missing.isEmpty()) {
            throw new InvalidRequestException(
                    "The following ids are not valid trainers: " + String.join(", ", missing));
        }

        trainers.forEach(t -> {
            if (t.getBatchIds() == null) {
                t.setBatchIds(new java.util.HashSet<>());
            }
            t.getBatchIds().add(batchId);
        });
        userRepository.saveAll(trainers);

        log.info("{} trainer(s) assigned to batch {} by {}", trainers.size(), batchId, admin.getUserId());
        return BatchResponse.fromEntity(batch, studentCountOf(batchId), trainerCountOf(batchId));
    }

    @Override
    public BatchResponse removeTrainers(String batchId, AssignTrainersRequest request, AuthenticatedUser admin) {
        requireAdmin(admin, "remove trainers from a batch");
        Batch batch = findOrThrow(batchId);

        List<String> requestedIds = request.getTrainerIds().stream().distinct().toList();
        List<User> trainers = userRepository.findByIdInAndRole(requestedIds, Role.TEACHER);

        List<User> toUpdate = trainers.stream()
                .filter(t -> t.getBatchIds() != null && t.getBatchIds().contains(batchId))
                .toList();

        toUpdate.forEach(t -> t.getBatchIds().remove(batchId));
        userRepository.saveAll(toUpdate);

        log.info("{} trainer(s) removed from batch {} by {}", toUpdate.size(), batchId, admin.getUserId());
        return BatchResponse.fromEntity(batch, studentCountOf(batchId), trainerCountOf(batchId));
    }

    @Override
    public List<BatchTrainerResponse> getTrainers(String batchId) {
        findOrThrow(batchId); // 404 if batch itself doesn't exist
        return userRepository.findByBatchIdsContainingAndRole(batchId, Role.TEACHER).stream()
                .map(BatchTrainerResponse::fromEntity)
                .toList();
    }

    @Override
    public List<BatchTrainerResponse> searchTrainers(String query, String excludeBatchId) {
        String q = StringUtils.hasText(query) ? query.trim().toLowerCase() : "";

        return userRepository.findByRole(Role.TEACHER).stream()
                .filter(u -> !StringUtils.hasText(excludeBatchId)
                        || u.getBatchIds() == null
                        || !u.getBatchIds().contains(excludeBatchId))
                .filter(u -> q.isEmpty()
                        || (u.getUsername() != null && u.getUsername().toLowerCase().contains(q))
                        || (u.getEmail() != null && u.getEmail().toLowerCase().contains(q)))
                .sorted(java.util.Comparator.comparing(User::getUsername, String.CASE_INSENSITIVE_ORDER))
                .limit(20)
                .map(BatchTrainerResponse::fromEntity)
                .toList();
    }

    @Override
    public BatchAnalyticsResponse getBatchAnalytics(String batchId) {
        Batch batch = findOrThrow(batchId);

        List<Session> sessions = sessionRepository.findByBatchId(batchId);
        long liveSessions = sessions.stream().filter(s -> s.getStatus() == SessionStatus.LIVE).count();
        long endedSessions = sessions.stream().filter(s -> s.getStatus() == SessionStatus.ENDED).count();

        return BatchAnalyticsResponse.builder()
                .batchId(batch.getId())
                .name(batch.getName())
                .status(batch.getStatus())
                .studentCount(studentCountOf(batchId))
                .trainerCount(trainerCountOf(batchId))
                .totalSessions(sessions.size())
                .liveSessions(liveSessions)
                .completedSessions(endedSessions)
                .createdAt(batch.getCreatedAt())
                .updatedAt(batch.getUpdatedAt())
                .build();
    }

    private long studentCountOf(String batchId) {
        return userRepository.countByBatchIdsContainingAndRole(batchId, Role.STUDENT);
    }

    private long trainerCountOf(String batchId) {
        return userRepository.countByBatchIdsContainingAndRole(batchId, Role.TEACHER);
    }

    private Batch findOrThrow(String batchId) {
        return batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + batchId));
    }

    private void requireAdmin(AuthenticatedUser user, String action) {
        if (!user.hasRole(Role.ADMIN) && !user.hasRole(Role.TEACHER)) {
            throw new UnauthorizedActionException("Only an admin can " + action);
        }
    }
}