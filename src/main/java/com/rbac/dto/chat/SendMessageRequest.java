package com.rbac.dto.chat;

import com.rbac.model.chat.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {

    @NotBlank(message = "sessionId is required")
    private String sessionId;

    @NotBlank(message = "message cannot be empty")
    @Size(max = 1000, message = "message cannot exceed 1000 characters")
    private String message;

    @NotNull(message = "messageType is required")
    private MessageType messageType;
}
