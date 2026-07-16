package com.rbac.security.chat;

import com.rbac.model.login.Role;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
public class AuthenticatedUser {

    private String userId;
    private String userName;

    private Set<String> roles;

    public boolean isTrainerOrAdmin() {
        return hasRole(Role.TEACHER) || hasRole(Role.ADMIN);
    }

    public boolean hasRole(Role role) {
        return getRoleEnums().contains(role);
    }

    public Set<Role> getRoleEnums() {
        if (roles == null) {
            return EnumSet.noneOf(Role.class);
        }
        return roles.stream()
                .map(AuthenticatedUser::toRole)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(Role.class)));
    }

    private static Role toRole(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.startsWith("ROLE_") ? raw.substring(5) : raw;
        try {
            return Role.valueOf(normalized.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}