package com.slotsim.engine;

import com.slotsim.config.MathModelLoader;
import com.slotsim.model.MathModel;
import com.slotsim.rng.RandomNumberGenerator;
import com.slotsim.rng.SplittableRandomGenerator;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpinEngineTest {

    private final MathModel model = new MathModelLoader().loadDefault();
    private final WinEvaluator evaluator = CompositeWinEvaluator.forModel(model);

    @Test
    void drawsOneStopPerReelWithinReelLength() {
        ScriptedRng rng = new ScriptedRng(38, 43, 48);
        SpinEngine engine = new SpinEngine(model, evaluator, rng);

        long win = engine.spin();

        assertThat(rng.bounds).containsExactly(61, 58, 54);
        assertThat(engine.lastStops()).containsExactly(38, 43, 48);
        assertThat(win).isEqualTo(220);
    }

    @Test
    void replaysKnownStops() {
        SpinEngine engine = new SpinEngine(model, evaluator, new ScriptedRng());
        WinRecorder recorder = new WinRecorder();

        assertThat(engine.spinAt(new int[]{43, 25, 20}, recorder)).isEqualTo(240);
        assertThat(recorder.wins()).extracting(Win::paylineId).containsExactly(2, 4, 5);
        assertThat(engine.spinAt(new int[]{10, 15, 23}, WinListener.NONE)).isZero();
    }

    @Test
    void rejectsStopOutsideReel() {
        SpinEngine engine = new SpinEngine(model, evaluator, new ScriptedRng());

        assertThatThrownBy(() -> engine.spinAt(new int[]{0, 58, 0}, WinListener.NONE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("outside reel 1");
    }

    @Test
    void sameSeedPlaysSameRounds() {
        SpinEngine first = new SpinEngine(model, evaluator, new SplittableRandomGenerator(42));
        SpinEngine second = new SpinEngine(model, evaluator, new SplittableRandomGenerator(42));

        for (int round = 0; round < 1_000; round++) {
            assertThat(first.spin()).isEqualTo(second.spin());
            assertThat(first.lastStops()).containsExactly(second.lastStops());
        }
    }

    /** Returns predefined values and records the bounds it was asked for. */
    private static final class ScriptedRng implements RandomNumberGenerator {

        private final Deque<Integer> values = new ArrayDeque<>();
        private final List<Integer> bounds = new ArrayList<>();

        ScriptedRng(int... values) {
            for (int value : values) {
                this.values.add(value);
            }
        }

        @Override
        public int nextInt(int bound) {
            bounds.add(bound);
            return values.remove();
        }
    }
}
