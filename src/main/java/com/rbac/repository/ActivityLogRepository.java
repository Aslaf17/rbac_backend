package com.rbac.repository;

import com.rbac.model.classroom.ActivityLog;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ActivityLogRepository extends MongoRepository<ActivityLog, String> {
    List<ActivityLog> findBySessionIdOrderByTimestampDesc(String sessionId);
}