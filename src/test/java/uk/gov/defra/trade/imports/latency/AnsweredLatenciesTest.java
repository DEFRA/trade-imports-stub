package uk.gov.defra.trade.imports.latency;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;
import uk.gov.defra.trade.imports.latency.AnsweredLatencies.AnsweredSnapshot;

class AnsweredLatenciesTest {

    @Test
    void snapshot_shouldHaveNoCountOrPercentiles_whenNothingIsRecorded() {
        AnsweredSnapshot snapshot = new AnsweredLatencies().snapshot();

        assertThat(snapshot.count()).isZero();
        assertThat(snapshot.p50Ms()).isNull();
        assertThat(snapshot.p95Ms()).isNull();
        assertThat(snapshot.p99Ms()).isNull();
    }

    @Test
    void snapshot_shouldReportNoPeak_whenNothingIsRecorded() {
        AnsweredSnapshot snapshot = new AnsweredLatencies().snapshot();

        assertThat(snapshot.peakPerSecond()).isZero();
    }

    @Test
    void snapshot_shouldUseNearestRank_whenOneToOneHundredAreRecorded() {
        AnsweredLatencies latencies = new AnsweredLatencies();
        LongStream.rangeClosed(1, 100).forEach(latencies::record);

        AnsweredSnapshot snapshot = latencies.snapshot();

        assertThat(snapshot.count()).isEqualTo(100);
        assertThat(snapshot.p50Ms()).isEqualTo(50);
        assertThat(snapshot.p95Ms()).isEqualTo(95);
        assertThat(snapshot.p99Ms()).isEqualTo(99);
    }

    @Test
    void snapshot_shouldCountEveryAnswerAndCoverTheWholeRange_whenTwiceCapacityIsRecorded() {
        int capacity = 1_000;
        AnsweredLatencies latencies = new AnsweredLatencies(capacity, new Random(42));
        LongStream.rangeClosed(1, 2L * capacity).forEach(latencies::record);

        AnsweredSnapshot snapshot = latencies.snapshot();

        assertThat(snapshot.count()).isEqualTo(2L * capacity);
        assertThat(snapshot.p50Ms()).isBetween(800L, 1_200L);
        assertThat(snapshot.p95Ms()).isBetween(1_800L, 2_000L);
        assertThat(snapshot.p99Ms()).isGreaterThan(1_900L);
    }

    @Test
    void snapshot_shouldReportTheBusiestSecond_whenAnswersSpanSeconds() {
        AtomicLong clock = new AtomicLong();
        AnsweredLatencies latencies = new AnsweredLatencies(10, new Random(42), clock::get);

        for (long millis : new long[] {1_000, 1_500, 1_999, 2_000, 2_250, 2_500, 2_750, 2_999, 3_000}) {
            clock.set(millis);
            latencies.record(5);
        }

        assertThat(latencies.snapshot().peakPerSecond()).isEqualTo(5);
    }

    @Test
    void clear_shouldEmptyTheSnapshot() {
        AnsweredLatencies latencies = new AnsweredLatencies(3, new Random(42));
        LongStream.rangeClosed(1, 10).forEach(latencies::record);

        latencies.clear();
        AnsweredSnapshot snapshot = latencies.snapshot();

        assertThat(snapshot.count()).isZero();
        assertThat(snapshot.p50Ms()).isNull();
        assertThat(snapshot.p99Ms()).isNull();
    }

    @Test
    void clear_shouldResetThePeak() {
        AtomicLong clock = new AtomicLong(1_000);
        AnsweredLatencies latencies = new AnsweredLatencies(10, new Random(42), clock::get);
        LongStream.rangeClosed(1, 5).forEach(latencies::record);

        latencies.clear();
        latencies.record(1);

        assertThat(latencies.snapshot().peakPerSecond()).isEqualTo(1);
    }

    @Test
    void record_shouldStartAgain_afterClear() {
        AnsweredLatencies latencies = new AnsweredLatencies(3, new Random(42));
        LongStream.rangeClosed(1, 10).forEach(latencies::record);
        latencies.clear();

        latencies.record(7);
        AnsweredSnapshot snapshot = latencies.snapshot();

        assertThat(snapshot.count()).isEqualTo(1);
        assertThat(snapshot.p50Ms()).isEqualTo(7);
    }
}
