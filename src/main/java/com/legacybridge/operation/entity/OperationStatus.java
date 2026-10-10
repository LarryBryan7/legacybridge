package com.legacybridge.operation.entity;

public enum OperationStatus {
    /** Recien creada, todavia no se envio al sistema legacy. */
    PENDING,
    /** El gateway esta esperando la respuesta SOAP del sistema legacy. */
    PROCESSING,
    /** El sistema legacy autorizo la operacion. */
    COMPLETED,
    /** El sistema legacy la rechazo, o hubo un error de comunicacion/respuesta invalida. */
    FAILED
}
