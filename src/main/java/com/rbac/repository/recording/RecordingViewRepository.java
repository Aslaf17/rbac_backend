package com.rbac.repository.recording;

import com.rbac.model.recording.RecordingView;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface RecordingViewRepository extends MongoRepository<RecordingView, String> {

    Optional<RecordingView> findByRecordingIdAndUserId(String recordingId, String userId);

    List<RecordingView> findByRecordingId(String recordingId);
}

