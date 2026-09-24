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

import java.util.List;

import static com.slotsim.engine.Pictures.picture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScatterWinEvaluatorTest {

    private final MathModel model = new MathModelLoader().loadDefault();
    private final ScatterWinEvaluator evaluator = new ScatterWinEvaluator(model);
    private final WinRecorder recorder = new WinRecorder();

    @Test
    void paysThreeScattersAnywhere() {
        ReelWindow window = picture(model,
                "SCA H3  L1",
                "L2  SCA SCA",
                "L2  L2  L2");

        assertThat(evaluator.evaluate(window, recorder)).isEqualTo(200);
        assertThat(recorder.wins()).containsExactly(
                new Win(WinType.SCATTER, WinListener.NO_PAYLINE, model.symbol("SCA").id(), 3, 200));
    }

    @Test
    void paysScattersOnSameRow() {
        ReelWindow window = picture(model,
                "L1  L2  L3",
                "SCA SCA SCA",
                "L2  L3  L4");

        assertThat(evaluator.evaluate(window)).isEqualTo(200);
    }

    @Test
    void paysNothingForTwoScatters() {
        ReelWindow window = picture(model,
                "SCA L2 L3",
                "L1  L2 SCA",
                "L2  L3 L4");

        assertThat(evaluator.evaluate(window, recorder)).isZero();
        assertThat(recorder.wins()).isEmpty();
    }

    @Test
    void wildDoesNotCountAsScatter() {
        ReelWindow window = picture(model,
                "SCA L2 L3",
                "L1  W1 SCA",
                "L2  L3 L4");

        assertThat(evaluator.evaluate(window)).isZero();
    }

    @Test
    void paysHighestListedCountForMoreScatters() {
        ReelWindow window = picture(model,
                "SCA L2  L3",
                "SCA SCA SCA",
                "L2  L3  L4");

        assertThat(evaluator.evaluate(window)).isEqualTo(200);
    }

    @Test
    void requiresScatterSymbol() {
        MathModel noScatter = new MathModel("plain", 1, 1,
                List.of(new Symbol(0, "A", "Ace", SymbolType.REGULAR)),
                new ReelSet(List.of(new ReelStrip(new int[]{0}))),
                List.of(new Payline(1, new int[]{0})),
                new Paytable(new long[][]{{0, 1}}, new long[0]));

        assertThatThrownBy(() -> new ScatterWinEvaluator(noScatter))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no scatter");
    }
}
