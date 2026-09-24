package com.slotsim.stats;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class RoundStatisticsTest {

    @Test
    void accumulatesTotals() {
        RoundStatistics statistics = record(0, 20, 0, 5);

        assertThat(statistics.rounds()).isEqualTo(4);
        assertThat(statistics.totalWin()).isEqualTo(25);
        assertThat(statistics.sumOfSquares()).isEqualTo(425);
        assertThat(statistics.winningRounds()).isEqualTo(2);
        assertThat(statistics.maxWin()).isEqualTo(20);
    }

    @Test
    void mergeEqualsRecordingEverythingInOneAccumulator() {
        RoundStatistics merged = record(0, 20).merge(record(0, 5, 100));
        RoundStatistics single = record(0, 20, 0, 5, 100);

        assertThat(merged).usingRecursiveComparison().isEqualTo(single);
    }

    @Test
    void summarizesRtpAndVolatility() {
        // wins 0, 20, 0, 20: mean 10, variance 100, bet 10
        StatisticsSummary summary = record(0, 20, 0, 20).summarize(10);

        assertThat(summary.totalBet()).isEqualTo(40);
        assertThat(summary.totalWin()).isEqualTo(40);
        assertThat(summary.rtp()).isEqualTo(1.0);
        assertThat(summary.standardDeviation()).isCloseTo(10.0, within(1e-12));
        assertThat(summary.volatilityIndex()).isCloseTo(1.0, within(1e-12));
        assertThat(summary.hitFrequency()).isEqualTo(0.5);
        assertThat(summary.maxWin()).isEqualTo(20);
    }

    @Test
    void confidenceIntervalShrinksWithSquareRootOfRounds() {
        // RTP 1.0, volatility index 1.0, 4 rounds: half width = 1.96 * 1 / 2
        StatisticsSummary summary = record(0, 20, 0, 20).summarize(10);

        StatisticsSummary.ConfidenceInterval interval = summary.rtpConfidenceInterval(StatisticsSummary.Z_95);

        assertThat(interval.halfWidth()).isCloseTo(StatisticsSummary.Z_95 / 2, within(1e-12));
        assertThat(interval.contains(1.0)).isTrue();
        assertThat(interval.contains(2.0)).isFalse();
    }

    @Test
    void constantWinsHaveZeroDeviation() {
        assertThat(record(7, 7, 7).summarize(10).standardDeviation()).isZero();
    }

    @Test
    void failsLoudlyInsteadOfOverflowing() {
        RoundStatistics statistics = record(3_000_000_000L);

        assertThatThrownBy(() -> statistics.record(3_000_000_000L))
                .isInstanceOf(ArithmeticException.class);
        assertThatThrownBy(() -> statistics.record(4_000_000_000L))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void rejectsNegativeWin() {
        assertThatThrownBy(() -> new RoundStatistics().record(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cannotSummarizeWithoutRounds() {
        assertThatThrownBy(() -> new RoundStatistics().summarize(10))
                .isInstanceOf(IllegalStateException.class);
    }

    private static RoundStatistics record(long... wins) {
        RoundStatistics statistics = new RoundStatistics();
        for (long win : wins) {
            statistics.record(win);
        }
        return statistics;
    }
}
