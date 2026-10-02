package uk.gov.defra.trade.imports.latency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SlaTargetsTest {

    @Test
    void constructor_shouldAccept_whenTargetsAreInOrder() {
        assertThat(new SlaTargets(100, 400, 1000).p95Ms()).isEqualTo(400);
    }

    @Test
    void constructor_shouldReject_whenP95IsBelowP50() {
        assertThatThrownBy(() -> new SlaTargets(100, 50, 1000))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_shouldReject_whenP99IsBelowP95() {
        assertThatThrownBy(() -> new SlaTargets(100, 400, 300))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
