package com.rbac.service.user;

import com.rbac.dto.user.AssignRoleRequest;
import com.rbac.dto.user.ChangePasswordRequest;
import com.rbac.dto.user.CreateUserRequest;
import com.rbac.dto.user.PageResponse;
import com.rbac.dto.user.ResetPasswordRequest;
import com.rbac.dto.user.UpdateProfileRequest;
import com.rbac.dto.user.UpdateStatusRequest;
import com.rbac.dto.user.UpdateUserRequest;
import com.rbac.dto.user.UserResponse;
import com.rbac.exception.user.DuplicateUserException;
import com.rbac.exception.user.InvalidRequestException;
import com.rbac.exception.user.ResourceNotFoundException;
import com.rbac.exception.user.UnauthorizedActionException;
import com.rbac.model.login.Role;
import com.rbac.model.login.User;
import com.rbac.repository.UserRepository;
import com.rbac.security.chat.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;

public interface UserService {

    UserResponse getMyProfile(AuthenticatedUser requester);

    UserResponse updateMyProfile(AuthenticatedUser requester, UpdateProfileRequest request);

    void changeMyPassword(AuthenticatedUser requester, ChangePasswordRequest request);

    PageResponse<UserResponse> searchUsers(String search, Role role, Boolean active, int page, int size);

    UserResponse getUser(String userId, AuthenticatedUser requester);

    UserResponse createUser(CreateUserRequest request);

    UserResponse updateUser(String userId, UpdateUserRequest request);

    void deleteUser(String userId, AuthenticatedUser admin);

    UserResponse updateStatus(String userId, UpdateStatusRequest request, AuthenticatedUser admin);

    UserResponse assignRole(String userId, AssignRoleRequest request);

    void resetPassword(String userId, ResetPasswordRequest request);
}

@Slf4j
@Service
@RequiredArgsConstructor
class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponse getMyProfile(AuthenticatedUser requester) {
        return UserResponse.fromEntity(findOrThrow(requester.getUserId()));
    }

    @Override
    public UserResponse updateMyProfile(AuthenticatedUser requester, UpdateProfileRequest request) {
        User user = findOrThrow(requester.getUserId());

        user.setDisplayName(request.getDisplayName());
        user.setDepartment(request.getDepartment());
        user.setDesignation(request.getDesignation());
        user.setStudentId(request.getStudentId());
        user.setTrainerId(request.getTrainerId());
        user.setEmployeeId(request.getEmployeeId());
        user.setUpdatedAt(Instant.now());

        userRepository.save(user);
        return UserResponse.fromEntity(user);
    }

    @Override
    public void changeMyPassword(AuthenticatedUser requester, ChangePasswordRequest request) {
        User user = findOrThrow(requester.getUserId());

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidRequestException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
    }

    @Override
    public PageResponse<UserResponse> searchUsers(String search, Role role, Boolean active, int page, int size) {
        Query query = new Query();

        if (StringUtils.hasText(search)) {
            String needle = search.trim();
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("username").regex(needle, "i"),
                    Criteria.where("email").regex(needle, "i"),
                    Criteria.where("displayName").regex(needle, "i")
            ));
        }
        if (role != null) {
            query.addCriteria(Criteria.where("role").is(role));
        }
        if (active != null) {
            query.addCriteria(Criteria.where("active").is(active));
        }

        long total = mongoTemplate.count(query, User.class);

        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 10 : size;
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        query.with(pageable);

        List<User> users = mongoTemplate.find(query, User.class);
        List<UserResponse> content = users.stream().map(UserResponse::fromEntity).toList();

        int totalPages = safeSize == 0 ? 0 : (int) Math.ceil((double) total / safeSize);
        return new PageResponse<>(content, safePage, safeSize, total, totalPages);
    }

    @Override
    public UserResponse getUser(String userId, AuthenticatedUser requester) {
        if (!requester.hasRole(Role.ADMIN) && !requester.getUserId().equals(userId)) {
            throw new UnauthorizedActionException("You can only view your own profile");
        }
        return UserResponse.fromEntity(findOrThrow(userId));
    }

    @Override
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateUserException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateUserException("Email is already registered");
        }

        Role role = parseRole(request.getRole());

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        user.setActive(request.isActive());
        user.setDisplayName(request.getDisplayName());
        user.setDepartment(request.getDepartment());
        user.setDesignation(request.getDesignation());
        user.setStudentId(request.getStudentId());
        user.setTrainerId(request.getTrainerId());
        user.setEmployeeId(request.getEmployeeId());

        try {
            userRepository.save(user);
        } catch (DuplicateKeyException ex) {
            throw new DuplicateUserException("Username or email is already in use");
        }

        log.info("Admin created user {} with role {}", user.getUsername(), role);
        return UserResponse.fromEntity(user);
    }

    @Override
    public UserResponse updateUser(String userId, UpdateUserRequest request) {
        User user = findOrThrow(userId);

        if (StringUtils.hasText(request.getEmail()) && !request.getEmail().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmailAndIdNot(request.getEmail(), userId)) {
                throw new DuplicateUserException("Email is already registered to another user");
            }
            user.setEmail(request.getEmail());
        }

        user.setDisplayName(request.getDisplayName());
        user.setDepartment(request.getDepartment());
        user.setDesignation(request.getDesignation());
        user.setStudentId(request.getStudentId());
        user.setTrainerId(request.getTrainerId());
        user.setEmployeeId(request.getEmployeeId());
        user.setUpdatedAt(Instant.now());

        userRepository.save(user);
        return UserResponse.fromEntity(user);
    }

    @Override
    public void deleteUser(String userId, AuthenticatedUser admin) {
        User user = findOrThrow(userId);

        if (user.getId().equals(admin.getUserId())) {
            throw new InvalidRequestException("You cannot delete your own account");
        }

        userRepository.delete(user);
        log.info("Admin {} deleted user {}", admin.getUserName(), user.getUsername());
    }

    @Override
    public UserResponse updateStatus(String userId, UpdateStatusRequest request, AuthenticatedUser admin) {
        User user = findOrThrow(userId);

        if (user.getId().equals(admin.getUserId()) && !request.getActive()) {
            throw new InvalidRequestException("You cannot deactivate your own account");
        }

        user.setActive(request.getActive());
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
        return UserResponse.fromEntity(user);
    }

    @Override
    public UserResponse assignRole(String userId, AssignRoleRequest request) {
        User user = findOrThrow(userId);
        Role role = parseRole(request.getRole());

        user.setRole(role);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
        return UserResponse.fromEntity(user);
    }

    @Override
    public void resetPassword(String userId, ResetPasswordRequest request) {
        User user = findOrThrow(userId);
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
        log.info("Password was reset for user {}", user.getUsername());
    }

    private User findOrThrow(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    private Role parseRole(String rawRole) {
        try {
            return Role.valueOf(rawRole.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidRequestException(
                    "Role must be one of: STUDENT, TEACHER, EMPLOYER, EMPLOYEE, ADMIN");
        }
    }
}
