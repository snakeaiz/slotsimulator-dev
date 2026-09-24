package com.slotsim.math;

import com.slotsim.config.MathModelLoader;
import com.slotsim.engine.CompositeWinEvaluator;
import com.slotsim.model.MathModel;
import com.slotsim.stats.RoundStatistics;
import com.slotsim.stats.StatisticsSummary;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class FullCycleCalculatorTest {

    private final MathModel model = new MathModelLoader().loadDefault();

    @Test
    void reproducesTheoreticalRtpOfParSheet() {
        RoundStatistics statistics = new FullCycleCalculator(model, CompositeWinEvaluator.forModel(model)).calculate();
        StatisticsSummary summary = statistics.summarize(model.bet());

        assertThat(statistics.rounds()).isEqualTo(61 * 58 * 54);
        // PAR sheet "Task math model", cell X23
        assertThat(summary.rtp()).isCloseTo(0.9335809099, within(1e-10));
    }

    @Test
    void producesExactTotals() {
        RoundStatistics statistics = new FullCycleCalculator(model, CompositeWinEvaluator.forModel(model)).calculate();
        StatisticsSummary summary = statistics.summarize(model.bet());

        // Independently computed by brute force outside the application
        assertThat(statistics.totalWin()).isEqualTo(1_783_625);
        assertThat(statistics.sumOfSquares()).isEqualTo(469_326_225);
        assertThat(statistics.winningRounds()).isEqualTo(25_449);
        assertThat(statistics.maxWin()).isEqualTo(2_500);
        assertThat(summary.volatilityIndex()).isCloseTo(4.867627, within(1e-6));
    }

    @Test
    void refusesCyclesAboveLimit() {
        FullCycleCalculator calculator = new FullCycleCalculator(model, CompositeWinEvaluator.forModel(model), 1_000);

        assertThatThrownBy(calculator::calculate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("exceeds the limit");
    }
}
