package com.legacybridge.operation.service;

import com.legacybridge.common.exception.ResourceNotFoundException;
import com.legacybridge.legacy.LegacyIntegrationException;
import com.legacybridge.legacy.LegacyOperationGateway;
import com.legacybridge.legacy.LegacyOperationResult;
import com.legacybridge.operation.dto.OperationCreateRequest;
import com.legacybridge.operation.dto.OperationResponse;
import com.legacybridge.operation.entity.Operation;
import com.legacybridge.operation.entity.OperationStatus;
import com.legacybridge.operation.repository.OperationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Logica de negocio de Operation. Solo conoce LegacyOperationGateway (la
 * interfaz): no sabe, ni le interesa, que la implementacion real habla
 * SOAP/XML.
 */
@Service
public class OperationService {

    private final OperationRepository operationRepository;
    private final LegacyOperationGateway legacyOperationGateway;

    public OperationService(OperationRepository operationRepository, LegacyOperationGateway legacyOperationGateway) {
        this.operationRepository = operationRepository;
        this.legacyOperationGateway = legacyOperationGateway;
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

    /**
     * Envia la Operation al sistema legacy y actualiza su estado segun la
     * respuesta. No esta en una unica transaccion "todo o nada" a
     * proposito: el estado PROCESSING se persiste ANTES de llamar al
     * legacy, para que si la app se cayera a mitad de la llamada SOAP, la
     * Operation quede visible como PROCESSING (y no mentirosamente como
     * PENDING) en vez de perderse silenciosamente.
     */
    public OperationResponse execute(Long id) {
        Operation operation = operationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe una Operation con id " + id));

        marcarComo(operation, OperationStatus.PROCESSING, null);

        LegacyOperationResult result;
        try {
            result = legacyOperationGateway.authorize(
                    operation.getExternalId(), operation.getAmount(), operation.getCurrency());
        } catch (LegacyIntegrationException e) {
            marcarComo(operation, OperationStatus.FAILED, null);
            throw e;
        }

        OperationStatus estadoFinal = result.isSuccessful() ? OperationStatus.COMPLETED : OperationStatus.FAILED;
        marcarComo(operation, estadoFinal, result.legacyOperationId());

        return OperationResponse.fromEntity(operation);
    }

    // Nota deliberada: NO se anota @Transactional aqui. Cada llamada a
    // operationRepository.save(...) ya es atomica por si sola (Spring Data
    // JPA envuelve cada metodo de repositorio en su propia transaccion), y
    // ademas @Transactional en un metodo privado/con auto-invocacion desde
    // execute() no funcionaria: el proxy AOP de Spring no intercepta
    // llamadas "this.metodo(...)" dentro de la misma clase.
    private void marcarComo(Operation operation, OperationStatus status, String legacyOperationId) {
        operation.setStatus(status);
        if (legacyOperationId != null) {
            operation.setLegacyOperationId(legacyOperationId);
        }
        operationRepository.save(operation);
    }
}
