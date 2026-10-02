package uk.gov.defra.trade.imports.latency;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/** Registers the {@code stub-latency} configuration properties and the latency filter. */
@Configuration
@EnableConfigurationProperties(StubLatencyProperties.class)
public class LatencyConfig {

    /**
     * Registers the latency filter straight after the request tracing filter.
     *
     * @param stubLatencies the profiled integrations
     * @return the filter registration
     */
    @Bean
    FilterRegistrationBean<LatencyFilter> latencyFilter(StubLatencies stubLatencies) {
        FilterRegistrationBean<LatencyFilter> registration =
            new FilterRegistrationBean<>(new LatencyFilter(stubLatencies));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return registration;
    }
}
