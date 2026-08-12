package com.rbac.dto.user;

import com.rbac.model.login.Role;
import com.rbac.model.login.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private String id;
    private String username;
    private String email;
    private Role role;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
    private Set<String> batchIds;

    private String displayName;
    private String department;
    private String designation;
    private String studentId;
    private String trainerId;
    private String employeeId;

    public static UserResponse fromEntity(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .batchIds(user.getBatchIds())
                .displayName(user.getDisplayName())
                .department(user.getDepartment())
                .designation(user.getDesignation())
                .studentId(user.getStudentId())
                .trainerId(user.getTrainerId())
                .employeeId(user.getEmployeeId())
                .build();
    }
}
