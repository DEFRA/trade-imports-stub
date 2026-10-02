package uk.gov.defra.trade.imports.latency;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The latency profile an integration answers with.
 *
 * <p>{@link #ZERO_DELAY} adds no delay. {@link #SLA} draws each answer's delay from the lognormal
 * distribution fitted to the integration's service-level targets.
 */
public enum ProfileName {
    ZERO_DELAY("zero-delay"),
    SLA("sla");

    private final String label;

    ProfileName(String label) {
        this.label = label;
    }

    /**
     * The name used in configuration values and in the {@code /latency-profiles} response.
     *
     * @return the profile's name, such as {@code zero-delay}
     */
    @JsonValue
    public String label() {
        return label;
    }
}
