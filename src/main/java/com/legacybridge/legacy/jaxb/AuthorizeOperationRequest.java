package com.legacybridge.legacy.jaxb;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Mensaje SOAP de request. El nombre del elemento raiz ("authorizeOperationRequest")
 * es lo que Spring-WS usa para enrutar al @Endpoint correcto via @PayloadRoot.
 */
@XmlRootElement(name = "authorizeOperationRequest")
@XmlAccessorType(XmlAccessType.FIELD)
@Getter
@Setter
@NoArgsConstructor
public class AuthorizeOperationRequest {

    @XmlElement(required = true)
    private String externalId;

    @XmlElement(required = true)
    private BigDecimal amount;

    @XmlElement(required = true)
    private String currency;
}
