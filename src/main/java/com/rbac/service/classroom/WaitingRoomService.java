package com.rbac.service.classroom;

import com.rbac.dto.classroom.ParticipantResponse;
import com.rbac.exception.chat.ResourceNotFoundException;
import com.rbac.exception.chat.UnauthorizedActionException;
import com.rbac.model.classroom.ActivityType;
import com.rbac.model.classroom.Participant;
import com.rbac.model.classroom.ParticipantStatus;
import com.rbac.repository.ParticipantRepository;
import com.rbac.security.chat.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

public interface WaitingRoomService {
    List<ParticipantResponse> getWaitingList(String sessionId, AuthenticatedUser requester);
    ParticipantResponse approve(String sessionId, String participantId, AuthenticatedUser requester);
    ParticipantResponse reject(String sessionId, String participantId, AuthenticatedUser requester);
}

@Service
@RequiredArgsConstructor
class WaitingRoomServiceImpl implements WaitingRoomService {

    private final ParticipantRepository participantRepository;
    private final ActivityLogService activityLogService;
    private final ClassroomNotificationService notificationService;

    @Override
    public List<ParticipantResponse> getWaitingList(String sessionId, AuthenticatedUser requester) {
        requireTrainerOrAdmin(requester);
        return participantRepository.findBySessionIdAndStatus(sessionId, ParticipantStatus.WAITING)
                .stream().map(ParticipantResponse::fromEntity).toList();
    }

    @Override
    public ParticipantResponse approve(String sessionId, String participantId, AuthenticatedUser requester) {
        requireTrainerOrAdmin(requester);
        Participant participant = getWaiting(participantId);

        participant.setStatus(ParticipantStatus.ACTIVE);
        participant.setJoinedAt(Instant.now());
        participant.setUpdatedAt(Instant.now());
        Participant saved = participantRepository.save(participant);

        activityLogService.record(sessionId, requester.getUserId(), requester.getUserName(),
                ActivityType.WAITING_ROOM_APPROVED, "Admitted " + participant.getUserName());

        notificationService.broadcast(sessionId, "WAITING_ROOM_APPROVED", ParticipantResponse.fromEntity(saved));
        notificationService.notifyUser(participant.getUserId(), "JOIN_APPROVED", ParticipantResponse.fromEntity(saved));

        return ParticipantResponse.fromEntity(saved);
    }

    @Override
    public ParticipantResponse reject(String sessionId, String participantId, AuthenticatedUser requester) {
        requireTrainerOrAdmin(requester);
        Participant participant = getWaiting(participantId);

        participant.setStatus(ParticipantStatus.REJECTED);
        participant.setUpdatedAt(Instant.now());
        Participant saved = participantRepository.save(participant);

        activityLogService.record(sessionId, requester.getUserId(), requester.getUserName(),
                ActivityType.WAITING_ROOM_REJECTED, "Rejected " + participant.getUserName());

        notificationService.broadcast(sessionId, "WAITING_ROOM_REJECTED", ParticipantResponse.fromEntity(saved));
        notificationService.notifyUser(participant.getUserId(), "JOIN_REJECTED", ParticipantResponse.fromEntity(saved));

        return ParticipantResponse.fromEntity(saved);
    }

    private Participant getWaiting(String participantId) {
        return participantRepository.findById(participantId)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found: " + participantId));
    }

    private void requireTrainerOrAdmin(AuthenticatedUser requester) {
        if (!requester.isTrainerOrAdmin()) {
            throw new UnauthorizedActionException("Only the trainer or an admin can manage the waiting room");
        }
    }
}