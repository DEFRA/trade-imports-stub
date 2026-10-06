package uk.gov.defra.trade.imports.faults;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;

/**
 * The body of {@code PUT /faults/{integration}}.
 *
 * @param kind what the fault does to a request
 * @param rate the probability, from 0 to 1, that a request is faulted
 * @param delayMs how long a {@code slow} fault waits and a {@code hang} fault holds the request;
 *     required for those two kinds
 * @param status the 5xx status an {@code error} fault answers, 503 when omitted
 * @param retryAfterSeconds the {@code Retry-After} a {@code throttle} fault answers, 1 when
 *     omitted
 * @param paths the integration's paths the fault applies to, all of them when omitted
 * @param expiresInSeconds how long the fault lasts before it stops by itself
 */
public record FaultRequest(
    @NotNull FaultKind kind,
    @NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double rate,
    @Nullable @PositiveOrZero Integer delayMs,
    @Nullable @Min(500) @Max(599) Integer status,
    @Nullable @Positive Integer retryAfterSeconds,
    @Nullable List<@NotNull String> paths,
    @Min(1) @Max(86400) int expiresInSeconds) {

    /**
     * Whether a delay was given to the kinds that need one.
     *
     * @return false for a {@code slow} or {@code hang} fault with no {@code delayMs}
     */
    @JsonIgnore
    @AssertTrue(message = "delayMs is required for slow and hang faults")
    public boolean isDelayGivenForSlowAndHang() {
        boolean needsDelay = kind == FaultKind.SLOW || kind == FaultKind.HANG;
        return !needsDelay || delayMs != null;
    }
}
