package com.slotsim.engine;

import com.slotsim.config.MathModelLoader;
import com.slotsim.model.MathModel;
import org.junit.jupiter.api.Test;

import static com.slotsim.engine.Pictures.picture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class CompositeWinEvaluatorTest {

    private final MathModel model = new MathModelLoader().loadDefault();
    private final WinEvaluator evaluator = CompositeWinEvaluator.forModel(model);

    @Test
    void addsLineAndScatterWinsOfScatterExample() {
        ReelWindow window = picture(model,
                "SCA H3  L1",
                "L2  SCA SCA",
                "L2  L2  L2");
        WinRecorder recorder = new WinRecorder();

        assertThat(evaluator.evaluate(window, recorder)).isEqualTo(220);
        assertThat(recorder.wins()).extracting(Win::type, Win::amount)
                .containsExactly(
                        tuple(WinType.LINE, 20L),
                        tuple(WinType.SCATTER, 200L));
    }

    @Test
    void paysThreeLinesExample() {
        ReelWindow window = picture(model,
                "H3 L4 H3",
                "H3 H3 H3",
                "H3 L1 H3");

        assertThat(evaluator.evaluate(window)).isEqualTo(240);
    }
}
