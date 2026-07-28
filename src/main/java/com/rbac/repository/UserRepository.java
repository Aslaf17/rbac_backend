package com.rbac.repository;

import com.rbac.model.login.Role;
import com.rbac.model.login.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<User> findByRole(Role role);

    java.util.List<com.rbac.model.login.User> findByBatchId(String batchId);
}