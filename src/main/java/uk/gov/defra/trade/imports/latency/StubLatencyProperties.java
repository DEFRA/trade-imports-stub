package uk.gov.defra.trade.imports.latency;

import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * The latency profile of every integration this stub stands in for, keyed by integration name.
 *
 * @param integrations the configured integrations, in configuration order
 */
@Validated
@ConfigurationProperties(prefix = "stub-latency")
public record StubLatencyProperties(@NotEmpty Map<String, @Valid Integration> integrations) {

    /**
     * One integration's profile and the metadata that says what the profile represents.
     *
     * @param interfaceName the interface the profile represents
     * @param owner who owns that interface
     * @param serviceLevelSource the service level the targets were derived from
     * @param agreed whether the targets have been agreed with the owner
     * @param lastConformed the date the profile was last conformed to the real system, if ever
     * @param profile the profile the integration answers with
     * @param paths the request paths that get this integration's latency
     * @param sla the targets the {@code sla} profile is fitted to
     */
    public record Integration(
        @NotBlank String interfaceName,
        @NotBlank String owner,
        @NotBlank String serviceLevelSource,
        boolean agreed,
        @Nullable LocalDate lastConformed,
        @NotNull ProfileName profile,
        @NotEmpty List<String> paths,
        @NotNull @Valid SlaTargets sla) {
    }
}
