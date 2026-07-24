package com.rbac.service.classroom;

import com.rbac.dto.classroom.ParticipantResponse;
import com.rbac.exception.chat.InvalidRequestException;
import com.rbac.exception.chat.ResourceNotFoundException;
import com.rbac.exception.chat.UnauthorizedActionException;
import com.rbac.model.classroom.ActivityType;
import com.rbac.model.classroom.Participant;
import com.rbac.model.classroom.ParticipantStatus;
import com.rbac.model.session.Session;
import com.rbac.model.session.SessionStatus;
import com.rbac.repository.ParticipantRepository;
import com.rbac.repository.SessionRepository;
import com.rbac.security.chat.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ParticipantService {

    ParticipantResponse requestJoin(String sessionId, AuthenticatedUser user);

    List<ParticipantResponse> getParticipants(String sessionId, String search, AuthenticatedUser requester);

    ParticipantResponse removeParticipant(String sessionId, String participantId, boolean allowRejoin, AuthenticatedUser requester);

    ParticipantResponse rejoin(String sessionId, AuthenticatedUser user);

    ParticipantResponse markDisconnected(String sessionId, String userId);

    ParticipantResponse leave(String sessionId, AuthenticatedUser user);

    long getLiveCount(String sessionId);
}

@Slf4j
@Service
@RequiredArgsConstructor
class ParticipantServiceImpl implements ParticipantService {

    private final ParticipantRepository participantRepository;
    private final SessionRepository sessionRepository;
    private final ActivityLogService activityLogService;
    private final ClassroomNotificationService notificationService;

    @Override
    public ParticipantResponse requestJoin(String sessionId, AuthenticatedUser user) {
        Session session = requireLiveSession(sessionId);
        if (session.isLocked()) {
            throw new InvalidRequestException("This session is locked and not accepting new participants");
        }

        List<Participant> existing =
                participantRepository.findAllBySessionIdAndUserId(
                        sessionId,
                        user.getUserId());

        Participant participant;

        if (existing.isEmpty()) {

            participant = Participant.builder()
                    .sessionId(sessionId)
                    .userId(user.getUserId())
                    .userName(user.getUserName())
                    .build();

        } else {

            participant = existing.get(0);

            if (participant.getStatus() == ParticipantStatus.REMOVED
                    && !participant.isCanRejoin()) {

                throw new UnauthorizedActionException(
                        "You are not permitted to rejoin this session");
            }
        }

        boolean isTrainer = user.isTrainerOrAdmin();
        participant.setStatus(isTrainer ? ParticipantStatus.ACTIVE : ParticipantStatus.WAITING);
        if (participant.getStatus() == ParticipantStatus.ACTIVE) {
            participant.setJoinedAt(Instant.now());
        }
        participant.setUpdatedAt(Instant.now());
        Participant saved = participantRepository.save(participant);

        activityLogService.record(sessionId, user.getUserId(), user.getUserName(),
                isTrainer ? ActivityType.JOIN : ActivityType.WAITING_ROOM_REQUEST,
                isTrainer ? "Trainer joined the session" : "Requested to join, placed in waiting room");

        notificationService.broadcast(sessionId,
                isTrainer ? "PARTICIPANT_JOINED" : "WAITING_ROOM_REQUEST",
                ParticipantResponse.fromEntity(saved));

        return ParticipantResponse.fromEntity(saved);
    }

    @Override
    public List<ParticipantResponse> getParticipants(String sessionId, String search, AuthenticatedUser requester) {
        requireSessionExists(sessionId);
        List<Participant> participants = participantRepository.findBySessionId(sessionId);

        if (search != null && !search.isBlank()) {
            String q = search.toLowerCase();
            participants = participants.stream()
                    .filter(p -> (p.getUserName() != null && p.getUserName().toLowerCase().contains(q))
                            || (p.getEmail() != null && p.getEmail().toLowerCase().contains(q)))
                    .toList();
        }

        return participants.stream().map(ParticipantResponse::fromEntity).toList();
    }

    @Override
    public ParticipantResponse removeParticipant(String sessionId, String participantId, boolean allowRejoin, AuthenticatedUser requester) {
        requireTrainerOrAdmin(requester);
        Participant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found: " + participantId));

        participant.setStatus(ParticipantStatus.REMOVED);
        participant.setCanRejoin(allowRejoin);
        participant.setLeftAt(Instant.now());
        participant.setUpdatedAt(Instant.now());
        Participant saved = participantRepository.save(participant);

        activityLogService.record(sessionId, requester.getUserId(), requester.getUserName(),
                ActivityType.PARTICIPANT_REMOVED, "Removed " + participant.getUserName() + " from the session");

        notificationService.broadcast(sessionId, "PARTICIPANT_REMOVED", ParticipantResponse.fromEntity(saved));
        notificationService.notifyUser(participant.getUserId(), "YOU_WERE_REMOVED", ParticipantResponse.fromEntity(saved));

        return ParticipantResponse.fromEntity(saved);
    }

    @Override
    public ParticipantResponse rejoin(String sessionId, AuthenticatedUser user) {
        Participant participant = participantRepository.findBySessionIdAndUserId(sessionId, user.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("You have not previously joined this session"));

        if (participant.getStatus() == ParticipantStatus.REMOVED && !participant.isCanRejoin()) {
            throw new UnauthorizedActionException("You are not permitted to rejoin this session");
        }

        Session session = requireLiveSession(sessionId);
        if (session.isLocked()) {
            throw new InvalidRequestException("This session is locked and not accepting new participants");
        }

        participant.setStatus(ParticipantStatus.ACTIVE);
        participant.setJoinedAt(Instant.now());
        participant.setUpdatedAt(Instant.now());
        Participant saved = participantRepository.save(participant);

        activityLogService.record(sessionId, user.getUserId(), user.getUserName(),
                ActivityType.PARTICIPANT_REJOINED, "Rejoined the session");
        notificationService.broadcast(sessionId, "PARTICIPANT_REJOINED", ParticipantResponse.fromEntity(saved));

        return ParticipantResponse.fromEntity(saved);
    }

    @Override
    public ParticipantResponse markDisconnected(String sessionId, String userId) {
        Participant participant = participantRepository.findBySessionIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found"));

        participant.setStatus(ParticipantStatus.DISCONNECTED);
        participant.setUpdatedAt(Instant.now());
        Participant saved = participantRepository.save(participant);

        activityLogService.record(sessionId, userId, participant.getUserName(),
                ActivityType.DISCONNECTED, "Connection lost");
        notificationService.broadcast(sessionId, "PARTICIPANT_DISCONNECTED", ParticipantResponse.fromEntity(saved));

        return ParticipantResponse.fromEntity(saved);
    }

    @Override
    public ParticipantResponse leave(String sessionId, AuthenticatedUser user) {
        Participant participant = participantRepository.findBySessionIdAndUserId(sessionId, user.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found"));

        participant.setStatus(ParticipantStatus.LEFT);
        participant.setLeftAt(Instant.now());
        participant.setUpdatedAt(Instant.now());
        Participant saved = participantRepository.save(participant);

        activityLogService.record(sessionId, user.getUserId(), user.getUserName(),
                ActivityType.LEAVE, "Left the session");
        notificationService.broadcast(sessionId, "PARTICIPANT_LEFT", ParticipantResponse.fromEntity(saved));

        return ParticipantResponse.fromEntity(saved);
    }

    @Override
    public long getLiveCount(String sessionId) {
        return participantRepository.countBySessionIdAndStatus(sessionId, ParticipantStatus.ACTIVE);
    }

    private Session requireLiveSession(String sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));
        if (session.getStatus() != SessionStatus.LIVE) {
            throw new InvalidRequestException("Session is not live");
        }
        return session;
    }

    private void requireSessionExists(String sessionId) {
        if (!sessionRepository.existsById(sessionId)) {
            throw new ResourceNotFoundException("Session not found: " + sessionId);
        }
    }

    private void requireTrainerOrAdmin(AuthenticatedUser requester) {
        if (!requester.isTrainerOrAdmin()) {
            throw new UnauthorizedActionException("Only the trainer or an admin can perform this action");
        }
    }
}