package com.rbac.service.classroom;

import com.rbac.dto.classroom.ParticipantResponse;
import com.rbac.exception.chat.ResourceNotFoundException;
import com.rbac.exception.chat.UnauthorizedActionException;
import com.rbac.model.classroom.ActivityType;
import com.rbac.model.classroom.HandStatus;
import com.rbac.model.classroom.Participant;
import com.rbac.repository.ParticipantRepository;
import com.rbac.security.chat.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

public interface HandRaiseService {
    ParticipantResponse raiseHand(String sessionId, AuthenticatedUser user);
    ParticipantResponse lowerHand(String sessionId, AuthenticatedUser user);
    List<ParticipantResponse> getRaisedHands(String sessionId, AuthenticatedUser requester);
    ParticipantResponse approve(String sessionId, String participantId, AuthenticatedUser requester);
    ParticipantResponse dismiss(String sessionId, String participantId, AuthenticatedUser requester);
}

@Service
@RequiredArgsConstructor
class HandRaiseServiceImpl implements HandRaiseService {

    private final ParticipantRepository participantRepository;
    private final ActivityLogService activityLogService;
    private final ClassroomNotificationService notificationService;

    @Override
    public ParticipantResponse raiseHand(String sessionId, AuthenticatedUser user) {
        Participant participant = getSelf(sessionId, user);
        participant.setHandStatus(HandStatus.RAISED);
        participant.setHandRaisedAt(Instant.now());
        participant.setUpdatedAt(Instant.now());
        Participant saved = participantRepository.save(participant);

        activityLogService.record(sessionId, user.getUserId(), user.getUserName(),
                ActivityType.HAND_RAISED, user.getUserName() + " raised their hand");
        notificationService.broadcast(sessionId, "HAND_RAISED", ParticipantResponse.fromEntity(saved));

        return ParticipantResponse.fromEntity(saved);
    }

    @Override
    public ParticipantResponse lowerHand(String sessionId, AuthenticatedUser user) {
        Participant participant = getSelf(sessionId, user);
        participant.setHandStatus(HandStatus.NONE);
        participant.setHandRaisedAt(null);
        participant.setUpdatedAt(Instant.now());
        Participant saved = participantRepository.save(participant);

        activityLogService.record(sessionId, user.getUserId(), user.getUserName(),
                ActivityType.HAND_LOWERED, user.getUserName() + " lowered their hand");
        notificationService.broadcast(sessionId, "HAND_LOWERED", ParticipantResponse.fromEntity(saved));

        return ParticipantResponse.fromEntity(saved);
    }

    @Override
    public List<ParticipantResponse> getRaisedHands(String sessionId, AuthenticatedUser requester) {
        requireTrainerOrAdmin(requester);
        return participantRepository.findBySessionIdAndHandStatus(sessionId, HandStatus.RAISED)
                .stream().map(ParticipantResponse::fromEntity).toList();
    }

    @Override
    public ParticipantResponse approve(String sessionId, String participantId, AuthenticatedUser requester) {
        requireTrainerOrAdmin(requester);
        Participant participant = getById(participantId);

        participant.setHandStatus(HandStatus.APPROVED);
        participant.setCanSpeak(true);
        participant.setUpdatedAt(Instant.now());
        Participant saved = participantRepository.save(participant);

        activityLogService.record(sessionId, requester.getUserId(), requester.getUserName(),
                ActivityType.HAND_APPROVED, "Approved " + participant.getUserName() + " to speak");

        notificationService.broadcast(sessionId, "HAND_APPROVED", ParticipantResponse.fromEntity(saved));
        notificationService.notifyUser(participant.getUserId(), "HAND_APPROVED", ParticipantResponse.fromEntity(saved));

        return ParticipantResponse.fromEntity(saved);
    }

    @Override
    public ParticipantResponse dismiss(String sessionId, String participantId, AuthenticatedUser requester) {
        requireTrainerOrAdmin(requester);
        Participant participant = getById(participantId);

        participant.setHandStatus(HandStatus.DISMISSED);
        participant.setUpdatedAt(Instant.now());
        Participant saved = participantRepository.save(participant);

        activityLogService.record(sessionId, requester.getUserId(), requester.getUserName(),
                ActivityType.HAND_DISMISSED, "Dismissed " + participant.getUserName() + "'s raised hand");

        notificationService.broadcast(sessionId, "HAND_DISMISSED", ParticipantResponse.fromEntity(saved));
        notificationService.notifyUser(participant.getUserId(), "HAND_DISMISSED", ParticipantResponse.fromEntity(saved));

        return ParticipantResponse.fromEntity(saved);
    }

    private Participant getSelf(String sessionId, AuthenticatedUser user) {

        List<Participant> participants =
                participantRepository.findAllBySessionIdAndUserId(sessionId, user.getUserId());
        if (participants.isEmpty()) {
            throw new ResourceNotFoundException(
                    "You are not a participant of this session");
        }

        return participants.get(0);
    }

    private Participant getById(String participantId) {
        return participantRepository.findById(participantId)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found: " + participantId));
    }

    private void requireTrainerOrAdmin(AuthenticatedUser requester) {
        if (!requester.isTrainerOrAdmin()) {
            throw new UnauthorizedActionException("Only the trainer or an admin can manage raised hands");
        }
    }
}