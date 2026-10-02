package uk.gov.defra.trade.imports.latency;

import java.util.List;
import java.util.Objects;
import java.util.function.DoubleSupplier;
import uk.gov.defra.trade.imports.latency.AnsweredLatencies.AnsweredSnapshot;
import uk.gov.defra.trade.imports.latency.LatencyProfilesResponse.AnsweredReport;
import uk.gov.defra.trade.imports.latency.LatencyProfilesResponse.IntegrationReport;
import uk.gov.defra.trade.imports.latency.LatencyProfilesResponse.Quantiles;

/** One integration at runtime: its configured profile, its fitted distribution and its answers. */
public class IntegrationLatency {

    private final String name;
    private final StubLatencyProperties.Integration configuration;
    private final LognormalFit fit;
    private final AnsweredLatencies answered;

    /**
     * Creates the runtime state of one integration.
     *
     * @param name the integration's name
     * @param configuration the integration's configuration
     */
    public IntegrationLatency(String name, StubLatencyProperties.Integration configuration) {
        this.name = Objects.requireNonNull(name, "name");
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.fit = LognormalFit.of(configuration.sla());
        this.answered = new AnsweredLatencies();
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
     * The request paths that get this integration's latency.
     *
     * @return the paths
     */
    public List<String> paths() {
        return configuration.paths();
    }

    /**
     * Draws the delay for one request.
     *
     * @param gaussian the source of standard normal draws
     * @return zero for {@code zero-delay}, otherwise a delay in milliseconds from the fitted distribution
     */
    public long nextDelayMs(DoubleSupplier gaussian) {
        if (configuration.profile() == ProfileName.ZERO_DELAY) {
            return 0;
        }
        return fit.delayMs(gaussian.getAsDouble());
    }

    /**
     * Records how long the stub took to answer one request.
     *
     * @param millis the elapsed time in milliseconds
     */
    public void recordAnswered(long millis) {
        answered.record(millis);
    }

    /** Forgets the answers recorded so far, so the next report covers only what follows. */
    public void clearAnswered() {
        answered.clear();
    }

    /**
     * Reports the profile, its metadata and the latency answered so far.
     *
     * @return the report
     */
    public IntegrationReport report() {
        SlaTargets sla = configuration.sla();
        Quantiles slaTargets = new Quantiles(sla.p50Ms(), sla.p95Ms(), sla.p99Ms());
        Quantiles fitted = new Quantiles(
            fit.delayMs(0), fit.delayMs(LognormalFit.Z95), fit.delayMs(LognormalFit.Z99));
        Quantiles targets = configuration.profile() == ProfileName.SLA
            ? slaTargets
            : new Quantiles(0, 0, 0);
        AnsweredSnapshot snapshot = answered.snapshot();
        return new IntegrationReport(
            name,
            configuration.interfaceName(),
            configuration.owner(),
            configuration.serviceLevelSource(),
            configuration.agreed(),
            configuration.lastConformed(),
            configuration.profile(),
            slaTargets,
            fitted,
            targets,
            new AnsweredReport(
                snapshot.count(), snapshot.p50Ms(), snapshot.p95Ms(), snapshot.p99Ms()));
    }
}
