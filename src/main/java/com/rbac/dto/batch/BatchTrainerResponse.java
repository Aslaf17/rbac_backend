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
public class BatchTrainerResponse {

    private String userId;
    private String username;
    private String email;

    public static BatchTrainerResponse fromEntity(User user) {
        return BatchTrainerResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
    }
}