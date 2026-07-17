package com.rbac.service.chat;

import com.rbac.dto.chat.ChatMessageResponse;
import com.rbac.dto.chat.SendMessageRequest;
import com.rbac.exception.chat.InvalidRequestException;
import com.rbac.exception.chat.ResourceNotFoundException;
import com.rbac.exception.chat.UnauthorizedActionException;
import com.rbac.model.chat.ChatMessage;
import com.rbac.security.chat.AuthenticatedUser;
import com.rbac.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatService {

    ChatMessageResponse sendMessage(SendMessageRequest request, AuthenticatedUser sender);

    List<ChatMessageResponse> getSessionMessages(String sessionId, AuthenticatedUser requester);

    void deleteMessage(String messageId, AuthenticatedUser requester);
}

@Slf4j
@Service
@RequiredArgsConstructor
class ChatServiceImpl implements ChatService {

    private static final int MAX_MESSAGE_LENGTH = 1000;

    private final ChatMessageRepository chatMessageRepository;
    private final SessionValidationService sessionValidationService;

    @Override
    public ChatMessageResponse sendMessage(SendMessageRequest request, AuthenticatedUser sender) {
        validateMessagePayload(request);
        validateSessionAndMembership(request.getSessionId(), sender.getUserId());

        ChatMessage chatMessage = ChatMessage.builder()
                .sessionId(request.getSessionId())
                .senderId(sender.getUserId())
                .senderName(sender.getUserName())
                .message(request.getMessage())
                .messageType(request.getMessageType())
                .timestamp(LocalDateTime.now())
                .deleted(false)
                .build();

        ChatMessage saved = chatMessageRepository.save(chatMessage);
        log.info("Chat message {} saved for session {} by user {}", saved.getId(), saved.getSessionId(), sender.getUserId());

        return ChatMessageResponse.fromEntity(saved);
    }

    @Override
    public List<ChatMessageResponse> getSessionMessages(String sessionId, AuthenticatedUser requester) {
        if (!StringUtils.hasText(sessionId)) {
            throw new InvalidRequestException("sessionId is required");
        }
        validateSessionAndMembership(sessionId, requester.getUserId());

        return chatMessageRepository.findBySessionIdAndDeletedFalseOrderByTimestampAsc(sessionId)
                .stream()
                .map(ChatMessageResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public void deleteMessage(String messageId, AuthenticatedUser requester) {
        if (!requester.isTrainerOrAdmin()) {
            throw new UnauthorizedActionException("Only trainers or admins can delete messages");
        }

        ChatMessage message = chatMessageRepository.findByIdAndDeletedFalse(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found: " + messageId));

        message.setDeleted(true);
        message.setDeletedBy(requester.getUserId());
        message.setDeletedAt(LocalDateTime.now());
        chatMessageRepository.save(message);

        log.info("Chat message {} deleted by {}", messageId, requester.getUserId());
    }

    private void validateMessagePayload(SendMessageRequest request) {
        if (!StringUtils.hasText(request.getSessionId())) {
            throw new InvalidRequestException("sessionId is required");
        }
        if (!StringUtils.hasText(request.getMessage())) {
            throw new InvalidRequestException("Message cannot be empty");
        }
        if (request.getMessage().length() > MAX_MESSAGE_LENGTH) {
            throw new InvalidRequestException("Message cannot exceed " + MAX_MESSAGE_LENGTH + " characters");
        }
        if (request.getMessageType() == null) {
            throw new InvalidRequestException("messageType is required");
        }
    }

    private void validateSessionAndMembership(String sessionId, String userId) {
        if (!sessionValidationService.sessionExists(sessionId)) {
            throw new ResourceNotFoundException("Session not found: " + sessionId);
        }
        if (!sessionValidationService.isUserPartOfSession(sessionId, userId)) {
            throw new UnauthorizedActionException("User is not a participant of this session");
        }
    }
}

