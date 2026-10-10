package com.legacybridge.operation.service;

import com.legacybridge.common.exception.ResourceNotFoundException;
import com.legacybridge.legacy.LegacyOperationGateway;
import com.legacybridge.operation.dto.OperationCreateRequest;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperationServiceTest {

    @Mock
    private OperationRepository operationRepository;

    // No participa en estos tests (create/findById/findAll no llaman al
    // legacy): ver OperationServiceExecuteTest para el comportamiento de
    // execute() con LegacyOperationGateway.
    @Mock
    private LegacyOperationGateway legacyOperationGateway;

    private OperationService operationService;

    @Test
    void create_deberiaPersistirOperacionEnEstadoPending() {
        operationService = new OperationService(operationRepository, legacyOperationGateway);

        OperationCreateRequest request = new OperationCreateRequest("OP-1001", new BigDecimal("2500.00"), "PEN");

        when(operationRepository.save(any(Operation.class))).thenAnswer(invocation -> {
            Operation toSave = invocation.getArgument(0);
            toSave.setId(1L);
            toSave.setStatus(OperationStatus.PENDING);
            toSave.setCreatedAt(Instant.now());
            toSave.setUpdatedAt(Instant.now());
            return toSave;
        });

        OperationResponse response = operationService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.externalId()).isEqualTo("OP-1001");
        assertThat(response.amount()).isEqualByComparingTo("2500.00");
        assertThat(response.currency()).isEqualTo("PEN");
        assertThat(response.status()).isEqualTo(OperationStatus.PENDING);

        ArgumentCaptor<Operation> captor = ArgumentCaptor.forClass(Operation.class);
        verify(operationRepository).save(captor.capture());
        assertThat(captor.getValue().getExternalId()).isEqualTo("OP-1001");
    }

    @Test
    void findById_deberiaRetornarLaOperacionCuandoExiste() {
        operationService = new OperationService(operationRepository, legacyOperationGateway);

        Operation existing = new Operation();
        existing.setId(5L);
        existing.setExternalId("OP-2002");
        existing.setAmount(new BigDecimal("100.00"));
        existing.setCurrency("USD");
        existing.setStatus(OperationStatus.COMPLETED);
        existing.setCreatedAt(Instant.now());
        existing.setUpdatedAt(Instant.now());

        when(operationRepository.findById(5L)).thenReturn(Optional.of(existing));

        OperationResponse response = operationService.findById(5L);

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.status()).isEqualTo(OperationStatus.COMPLETED);
    }

    @Test
    void findById_deberiaLanzarExcepcionCuandoNoExiste() {
        operationService = new OperationService(operationRepository, legacyOperationGateway);

        when(operationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> operationService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }
}
