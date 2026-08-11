package com.rbac.model.login;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    @Indexed(unique = true)
    private String email;

    private String password;

    private Role role;

    private boolean active = true;

    private Instant createdAt = Instant.now();

    private Instant updatedAt;

    @Indexed
    private Set<String> batchIds = new HashSet<>();

    // Optional profile fields — set via self-service or admin profile updates
    private String displayName;
    private String department;
    private String designation;
    private String studentId;
    private String trainerId;
    private String employeeId;

}
