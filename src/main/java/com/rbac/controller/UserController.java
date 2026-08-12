package com.rbac.controller;

import com.rbac.dto.user.AssignRoleRequest;
import com.rbac.dto.user.ChangePasswordRequest;
import com.rbac.dto.user.CreateUserRequest;
import com.rbac.dto.user.PageResponse;
import com.rbac.dto.user.ResetPasswordRequest;
import com.rbac.dto.user.UpdateProfileRequest;
import com.rbac.dto.user.UpdateStatusRequest;
import com.rbac.dto.user.UpdateUserRequest;
import com.rbac.dto.user.UserResponse;
import com.rbac.model.login.Role;
import com.rbac.security.chat.AuthenticatedUser;
import com.rbac.security.chat.CurrentUserProvider;
import com.rbac.service.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final CurrentUserProvider currentUserProvider;

    // ---- Self-service ----

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyProfile() {
        AuthenticatedUser me = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(userService.getMyProfile(me));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMyProfile(@RequestBody UpdateProfileRequest request) {
        AuthenticatedUser me = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(userService.updateMyProfile(me, request));
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> changeMyPassword(@Valid @RequestBody ChangePasswordRequest request) {
        AuthenticatedUser me = currentUserProvider.getCurrentUser();
        userService.changeMyPassword(me, request);
        return ResponseEntity.noContent().build();
    }

    // ---- Admin: user directory ----

    @GetMapping
    public ResponseEntity<PageResponse<UserResponse>> searchUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(userService.searchUsers(search, role, active, page, size));
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUser(@PathVariable String userId) {
        AuthenticatedUser requester = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(userService.getUser(userId, requester));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable String userId, @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(userId, request));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable String userId) {
        AuthenticatedUser admin = currentUserProvider.getCurrentUser();
        userService.deleteUser(userId, admin);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{userId}/status")
    public ResponseEntity<UserResponse> updateStatus(
            @PathVariable String userId, @Valid @RequestBody UpdateStatusRequest request) {
        AuthenticatedUser admin = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(userService.updateStatus(userId, request, admin));
    }

    @PutMapping("/{userId}/role")
    public ResponseEntity<UserResponse> assignRole(
            @PathVariable String userId, @Valid @RequestBody AssignRoleRequest request) {
        return ResponseEntity.ok(userService.assignRole(userId, request));
    }

    @PostMapping("/{userId}/reset-password")
    public ResponseEntity<Void> resetPassword(
            @PathVariable String userId, @Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(userId, request);
        return ResponseEntity.noContent().build();
    }
}
