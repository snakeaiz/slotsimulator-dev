package com.slotsim.engine;

import com.slotsim.config.MathModelLoader;
import com.slotsim.model.MathModel;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Reel picture positioning against the Fruit Slot reels.
 * <p>
 * Expected pictures are the "Examples of Game play" from the PAR sheet. Only the non-winning
 * example's stops match the reel strips; the other examples are drawn 4 positions off on some
 * reels, so the stops below are the ones that actually produce the drawn pictures.
 */
class ReelWindowTest {

    private final MathModel model = new MathModelLoader().loadDefault();
    private final ReelWindow window = new ReelWindow(model.reelCount(), model.rows());

    @Test
    void showsNonWinningExample() {
        window.fill(model.reelSet(), new int[]{10, 15, 23});

        assertPicture("H3 L2 L4",
                      "H1 W1 L4",
                      "H2 L3 L4");
    }

    @Test
    void showsThreeLinesExample() {
        // PAR sheet states stops 39/25/20; reel 0 shows this column at 43 (or 20).
        window.fill(model.reelSet(), new int[]{43, 25, 20});

        assertPicture("H3 L4 H3",
                      "H3 H3 H3",
                      "H3 L1 H3");
    }

    @Test
    void showsScatterExample() {
        // PAR sheet states stops 34/39/44; the drawn picture is at 38/43/48.
        window.fill(model.reelSet(), new int[]{38, 43, 48});

        assertPicture("SCA H3  L1",
                      "L2  SCA SCA",
                      "L2  L2  L2");
        assertThat(window.count(model.symbol("SCA").id())).isEqualTo(3);
    }

    @Test
    void wrapsAroundEndOfEachStrip() {
        window.fill(model.reelSet(), new int[]{60, 57, 53});

        assertPicture("L3 L3 L1",
                      "L4 L4 L4",
                      "L3 L3 L3");
    }

    @Test
    void overwritesPreviousPictureWhenReused() {
        window.fill(model.reelSet(), new int[]{38, 43, 48});
        window.fill(model.reelSet(), new int[]{10, 15, 23});

        assertPicture("H3 L2 L4",
                      "H1 W1 L4",
                      "H2 L3 L4");
    }

    @Test
    void buildsWindowFromColumns() {
        ReelWindow custom = ReelWindow.ofColumns(new int[]{1, 2}, new int[]{3, 4}, new int[]{5, 6});

        assertThat(custom.reels()).isEqualTo(3);
        assertThat(custom.rows()).isEqualTo(2);
        assertThat(custom.symbolAt(1, 0)).isEqualTo(3);
        assertThat(custom.symbolAt(2, 1)).isEqualTo(6);
    }

    @Test
    void rejectsWrongNumberOfStops() {
        assertThatThrownBy(() -> window.fill(model.reelSet(), new int[]{1, 2}))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** Asserts the picture row by row, top row first, symbols given by code. */
    private void assertPicture(String... rows) {
        String[] actual = new String[window.rows()];
        for (int row = 0; row < window.rows(); row++) {
            StringBuilder line = new StringBuilder();
            for (int reel = 0; reel < window.reels(); reel++) {
                line.append(reel == 0 ? "" : " ").append(model.symbol(window.symbolAt(reel, row)).code());
            }
            actual[row] = line.toString();
        }
        String[] expected = Arrays.stream(rows).map(row -> row.trim().replaceAll("\\s+", " ")).toArray(String[]::new);
        assertThat(actual).containsExactly(expected);
    }
}
