package com.rbac.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AssignRoleRequest {

    @NotBlank(message = "Role is required")
    private String role;
}
