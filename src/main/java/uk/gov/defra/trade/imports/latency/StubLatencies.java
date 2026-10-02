package uk.gov.defra.trade.imports.latency;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Every integration this stub stands in for, and the request paths each one answers. */
@Component
public class StubLatencies {

    private final List<IntegrationLatency> integrations;
    private final Map<String, IntegrationLatency> byPath = new LinkedHashMap<>();
    private final DoubleSupplier gaussian;

    /**
     * Builds the integrations from configuration.
     *
     * @param properties the {@code stub-latency} configuration
     */
    @Autowired
    public StubLatencies(StubLatencyProperties properties) {
        this(properties, () -> ThreadLocalRandom.current().nextGaussian());
    }

    StubLatencies(StubLatencyProperties properties, DoubleSupplier gaussian) {
        this.gaussian = gaussian;
        this.integrations = properties.integrations().entrySet().stream()
            .map(entry -> new IntegrationLatency(entry.getKey(), entry.getValue()))
            .toList();
        for (IntegrationLatency integration : integrations) {
            for (String path : integration.paths()) {
                IntegrationLatency existing = byPath.put(path, integration);
                if (existing != null) {
                    throw new IllegalStateException(
                        "Path %s is claimed by both %s and %s"
                            .formatted(path, existing.name(), integration.name()));
                }
            }
        }
    }

    /**
     * Finds the integration that answers a request path.
     *
     * @param requestUri the request URI, matched exactly
     * @return the integration, or empty when the path has no profile
     */
    public Optional<IntegrationLatency> forPath(String requestUri) {
        return Optional.ofNullable(byPath.get(requestUri));
    }

    /**
     * Draws the delay for one request to an integration.
     *
     * @param integration the integration being called
     * @return the delay in milliseconds
     */
    public long nextDelayMs(IntegrationLatency integration) {
        return integration.nextDelayMs(gaussian);
    }

    /** Forgets the answered latencies of every integration. */
    public void clearAnswered() {
        integrations.forEach(IntegrationLatency::clearAnswered);
    }

    /**
     * Every integration, in configuration order.
     *
     * @return the integrations
     */
    public List<IntegrationLatency> all() {
        return integrations;
    }
}
