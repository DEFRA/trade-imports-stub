package uk.gov.defra.trade.imports.faults;

import java.time.Instant;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import uk.gov.defra.trade.imports.faults.FaultsResponse.FaultReport;
import uk.gov.defra.trade.imports.faults.FaultsResponse.IntegrationFaultReport;

/** One integration's fault state at runtime: its active fault and its counters. */
public class IntegrationFaults {

    private final String name;
    private final List<String> paths;
    private final AtomicLong requests = new AtomicLong();
    private final Map<FaultKind, AtomicLong> injected = new EnumMap<>(FaultKind.class);
    private final AtomicReference<ActiveFault> active = new AtomicReference<>();

    /**
     * Creates the fault state of one integration.
     *
     * @param name the integration's name
     * @param paths the paths the integration answers
     */
    public IntegrationFaults(String name, List<String> paths) {
        this.name = Objects.requireNonNull(name, "name");
        this.paths = List.copyOf(Objects.requireNonNull(paths, "paths"));
        for (FaultKind kind : FaultKind.values()) {
            injected.put(kind, new AtomicLong());
        }
    }

    /**
     * The integration's name.
     *
     * @return the name, such as {@code mdm}
     */
    public String name() {
        return name;
    }

    /**
     * The paths the integration answers.
     *
     * @return the paths
     */
    public List<String> paths() {
        return paths;
    }

    /**
     * Counts a request and decides whether to fault it.
     *
     * @param path the request path
     * @param now the current time
     * @param draw a uniform draw from 0 (inclusive) to 1 (exclusive)
     * @return the fault to inject, or empty to answer normally
     */
    public Optional<ActiveFault> decide(String path, Instant now, double draw) {
        requests.incrementAndGet();
        ActiveFault fault = active.get();
        if (fault == null || !fault.appliesTo(path, now) || draw >= fault.rate()) {
            return Optional.empty();
        }
        injected.get(fault.kind()).incrementAndGet();
        return Optional.of(fault);
    }

    /**
     * Switches a fault on, replacing any active one.
     *
     * @param fault the fault
     */
    public void apply(ActiveFault fault) {
        active.set(Objects.requireNonNull(fault, "fault"));
    }

    /** Switches the active fault off. The counters are kept. */
    public void clear() {
        active.set(null);
    }

    /**
     * Reports the active fault and the counters.
     *
     * @param now the current time; an expired fault reports as none and is dropped
     * @return the report
     */
    public IntegrationFaultReport report(Instant now) {
        ActiveFault current = active.get();
        ActiveFault fault = current;
        if (current != null && !now.isBefore(current.expiresAt())) {
            active.compareAndSet(current, null);
            fault = null;
        }
        Map<String, Long> counts = new LinkedHashMap<>();
        injected.forEach((kind, count) -> counts.put(kind.label(), count.get()));
        return new IntegrationFaultReport(
            name, paths, fault == null ? null : reportOf(fault), requests.get(), counts);
    }

    private static FaultReport reportOf(ActiveFault fault) {
        return new FaultReport(
            fault.kind().label(),
            fault.rate(),
            fault.delayMs(),
            fault.status(),
            fault.retryAfterSeconds(),
            fault.paths(),
            fault.expiresAt());
    }
}
