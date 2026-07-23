package com.rbac.service.classroom;

import com.rbac.dto.classroom.ParticipantResponse;
import com.rbac.exception.chat.ResourceNotFoundException;
import com.rbac.exception.chat.UnauthorizedActionException;
import com.rbac.model.classroom.ActivityType;
import com.rbac.model.classroom.CameraStatus;
import com.rbac.model.classroom.MicStatus;
import com.rbac.model.classroom.Participant;
import com.rbac.model.classroom.ParticipantStatus;
import com.rbac.repository.ParticipantRepository;
import com.rbac.security.chat.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

public interface MediaControlService {
    ParticipantResponse toggleOwnMic(String sessionId, AuthenticatedUser user, boolean unmute);
    ParticipantResponse toggleOwnCamera(String sessionId, AuthenticatedUser user, boolean on);
    ParticipantResponse trainerMute(String sessionId, String participantId, AuthenticatedUser requester);
    List<ParticipantResponse> muteAll(String sessionId, AuthenticatedUser requester);
    ParticipantResponse setSelfUnmuteAllowed(String sessionId, String participantId, boolean allowed, AuthenticatedUser requester);
    ParticipantResponse requestCameraOn(String sessionId, String participantId, AuthenticatedUser requester);
}

@Service
@RequiredArgsConstructor
class MediaControlServiceImpl implements MediaControlService {

    private final ParticipantRepository participantRepository;
    private final ActivityLogService activityLogService;
    private final ClassroomNotificationService notificationService;

    @Override
    public ParticipantResponse toggleOwnMic(String sessionId, AuthenticatedUser user, boolean unmute) {
        Participant participant = getSelf(sessionId, user);

        if (unmute && !participant.isCanUnmuteSelf() && !user.isTrainerOrAdmin()) {
            throw new UnauthorizedActionException("The trainer has not allowed you to unmute yourself");
        }

        participant.setMicStatus(unmute ? MicStatus.UNMUTED : MicStatus.MUTED);
        participant.setUpdatedAt(Instant.now());
        Participant saved = participantRepository.save(participant);

        activityLogService.record(sessionId, user.getUserId(), user.getUserName(),
                unmute ? ActivityType.UNMUTED : ActivityType.MUTED,
                user.getUserName() + (unmute ? " unmuted themselves" : " muted themselves"));
        notificationService.broadcast(sessionId, "MIC_STATUS_CHANGED", ParticipantResponse.fromEntity(saved));

        return ParticipantResponse.fromEntity(saved);
    }

    @Override
    public ParticipantResponse toggleOwnCamera(String sessionId, AuthenticatedUser user, boolean on) {
        Participant participant = getSelf(sessionId, user);
        participant.setCameraStatus(on ? CameraStatus.ON : CameraStatus.OFF);
        participant.setUpdatedAt(Instant.now());
        Participant saved = participantRepository.save(participant);

        activityLogService.record(sessionId, user.getUserId(), user.getUserName(),
                on ? ActivityType.CAMERA_ON : ActivityType.CAMERA_OFF,
                user.getUserName() + (on ? " turned camera on" : " turned camera off"));
        notificationService.broadcast(sessionId, "CAMERA_STATUS_CHANGED", ParticipantResponse.fromEntity(saved));

        return ParticipantResponse.fromEntity(saved);
    }

    @Override
    public ParticipantResponse trainerMute(String sessionId, String participantId, AuthenticatedUser requester) {
        requireTrainerOrAdmin(requester);
        Participant participant = getById(participantId);

        participant.setMicStatus(MicStatus.MUTED);
        participant.setUpdatedAt(Instant.now());
        Participant saved = participantRepository.save(participant);

        activityLogService.record(sessionId, requester.getUserId(), requester.getUserName(),
                ActivityType.MUTED, "Trainer muted " + participant.getUserName());

        notificationService.broadcast(sessionId, "MIC_STATUS_CHANGED", ParticipantResponse.fromEntity(saved));
        notificationService.notifyUser(participant.getUserId(), "YOU_WERE_MUTED", ParticipantResponse.fromEntity(saved));

        return ParticipantResponse.fromEntity(saved);
    }

    @Override
    public List<ParticipantResponse> muteAll(String sessionId, AuthenticatedUser requester) {
        requireTrainerOrAdmin(requester);
        List<Participant> active = participantRepository.findBySessionIdAndStatus(sessionId, ParticipantStatus.ACTIVE);

        active.forEach(p -> {
            p.setMicStatus(MicStatus.MUTED);
            p.setUpdatedAt(Instant.now());
        });
        List<Participant> saved = participantRepository.saveAll(active);

        activityLogService.record(sessionId, requester.getUserId(), requester.getUserName(),
                ActivityType.MUTE_ALL, "Trainer muted all participants");
        notificationService.broadcast(sessionId, "MUTE_ALL", null);

        return saved.stream().map(ParticipantResponse::fromEntity).toList();
    }

    @Override
    public ParticipantResponse setSelfUnmuteAllowed(String sessionId, String participantId, boolean allowed, AuthenticatedUser requester) {
        requireTrainerOrAdmin(requester);
        Participant participant = getById(participantId);

        participant.setCanUnmuteSelf(allowed);
        participant.setUpdatedAt(Instant.now());
        Participant saved = participantRepository.save(participant);

        activityLogService.record(sessionId, requester.getUserId(), requester.getUserName(),
                ActivityType.SELF_UNMUTE_ALLOWED,
                (allowed ? "Allowed " : "Disallowed ") + participant.getUserName() + " to self-unmute");

        notificationService.broadcast(sessionId, "SELF_UNMUTE_PERMISSION_CHANGED", ParticipantResponse.fromEntity(saved));
        return ParticipantResponse.fromEntity(saved);
    }

    @Override
    public ParticipantResponse requestCameraOn(String sessionId, String participantId, AuthenticatedUser requester) {
        requireTrainerOrAdmin(requester);
        Participant participant = getById(participantId);

        activityLogService.record(sessionId, requester.getUserId(), requester.getUserName(),
                ActivityType.CAMERA_ON_REQUESTED, "Trainer requested " + participant.getUserName() + " to enable camera");

        notificationService.notifyUser(participant.getUserId(), "CAMERA_ON_REQUESTED", ParticipantResponse.fromEntity(participant));
        return ParticipantResponse.fromEntity(participant);
    }

    private Participant getSelf(String sessionId, AuthenticatedUser user) {
        return participantRepository.findBySessionIdAndUserId(sessionId, user.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("You are not a participant of this session"));
    }

    private Participant getById(String participantId) {
        return participantRepository.findById(participantId)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found: " + participantId));
    }

    private void requireTrainerOrAdmin(AuthenticatedUser requester) {
        if (!requester.isTrainerOrAdmin()) {
            throw new UnauthorizedActionException("Only the trainer or an admin can control media for others");
        }
    }
}