package com.rbac.service.recording;

import com.rbac.dto.recording.PageResponse;
import com.rbac.dto.recording.PlaybackAuthorizationResponse;
import com.rbac.dto.recording.RecordingAnalyticsResponse;
import com.rbac.dto.recording.RecordingResponse;
import com.rbac.dto.recording.StreamResponse;
import com.rbac.dto.recording.UpdateRecordingRequest;
import com.rbac.dto.recording.UpdateRecordingStatusRequest;
import com.rbac.dto.recording.UploadRecordingRequest;
import com.rbac.dto.recording.WatchProgressRequest;
import com.rbac.exception.recording.DuplicateRecordingException;
import com.rbac.exception.recording.InvalidRequestException;
import com.rbac.exception.recording.ResourceNotFoundException;
import com.rbac.exception.recording.UnauthorizedActionException;
import com.rbac.model.login.Role;
import com.rbac.model.login.User;
import com.rbac.model.recording.Recording;
import com.rbac.model.recording.RecordingStatus;
import com.rbac.model.recording.RecordingView;
import com.rbac.model.recording.Visibility;
import com.rbac.model.session.Session;
import com.rbac.repository.recording.RecordingRepository;
import com.rbac.repository.recording.RecordingViewRepository;
import com.rbac.repository.SessionRepository;
import com.rbac.repository.UserRepository;
import com.rbac.security.chat.AuthenticatedUser;
import com.rbac.security.recording.PlaybackTokenService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public interface RecordingService {

    RecordingResponse upload(UploadRecordingRequest request, AuthenticatedUser requester);

    PageResponse<RecordingResponse> getAll(String search, String batchId, String trainerId, String sessionId,
                                           int page, int size, AuthenticatedUser requester);

    RecordingResponse getById(String id, AuthenticatedUser requester);

    RecordingResponse update(String id, UpdateRecordingRequest request, AuthenticatedUser requester);

    RecordingResponse updateStatus(String id, UpdateRecordingStatusRequest request, AuthenticatedUser requester);

    void softDelete(String id, AuthenticatedUser requester);

    void permanentDelete(String id, AuthenticatedUser requester);

    PlaybackAuthorizationResponse authorizePlayback(String id, AuthenticatedUser requester);

    StreamResponse stream(String id, String playbackToken, AuthenticatedUser requester);

    StreamResponse download(String id, String playbackToken, AuthenticatedUser requester);

    void trackWatchProgress(String id, WatchProgressRequest request, AuthenticatedUser requester);

    RecordingAnalyticsResponse getAnalytics(String id, AuthenticatedUser requester);

    List<RecordingResponse> getMostViewed(int limit, AuthenticatedUser requester);
}

@Slf4j
@Service
@RequiredArgsConstructor
class RecordingServiceImpl implements RecordingService {

    private static final Map<RecordingStatus, Set<RecordingStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(RecordingStatus.class);
    static {
        ALLOWED_TRANSITIONS.put(RecordingStatus.PENDING_UPLOAD, EnumSet.of(RecordingStatus.PROCESSING, RecordingStatus.FAILED));
        ALLOWED_TRANSITIONS.put(RecordingStatus.PROCESSING, EnumSet.of(RecordingStatus.ENCODING_COMPLETED, RecordingStatus.FAILED));
        ALLOWED_TRANSITIONS.put(RecordingStatus.ENCODING_COMPLETED, EnumSet.of(RecordingStatus.READY, RecordingStatus.FAILED));
        ALLOWED_TRANSITIONS.put(RecordingStatus.READY, EnumSet.of(RecordingStatus.PROCESSING, RecordingStatus.FAILED));
        ALLOWED_TRANSITIONS.put(RecordingStatus.FAILED, EnumSet.of(RecordingStatus.PROCESSING));
    }

    private final RecordingRepository recordingRepository;
    private final RecordingViewRepository recordingViewRepository;
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;
    private final PlaybackTokenService playbackTokenService;

    @Override
    public RecordingResponse upload(UploadRecordingRequest request, AuthenticatedUser requester) {
        if (!(requester.hasRole(Role.TEACHER) || requester.hasRole(Role.ADMIN))) {
            throw new UnauthorizedActionException("Only a trainer or admin can upload recording metadata");
        }

        Session session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + request.getSessionId()));

        String trainerId = resolveTrainerId(request, requester);
        User trainer = userRepository.findById(trainerId)
                .orElseThrow(() -> new ResourceNotFoundException("Trainer not found: " + trainerId));
        if (trainer.getRole() != Role.TEACHER) {
            throw new InvalidRequestException("User " + trainerId + " is not a trainer");
        }

        if (recordingRepository.existsBySessionIdAndDeletedFalse(session.getId())) {
            throw new DuplicateRecordingException("A recording already exists for session " + session.getId());
        }

        if (request.getRecordingEndTime() != null
                && request.getRecordingEndTime().isBefore(request.getRecordingStartTime())) {
            throw new InvalidRequestException("recordingEndTime cannot be before recordingStartTime");
        }

        Recording recording = Recording.builder()
                .sessionId(session.getId())
                .batchId(request.getBatchId())
                .trainerId(trainer.getId())
                .trainerName(trainer.getUsername())
                .title(request.getTitle())
                .description(request.getDescription())
                .videoUrl(request.getVideoUrl())
                .thumbnailUrl(request.getThumbnailUrl())
                .durationSeconds(request.getDurationSeconds())
                .fileSizeBytes(request.getFileSizeBytes())
                .status(RecordingStatus.PROCESSING)
                .recordingStartTime(request.getRecordingStartTime())
                .recordingEndTime(request.getRecordingEndTime())
                .recordingDate(LocalDate.ofInstant(request.getRecordingStartTime(), ZoneOffset.UTC))
                .playbackCount(0)
                .downloadEnabled(request.isDownloadEnabled())
                .visibility(request.getVisibility() != null ? request.getVisibility() : Visibility.PRIVATE_TRAINER)
                .deleted(false)
                .build();

        Recording saved;
        try {
            saved = recordingRepository.save(recording);
        } catch (org.springframework.dao.DuplicateKeyException ex) {
            throw new DuplicateRecordingException("A recording already exists for session " + session.getId());
        }

        log.info("Recording {} uploaded by {} for session {}", saved.getId(), requester.getUserId(), session.getId());
        return RecordingResponse.fromEntity(saved);
    }

    @Override
    public PageResponse<RecordingResponse> getAll(String search, String batchId, String trainerId, String sessionId,
                                                  int page, int size, AuthenticatedUser requester) {
        Criteria scope = buildScopeCriteria(requester, batchId, trainerId, sessionId);
        Query query = new Query(scope);

        if (StringUtils.hasText(search)) {
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("title").regex(search, "i"),
                    Criteria.where("description").regex(search, "i")
            ));
        }

        long total = mongoTemplate.count(query, Recording.class);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        query.with(pageable);

        List<Recording> results = mongoTemplate.find(query, Recording.class);
        List<RecordingResponse> content = results.stream().map(RecordingResponse::fromEntity).toList();

        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) total / size);
        return new PageResponse<>(content, page, size, total, totalPages);
    }

    @Override
    public RecordingResponse getById(String id, AuthenticatedUser requester) {
        Recording recording = loadVisible(id, requester);
        assertCanView(recording, requester);
        return RecordingResponse.fromEntity(recording);
    }

    @Override
    public RecordingResponse update(String id, UpdateRecordingRequest request, AuthenticatedUser requester) {
        Recording recording = getOwnedRecording(id, requester);

        if (request.getTitle() != null) {
            if (!StringUtils.hasText(request.getTitle())) {
                throw new InvalidRequestException("title cannot be blank");
            }
            recording.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            recording.setDescription(request.getDescription());
        }
        if (request.getVisibility() != null) {
            recording.setVisibility(request.getVisibility());
        }
        if (request.getThumbnailUrl() != null) {
            recording.setThumbnailUrl(request.getThumbnailUrl());
        }
        if (request.getDownloadEnabled() != null) {
            recording.setDownloadEnabled(request.getDownloadEnabled());
        }
        recording.setUpdatedAt(Instant.now());

        Recording saved = recordingRepository.save(recording);
        log.info("Recording {} updated by {}", id, requester.getUserId());
        return RecordingResponse.fromEntity(saved);
    }

    @Override
    public RecordingResponse updateStatus(String id, UpdateRecordingStatusRequest request, AuthenticatedUser requester) {
        Recording recording = getOwnedRecording(id, requester);

        RecordingStatus current = recording.getStatus();
        RecordingStatus next = request.getStatus();

        if (current == next) {
            throw new InvalidRequestException("Recording is already in status " + current);
        }
        Set<RecordingStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(current, EnumSet.noneOf(RecordingStatus.class));
        if (!allowed.contains(next)) {
            throw new InvalidRequestException("Cannot transition recording status from " + current + " to " + next);
        }

        recording.setStatus(next);
        recording.setFailureReason(next == RecordingStatus.FAILED ? request.getFailureReason() : null);
        recording.setUpdatedAt(Instant.now());

        Recording saved = recordingRepository.save(recording);
        log.info("Recording {} status changed {} -> {} by {}", id, current, next, requester.getUserId());
        return RecordingResponse.fromEntity(saved);
    }

    @Override
    public void softDelete(String id, AuthenticatedUser requester) {
        Recording recording = getOwnedRecording(id, requester);
        if (recording.isDeleted()) {
            throw new InvalidRequestException("Recording is already deleted");
        }
        recording.setDeleted(true);
        recording.setDeletedAt(Instant.now());
        recording.setUpdatedAt(Instant.now());
        recordingRepository.save(recording);
        log.info("Recording {} soft-deleted by {}", id, requester.getUserId());
    }

    @Override
    public void permanentDelete(String id, AuthenticatedUser requester) {
        if (!requester.hasRole(Role.ADMIN)) {
            throw new UnauthorizedActionException("Only an admin can permanently delete a recording");
        }
        Recording recording = recordingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recording not found: " + id));

        recordingRepository.delete(recording);
        recordingViewRepository.findByRecordingId(id).forEach(recordingViewRepository::delete);
        log.info("Recording {} permanently deleted by admin {}", id, requester.getUserId());
    }

    @Override
    public PlaybackAuthorizationResponse authorizePlayback(String id, AuthenticatedUser requester) {
        Recording recording = loadVisible(id, requester);
        assertCanPlay(recording, requester);

        String token = playbackTokenService.generate(recording.getId(), requester.getUserId());
        return PlaybackAuthorizationResponse.builder()
                .recordingId(recording.getId())
                .playbackToken(token)
                .expiresAt(playbackTokenService.expiresAt())
                .downloadAllowed(recording.isDownloadEnabled())
                .build();
    }

    @Override
    public StreamResponse stream(String id, String playbackToken, AuthenticatedUser requester) {
        Recording recording = loadVisible(id, requester);
        verifyToken(playbackToken, recording.getId(), requester.getUserId());

        recordView(recording, requester);
        Recording refreshed = recordingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recording not found: " + id));

        return StreamResponse.builder()
                .recordingId(refreshed.getId())
                .videoUrl(refreshed.getVideoUrl())
                .playbackCount(refreshed.getPlaybackCount())
                .build();
    }

    @Override
    public StreamResponse download(String id, String playbackToken, AuthenticatedUser requester) {
        Recording recording = loadVisible(id, requester);
        if (!recording.isDownloadEnabled()) {
            throw new UnauthorizedActionException("Downloads are disabled for this recording");
        }
        verifyToken(playbackToken, recording.getId(), requester.getUserId());

        RecordingView view = loadOrCreateView(recording, requester);
        view.setDownloadCount(view.getDownloadCount() + 1);
        view.setLastViewedAt(Instant.now());
        recordingViewRepository.save(view);

        log.info("Recording {} downloaded by {}", id, requester.getUserId());
        return StreamResponse.builder()
                .recordingId(recording.getId())
                .videoUrl(recording.getVideoUrl())
                .playbackCount(recording.getPlaybackCount())
                .build();
    }

    @Override
    public void trackWatchProgress(String id, WatchProgressRequest request, AuthenticatedUser requester) {
        Recording recording = loadVisible(id, requester);
        verifyToken(request.getPlaybackToken(), recording.getId(), requester.getUserId());

        RecordingView view = loadOrCreateView(recording, requester);
        view.setTotalWatchDurationSeconds(view.getTotalWatchDurationSeconds() + request.getWatchedSeconds());
        view.setLastViewedAt(Instant.now());
        recordingViewRepository.save(view);
    }

    @Override
    public RecordingAnalyticsResponse getAnalytics(String id, AuthenticatedUser requester) {
        Recording recording = getOwnedRecording(id, requester);
        List<RecordingView> views = recordingViewRepository.findByRecordingId(recording.getId());

        long totalViews = views.stream().mapToLong(RecordingView::getViewCount).sum();
        long uniqueViewers = views.size();
        long totalWatchDuration = views.stream().mapToLong(RecordingView::getTotalWatchDurationSeconds).sum();
        long downloadCount = views.stream().mapToLong(RecordingView::getDownloadCount).sum();
        Instant lastViewed = views.stream()
                .map(RecordingView::getLastViewedAt)
                .filter(Objects::nonNull)
                .max(Instant::compareTo)
                .orElse(null);

        return RecordingAnalyticsResponse.builder()
                .recordingId(recording.getId())
                .totalViews(totalViews)
                .uniqueViewers(uniqueViewers)
                .totalWatchDurationSeconds(totalWatchDuration)
                .lastViewedAt(lastViewed)
                .downloadCount(downloadCount)
                .build();
    }

    @Override
    public List<RecordingResponse> getMostViewed(int limit, AuthenticatedUser requester) {
        if (!(requester.hasRole(Role.TEACHER) || requester.hasRole(Role.ADMIN))) {
            throw new UnauthorizedActionException("You do not have permission to view recording analytics");
        }

        Query query = new Query(Criteria.where("deleted").is(false));
        if (!requester.hasRole(Role.ADMIN)) {
            query.addCriteria(Criteria.where("trainerId").is(requester.getUserId()));
        }
        int cappedLimit = Math.max(1, Math.min(limit, 100));
        query.with(Sort.by(Sort.Direction.DESC, "playbackCount")).limit(cappedLimit);

        return mongoTemplate.find(query, Recording.class).stream()
                .map(RecordingResponse::fromEntity)
                .toList();
    }

    // ---- helpers ----

    private String resolveTrainerId(UploadRecordingRequest request, AuthenticatedUser requester) {
        if (requester.hasRole(Role.TEACHER)) {
            if (StringUtils.hasText(request.getTrainerId()) && !request.getTrainerId().equals(requester.getUserId())) {
                throw new InvalidRequestException("trainerId does not match the authenticated trainer");
            }
            return requester.getUserId();
        }
        // ADMIN
        if (!StringUtils.hasText(request.getTrainerId())) {
            throw new InvalidRequestException("trainerId is required when an admin uploads a recording");
        }
        return request.getTrainerId();
    }

    private Criteria buildScopeCriteria(AuthenticatedUser requester, String batchId, String trainerId, String sessionId) {
        List<Criteria> criteria = new ArrayList<>();
        criteria.add(Criteria.where("deleted").is(false));

        if (requester.hasRole(Role.ADMIN)) {
            if (StringUtils.hasText(batchId)) {
                criteria.add(Criteria.where("batchId").is(batchId));
            }
            if (StringUtils.hasText(trainerId)) {
                criteria.add(Criteria.where("trainerId").is(trainerId));
            }
        } else if (requester.hasRole(Role.TEACHER)) {
            criteria.add(Criteria.where("trainerId").is(requester.getUserId()));
            if (StringUtils.hasText(batchId)) {
                criteria.add(Criteria.where("batchId").is(batchId));
            }
        } else if (requester.hasRole(Role.STUDENT)) {
            User user = userRepository.findById(requester.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + requester.getUserId()));
            criteria.add(Criteria.where("status").is(RecordingStatus.READY));
            criteria.add(Criteria.where("visibility").is(Visibility.PUBLIC_BATCH));
            if (user.getBatchIds() != null && !user.getBatchIds().isEmpty()) {
                criteria.add(Criteria.where("batchId").in(user.getBatchIds()));
            } else {
                criteria.add(Criteria.where("batchId").is("__no_batch__"));
            }
        } else {
            throw new UnauthorizedActionException("You do not have permission to view recordings");
        }

        if (StringUtils.hasText(sessionId)) {
            criteria.add(Criteria.where("sessionId").is(sessionId));
        }

        return new Criteria().andOperator(criteria.toArray(new Criteria[0]));
    }

    /** Loads a recording, masking soft-deleted ones as 404 for anyone other than an admin. */
    private Recording loadVisible(String id, AuthenticatedUser requester) {
        Recording recording = recordingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recording not found: " + id));
        if (recording.isDeleted() && !requester.hasRole(Role.ADMIN)) {
            throw new ResourceNotFoundException("Recording not found: " + id);
        }
        return recording;
    }

    /** Loads a recording and enforces that the requester owns it (trainer) or is an admin - used for mutations. */
    private Recording getOwnedRecording(String id, AuthenticatedUser requester) {
        Recording recording = recordingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recording not found: " + id));
        if (requester.hasRole(Role.ADMIN)) {
            return recording;
        }
        if (requester.hasRole(Role.TEACHER) && recording.getTrainerId().equals(requester.getUserId())) {
            return recording;
        }
        throw new UnauthorizedActionException("You do not have permission to modify this recording");
    }

    private void assertCanView(Recording recording, AuthenticatedUser requester) {
        if (requester.hasRole(Role.ADMIN)) {
            return;
        }
        if (requester.hasRole(Role.TEACHER)) {
            if (recording.getTrainerId().equals(requester.getUserId())) {
                return;
            }
            throw new UnauthorizedActionException("You do not have permission to view this recording");
        }
        if (requester.hasRole(Role.STUDENT)) {
            User user = userRepository.findById(requester.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + requester.getUserId()));
            if (recording.getStatus() != RecordingStatus.READY
                    || recording.getVisibility() != Visibility.PUBLIC_BATCH
                    || user.getBatchIds() == null || !user.getBatchIds().contains(recording.getBatchId())) {
                throw new UnauthorizedActionException("You do not have permission to view this recording");
            }
            return;
        }
        throw new UnauthorizedActionException("You do not have permission to view this recording");
    }

    /** Same rule as assertCanView, but this is the gate specifically for issuing a playback token. */
    private void assertCanPlay(Recording recording, AuthenticatedUser requester) {
        assertCanView(recording, requester);
        if (requester.hasRole(Role.STUDENT) && recording.getStatus() != RecordingStatus.READY) {
            throw new UnauthorizedActionException("Recording is not ready for playback");
        }
    }

    private void verifyToken(String token, String recordingId, String userId) {
        if (!StringUtils.hasText(token)) {
            throw new UnauthorizedActionException("A valid playback token is required");
        }
        try {
            playbackTokenService.validate(token, recordingId, userId);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new UnauthorizedActionException("Invalid or expired playback token");
        }
    }

    private void recordView(Recording recording, AuthenticatedUser requester) {
        RecordingView view = loadOrCreateView(recording, requester);
        view.setViewCount(view.getViewCount() + 1);
        view.setLastViewedAt(Instant.now());
        recordingViewRepository.save(view);

        recording.setPlaybackCount(recording.getPlaybackCount() + 1);
        recording.setUpdatedAt(Instant.now());
        recordingRepository.save(recording);
    }

    private RecordingView loadOrCreateView(Recording recording, AuthenticatedUser requester) {
        return recordingViewRepository.findByRecordingIdAndUserId(recording.getId(), requester.getUserId())
                .orElseGet(() -> RecordingView.builder()
                        .recordingId(recording.getId())
                        .userId(requester.getUserId())
                        .userName(requester.getUserName())
                        .firstViewedAt(Instant.now())
                        .build());
    }
}
