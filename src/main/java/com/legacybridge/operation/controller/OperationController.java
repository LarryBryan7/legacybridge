package com.legacybridge.operation.controller;

import com.legacybridge.operation.dto.OperationCreateRequest;
import com.legacybridge.operation.dto.OperationResponse;
import com.legacybridge.operation.service.OperationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/operations")
@Tag(name = "Operations", description = "Operaciones gestionadas por LegacyBridge")
public class OperationController {

    private final OperationService operationService;

    public OperationController(OperationService operationService) {
        this.operationService = operationService;
    }

    @GetMapping
    @Operation(summary = "Lista todas las operaciones registradas")
    public List<OperationResponse> getAll() {
        return operationService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene una operacion por su id interno")
    public OperationResponse getById(@PathVariable Long id) {
        return operationService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Registra una nueva operacion en estado PENDING (requiere ROLE_ADMIN)")
    public OperationResponse create(@Valid @RequestBody OperationCreateRequest request) {
        return operationService.create(request);
    }
}
