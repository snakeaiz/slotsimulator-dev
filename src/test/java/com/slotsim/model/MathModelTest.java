package com.slotsim.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MathModelTest {

    private static final int A = 0;
    private static final int WILD = 1;
    private static final int SCATTER = 2;

    private static final List<Symbol> SYMBOLS = List.of(
            new Symbol(A, "A", "Ace", SymbolType.REGULAR),
            new Symbol(WILD, "W", "Wild", SymbolType.WILD),
            new Symbol(SCATTER, "S", "Scatter", SymbolType.SCATTER));

    private static final ReelSet REELS = new ReelSet(List.of(
            new ReelStrip(new int[]{A, WILD, SCATTER, A}),
            new ReelStrip(new int[]{A, A, SCATTER}),
            new ReelStrip(new int[]{WILD, A, SCATTER, A, A})));

    private static final Paytable PAYTABLE = new Paytable(
            new long[][]{{0, 0, 0, 5}, {0, 0, 0, 50}},
            new long[]{0, 0, 0, 20});

    @Test
    void acceptsValidModel() {
        MathModel model = model(2, REELS, List.of(new Payline(1, new int[]{0, 0, 0})), PAYTABLE);

        assertThat(model.reelCount()).isEqualTo(3);
        assertThat(model.symbol("W").isWild()).isTrue();
        assertThat(model.reelSet().cycleSize()).isEqualTo(4 * 3 * 5);
    }

    @Test
    void rejectsPaylineOutsideWindow() {
        List<Payline> lines = List.of(new Payline(1, new int[]{0, 1, 2}));

        assertThatThrownBy(() -> model(2, REELS, lines, PAYTABLE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("outside");
    }

    @Test
    void rejectsPaylineNotCrossingAllReels() {
        List<Payline> lines = List.of(new Payline(1, new int[]{0, 0}));

        assertThatThrownBy(() -> model(2, REELS, lines, PAYTABLE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must cross all");
    }

    @Test
    void rejectsReelShorterThanWindow() {
        assertThatThrownBy(() -> model(4, REELS, List.of(new Payline(1, new int[]{0, 0, 0})), PAYTABLE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("shorter than the window");
    }

    @Test
    void rejectsUnknownSymbolOnReel() {
        ReelSet reels = new ReelSet(List.of(
                new ReelStrip(new int[]{A, 7}), new ReelStrip(new int[]{A, A}), new ReelStrip(new int[]{A, A})));

        assertThatThrownBy(() -> model(2, reels, List.of(new Payline(1, new int[]{0, 0, 0})), PAYTABLE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown symbol id 7");
    }

    @Test
    void rejectsLinePaysForScatter() {
        Paytable paytable = new Paytable(new long[][]{{}, {}, {0, 0, 0, 100}}, new long[]{0, 0, 0, 20});

        assertThatThrownBy(() -> model(2, REELS, List.of(new Payline(1, new int[]{0, 0, 0})), paytable))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot have line pays");
    }

    @Test
    void rejectsNonDenseSymbolIds() {
        List<Symbol> symbols = List.of(new Symbol(1, "A", "Ace", SymbolType.REGULAR));

        assertThatThrownBy(() -> new MathModel("test", 10, 2, symbols, REELS, List.of(), PAYTABLE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dense");
    }

    private static MathModel model(int rows, ReelSet reels, List<Payline> lines, Paytable paytable) {
        return new MathModel("test", 10, rows, SYMBOLS, reels, lines, paytable);
    }
}
