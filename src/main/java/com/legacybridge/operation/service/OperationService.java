package com.legacybridge.operation.service;

import com.legacybridge.common.exception.ResourceNotFoundException;
import com.legacybridge.operation.dto.OperationCreateRequest;
import com.legacybridge.operation.dto.OperationResponse;
import com.legacybridge.operation.entity.Operation;
import com.legacybridge.operation.repository.OperationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Logica de negocio de Operation. En esta fase es CRUD simple; en fases
 * posteriores este servicio delegara al Legacy Adapter para autorizar
 * la operacion contra el sistema legacy.
 */
@Service
public class OperationService {

    private final OperationRepository operationRepository;

    public OperationService(OperationRepository operationRepository) {
        this.operationRepository = operationRepository;
    }

    public OperationResponse create(OperationCreateRequest request) {
        Operation operation = new Operation();
        operation.setExternalId(request.externalId());
        operation.setAmount(request.amount());
        operation.setCurrency(request.currency());

        Operation saved = operationRepository.save(operation);
        return OperationResponse.fromEntity(saved);
    }

    public OperationResponse findById(Long id) {
        Operation operation = operationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe una Operation con id " + id));
        return OperationResponse.fromEntity(operation);
    }

    public List<OperationResponse> findAll() {
        return operationRepository.findAll().stream()
                .map(OperationResponse::fromEntity)
                .toList();
    }
}
