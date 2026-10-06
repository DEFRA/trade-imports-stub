package uk.gov.defra.trade.imports.faults;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.defra.trade.imports.exceptions.NotFoundException;
import uk.gov.defra.trade.imports.faults.FaultsResponse.IntegrationFaultReport;
import uk.gov.defra.trade.imports.latency.IntegrationLatency;
import uk.gov.defra.trade.imports.latency.StubLatencies;

/** The faults that can be injected into each integration this stub stands in for. */
@Component
public class StubFaults {

    private final Map<String, IntegrationFaults> byName = new LinkedHashMap<>();
    private final Map<String, IntegrationFaults> byPath = new LinkedHashMap<>();
    private final Clock clock;
    private final DoubleSupplier random;

    /**
     * Builds one fault state per integration, from the same path catalogue the latency filter uses.
     *
     * @param stubLatencies the integrations and their paths
     */
    @Autowired
    public StubFaults(StubLatencies stubLatencies) {
        this(stubLatencies, Clock.systemUTC(), () -> ThreadLocalRandom.current().nextDouble());
    }

    StubFaults(StubLatencies stubLatencies, Clock clock, DoubleSupplier random) {
        this.clock = clock;
        this.random = random;
        for (IntegrationLatency integration : stubLatencies.all()) {
            IntegrationFaults faults =
                new IntegrationFaults(integration.name(), integration.paths());
            byName.put(integration.name(), faults);
            integration.paths().forEach(path -> byPath.put(path, faults));
        }
    }

    /**
     * Counts a request to an integration's path and decides whether to fault it.
     *
     * @param requestUri the request URI, matched exactly against the integrations' paths
     * @return the fault to inject, or empty to answer normally
     */
    public Optional<ActiveFault> decide(String requestUri) {
        IntegrationFaults integration = byPath.get(requestUri);
        if (integration == null) {
            return Optional.empty();
        }
        return integration.decide(requestUri, clock.instant(), random.getAsDouble());
    }

    /**
     * Switches a fault on for an integration.
     *
     * @param integration the integration's name
     * @param request the fault asked for
     * @return the integration's report
     * @throws NotFoundException when there is no such integration
     * @throws UnknownFaultPathException when the fault names a path the integration does not answer
     */
    public IntegrationFaultReport apply(String integration, FaultRequest request) {
        IntegrationFaults faults = find(integration);
        if (request.paths() != null) {
            requireKnownPaths(faults, integration, request.paths());
        }
        Instant now = clock.instant();
        faults.apply(ActiveFault.from(request, faults.paths(), now));
        return faults.report(now);
    }

    /**
     * Switches an integration's fault off.
     *
     * @param integration the integration's name
     * @throws NotFoundException when there is no such integration
     */
    public void clear(String integration) {
        find(integration).clear();
    }

    /** Switches every integration's fault off. */
    public void clearAll() {
        byName.values().forEach(IntegrationFaults::clear);
    }

    /**
     * Reports every integration, in configuration order.
     *
     * @return the reports
     */
    public List<IntegrationFaultReport> report() {
        Instant now = clock.instant();
        return byName.values().stream().map(faults -> faults.report(now)).toList();
    }

    private static void requireKnownPaths(
        IntegrationFaults faults, String integration, List<String> requested) {
        for (String path : requested) {
            if (path == null || !faults.paths().contains(path)) {
                throw new UnknownFaultPathException(
                    "Path %s is not one of %s's paths".formatted(path, integration));
            }
        }
    }

    private IntegrationFaults find(String integration) {
        IntegrationFaults faults = byName.get(integration);
        if (faults == null) {
            throw new NotFoundException("No integration named " + integration);
        }
        return faults;
    }
}
