package uk.gov.defra.trade.imports.latency;

import jakarta.validation.constraints.Positive;

/**
 * The service-level targets a latency profile is fitted to, in milliseconds.
 *
 * @param p50Ms the median latency
 * @param p95Ms the 95th percentile latency
 * @param p99Ms the 99th percentile latency
 */
public record SlaTargets(@Positive int p50Ms, @Positive int p95Ms, @Positive int p99Ms) {

    public SlaTargets {
        if (p50Ms > p95Ms || p95Ms > p99Ms) {
            throw new IllegalArgumentException(
                "Latency targets must satisfy p50 <= p95 <= p99 but were %d, %d, %d"
                    .formatted(p50Ms, p95Ms, p99Ms));
        }
    }
}
