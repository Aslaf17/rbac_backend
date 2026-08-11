package com.rbac.repository.recording;

import com.rbac.model.recording.Recording;
import com.rbac.model.recording.RecordingStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface RecordingRepository extends MongoRepository<Recording, String> {

    Optional<Recording> findBySessionIdAndDeletedFalse(String sessionId);

    boolean existsBySessionIdAndDeletedFalse(String sessionId);

    List<Recording> findByTrainerIdAndDeletedFalse(String trainerId);

    List<Recording> findByStatusAndDeletedFalse(RecordingStatus status);
}

