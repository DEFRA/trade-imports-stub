package uk.gov.defra.trade.imports.latency;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/** Every integration this stub stands in for, and the request paths each one answers. */
@Component
public class StubLatencies {

    private final List<IntegrationLatency> integrations;
    private final Map<String, IntegrationLatency> byPath = new LinkedHashMap<>();
    private final Map<PathPattern, String> configuredPathByPattern = new LinkedHashMap<>();
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
                configuredPathByPattern.put(PathPatternParser.defaultInstance.parse(path), path);
            }
        }
    }

    /**
     * Finds the configured path that a request URI matches. A configured path matches a URI
     * exactly, or with {@code *} standing for one path segment.
     *
     * @param requestUri the request URI
     * @return the configured path, or empty when no integration answers the URI
     */
    public Optional<String> configuredPathFor(String requestUri) {
        PathContainer requestPath = PathContainer.parsePath(requestUri);
        return configuredPathByPattern.entrySet().stream()
            .filter(entry -> entry.getKey().matches(requestPath))
            .map(Map.Entry::getValue)
            .findFirst();
    }

    /**
     * Finds the integration that answers a request path.
     *
     * @param requestUri the request URI, matched as {@link #configuredPathFor} matches it
     * @return the integration, or empty when the path has no profile
     */
    public Optional<IntegrationLatency> forPath(String requestUri) {
        return configuredPathFor(requestUri).map(byPath::get);
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
