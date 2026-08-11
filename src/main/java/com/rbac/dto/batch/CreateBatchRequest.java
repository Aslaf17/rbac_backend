package com.rbac.dto.batch;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateBatchRequest {

    @Pattern(regexp = "^[A-Za-z0-9_-]{3,40}$", message = "batchId may only contain letters, digits, '-' and '_' (3-40 chars)")
    private String batchId;

    @Size(max = 15, message = "prefix must be at most 15 characters")
    private String prefix;

    @NotBlank(message = "name is required")
    @Size(max = 120, message = "name must be at most 120 characters")
    private String name;

    @Size(max = 1000, message = "description must be at most 1000 characters")
    private String description;
}
