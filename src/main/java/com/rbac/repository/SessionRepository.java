package com.rbac.repository;

import com.rbac.model.session.Session;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SessionRepository extends MongoRepository<Session, String> {
}
