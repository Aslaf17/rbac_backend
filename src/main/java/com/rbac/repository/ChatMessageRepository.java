package com.rbac.repository;

import com.rbac.model.chat.ChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

    List<ChatMessage> findBySessionIdAndDeletedFalseOrderByTimestampAsc(String sessionId);

    Optional<ChatMessage> findByIdAndDeletedFalse(String id);
}
