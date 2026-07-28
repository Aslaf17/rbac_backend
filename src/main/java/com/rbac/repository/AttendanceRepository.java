package com.rbac.repository;

import com.rbac.model.attendance.Attendance;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends MongoRepository<Attendance, String> {

    List<Attendance> findBySessionId(String sessionId);

    List<Attendance> findByUserId(String userId);

    Optional<Attendance> findBySessionIdAndUserId(String sessionId, String userId);

    boolean existsBySessionIdAndUserId(String sessionId, String userId);
}