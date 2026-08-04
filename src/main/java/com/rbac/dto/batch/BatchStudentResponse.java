package com.rbac.dto.batch;

import com.rbac.model.login.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchStudentResponse {

    private String userId;
    private String username;
    private String email;

    public static BatchStudentResponse fromEntity(User user) {
        return BatchStudentResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
    }
}
