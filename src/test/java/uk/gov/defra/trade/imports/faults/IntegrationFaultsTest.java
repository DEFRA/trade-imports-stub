package uk.gov.defra.trade.imports.faults;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class IntegrationFaultsTest {

    private static final String PATH = "/mdm/geo/countries";
    private static final Instant NOW = Instant.parse("2026-10-06T12:00:00Z");

    private final IntegrationFaults faults = new IntegrationFaults("mdm", List.of(PATH));

    @Test
    void report_shouldKeepAFaultAppliedAfterAnotherExpired() {
        faults.apply(fault(FaultKind.ERROR, NOW.minusSeconds(1)));
        faults.apply(fault(FaultKind.THROTTLE, NOW.plusSeconds(60)));

        assertThat(faults.report(NOW).fault().kind()).isEqualTo("throttle");
        assertThat(faults.decide(PATH, NOW, 0.0)).isPresent();
    }

    @Test
    void report_shouldDropAnExpiredFault() {
        faults.apply(fault(FaultKind.ERROR, NOW.minusSeconds(1)));

        assertThat(faults.report(NOW).fault()).isNull();
        assertThat(faults.decide(PATH, NOW, 0.0)).isEmpty();
    }

    private static ActiveFault fault(FaultKind kind, Instant expiresAt) {
        return new ActiveFault(kind, 1.0, 0, 503, 1, List.of(PATH), expiresAt);
    }
}
