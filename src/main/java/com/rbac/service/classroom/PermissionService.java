package com.rbac.service.classroom;

import com.rbac.dto.classroom.ParticipantResponse;
import com.rbac.dto.classroom.UpdatePermissionsRequest;
import com.rbac.exception.chat.ResourceNotFoundException;
import com.rbac.exception.chat.UnauthorizedActionException;
import com.rbac.model.classroom.ActivityType;
import com.rbac.model.classroom.Participant;
import com.rbac.repository.ParticipantRepository;
import com.rbac.security.chat.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

public interface PermissionService {
    ParticipantResponse updatePermissions(String sessionId, String participantId, UpdatePermissionsRequest request, AuthenticatedUser requester);
}

@Service
@RequiredArgsConstructor
class PermissionServiceImpl implements PermissionService {

    private final ParticipantRepository participantRepository;
    private final ActivityLogService activityLogService;
    private final ClassroomNotificationService notificationService;

    @Override
    public ParticipantResponse updatePermissions(String sessionId, String participantId,
                                                 UpdatePermissionsRequest request, AuthenticatedUser requester) {
        if (!requester.isTrainerOrAdmin()) {
            throw new UnauthorizedActionException("Only the trainer or an admin can change permissions");
        }

        Participant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found: " + participantId));

        if (request.getCanSpeak() != null) participant.setCanSpeak(request.getCanSpeak());
        if (request.getCanChat() != null) participant.setCanChat(request.getCanChat());
        if (request.getCanScreenShare() != null) participant.setCanScreenShare(request.getCanScreenShare());
        participant.setUpdatedAt(Instant.now());

        Participant saved = participantRepository.save(participant);

        activityLogService.record(sessionId, requester.getUserId(), requester.getUserName(),
                ActivityType.PERMISSION_CHANGED, "Updated permissions for " + participant.getUserName());

        notificationService.broadcast(sessionId, "PERMISSIONS_CHANGED", ParticipantResponse.fromEntity(saved));
        notificationService.notifyUser(participant.getUserId(), "PERMISSIONS_CHANGED", ParticipantResponse.fromEntity(saved));

        return ParticipantResponse.fromEntity(saved);
    }
}