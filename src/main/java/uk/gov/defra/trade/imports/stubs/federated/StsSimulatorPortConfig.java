package uk.gov.defra.trade.imports.stubs.federated;

import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The STS simulator gets its own port (plan Step 6, confirmed 2026-09-14 against a live
 * {@code StsClient} call): the AWS SDK's Query-protocol marshaller always POSTs
 * {@code GetWebIdentityToken} to the root path {@code /} of the configured endpoint override,
 * discarding any path segment — so it cannot be routed as a sub-path alongside this app's other
 * endpoints on the main port. {@link StsSimulatorController} answers only on this port; every
 * other endpoint in this app (the main port) is unaffected.
 */
@Configuration
class StsSimulatorPortConfig {

    @Bean
    WebServerFactoryCustomizer<TomcatServletWebServerFactory> stsSimulatorConnector(
        @Value("${sts-simulator.port}") int stsSimulatorPort) {
        return factory -> {
            Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
            connector.setPort(stsSimulatorPort);
            factory.addAdditionalTomcatConnectors(connector);
        };
    }
}
