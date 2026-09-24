package com.slotsim.simulation;

import com.slotsim.config.MathModelLoader;
import com.slotsim.engine.CompositeWinEvaluator;
import com.slotsim.model.MathModel;
import com.slotsim.stats.RoundStatistics;
import com.slotsim.stats.StatisticsSummary;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SimulationRunnerTest {

    /** Exact RTP from the full cycle, PAR sheet cell X23. */
    private static final double THEORETICAL_RTP = 0.9335809099;

    private final MathModel model = new MathModelLoader().loadDefault();
    private final SimulationRunner runner = new SimulationRunner(model, CompositeWinEvaluator.forModel(model));

    @Test
    void playsExactlyTheRequestedRoundsIncludingPartialChunk() {
        SimulationResult result = runner.run(new SimulationConfig(25_001, 2, 1, 10_000));

        assertThat(result.statistics().rounds()).isEqualTo(25_001);
    }

    @Test
    void resultDoesNotDependOnThreadCount() {
        RoundStatistics single = runner.run(new SimulationConfig(300_000, 1, 42, 10_000)).statistics();
        RoundStatistics parallel = runner.run(new SimulationConfig(300_000, 4, 42, 10_000)).statistics();

        assertThat(parallel).usingRecursiveComparison().isEqualTo(single);
    }

    @Test
    void breakdownAddsUpToTotalWin() {
        SimulationResult result = runner.run(new SimulationConfig(100_000, 2, 7));

        long symbolWins = 0;
        for (int symbol = 0; symbol < result.breakdown().symbolCount(); symbol++) {
            symbolWins += result.breakdown().wins(symbol);
        }
        assertThat(symbolWins).isEqualTo(result.statistics().totalWin());
    }

    @Test
    void differentSeedsPlayDifferentRounds() {
        RoundStatistics first = runner.run(new SimulationConfig(100_000, 2, 1)).statistics();
        RoundStatistics second = runner.run(new SimulationConfig(100_000, 2, 2)).statistics();

        assertThat(first.totalWin()).isNotEqualTo(second.totalWin());
    }

    @Test
    void convergesToTheoreticalRtp() {
        StatisticsSummary summary = runner.run(new SimulationConfig(2_000_000, 4, 2026)).statistics()
                .summarize(model.bet());

        // 99.9% interval (z = 3.29) around the simulated RTP must contain the exact value
        assertThat(summary.rtpConfidenceInterval(3.29).contains(THEORETICAL_RTP)).isTrue();
    }

    @Test
    void rejectsInvalidConfig() {
        assertThatThrownBy(() -> new SimulationConfig(0, 1, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SimulationConfig(10, 0, 1)).isInstanceOf(IllegalArgumentException.class);
    }
}
