package com.rbac.repository;

import com.rbac.model.session.Session;
import com.rbac.model.session.SessionStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SessionRepository extends MongoRepository<Session, String> {

    java.util.List<Session> findByStatus(SessionStatus status);

    java.util.List<Session> findByTrainerIdAndStatus(String trainerId, SessionStatus status);

    java.util.List<Session> findByBatchId(String batchId);
}