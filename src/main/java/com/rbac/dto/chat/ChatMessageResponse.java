package com.rbac.dto.chat;

import com.rbac.model.chat.ChatMessage;
import com.rbac.model.chat.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {

    private String messageId;
    private String sessionId;
    private String senderId;
    private String senderName;
    private String message;
    private MessageType messageType;
    private LocalDateTime timestamp;
    private LocalDateTime dateCreated;

    public static ChatMessageResponse fromEntity(ChatMessage entity) {
        return ChatMessageResponse.builder()
                .messageId(entity.getId())
                .sessionId(entity.getSessionId())
                .senderId(entity.getSenderId())
                .senderName(entity.getSenderName())
                .message(entity.getMessage())
                .messageType(entity.getMessageType())
                .timestamp(entity.getTimestamp())
                .dateCreated(entity.getDateCreated())
                .build();
    }
}
