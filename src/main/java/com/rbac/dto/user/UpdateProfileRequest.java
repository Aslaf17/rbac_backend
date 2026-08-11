package com.rbac.dto.user;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String displayName;
    private String department;
    private String designation;
    private String studentId;
    private String trainerId;
    private String employeeId;
}
