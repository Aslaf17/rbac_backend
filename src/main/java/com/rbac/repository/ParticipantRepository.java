package com.rbac.repository;

import com.rbac.model.classroom.HandStatus;
import com.rbac.model.classroom.Participant;
import com.rbac.model.classroom.ParticipantStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ParticipantRepository extends MongoRepository<Participant, String> {

    Optional<Participant> findBySessionIdAndUserId(String sessionId, String userId);

    List<Participant> findAllBySessionIdAndUserId(String sessionId, String userId);

    List<Participant> findBySessionId(String sessionId);

    List<Participant> findBySessionIdAndStatus(String sessionId, ParticipantStatus status);

    List<Participant> findByUserIdAndStatus(String userId, ParticipantStatus status);

    List<Participant> findBySessionIdAndHandStatus(String sessionId, HandStatus handStatus);

    long countBySessionIdAndStatus(String sessionId, ParticipantStatus status);

    List<Participant> findBySessionIdAndUserNameContainingIgnoreCaseOrSessionIdAndEmailContainingIgnoreCase(
            String sessionId1,
            String name,
            String sessionId2,
            String email
    );
}