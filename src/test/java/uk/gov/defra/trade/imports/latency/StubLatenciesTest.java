package uk.gov.defra.trade.imports.latency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import uk.gov.defra.trade.imports.latency.StubLatencyProperties.Integration;

class StubLatenciesTest {

    private static final SlaTargets INTERIM = new SlaTargets(100, 400, 1000);

    @Test
    void forPath_shouldMapEachProfiledPathToItsIntegration() {
        StubLatencies latencies = new StubLatencies(properties(ProfileName.ZERO_DELAY), () -> 0);

        assertThat(latencies.forPath("/mdm/geo/countries")).get()
            .extracting(IntegrationLatency::name).isEqualTo("mdm");
        assertThat(latencies.forPath("/mdm/trade/bcp/poes")).get()
            .extracting(IntegrationLatency::name).isEqualTo("mdm");
        assertThat(latencies.forPath("/tenant/oauth2/v2.0/token")).get()
            .extracting(IntegrationLatency::name).isEqualTo("trade-token");
        assertThat(latencies.forPath("/health")).isEmpty();
        assertThat(latencies.forPath("/latency-profiles")).isEmpty();
    }

    @Test
    void nextDelayMs_shouldBeZero_whenTheProfileIsZeroDelay() {
        StubLatencies latencies = new StubLatencies(properties(ProfileName.ZERO_DELAY), () -> 5.0);

        IntegrationLatency mdm = latencies.forPath("/mdm/geo/countries").orElseThrow();

        assertThat(latencies.nextDelayMs(mdm)).isZero();
    }

    @Test
    void nextDelayMs_shouldBeTheFittedP95_whenTheProfileIsSlaAndTheDrawIsZ95() {
        StubLatencies latencies =
            new StubLatencies(properties(ProfileName.SLA), () -> LognormalFit.Z95);

        IntegrationLatency mdm = latencies.forPath("/mdm/geo/countries").orElseThrow();

        assertThat(latencies.nextDelayMs(mdm)).isEqualTo(470);
    }

    @Test
    void constructor_shouldFail_whenTwoIntegrationsClaimTheSamePath() {
        Map<String, Integration> integrations = new LinkedHashMap<>();
        integrations.put("one", integration(ProfileName.SLA, "/same"));
        integrations.put("two", integration(ProfileName.SLA, "/same"));

        assertThatThrownBy(() -> new StubLatencies(new StubLatencyProperties(integrations), () -> 0))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("/same");
    }

    private static StubLatencyProperties properties(ProfileName profile) {
        Map<String, Integration> integrations = new LinkedHashMap<>();
        integrations.put("trade-token", integration(profile, "/tenant/oauth2/v2.0/token"));
        integrations.put("mdm", integration(profile, "/mdm/geo/countries", "/mdm/trade/bcp/poes"));
        return new StubLatencyProperties(integrations);
    }

    private static Integration integration(ProfileName profile, String... paths) {
        return new Integration(
            "interface", "owner", "source", false, null, profile, List.of(paths), INTERIM);
    }
}
