package com.slotsim.stats;

import com.slotsim.engine.WinListener;
import com.slotsim.engine.WinType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WinBreakdownTest {

    @Test
    void accumulatesWinsAndHitsPerSymbol() {
        WinBreakdown breakdown = new WinBreakdown(3);
        breakdown.onWin(WinType.LINE, 1, 0, 3, 10);
        breakdown.onWin(WinType.LINE, 2, 0, 3, 10);
        breakdown.onWin(WinType.SCATTER, WinListener.NO_PAYLINE, 2, 3, 200);

        assertThat(breakdown.wins(0)).isEqualTo(20);
        assertThat(breakdown.hits(0)).isEqualTo(2);
        assertThat(breakdown.wins(1)).isZero();
        assertThat(breakdown.wins(2)).isEqualTo(200);
        assertThat(breakdown.rtpContribution(2, 1_000)).isEqualTo(0.2);
    }

    @Test
    void mergesPerSymbol() {
        WinBreakdown first = new WinBreakdown(2);
        first.onWin(WinType.LINE, 1, 0, 3, 5);
        WinBreakdown second = new WinBreakdown(2);
        second.onWin(WinType.LINE, 1, 0, 3, 7);
        second.onWin(WinType.LINE, 1, 1, 3, 1);

        first.merge(second);

        assertThat(first.wins(0)).isEqualTo(12);
        assertThat(first.hits(0)).isEqualTo(2);
        assertThat(first.wins(1)).isEqualTo(1);
    }
}
