package com.rbac.security.chat;

import com.rbac.model.login.User;
import com.rbac.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CurrentUserProvider {

    private final UserRepository userRepository;

    public AuthenticatedUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user found in security context");
        }

        Object principal = authentication.getPrincipal();

        String userId;
        String userName;

        if (principal instanceof CurrentUserAware aware) {
            userId = aware.getUserId();
            userName = aware.getFullName();
        } else {
            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalStateException("Authenticated user not found in database: " + username));
            userId = user.getId();
            userName = user.getUsername();
        }

        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        return new AuthenticatedUser(userId, userName, roles);
    }

    public interface CurrentUserAware {
        String getUserId();
        String getFullName();
    }
}
