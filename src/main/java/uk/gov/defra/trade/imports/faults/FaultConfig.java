package uk.gov.defra.trade.imports.faults;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/** Registers the fault filter. */
@Configuration
public class FaultConfig {

    /**
     * Registers the fault filter straight after the latency filter, so a fault's time adds to the
     * latency the stub answered with.
     *
     * @param stubFaults the faults that can be injected
     * @return the filter registration
     */
    @Bean
    FilterRegistrationBean<FaultFilter> faultFilter(StubFaults stubFaults) {
        FilterRegistrationBean<FaultFilter> registration =
            new FilterRegistrationBean<>(new FaultFilter(stubFaults));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 2);
        return registration;
    }
}
