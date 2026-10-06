package uk.gov.defra.trade.imports.faults;

import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The {@code GET /faults} response. The same JSON contract is served by the Defra ID stub, so the
 * performance tests read both the same way.
 *
 * @param stub the stub service that answered
 * @param integrations one report per integration the stub stands in for
 */
public record FaultsResponse(String stub, List<IntegrationFaultReport> integrations) {

    public FaultsResponse {
        Objects.requireNonNull(stub, "stub");
        integrations = List.copyOf(Objects.requireNonNull(integrations, "integrations"));
    }

    /**
     * One integration's active fault and counters.
     *
     * @param integration the integration's name
     * @param paths the paths a fault can apply to
     * @param fault the active fault, or null when none is on
     * @param requests every request to the integration's paths since the stub started
     * @param injected faults injected since the stub started, by kind
     */
    public record IntegrationFaultReport(
        String integration,
        List<String> paths,
        @Nullable FaultReport fault,
        long requests,
        Map<String, Long> injected) {

        public IntegrationFaultReport {
            Objects.requireNonNull(integration, "integration");
            paths = List.copyOf(Objects.requireNonNull(paths, "paths"));
            injected = Collections.unmodifiableMap(
                new LinkedHashMap<>(Objects.requireNonNull(injected, "injected")));
        }
    }

    /**
     * The active fault as reported.
     *
     * @param kind the fault kind's label
     * @param rate the probability a request is faulted
     * @param delayMs how long a slow fault waits and a hang fault holds the request
     * @param status the status an error fault answers
     * @param retryAfterSeconds the {@code Retry-After} a throttle fault answers
     * @param paths the paths the fault applies to
     * @param expiresAt when the fault stops applying
     */
    public record FaultReport(
        String kind,
        double rate,
        long delayMs,
        int status,
        int retryAfterSeconds,
        List<String> paths,
        Instant expiresAt) {

        public FaultReport {
            Objects.requireNonNull(kind, "kind");
            paths = List.copyOf(Objects.requireNonNull(paths, "paths"));
            Objects.requireNonNull(expiresAt, "expiresAt");
        }
    }
}
