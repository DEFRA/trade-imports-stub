package uk.gov.defra.trade.imports.faults;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FaultRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validate_shouldAccept_anErrorFaultWithNoDelay() {
        assertThat(violations(new FaultRequest(FaultKind.ERROR, 0.5, null, 503, null, null, 150)))
            .isEmpty();
    }

    @Test
    void validate_shouldReject_aSlowFaultWithoutADelay() {
        assertThat(violations(new FaultRequest(FaultKind.SLOW, 1.0, null, null, null, null, 60)))
            .hasSize(1);
    }

    @Test
    void validate_shouldReject_aHangFaultWithoutADelay() {
        assertThat(violations(new FaultRequest(FaultKind.HANG, 1.0, null, null, null, null, 60)))
            .hasSize(1);
    }

    @Test
    void validate_shouldReject_aRateAboveOne() {
        assertThat(violations(new FaultRequest(FaultKind.ERROR, 1.5, null, null, null, null, 60)))
            .hasSize(1);
    }

    @Test
    void validate_shouldReject_aMissingRate() {
        assertThat(violations(new FaultRequest(FaultKind.ERROR, null, null, null, null, null, 60)))
            .extracting(violation -> violation.getPropertyPath().toString())
            .containsExactly("rate");
    }

    @Test
    void validate_shouldReject_aStatusThatIsNotA5xx() {
        assertThat(violations(new FaultRequest(FaultKind.ERROR, 1.0, null, 404, null, null, 60)))
            .hasSize(1);
    }

    @Test
    void validate_shouldReject_anExpiryOutsideOneSecondToADay() {
        assertThat(violations(new FaultRequest(FaultKind.ERROR, 1.0, null, null, null, null, 0)))
            .hasSize(1);
        assertThat(violations(new FaultRequest(FaultKind.ERROR, 1.0, null, null, null, null, 86401)))
            .hasSize(1);
    }

    @Test
    void validate_shouldReject_aMissingKind() {
        assertThat(violations(new FaultRequest(null, 1.0, null, null, null, null, 60))).isNotEmpty();
    }

    private Set<ConstraintViolation<FaultRequest>> violations(FaultRequest request) {
        return validator.validate(request);
    }
}
