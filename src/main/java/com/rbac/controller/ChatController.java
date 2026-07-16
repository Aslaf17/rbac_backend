package com.rbac.controller;

import com.rbac.dto.chat.ChatMessageResponse;
import com.rbac.dto.chat.SendMessageRequest;
import com.rbac.security.chat.AuthenticatedUser;
import com.rbac.security.chat.CurrentUserProvider;
import com.rbac.service.chat.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping("/send")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ChatMessageResponse> sendMessage(@Valid @RequestBody SendMessageRequest request) {
        AuthenticatedUser sender = currentUserProvider.getCurrentUser();
        ChatMessageResponse response = chatService.sendMessage(request, sender);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/session/{sessionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ChatMessageResponse>> getSessionMessages(@PathVariable String sessionId) {
        AuthenticatedUser requester = currentUserProvider.getCurrentUser();
        List<ChatMessageResponse> messages = chatService.getSessionMessages(sessionId, requester);
        return ResponseEntity.ok(messages);
    }

    @DeleteMapping("/{messageId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<Void> deleteMessage(@PathVariable String messageId) {
        AuthenticatedUser requester = currentUserProvider.getCurrentUser();
        chatService.deleteMessage(messageId, requester);
        return ResponseEntity.noContent().build();
    }
}
