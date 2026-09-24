package com.slotsim.engine;

import com.slotsim.config.MathModelLoader;
import com.slotsim.model.MathModel;
import com.slotsim.model.Payline;
import com.slotsim.model.Paytable;
import com.slotsim.model.ReelSet;
import com.slotsim.model.ReelStrip;
import com.slotsim.model.Symbol;
import com.slotsim.model.SymbolType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static com.slotsim.engine.Pictures.picture;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Line wins on Fruit Slot. Paylines: 1 top, 2 middle, 3 bottom, 4 diagonal down, 5 diagonal up.
 */
class LineWinEvaluatorTest {

    private final MathModel model = new MathModelLoader().loadDefault();
    private final LineWinEvaluator evaluator = new LineWinEvaluator(model);
    private final WinRecorder recorder = new WinRecorder();

    @Test
    void paysNothingForNonWinningExample() {
        ReelWindow window = picture(model,
                "H3 L2 L4",
                "H1 W1 L4",
                "H2 L3 L4");

        assertThat(evaluator.evaluate(window, recorder)).isZero();
        assertThat(recorder.wins()).isEmpty();
    }

    @Test
    void paysOneLineExample() {
        ReelWindow window = picture(model,
                "L3 L4 L1",
                "L2 L4 L4",
                "L3 L3 L3");

        assertThat(evaluator.evaluate(window, recorder)).isEqualTo(15);
        assertThat(recorder.wins()).containsExactly(line(3, "L3", 15));
    }

    @Test
    void paysThreeLinesExample() {
        ReelWindow window = picture(model,
                "H3 L4 H3",
                "H3 H3 H3",
                "H3 L1 H3");

        assertThat(evaluator.evaluate(window, recorder)).isEqualTo(240);
        assertThat(recorder.wins()).containsExactly(line(2, "H3", 80), line(4, "H3", 80), line(5, "H3", 80));
    }

    @Test
    void paysLineWinOfScatterExampleWithoutScatter() {
        ReelWindow window = picture(model,
                "SCA H3  L1",
                "L2  SCA SCA",
                "L2  L2  L2");

        assertThat(evaluator.evaluate(window, recorder)).isEqualTo(20);
        assertThat(recorder.wins()).containsExactly(line(3, "L2", 20));
    }

    @Test
    void paysEveryLineIndependently() {
        ReelWindow window = picture(model,
                "L4 L4 L4",
                "L4 L4 L4",
                "L4 L4 L4");

        assertThat(evaluator.evaluate(window)).isEqualTo(5 * 10);
    }

    @ParameterizedTest(name = "{0} {1} {2} on a line pays {3}")
    @CsvSource({
            "W1,  W1,  W1,  2000",   // all wilds pay as wild, not as the best substituted symbol
            "W1,  W1,  H1,  800",
            "H1,  W1,  W1,  800",
            "W1,  H2,  W1,  500",
            "L4,  W1,  L4,  10",
            "W1,  H1,  H2,  0",
            "H1,  H1,  H2,  0",
            "W1,  W1,  SCA, 0",      // wild does not substitute for scatter
            "SCA, W1,  W1,  0",
            "SCA, SCA, SCA, 0"})     // scatters never pay on lines
    void appliesWildSubstitution(String reel0, String reel1, String reel2, long expected) {
        ReelWindow window = picture(model,
                "L1 L2 L3",
                reel0 + " " + reel1 + " " + reel2,
                "L2 L3 L4");

        assertThat(evaluator.evaluate(window)).isEqualTo(expected);
    }

    /**
     * A 4x1 game paying 3 and 4 of a kind from the left shows the evaluator is not tied to 3x3.
     */
    @ParameterizedTest(name = "{0} pays {1}")
    @CsvSource({
            "A A A B, 5",
            "A A A A, 20",
            "W A A A, 20",
            "W W W W, 100",
            "W W W A, 20",   // 4x A (20) beats 3x W (unpaid)
            "B A A A, 0",    // runs start at the leftmost reel
            "A A B A, 0"})
    void paysCountOfAKindFromLeftOnWiderGrid(String picture, long expected) {
        MathModel wide = new MathModel("wide", 1, 1,
                List.of(new Symbol(0, "A", "Ace", SymbolType.REGULAR),
                        new Symbol(1, "B", "Bee", SymbolType.REGULAR),
                        new Symbol(2, "W", "Wild", SymbolType.WILD)),
                new ReelSet(List.of(new ReelStrip(new int[]{0, 1, 2}), new ReelStrip(new int[]{0, 1, 2}),
                        new ReelStrip(new int[]{0, 1, 2}), new ReelStrip(new int[]{0, 1, 2}))),
                List.of(new Payline(1, new int[]{0, 0, 0, 0})),
                new Paytable(new long[][]{{0, 0, 0, 5, 20}, {}, {0, 0, 0, 0, 100}}, new long[0]));

        assertThat(new LineWinEvaluator(wide).evaluate(picture(wide, picture))).isEqualTo(expected);
    }

    private Win line(int paylineId, String code, long amount) {
        return new Win(WinType.LINE, paylineId, model.symbol(code).id(), 3, amount);
    }
}
