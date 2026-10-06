package uk.gov.defra.trade.imports.faults;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleSupplier;
import org.junit.jupiter.api.Test;
import uk.gov.defra.trade.imports.exceptions.NotFoundException;
import uk.gov.defra.trade.imports.faults.FaultsResponse.IntegrationFaultReport;
import uk.gov.defra.trade.imports.latency.ProfileName;
import uk.gov.defra.trade.imports.latency.SlaTargets;
import uk.gov.defra.trade.imports.latency.StubLatencies;
import uk.gov.defra.trade.imports.latency.StubLatencyProperties;
import uk.gov.defra.trade.imports.latency.StubLatencyProperties.Integration;

class StubFaultsTest {

    private static final String COUNTRIES = "/mdm/geo/countries";
    private static final String POES = "/mdm/trade/bcp/poes";
    private static final String TOKEN = "/tenant/oauth2/v2.0/token";
    private static final Instant NOW = Instant.parse("2026-10-06T12:00:00Z");
    private static final SlaTargets INTERIM = new SlaTargets(100, 400, 1000);

    private final StubLatencies latencies = new StubLatencies(properties());

    @Test
    void decide_shouldInjectAnErrorFaultAtRateOneAndCountTheRequestAndTheInjection() {
        StubFaults faults = faultsWithDraws(0.0);
        faults.apply("mdm", error(1.0, null));

        assertThat(faults.decide(COUNTRIES)).hasValueSatisfying(
            fault -> assertThat(fault.kind()).isEqualTo(FaultKind.ERROR));

        IntegrationFaultReport mdm = reportFor(faults, "mdm");
        assertThat(mdm.requests()).isEqualTo(1);
        assertThat(mdm.injected()).containsEntry("error", 1L).containsEntry("slow", 0L);
    }

    @Test
    void decide_shouldInjectOnceInTwoAtRateHalfWithDrawsBelowAndAboveIt() {
        StubFaults faults = faultsWithDraws(0.4, 0.6);
        faults.apply("mdm", error(0.5, null));

        assertThat(faults.decide(COUNTRIES)).isPresent();
        assertThat(faults.decide(COUNTRIES)).isEmpty();

        IntegrationFaultReport mdm = reportFor(faults, "mdm");
        assertThat(mdm.requests()).isEqualTo(2);
        assertThat(mdm.injected()).containsEntry("error", 1L);
    }

    @Test
    void decide_shouldLeaveAPathOutsideTheFaultsPathsAlone() {
        StubFaults faults = faultsWithDraws(0.0);
        faults.apply("mdm", error(1.0, List.of(POES)));

        assertThat(faults.decide(COUNTRIES)).isEmpty();
        assertThat(faults.decide(POES)).isPresent();
        assertThat(reportFor(faults, "mdm").requests()).isEqualTo(2);
    }

    @Test
    void decide_shouldStopApplyingAndReportNoFault_whenTheFaultHasExpired() {
        MutableClock clock = new MutableClock(NOW);
        StubFaults faults = new StubFaults(latencies, clock, () -> 0.0);
        faults.apply("mdm", error(1.0, null));
        assertThat(faults.decide(COUNTRIES)).isPresent();

        clock.advanceSeconds(61);

        assertThat(faults.decide(COUNTRIES)).isEmpty();
        assertThat(reportFor(faults, "mdm").fault()).isNull();
    }

    @Test
    void clear_shouldSwitchTheFaultOffAndKeepTheCounters() {
        StubFaults faults = faultsWithDraws(0.0);
        faults.apply("mdm", error(1.0, null));
        faults.decide(COUNTRIES);

        faults.clear("mdm");

        assertThat(faults.decide(COUNTRIES)).isEmpty();
        IntegrationFaultReport mdm = reportFor(faults, "mdm");
        assertThat(mdm.fault()).isNull();
        assertThat(mdm.requests()).isEqualTo(2);
        assertThat(mdm.injected()).containsEntry("error", 1L);
    }

    @Test
    void clearAll_shouldSwitchEveryIntegrationsFaultOff() {
        StubFaults faults = faultsWithDraws(0.0);
        faults.apply("mdm", error(1.0, null));
        faults.apply("trade-token", error(1.0, null));

        faults.clearAll();

        assertThat(faults.report()).allSatisfy(report -> assertThat(report.fault()).isNull());
    }

    @Test
    void decide_shouldNeverCountOrFaultPathsThatAreNotAnIntegrations() {
        StubFaults faults = faultsWithDraws(0.0);
        faults.apply("mdm", error(1.0, null));

        assertThat(faults.decide("/health")).isEmpty();
        assertThat(faults.decide("/latency-profiles")).isEmpty();
        assertThat(faults.decide("/faults")).isEmpty();

        assertThat(faults.report()).allSatisfy(report -> assertThat(report.requests()).isZero());
    }

    @Test
    void apply_shouldThrowNotFound_whenTheIntegrationIsUnknown() {
        StubFaults faults = faultsWithDraws(0.0);
        FaultRequest request = error(1.0, null);

        assertThatThrownBy(() -> faults.apply("sqs", request))
            .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> faults.clear("sqs")).isInstanceOf(NotFoundException.class);
    }

    @Test
    void apply_shouldThrowUnknownPath_whenThePathBelongsToAnotherIntegration() {
        StubFaults faults = faultsWithDraws(0.0);
        FaultRequest request = error(1.0, List.of(TOKEN));

        assertThatThrownBy(() -> faults.apply("mdm", request))
            .isInstanceOf(UnknownFaultPathException.class);
    }

    @Test
    void report_shouldListTheTradeTokenThenMdmWithEveryKindCounted() {
        StubFaults faults = faultsWithDraws(0.0);

        assertThat(faults.report()).extracting(IntegrationFaultReport::integration)
            .containsExactly("trade-token", "mdm");
        assertThat(reportFor(faults, "mdm").injected().keySet())
            .containsExactly("slow", "hang", "reset", "throttle", "error");
    }

    @Test
    void apply_shouldDefaultTheStatusRetryAfterAndPaths() {
        StubFaults faults = faultsWithDraws(0.0);

        IntegrationFaultReport report = faults.apply("mdm", error(1.0, null));

        assertThat(report.fault().status()).isEqualTo(503);
        assertThat(report.fault().retryAfterSeconds()).isEqualTo(1);
        assertThat(report.fault().paths()).containsExactly(COUNTRIES, POES);
    }

    private StubFaults faultsWithDraws(double... draws) {
        Deque<Double> queue = new ArrayDeque<>();
        for (double draw : draws) {
            queue.add(draw);
        }
        DoubleSupplier random = () -> queue.size() > 1 ? queue.poll() : queue.peek();
        return new StubFaults(latencies, Clock.fixed(NOW, ZoneOffset.UTC), random);
    }

    private static FaultRequest error(double rate, List<String> paths) {
        return new FaultRequest(FaultKind.ERROR, rate, null, null, null, paths, 60);
    }

    private static IntegrationFaultReport reportFor(StubFaults faults, String integration) {
        return faults.report().stream()
            .filter(report -> report.integration().equals(integration))
            .findFirst()
            .orElseThrow();
    }

    private static StubLatencyProperties properties() {
        Map<String, Integration> integrations = new LinkedHashMap<>();
        integrations.put("trade-token", integration(TOKEN));
        integrations.put("mdm", integration(COUNTRIES, POES));
        return new StubLatencyProperties(integrations);
    }

    private static Integration integration(String... paths) {
        return new Integration(
            "interface", "owner", "source", false, null, ProfileName.ZERO_DELAY,
            List.of(paths), INTERIM);
    }

    /** A clock the test moves forward by hand. */
    private static final class MutableClock extends Clock {
        private Instant now;

        private MutableClock(Instant now) {
            this.now = now;
        }

        private void advanceSeconds(long seconds) {
            now = now.plusSeconds(seconds);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
