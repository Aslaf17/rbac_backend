package com.rbac.service.chat;

import com.rbac.repository.AttendanceRepository;
import com.rbac.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

public interface SessionValidationService {

    boolean sessionExists(String sessionId);

    boolean isUserPartOfSession(String sessionId, String userId);
}

@Service
@RequiredArgsConstructor
class SessionValidationServiceImpl implements SessionValidationService {

    private final SessionRepository sessionRepository;
    private final AttendanceRepository attendanceRepository;

    @Override
    public boolean sessionExists(String sessionId) {
        return sessionRepository.existsById(sessionId);
    }

    @Override
    public boolean isUserPartOfSession(String sessionId, String userId) {
        boolean isTrainer = sessionRepository.findById(sessionId)
                .map(session -> session.getTrainerId().equals(userId))
                .orElse(false);

        return isTrainer || attendanceRepository.existsBySessionIdAndUserId(sessionId, userId);
    }
}
