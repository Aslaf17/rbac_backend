package com.rbac.dto.whiteboard;

import com.rbac.model.whiteboard.ToolType;
import com.rbac.model.whiteboard.WhiteboardElement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhiteboardResponse {

    private String whiteboardId;
    private String sessionId;
    private String userId;
    private Map<String, Object> drawingData;
    private ToolType toolType;
    private String color;
    private Double strokeWidth;
    private LocalDateTime timestamp;
    private LocalDateTime dateCreated;
    private LocalDateTime updatedAt;

    public static WhiteboardResponse fromEntity(WhiteboardElement entity) {
        return WhiteboardResponse.builder()
                .whiteboardId(entity.getId())
                .sessionId(entity.getSessionId())
                .userId(entity.getUserId())
                .drawingData(entity.getDrawingData())
                .toolType(entity.getToolType())
                .color(entity.getColor())
                .strokeWidth(entity.getStrokeWidth())
                .timestamp(entity.getTimestamp())
                .dateCreated(entity.getDateCreated())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
