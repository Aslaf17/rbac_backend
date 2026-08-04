package com.rbac.dto.batch;

import com.rbac.model.batch.Batch;
import com.rbac.model.batch.BatchStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchResponse {

    private String batchId;
    private String name;
    private String description;
    private BatchStatus status;
    private String createdBy;
    private String createdByName;
    private Instant createdAt;
    private Instant updatedAt;
    private long studentCount;

    public static BatchResponse fromEntity(Batch batch, long studentCount) {
        return BatchResponse.builder()
                .batchId(batch.getId())
                .name(batch.getName())
                .description(batch.getDescription())
                .status(batch.getStatus())
                .createdBy(batch.getCreatedBy())
                .createdByName(batch.getCreatedByName())
                .createdAt(batch.getCreatedAt())
                .updatedAt(batch.getUpdatedAt())
                .studentCount(studentCount)
                .build();
    }
}
