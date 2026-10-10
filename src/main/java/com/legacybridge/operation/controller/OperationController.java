package com.legacybridge.operation.controller;

import com.legacybridge.operation.dto.OperationCreateRequest;
import com.legacybridge.operation.dto.OperationResponse;
import com.legacybridge.operation.service.OperationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

    @PostMapping("/{id}/execute")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(
            summary = "Envia la operacion al sistema legacy (SOAP/XML) y actualiza su estado (requiere ROLE_ADMIN)",
            description = "Delega en OperationService -> LegacyOperationGateway. La Operation pasa a "
                    + "PROCESSING y, segun la respuesta del legacy, termina en COMPLETED (con legacyOperationId) o FAILED."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "El legacy respondio; ver el campo status del resultado"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Autenticado pero sin ROLE_ADMIN"),
            @ApiResponse(responseCode = "404", description = "No existe una Operation con ese id"),
            @ApiResponse(responseCode = "502", description = "Error de comunicacion con el sistema legacy o respuesta invalida")
    })
    public OperationResponse execute(@PathVariable Long id) {
        return operationService.execute(id);
    }
}
