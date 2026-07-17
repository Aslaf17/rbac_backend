package com.rbac.model.chat;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "chat_messages")
@CompoundIndexes({
        @CompoundIndex(name = "session_timestamp_idx", def = "{'sessionId': 1, 'timestamp': 1}")
})
public class ChatMessage {

    @Id
    private String id; // messageId

    @Indexed
    private String sessionId;

    private String senderId;

    private String senderName;

    private String message;

    private MessageType messageType;

    private LocalDateTime timestamp;

    @CreatedDate
    private LocalDateTime dateCreated;

    @Builder.Default
    private boolean deleted = false;

    private String deletedBy;

    private LocalDateTime deletedAt;
}

