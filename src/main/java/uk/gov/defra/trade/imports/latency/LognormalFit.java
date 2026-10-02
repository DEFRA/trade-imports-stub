package uk.gov.defra.trade.imports.latency;

/**
 * A lognormal distribution fitted to a set of service-level targets.
 *
 * <p>The median is matched exactly: {@code mu = ln(p50)}. The spread is the least-squares fit of
 * {@code sigma} to the p95 and p99 targets in log space, so the tail follows both upper quantiles as
 * closely as one lognormal can.
 *
 * @param mu the mean of the underlying normal distribution
 * @param sigma the standard deviation of the underlying normal distribution
 */
public record LognormalFit(double mu, double sigma) {

    /** The standard normal quantile at the 95th percentile. */
    static final double Z95 = 1.6448536269514722;

    /** The standard normal quantile at the 99th percentile. */
    static final double Z99 = 2.3263478740408408;

    /**
     * Fits a lognormal distribution to the targets.
     *
     * @param targets the service-level targets
     * @return the fitted distribution
     */
    public static LognormalFit of(SlaTargets targets) {
        double mu = Math.log(targets.p50Ms());
        double sigma = (Z95 * Math.log((double) targets.p95Ms() / targets.p50Ms())
            + Z99 * Math.log((double) targets.p99Ms() / targets.p50Ms()))
            / (Z95 * Z95 + Z99 * Z99);
        return new LognormalFit(mu, sigma);
    }

    /**
     * The delay for one draw from the distribution.
     *
     * @param z a draw from the standard normal distribution
     * @return the delay in whole milliseconds
     */
    public long delayMs(double z) {
        return Math.round(Math.exp(mu + sigma * z));
    }
}
