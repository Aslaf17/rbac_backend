package com.rbac.repository;

import com.rbac.model.whiteboard.WhiteboardElement;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface WhiteboardRepository extends MongoRepository<WhiteboardElement, String> {

    List<WhiteboardElement> findBySessionIdAndDeletedFalseOrderByTimestampAsc(String sessionId);

    Optional<WhiteboardElement> findByIdAndDeletedFalse(String id);

    List<WhiteboardElement> findBySessionIdAndDeletedFalse(String sessionId);
}
