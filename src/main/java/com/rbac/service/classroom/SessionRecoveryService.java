package com.rbac.service.classroom;

import com.rbac.dto.classroom.ParticipantResponse;
import com.rbac.dto.session.SessionResponse;
import com.rbac.model.classroom.ActivityType;
import com.rbac.model.classroom.Participant;
import com.rbac.model.session.Session;
import com.rbac.model.session.SessionStatus;
import com.rbac.repository.ParticipantRepository;
import com.rbac.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

public interface SessionRecoveryService {

    void handleTrainerDisconnected(String trainerUserId, String trainerUserName);

    void handleTrainerReconnected(String trainerUserId, String trainerUserName);
}

@Slf4j
@Service
@RequiredArgsConstructor
class SessionRecoveryServiceImpl implements SessionRecoveryService {

    private final SessionRepository sessionRepository;
    private final ParticipantRepository participantRepository;
    private final ActivityLogService activityLogService;
    private final ClassroomNotificationService notificationService;
    private final TaskScheduler taskScheduler;

    /** sessionId -> pending auto-end task, so a timely reconnect can cancel it. */
    private final Map<String, ScheduledFuture<?>> pendingTimeouts = new ConcurrentHashMap<>();

    @Override
    public void handleTrainerDisconnected(String trainerUserId, String trainerUserName) {
        List<Session> liveSessions = sessionRepository.findByTrainerIdAndStatus(trainerUserId, SessionStatus.LIVE);

        for (Session session : liveSessions) {
            if (!session.isTrainerConnected()) {
                // Already marked disconnected (e.g. duplicate event) - leave the existing timer running.
                continue;
            }

            session.setTrainerConnected(false);
            session.setTrainerDisconnectedAt(Instant.now());
            Session saved = sessionRepository.save(session);

            log.info("Trainer {} disconnected from session {}. Reconnect window: {}s",
                    trainerUserId, session.getId(), session.getReconnectTimeoutSeconds());

            activityLogService.record(session.getId(), trainerUserId, trainerUserName,
                    ActivityType.TRAINER_DISCONNECTED,
                    "Trainer disconnected. Students remain connected while the trainer has "
                            + session.getReconnectTimeoutSeconds() + "s to reconnect.");

            // Students stay exactly as they are - we only inform them the trainer dropped.
            notificationService.broadcast(session.getId(), "TRAINER_DISCONNECTED", SessionResponse.fromEntity(saved));

            scheduleAutoEnd(session.getId(), session.getReconnectTimeoutSeconds());
        }
    }

    @Override
    public void handleTrainerReconnected(String trainerUserId, String trainerUserName) {
        List<Session> liveSessions = sessionRepository.findByTrainerIdAndStatus(trainerUserId, SessionStatus.LIVE);

        for (Session session : liveSessions) {
            if (session.isTrainerConnected()) {
                // Nothing to recover from (e.g. first-ever connect at session start).
                continue;
            }

            cancelPendingTimeout(session.getId());

            session.setTrainerConnected(true);
            session.setTrainerDisconnectedAt(null);
            Session saved = sessionRepository.save(session);

            log.info("Trainer {} reconnected to session {}", trainerUserId, session.getId());

            activityLogService.record(session.getId(), trainerUserId, trainerUserName,
                    ActivityType.TRAINER_RECONNECTED, "Trainer reconnected before the timeout expired");

            List<Participant> participants = participantRepository.findBySessionId(session.getId());

            notificationService.broadcast(session.getId(), "TRAINER_RECONNECTED", Map.of(
                    "session", SessionResponse.fromEntity(saved),
                    "participants", participants.stream().map(ParticipantResponse::fromEntity).toList()
            ));
        }
    }

    private void scheduleAutoEnd(String sessionId, int timeoutSeconds) {
        cancelPendingTimeout(sessionId);
        ScheduledFuture<?> future = taskScheduler.schedule(
                () -> autoEndIfStillDisconnected(sessionId),
                Instant.now().plusSeconds(timeoutSeconds)
        );
        pendingTimeouts.put(sessionId, future);
    }

    private void cancelPendingTimeout(String sessionId) {
        ScheduledFuture<?> existing = pendingTimeouts.remove(sessionId);
        if (existing != null) {
            existing.cancel(false);
        }
    }

    private void autoEndIfStillDisconnected(String sessionId) {
        pendingTimeouts.remove(sessionId);

        sessionRepository.findById(sessionId).ifPresent(session -> {
            if (session.getStatus() != SessionStatus.LIVE || session.isTrainerConnected()) {
                return; // already reconnected, or ended some other way
            }

            session.setStatus(SessionStatus.ENDED);
            session.setEndedAt(Instant.now());
            Session saved = sessionRepository.save(session);

            log.info("Session {} auto-ended: trainer did not reconnect within {}s",
                    sessionId, session.getReconnectTimeoutSeconds());

            activityLogService.record(sessionId, session.getTrainerId(), session.getTrainerName(),
                    ActivityType.SESSION_AUTO_ENDED,
                    "Session automatically ended after the trainer failed to reconnect within "
                            + session.getReconnectTimeoutSeconds() + "s");

            notificationService.broadcast(sessionId, "SESSION_ENDED", SessionResponse.fromEntity(saved));
        });
    }
}