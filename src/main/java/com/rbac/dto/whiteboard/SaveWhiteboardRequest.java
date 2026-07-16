package com.rbac.dto.whiteboard;

import com.rbac.model.whiteboard.ToolType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaveWhiteboardRequest {

    @NotBlank(message = "sessionId is required")
    private String sessionId;

    @NotEmpty(message = "drawingData cannot be empty")
    private Map<String, Object> drawingData;

    @NotNull(message = "toolType is required")
    private ToolType toolType;

    @NotBlank(message = "color is required")
    private String color;

    @NotNull(message = "strokeWidth is required")
    @Positive(message = "strokeWidth must be greater than 0")
    private Double strokeWidth;

    private String userId;
}