package com.slotsim.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReelStripTest {

    private final ReelStrip strip = new ReelStrip(new int[]{4, 5, 6, 7});

    @Test
    void returnsSymbolAtPosition() {
        assertThat(strip.symbolAt(0)).isEqualTo(4);
        assertThat(strip.symbolAt(3)).isEqualTo(7);
    }

    @Test
    void wrapsAroundEndOfStrip() {
        assertThat(strip.symbolAt(4)).isEqualTo(4);
        assertThat(strip.symbolAt(5)).isEqualTo(5);
        assertThat(strip.symbolAt(-1)).isEqualTo(7);
    }

    @Test
    void countsSymbolOccurrences() {
        ReelStrip reel = new ReelStrip(new int[]{1, 2, 1, 1, 3});

        assertThat(reel.count(1)).isEqualTo(3);
        assertThat(reel.count(9)).isZero();
    }

    @Test
    void isImmutable() {
        int[] source = {1, 2, 3};
        ReelStrip reel = new ReelStrip(source);
        source[0] = 9;

        assertThat(reel.symbolAt(0)).isEqualTo(1);
    }

    @Test
    void rejectsEmptyStrip() {
        assertThatThrownBy(() -> new ReelStrip(new int[0]))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
