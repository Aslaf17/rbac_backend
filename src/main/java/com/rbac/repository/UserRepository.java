package com.rbac.repository;

import com.rbac.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    java.util.List<com.rbac.model.login.User> findByRole(com.rbac.model.login.Role role);
}
