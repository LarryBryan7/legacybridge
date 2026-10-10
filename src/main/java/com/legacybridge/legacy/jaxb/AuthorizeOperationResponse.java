package com.legacybridge.legacy.jaxb;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@XmlRootElement(name = "authorizeOperationResponse")
@XmlAccessorType(XmlAccessType.FIELD)
@Getter
@Setter
@NoArgsConstructor
public class AuthorizeOperationResponse {

    @XmlElement
    private String legacyOperationId;

    @XmlElement(required = true)
    private String status;

    @XmlElement(required = true)
    private String message;
}
