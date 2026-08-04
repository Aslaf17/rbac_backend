package com.rbac.service;

import com.rbac.dto.attendance.AttendanceResponse;
import com.rbac.dto.session.SessionResponse;
import com.rbac.dto.session.StartSessionRequest;
import com.rbac.exception.chat.InvalidRequestException;
import com.rbac.exception.chat.ResourceNotFoundException;
import com.rbac.exception.chat.UnauthorizedActionException;
import com.rbac.model.attendance.Attendance;
import com.rbac.model.attendance.AttendanceStatus;
import com.rbac.model.batch.Batch;
import com.rbac.model.batch.BatchStatus;
import com.rbac.model.login.User;
import com.rbac.model.session.Session;
import com.rbac.model.session.SessionStatus;
import com.rbac.repository.AttendanceRepository;
import com.rbac.repository.BatchRepository;
import com.rbac.repository.SessionRepository;
import com.rbac.security.chat.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SessionService {

    SessionResponse startSession(StartSessionRequest request, AuthenticatedUser trainer);

    SessionResponse endSession(String sessionId, AuthenticatedUser requester);

    SessionResponse getSession(String sessionId);

    AttendanceResponse joinSession(String sessionId, AuthenticatedUser user);

    SessionResponse lockSession(String sessionId, AuthenticatedUser requester);

    SessionResponse unlockSession(String sessionId, AuthenticatedUser requester);

    List<SessionResponse> getAllSessions();
}

@Slf4j
@Service
@RequiredArgsConstructor
class SessionServiceImpl implements SessionService {

    private final SessionRepository sessionRepository;
    private final AttendanceRepository attendanceRepository;
    private final BatchRepository batchRepository;
    private final com.rbac.repository.UserRepository userRepository;
    private final com.rbac.service.classroom.EmailNotificationService emailNotificationService;
    private final com.rbac.service.classroom.ActivityLogService activityLogService;
    private final com.rbac.service.classroom.ClassroomNotificationService notificationService;

    // REPLACE the existing startSession method body with:
    @Override
    public SessionResponse startSession(StartSessionRequest request, AuthenticatedUser trainer) {
        Batch batch = batchRepository.findById(request.getBatchId())
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + request.getBatchId()));
        if (batch.getStatus() != BatchStatus.ACTIVE) {
            throw new InvalidRequestException("Cannot start a classroom for a batch that is not ACTIVE");
        }

        Session session = new Session();
        session.setTitle(request.getTitle());
        session.setTrainerId(trainer.getUserId());
        session.setTrainerName(trainer.getUserName());
        session.setBatchId(batch.getId());
        session.setStatus(SessionStatus.LIVE);
        session.setStartedAt(Instant.now());
        session.setLocked(false);
        session.setTrainerConnected(true);
        if (request.getReconnectTimeoutSeconds() != null && request.getReconnectTimeoutSeconds() > 0) {
            session.setReconnectTimeoutSeconds(request.getReconnectTimeoutSeconds());
        }

        Session saved = sessionRepository.save(session);
        log.info("Session {} started by {}", saved.getId(), trainer.getUserId());

        activityLogService.record(saved.getId(), trainer.getUserId(), trainer.getUserName(),
                com.rbac.model.classroom.ActivityType.SESSION_STARTED, "Session started");
        notificationService.broadcast(saved.getId(), "SESSION_STARTED", SessionResponse.fromEntity(saved));

        // Email only the students belonging to this session's batch — not every student.
        java.util.List<com.rbac.model.login.User> students =
                userRepository.findByBatchIdsContaining(saved.getBatchId());
        emailNotificationService.notifySessionStarted(saved, students);

        return SessionResponse.fromEntity(saved);
    }

    @Override
    public SessionResponse endSession(String sessionId, AuthenticatedUser requester) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));

        boolean isOwner = session.getTrainerId().equals(requester.getUserId());
        boolean isAdmin = requester.getRoles() != null && requester.getRoles().contains("ROLE_ADMIN");
        if (!isOwner && !isAdmin) {
            throw new UnauthorizedActionException("Only the session's trainer or an admin can end it");
        }

        session.setStatus(SessionStatus.ENDED);
        session.setEndedAt(Instant.now());
        Session saved = sessionRepository.save(session);

        log.info("Session {} ended by {}", sessionId, requester.getUserId());
        return SessionResponse.fromEntity(saved);
    }

    @Override
    public SessionResponse getSession(String sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));
        return SessionResponse.fromEntity(session);
    }

    @Override
    public AttendanceResponse joinSession(String sessionId, AuthenticatedUser user) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));

        if (session.getStatus() != SessionStatus.LIVE) {
            throw new InvalidRequestException("Cannot join a session that is not live");
        }

        if (session.getTrainerId().equals(user.getUserId())) {
            throw new UnauthorizedActionException("Trainer cannot join their own session as an attendee");
        }

        // Batch-wise classroom access: students may only join classrooms for their own batch.
        // Trainers/Admins are exempt so they can oversee or co-host any classroom.
        if (user.hasRole(com.rbac.model.login.Role.STUDENT) && !user.isTrainerOrAdmin()) {
            User student = userRepository.findById(user.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + user.getUserId()));
            if (session.getBatchId() == null || student.getBatchIds() == null
                    || !student.getBatchIds().contains(session.getBatchId())) {
                throw new UnauthorizedActionException(
                        "You are not assigned to this classroom's batch and cannot join");
            }
        }

        Optional<Attendance> existing = attendanceRepository
                .findBySessionIdAndUserId(sessionId, user.getUserId());

        Attendance attendance = existing.orElseGet(() -> createAttendance(session, user));

        log.info("User {} joined session {}", user.getUserId(), sessionId);
        return mapToAttendanceResponse(attendance, session, user);
    }

    private Attendance createAttendance(Session session, AuthenticatedUser user) {
        Attendance attendance = new Attendance();
        attendance.setUserId(user.getUserId());
        attendance.setSessionId(session.getId());
        attendance.setJoinTime(Instant.now());
        attendance.setStatus(AttendanceStatus.PRESENT);
        attendance.setCreatedAt(Instant.now());
        attendance.setUpdatedAt(Instant.now());

        try {
            return attendanceRepository.save(attendance);
        } catch (DuplicateKeyException ex) {
            return attendanceRepository.findBySessionIdAndUserId(session.getId(), user.getUserId())
                    .orElseThrow(() -> ex);
        }
    }

    private AttendanceResponse mapToAttendanceResponse(Attendance attendance, Session session, AuthenticatedUser user) {
        AttendanceResponse response = new AttendanceResponse();
        response.setId(attendance.getId());
        response.setUserId(attendance.getUserId());
        response.setStudentName(user.getUserName());
        response.setSessionId(attendance.getSessionId());
        response.setSessionName(session.getTitle());
        response.setJoinTime(attendance.getJoinTime());
        response.setLeaveTime(attendance.getLeaveTime());
        response.setDurationSeconds(attendance.getDurationSeconds());
        response.setStatus(attendance.getStatus());
        return response;
    }

    // ADD inside SessionServiceImpl:
    @Override
    public SessionResponse lockSession(String sessionId, AuthenticatedUser requester) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));

        boolean isOwner = session.getTrainerId().equals(requester.getUserId());
        boolean isAdmin = requester.getRoles() != null && requester.getRoles().contains("ROLE_ADMIN");
        if (!isOwner && !isAdmin) {
            throw new UnauthorizedActionException("Only the session's trainer or an admin can lock it");
        }

        session.setLocked(true);
        Session saved = sessionRepository.save(session);

        activityLogService.record(sessionId, requester.getUserId(), requester.getUserName(),
                com.rbac.model.classroom.ActivityType.SESSION_LOCKED, "Session locked");
        notificationService.broadcast(sessionId, "SESSION_LOCKED", SessionResponse.fromEntity(saved));

        return SessionResponse.fromEntity(saved);
    }

    @Override
    public SessionResponse unlockSession(String sessionId, AuthenticatedUser requester) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));

        boolean isOwner = session.getTrainerId().equals(requester.getUserId());
        boolean isAdmin = requester.getRoles() != null && requester.getRoles().contains("ROLE_ADMIN");
        if (!isOwner && !isAdmin) {
            throw new UnauthorizedActionException("Only the session's trainer or an admin can unlock it");
        }

        session.setLocked(false);
        Session saved = sessionRepository.save(session);

        activityLogService.record(sessionId, requester.getUserId(), requester.getUserName(),
                com.rbac.model.classroom.ActivityType.SESSION_UNLOCKED, "Session unlocked");
        notificationService.broadcast(sessionId, "SESSION_UNLOCKED", SessionResponse.fromEntity(saved));

        return SessionResponse.fromEntity(saved);
    }

    @Override
    public List<SessionResponse> getAllSessions() {
        return sessionRepository.findAll().stream()
                .map(SessionResponse::fromEntity)
                .toList();
    }
}