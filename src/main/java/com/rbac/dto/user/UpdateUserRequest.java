package com.rbac.dto.user;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class UpdateUserRequest {

    @Email(message = "Email must be valid")
    private String email;

    private String displayName;
    private String department;
    private String designation;
    private String studentId;
    private String trainerId;
    private String employeeId;
}
