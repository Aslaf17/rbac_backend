package com.rbac.model.batch;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "batches")
public class Batch {

    @Id
    private String id;

    @Indexed
    private String name;

    private String description;

    @Indexed
    private BatchStatus status = BatchStatus.ACTIVE;

    private String createdBy;
    private String createdByName;

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
}