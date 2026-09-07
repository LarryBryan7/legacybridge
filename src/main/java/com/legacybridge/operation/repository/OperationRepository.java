package com.legacybridge.operation.repository;

import com.legacybridge.operation.entity.Operation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OperationRepository extends JpaRepository<Operation, Long> {

    Optional<Operation> findByExternalId(String externalId);
}
