package com.rbac.service.admin;

import com.rbac.dto.admin.AdminLiveSessionResponse;
import com.rbac.dto.admin.AdminSessionWatchResponse;
import com.rbac.dto.admin.AttendanceSummaryResponse;
import com.rbac.dto.classroom.ParticipantResponse;
import com.rbac.dto.session.SessionResponse;
import com.rbac.exception.session.InvalidRequestException;
import com.rbac.exception.session.ResourceNotFoundException;
import com.rbac.model.attendance.Attendance;
import com.rbac.model.attendance.AttendanceStatus;
import com.rbac.model.batch.Batch;
import com.rbac.model.classroom.Participant;
import com.rbac.model.classroom.ParticipantStatus;
import com.rbac.model.login.User;
import com.rbac.model.session.Session;
import com.rbac.model.session.SessionStatus;
import com.rbac.repository.AttendanceRepository;
import com.rbac.repository.BatchRepository;
import com.rbac.repository.ParticipantRepository;
import com.rbac.repository.SessionRepository;
import com.rbac.repository.UserRepository;
import com.rbac.security.chat.AuthenticatedUser;
import com.rbac.service.SessionService;
import io.livekit.server.AccessToken;
import io.livekit.server.RoomJoin;
import io.livekit.server.RoomName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface AdminLiveSessionService {

    List<AdminLiveSessionResponse> getLiveSessions();

    List<AdminLiveSessionResponse> getAllSessions();

    AdminLiveSessionResponse forceEndSession(String sessionId, AuthenticatedUser admin);

    SessionResponse.SessionStatisticsResponse getStatistics(String sessionId);

    AttendanceSummaryResponse getAttendanceSummary(String sessionId);

    AdminSessionWatchResponse watchSession(String sessionId, AuthenticatedUser admin);

    List<ParticipantResponse> getParticipants(String sessionId);
}

@Slf4j
@Service
@RequiredArgsConstructor
class AdminLiveSessionServiceImpl implements AdminLiveSessionService {

    private final SessionRepository sessionRepository;
    private final BatchRepository batchRepository;
    private final AttendanceRepository attendanceRepository;
    private final ParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final SessionService sessionService;

    @Value("${livekit.api.key}")
    private String apiKey;

    @Value("${livekit.api.secret}")
    private String apiSecret;

    @Value("${livekit.url}")
    private String livekitUrl;

    @Override
    public List<AdminLiveSessionResponse> getLiveSessions() {
        return sessionRepository.findByStatus(SessionStatus.LIVE).stream()
                .map(this::toAdminResponse)
                .sorted(Comparator.comparing(AdminLiveSessionResponse::getStartedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Override
    public List<AdminLiveSessionResponse> getAllSessions() {
        return sessionRepository.findAll().stream()
                .map(this::toAdminResponse)
                .sorted(Comparator.comparing(AdminLiveSessionResponse::getStartedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Override
    public AdminLiveSessionResponse forceEndSession(String sessionId, AuthenticatedUser admin) {
        // Delegates to the existing SessionService so activity logs, notifications,
        // and the trainer/admin authorization rule stay in one place.
        sessionService.endSession(sessionId, admin);
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));
        log.info("Session {} force-ended by admin {}", sessionId, admin.getUserId());
        return toAdminResponse(session);
    }

    @Override
    public SessionResponse.SessionStatisticsResponse getStatistics(String sessionId) {
        return sessionService.getStatistics(sessionId);
    }

    @Override
    public AttendanceSummaryResponse getAttendanceSummary(String sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));

        String batchName = null;
        List<User> batchStudents = List.of();
        if (session.getBatchId() != null) {
            batchName = batchRepository.findById(session.getBatchId()).map(Batch::getName).orElse(null);
            batchStudents = userRepository.findByBatchIdsContaining(session.getBatchId());
        }

        List<Attendance> records = attendanceRepository.findBySessionId(sessionId);
        Map<String, Attendance> byUserId = records.stream()
                .collect(Collectors.toMap(Attendance::getUserId, a -> a, (a, b) -> a));

        // Every student assigned to the batch gets a row: PRESENT/LATE/LEFT_EARLY if they
        // have an attendance record, ABSENT if they never joined at all.
        List<AttendanceSummaryResponse.StudentAttendanceRow> rows = batchStudents.stream()
                .map(student -> {
                    Attendance record = byUserId.get(student.getId());
                    AttendanceStatus status = record != null ? record.getStatus() : AttendanceStatus.ABSENT;
                    return AttendanceSummaryResponse.StudentAttendanceRow.builder()
                            .userId(student.getId())
                            .studentName(student.getUsername())
                            .email(student.getEmail())
                            .status(status)
                            .joinTime(record != null ? record.getJoinTime() : null)
                            .leaveTime(record != null ? record.getLeaveTime() : null)
                            .durationSeconds(record != null ? record.getDurationSeconds() : null)
                            .build();
                })
                .sorted(Comparator.comparing(AttendanceSummaryResponse.StudentAttendanceRow::getStudentName,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        // Fall back to raw attendance records (e.g. batch roster changed since the session,
        // or the session has no batch) so nobody who actually joined gets dropped.
        if (batchStudents.isEmpty() && !records.isEmpty()) {
            rows = records.stream()
                    .map(record -> AttendanceSummaryResponse.StudentAttendanceRow.builder()
                            .userId(record.getUserId())
                            .studentName(userRepository.findById(record.getUserId())
                                    .map(User::getUsername).orElse(record.getUserId()))
                            .status(record.getStatus())
                            .joinTime(record.getJoinTime())
                            .leaveTime(record.getLeaveTime())
                            .durationSeconds(record.getDurationSeconds())
                            .build())
                    .toList();
        }

        long total = rows.size();
        long present = rows.stream().filter(r -> r.getStatus() == AttendanceStatus.PRESENT).count();
        long absent = rows.stream().filter(r -> r.getStatus() == AttendanceStatus.ABSENT).count();
        long late = rows.stream().filter(r -> r.getStatus() == AttendanceStatus.LATE).count();
        long leftEarly = rows.stream().filter(r -> r.getStatus() == AttendanceStatus.LEFT_EARLY).count();

        double attendancePercentage = total == 0 ? 0.0
                : Math.round(((present + late) * 1000.0 / total)) / 10.0;

        return AttendanceSummaryResponse.builder()
                .sessionId(sessionId)
                .sessionTitle(session.getTitle())
                .batchId(session.getBatchId())
                .batchName(batchName)
                .totalStudents(total)
                .presentCount(present)
                .absentCount(absent)
                .lateCount(late)
                .leftEarlyCount(leftEarly)
                .attendancePercentage(attendancePercentage)
                .students(rows)
                .build();
    }

    @Override
    public AdminSessionWatchResponse watchSession(String sessionId, AuthenticatedUser admin) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));

        if (session.getStatus() != SessionStatus.LIVE) {
            throw new InvalidRequestException("Session " + sessionId + " is not live");
        }


        String observerIdentity = "admin-observer-" + admin.getUserId();

        AccessToken token = new AccessToken(apiKey, apiSecret);
        token.setName(admin.getUserName() + " (Admin)");
        token.setIdentity(observerIdentity);
        token.addGrants(new RoomJoin(true), new RoomName(sessionId));

        List<ParticipantResponse> participants = participantRepository.findBySessionId(sessionId).stream()
                .map(ParticipantResponse::fromEntity)
                .toList();

        log.info("Admin {} watching session {}", admin.getUserId(), sessionId);

        return AdminSessionWatchResponse.builder()
                .sessionId(sessionId)
                .token(token.toJwt())
                .url(livekitUrl)
                .roomName(sessionId)
                .identity(observerIdentity)
                .session(toAdminResponse(session))
                .participants(participants)
                .build();
    }

    @Override
    public List<ParticipantResponse> getParticipants(String sessionId) {
        if (!sessionRepository.existsById(sessionId)) {
            throw new ResourceNotFoundException("Session not found: " + sessionId);
        }
        return participantRepository.findBySessionId(sessionId).stream()
                .map(ParticipantResponse::fromEntity)
                .toList();
    }

    private AdminLiveSessionResponse toAdminResponse(Session session) {
        String batchName = session.getBatchId() == null ? null :
                batchRepository.findById(session.getBatchId()).map(Batch::getName).orElse(null);

        long totalParticipants = attendanceRepository.findBySessionId(session.getId()).size();
        long activeParticipants = participantRepository
                .countBySessionIdAndStatus(session.getId(), ParticipantStatus.ACTIVE);

        long duration = session.getStartedAt() == null ? 0 :
                java.time.Duration.between(session.getStartedAt(),
                        session.getEndedAt() != null ? session.getEndedAt() : Instant.now()
                ).getSeconds();

        return AdminLiveSessionResponse.builder()
                .sessionId(session.getId())
                .title(session.getTitle())
                .trainerId(session.getTrainerId())
                .trainerName(session.getTrainerName())
                .batchId(session.getBatchId())
                .batchName(batchName)
                .status(session.getStatus())
                .startedAt(session.getStartedAt())
                .endedAt(session.getEndedAt())
                .durationSeconds(Math.max(duration, 0))
                .totalParticipants(totalParticipants)
                .activeParticipants(activeParticipants)
                .locked(session.isLocked())
                .trainerConnected(session.isTrainerConnected())
                .build();
    }
}