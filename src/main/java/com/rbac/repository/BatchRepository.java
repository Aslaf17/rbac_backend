package com.rbac.repository;

import com.rbac.model.batch.Batch;
import com.rbac.model.batch.BatchStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface BatchRepository extends MongoRepository<Batch, String> {

    List<Batch> findByStatus(BatchStatus status);
}
