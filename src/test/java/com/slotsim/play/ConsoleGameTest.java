package com.slotsim.play;

import com.slotsim.config.MathModelLoader;
import com.slotsim.engine.CompositeWinEvaluator;
import com.slotsim.model.MathModel;
import com.slotsim.rng.RandomNumberGenerator;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.PrimitiveIterator;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class ConsoleGameTest {

    private final MathModel model = new MathModelLoader().loadDefault();

    @Test
    void playsScatterExampleAndMarksWinningCells() {
        String output = play(1_000, "\nq\n", 38, 43, 48);

        assertThat(output)
                .contains("Round 1, stops [38, 43, 48]")
                .contains("[SCA]  H3    L1")
                .contains(" L2   [SCA] [SCA]")
                .contains("[L2]  [L2]  [L2]")
                .contains("Line 3: 3x L2 = 20")
                .contains("Scatter: 3x SCA = 200")
                .contains("Win: 220   Balance: 1,210")
                .contains("Session: 1 rounds, bet 10, won 220, RTP 2200.00%, balance 1,210");
    }

    @Test
    void stopsWhenBalanceCannotCoverTheBet() {
        // stops 10/15/23 are the PAR sheet's non-winning example
        String output = play(20, "\n\n\n\n", 10, 15, 23, 10, 15, 23);

        assertThat(output)
                .contains("Round 2")
                .doesNotContain("Round 3")
                .contains("Balance 0 is below the bet of 10.");
    }

    @Test
    void endsWhenInputEnds() {
        String output = play(1_000, "", 0, 0, 0);

        assertThat(output).contains("Session: 0 rounds").doesNotContain("Round 1");
    }

    private String play(long balance, String input, int... stops) {
        PrimitiveIterator.OfInt values = IntStream.of(stops).iterator();
        RandomNumberGenerator rng = bound -> values.nextInt();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        new ConsoleGame(model, CompositeWinEvaluator.forModel(model), rng, balance,
                new BufferedReader(new StringReader(input)), new PrintStream(bytes, true, StandardCharsets.UTF_8))
                .run();
        return bytes.toString(StandardCharsets.UTF_8);
    }
}
