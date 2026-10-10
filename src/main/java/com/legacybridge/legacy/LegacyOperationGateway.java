package com.legacybridge.legacy;

import java.math.BigDecimal;

/**
 * Frontera entre LegacyBridge y el sistema legacy. OperationService solo
 * conoce esta interfaz: no sabe (ni deberia saber) que la implementacion
 * concreta habla SOAP/XML por HTTP. Si en el futuro el legacy cambiara
 * de protocolo, solo cambiaria la implementacion, nunca el Service.
 */
public interface LegacyOperationGateway {

    LegacyOperationResult authorize(String externalId, BigDecimal amount, String currency);
}
