package com.rbac.service;

import com.rbac.dto.attendance.AttendanceResponse;
import com.rbac.dto.session.SessionResponse;
import com.rbac.dto.session.StartSessionRequest;
import com.rbac.exception.chat.InvalidRequestException;
import com.rbac.exception.chat.ResourceNotFoundException;
import com.rbac.exception.chat.UnauthorizedActionException;
import com.rbac.model.attendance.Attendance;
import com.rbac.model.attendance.AttendanceStatus;
import com.rbac.model.session.Session;
import com.rbac.model.session.SessionStatus;
import com.rbac.repository.AttendanceRepository;
import com.rbac.repository.SessionRepository;
import com.rbac.security.chat.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

public interface SessionService {

    SessionResponse startSession(StartSessionRequest request, AuthenticatedUser trainer);

    SessionResponse endSession(String sessionId, AuthenticatedUser requester);

    SessionResponse getSession(String sessionId);

    AttendanceResponse joinSession(String sessionId, AuthenticatedUser user);
}

@Slf4j
@Service
@RequiredArgsConstructor
class SessionServiceImpl implements SessionService {

    private final SessionRepository sessionRepository;
    private final AttendanceRepository attendanceRepository;

    @Override
    public SessionResponse startSession(StartSessionRequest request, AuthenticatedUser trainer) {
        Session session = new Session();
        session.setTitle(request.getTitle());
        session.setTrainerId(trainer.getUserId());
        session.setTrainerName(trainer.getUserName());
        session.setStatus(SessionStatus.LIVE);
        session.setStartedAt(Instant.now());

        Session saved = sessionRepository.save(session);
        log.info("Session {} started by {}", saved.getId(), trainer.getUserId());

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
}