package uk.gov.defra.trade.imports.latency;

import jakarta.annotation.Nullable;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.LongSupplier;
import java.util.random.RandomGenerator;

/**
 * The answered latencies of one integration since the last clear, kept as a uniform random sample
 * (reservoir sampling, Algorithm R) so the percentiles describe the whole window rather than only
 * its most recent answers. It also keeps the most answers recorded within any one wall-clock
 * second, the load the integration carried.
 */
public class AnsweredLatencies {

    static final int DEFAULT_CAPACITY = 10_000;

    private static final double P50 = 0.50;
    private static final double P95 = 0.95;
    private static final double P99 = 0.99;

    private final long[] buffer;
    private final RandomGenerator random;
    private final LongSupplier clockMillis;
    private long total;
    private long currentSecond = Long.MIN_VALUE;
    private long currentSecondCount;
    private long peakPerSecond;

    /** Creates a reservoir holding up to {@value #DEFAULT_CAPACITY} sampled answers. */
    public AnsweredLatencies() {
        this(DEFAULT_CAPACITY, RandomGenerator.getDefault(), System::currentTimeMillis);
    }

    AnsweredLatencies(int capacity, RandomGenerator random) {
        this(capacity, random, System::currentTimeMillis);
    }

    AnsweredLatencies(int capacity, RandomGenerator random, LongSupplier clockMillis) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be at least 1");
        }
        this.buffer = new long[capacity];
        this.random = Objects.requireNonNull(random, "random");
        this.clockMillis = Objects.requireNonNull(clockMillis, "clockMillis");
    }

    /**
     * Records one answered latency, keeping it in the sample with the probability that leaves every
     * answer since the last clear equally likely to be kept.
     *
     * @param millis how long the stub took to answer, in milliseconds
     */
    public synchronized void record(long millis) {
        long second = Math.floorDiv(clockMillis.getAsLong(), 1_000L);
        if (second != currentSecond) {
            currentSecond = second;
            currentSecondCount = 0;
        }
        currentSecondCount++;
        peakPerSecond = Math.max(peakPerSecond, currentSecondCount);
        total++;
        if (total <= buffer.length) {
            buffer[(int) (total - 1)] = millis;
            return;
        }
        long slot = random.nextLong(total);
        if (slot < buffer.length) {
            buffer[(int) slot] = millis;
        }
    }

    /** Forgets every answer recorded so far. */
    public synchronized void clear() {
        total = 0;
        currentSecond = Long.MIN_VALUE;
        currentSecondCount = 0;
        peakPerSecond = 0;
    }

    /**
     * Reads the number of answers since the last clear, the busiest second and the nearest-rank
     * percentiles of the sample kept.
     *
     * @return the snapshot, with null percentiles when nothing has been recorded
     */
    public synchronized AnsweredSnapshot snapshot() {
        if (total == 0) {
            return new AnsweredSnapshot(0, 0, null, null, null);
        }
        long[] sorted = Arrays.copyOf(buffer, (int) Math.min(total, buffer.length));
        Arrays.sort(sorted);
        return new AnsweredSnapshot(
            total,
            peakPerSecond,
            nearestRank(sorted, P50),
            nearestRank(sorted, P95),
            nearestRank(sorted, P99));
    }

    private static long nearestRank(long[] sorted, double quantile) {
        int index = (int) Math.ceil(quantile * sorted.length) - 1;
        return sorted[Math.max(index, 0)];
    }

    /**
     * The answered latencies at one moment.
     *
     * @param count how many answers were recorded since the last clear
     * @param peakPerSecond the most answers recorded within one wall-clock second since the last
     *     clear
     * @param p50Ms the median of the sample, or null when there are no answers
     * @param p95Ms the 95th percentile of the sample, or null when there are no answers
     * @param p99Ms the 99th percentile of the sample, or null when there are no answers
     */
    public record AnsweredSnapshot(
        long count,
        long peakPerSecond,
        @Nullable Long p50Ms,
        @Nullable Long p95Ms,
        @Nullable Long p99Ms) {
    }
}
