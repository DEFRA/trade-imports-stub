package uk.gov.defra.trade.imports.latency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class LognormalFitTest {

    @Test
    void of_shouldFitMedianExactlyAndTailByLeastSquares_whenGivenTheInterimTargets() {
        LognormalFit fit = LognormalFit.of(new SlaTargets(100, 400, 1000));

        assertThat(fit.mu()).isEqualTo(Math.log(100));
        assertThat(fit.sigma()).isCloseTo(0.9408, within(1e-4));
        assertThat(fit.delayMs(LognormalFit.Z95)).isEqualTo(470);
        assertThat(fit.delayMs(LognormalFit.Z99)).isEqualTo(892);
    }

    @Test
    void delayMs_shouldBeTheMedian_whenTheDrawIsZero() {
        LognormalFit fit = LognormalFit.of(new SlaTargets(100, 400, 1000));

        assertThat(fit.delayMs(0)).isEqualTo(100);
    }

    @Test
    void delayMs_shouldBeConstant_whenAllTargetsAreEqual() {
        LognormalFit fit = LognormalFit.of(new SlaTargets(200, 200, 200));

        assertThat(fit.sigma()).isZero();
        assertThat(fit.delayMs(-3)).isEqualTo(200);
        assertThat(fit.delayMs(0)).isEqualTo(200);
        assertThat(fit.delayMs(3)).isEqualTo(200);
    }
}
