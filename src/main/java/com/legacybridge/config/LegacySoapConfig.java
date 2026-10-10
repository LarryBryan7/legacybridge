package com.legacybridge.config;

import com.legacybridge.legacy.jaxb.AuthorizeOperationRequest;
import com.legacybridge.legacy.jaxb.AuthorizeOperationResponse;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;

/**
 * Configura las dos puntas de la integracion SOAP:
 *  - Cliente: Jaxb2Marshaller + WebServiceTemplate, usados por
 *    SoapLegacyOperationGateway para hablarle al legacy.
 *  - "Servidor" (Mock Legacy System): el MessageDispatcherServlet de
 *    Spring-WS en /ws/*, que enruta hacia MockLegacyEndpoint, mas la
 *    definicion que publica el WSDL real en /ws/legacyOperation.wsdl.
 *
 * Ambas puntas viven en la misma app a proposito (ver README): un
 * sistema legacy real estaria en otra red, pero el mecanismo de
 * comunicacion (HTTP + SOAP/XML real) es identico.
 */
@Configuration
public class LegacySoapConfig {

    private static final String NAMESPACE_URI = "http://legacybridge.com/legacy/operation";

    @Bean
    public Jaxb2Marshaller legacyMarshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        // setClassesToBeBound (no setContextPath): las clases se anotaron a
        // mano, sin generar un ObjectFactory/jaxb.index para el paquete -
        // ver la nota de diseño en el README sobre por que no se genero
        // codigo desde el XSD. Con setContextPath, JAXBContext exige uno de
        // esos dos mecanismos aunque no se use codigo generado.
        marshaller.setClassesToBeBound(AuthorizeOperationRequest.class, AuthorizeOperationResponse.class);
        return marshaller;
    }

    @Bean
    public WebServiceTemplate legacyWebServiceTemplate(Jaxb2Marshaller legacyMarshaller) {
        WebServiceTemplate template = new WebServiceTemplate();
        template.setMarshaller(legacyMarshaller);
        template.setUnmarshaller(legacyMarshaller);
        return template;
    }

    // --- Mock Legacy System: servidor SOAP embebido ---

    @Bean
    public ServletRegistrationBean<MessageDispatcherServlet> mockLegacyDispatcherServlet(ApplicationContext applicationContext) {
        MessageDispatcherServlet servlet = new MessageDispatcherServlet();
        servlet.setApplicationContext(applicationContext);
        servlet.setTransformWsdlLocations(true);
        return new ServletRegistrationBean<>(servlet, "/ws/*");
    }

    @Bean(name = "legacyOperation")
    public DefaultWsdl11Definition legacyOperationWsdl(XsdSchema legacyOperationSchema) {
        DefaultWsdl11Definition definition = new DefaultWsdl11Definition();
        definition.setPortTypeName("LegacyOperationPort");
        definition.setLocationUri("/ws/legacy");
        definition.setTargetNamespace(NAMESPACE_URI);
        definition.setSchema(legacyOperationSchema);
        return definition;
    }

    @Bean
    public XsdSchema legacyOperationSchema() {
        return new SimpleXsdSchema(new ClassPathResource("xsd/legacy-operation.xsd"));
    }
}
