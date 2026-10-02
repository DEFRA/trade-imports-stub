package uk.gov.defra.trade.imports.latency;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * The {@code GET /latency-profiles} response. The same JSON contract is served by the Defra ID
 * stub, so the performance tests read both the same way.
 *
 * @param stub the stub service that answered
 * @param integrations one report per integration the stub stands in for
 */
public record LatencyProfilesResponse(String stub, List<IntegrationReport> integrations) {

    public LatencyProfilesResponse {
        Objects.requireNonNull(stub, "stub");
        integrations = List.copyOf(Objects.requireNonNull(integrations, "integrations"));
    }

    /**
     * One integration's profile, its metadata and the latency it actually answered with.
     *
     * @param integration the integration's name
     * @param interfaceName the interface the profile represents
     * @param owner who owns that interface
     * @param serviceLevelSource the service level the targets were derived from
     * @param agreed whether the targets have been agreed with the owner
     * @param lastConformed the date the profile was last conformed, or null if never
     * @param profile the profile the integration runs
     * @param slaTargets the targets the {@code sla} profile is fitted to
     * @param fitted the percentiles of the lognormal fitted to those targets
     * @param targets the targets the running profile aims for: the sla targets, or zero
     * @param answered the latency the stub actually answered with
     */
    public record IntegrationReport(
        String integration,
        @JsonProperty("interface") String interfaceName,
        String owner,
        String serviceLevelSource,
        boolean agreed,
        @Nullable LocalDate lastConformed,
        ProfileName profile,
        Quantiles slaTargets,
        Quantiles fitted,
        Quantiles targets,
        AnsweredReport answered) {

        public IntegrationReport {
            Objects.requireNonNull(integration, "integration");
            Objects.requireNonNull(interfaceName, "interfaceName");
            Objects.requireNonNull(owner, "owner");
            Objects.requireNonNull(serviceLevelSource, "serviceLevelSource");
            Objects.requireNonNull(profile, "profile");
            Objects.requireNonNull(slaTargets, "slaTargets");
            Objects.requireNonNull(fitted, "fitted");
            Objects.requireNonNull(targets, "targets");
            Objects.requireNonNull(answered, "answered");
        }
    }

    /**
     * Three latency percentiles in milliseconds.
     *
     * @param p50Ms the median
     * @param p95Ms the 95th percentile
     * @param p99Ms the 99th percentile
     */
    public record Quantiles(long p50Ms, long p95Ms, long p99Ms) {
    }

    /**
     * The latency a stub actually answered with.
     *
     * @param count how many answers were recorded since the last clear
     * @param p50Ms the median, or null when there are no answers
     * @param p95Ms the 95th percentile, or null when there are no answers
     * @param p99Ms the 99th percentile, or null when there are no answers
     */
    public record AnsweredReport(
        long count, @Nullable Long p50Ms, @Nullable Long p95Ms, @Nullable Long p99Ms) {
    }
}
