package com.legacybridge.operation.service;

import com.legacybridge.common.exception.ResourceNotFoundException;
import com.legacybridge.legacy.LegacyIntegrationException;
import com.legacybridge.legacy.LegacyOperationGateway;
import com.legacybridge.legacy.LegacyOperationResult;
import com.legacybridge.operation.dto.OperationResponse;
import com.legacybridge.operation.entity.Operation;
import com.legacybridge.operation.entity.OperationStatus;
import com.legacybridge.operation.repository.OperationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifica que OperationService.execute() usa LegacyOperationGateway (no
 * SOAP/XML directamente) y que el estado de la Operation refleja
 * correctamente el resultado que devuelve el gateway.
 */
@ExtendWith(MockitoExtension.class)
class OperationServiceExecuteTest {

    @Mock
    private OperationRepository operationRepository;

    @Mock
    private LegacyOperationGateway legacyOperationGateway;

    private OperationService operationService;

    private Operation operacionPendiente() {
        Operation operation = new Operation();
        operation.setId(1L);
        operation.setExternalId("OP-1001");
        operation.setAmount(new BigDecimal("100.00"));
        operation.setCurrency("PEN");
        operation.setStatus(OperationStatus.PENDING);
        operation.setCreatedAt(Instant.now());
        operation.setUpdatedAt(Instant.now());
        return operation;
    }

    @Test
    void execute_cuandoElLegacyAutoriza_dejaLaOperacionEnCompletedConSuId() {
        operationService = new OperationService(operationRepository, legacyOperationGateway);
        Operation operation = operacionPendiente();

        when(operationRepository.findById(1L)).thenReturn(Optional.of(operation));
        when(operationRepository.save(any(Operation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(legacyOperationGateway.authorize("OP-1001", new BigDecimal("100.00"), "PEN"))
                .thenReturn(new LegacyOperationResult("LEGACY-999", "OK", "Operacion autorizada por el sistema legacy"));

        OperationResponse response = operationService.execute(1L);

        assertThat(response.status()).isEqualTo(OperationStatus.COMPLETED);
        assertThat(response.legacyOperationId()).isEqualTo("LEGACY-999");

        // Se persiste dos veces: al pasar a PROCESSING y al llegar a COMPLETED.
        verify(operationRepository, times(2)).save(any(Operation.class));
    }

    @Test
    void execute_cuandoElLegacyRechaza_dejaLaOperacionEnFailedSinLegacyOperationId() {
        operationService = new OperationService(operationRepository, legacyOperationGateway);
        Operation operation = operacionPendiente();

        when(operationRepository.findById(1L)).thenReturn(Optional.of(operation));
        when(operationRepository.save(any(Operation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(legacyOperationGateway.authorize(any(), any(), any()))
                .thenReturn(new LegacyOperationResult(null, "ERROR", "El monto excede el limite permitido"));

        OperationResponse response = operationService.execute(1L);

        assertThat(response.status()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.legacyOperationId()).isNull();
    }

    @Test
    void execute_cuandoFallaLaComunicacionConElLegacy_marcaFailedYPropagaLaExcepcion() {
        operationService = new OperationService(operationRepository, legacyOperationGateway);
        Operation operation = operacionPendiente();

        when(operationRepository.findById(1L)).thenReturn(Optional.of(operation));
        when(operationRepository.save(any(Operation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(legacyOperationGateway.authorize(any(), any(), any()))
                .thenThrow(new LegacyIntegrationException("No se pudo establecer comunicacion con el sistema legacy"));

        assertThatThrownBy(() -> operationService.execute(1L))
                .isInstanceOf(LegacyIntegrationException.class);

        ArgumentCaptor<Operation> captor = ArgumentCaptor.forClass(Operation.class);
        verify(operationRepository, times(2)).save(captor.capture());
        Operation ultimoGuardado = captor.getValue();
        assertThat(ultimoGuardado.getStatus()).isEqualTo(OperationStatus.FAILED);
    }

    @Test
    void execute_conIdInexistente_lanzaResourceNotFoundExceptionYNuncaLlamaAlGateway() {
        operationService = new OperationService(operationRepository, legacyOperationGateway);

        when(operationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> operationService.execute(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(legacyOperationGateway, org.mockito.Mockito.never()).authorize(any(), any(), any());
    }
}
