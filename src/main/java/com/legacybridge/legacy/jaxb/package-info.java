/**
 * Namespace SOAP compartido por todas las clases JAXB de este paquete.
 * Debe coincidir exactamente con el targetNamespace de
 * src/main/resources/xsd/legacy-operation.xsd y con el namespace usado
 * en @PayloadRoot de MockLegacyEndpoint.
 */
@XmlSchema(
        namespace = "http://legacybridge.com/legacy/operation",
        elementFormDefault = XmlNsForm.QUALIFIED
)
package com.legacybridge.legacy.jaxb;

import jakarta.xml.bind.annotation.XmlNsForm;
import jakarta.xml.bind.annotation.XmlSchema;
