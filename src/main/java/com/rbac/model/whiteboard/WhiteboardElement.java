package com.rbac.model.whiteboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "whiteboard_elements")
@CompoundIndexes({
        @CompoundIndex(name = "session_timestamp_idx", def = "{'sessionId': 1, 'timestamp': 1}")
})
public class WhiteboardElement {

    @Id
    private String id; // whiteboardId

    @Indexed
    private String sessionId;

    private String userId;

    private Map<String, Object> drawingData;

    private ToolType toolType;

    private String color;

    private Double strokeWidth;

    private LocalDateTime timestamp;

    @CreatedDate
    private LocalDateTime dateCreated;

    private LocalDateTime updatedAt;

    @Builder.Default
    private boolean deleted = false;

    private String deletedBy;

    private LocalDateTime deletedAt;
}
