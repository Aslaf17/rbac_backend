package com.rbac.dto.whiteboard;

import com.rbac.model.whiteboard.ToolType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateWhiteboardRequest {

    @NotBlank(message = "whiteboardId is required")
    private String whiteboardId;

    private Map<String, Object> drawingData;

    private ToolType toolType;

    private String color;

    @Positive(message = "strokeWidth must be greater than 0")
    private Double strokeWidth;
}
