package com.slotsim.math;

import com.slotsim.config.MathModelLoader;
import com.slotsim.engine.CompositeWinEvaluator;
import com.slotsim.model.MathModel;
import com.slotsim.model.ReelSet;
import com.slotsim.model.ReelStrip;
import com.slotsim.stats.RoundStatistics;
import com.slotsim.stats.StatisticsSummary;
import com.slotsim.stats.WinBreakdown;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

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

    /** Per-symbol return, PAR sheet cells V8:V16. */
    @ParameterizedTest(name = "{0} returns {1}")
    @CsvSource({
            "W1,  0.005234177083",
            "H1,  0.03559240416",
            "H2,  0.1949730963",
            "H3,  0.2300944246",
            "L1,  0.06346439713",
            "L2,  0.1230555032",
            "L3,  0.05507662835",
            "L4,  0.01410610724",
            "SCA, 0.2119841718"})
    void reproducesPerSymbolReturnOfParSheet(String code, double expectedReturn) {
        WinBreakdown breakdown = new WinBreakdown(model.symbols().size());
        RoundStatistics statistics = new FullCycleCalculator(model, CompositeWinEvaluator.forModel(model))
                .calculate(breakdown);
        long totalBet = statistics.rounds() * model.bet();

        assertThat(breakdown.rtpContribution(model.symbol(code).id(), totalBet))
                .isCloseTo(expectedReturn, within(1e-10));
    }

    @Test
    void checksCycleSizeWithoutOverflowing() {
        int[] longReel = new int[3_000_000];
        ReelSet huge = new ReelSet(List.of(new ReelStrip(longReel), new ReelStrip(longReel),
                new ReelStrip(longReel), new ReelStrip(longReel)));

        assertThat(FullCycleCalculator.canEnumerate(model.reelSet(), 191_052)).isTrue();
        assertThat(FullCycleCalculator.canEnumerate(model.reelSet(), 191_051)).isFalse();
        assertThat(FullCycleCalculator.canEnumerate(huge, Long.MAX_VALUE)).isFalse();
    }

    @Test
    void refusesCyclesAboveLimit() {
        FullCycleCalculator calculator = new FullCycleCalculator(model, CompositeWinEvaluator.forModel(model), 1_000);

        assertThatThrownBy(calculator::calculate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("exceeds the limit");
    }
}
