package com.slotsim.config;

import com.slotsim.model.MathModel;
import com.slotsim.model.ReelStrip;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cross-checks the bundled config against the PAR sheet ("Task math model" tab).
 */
class FruitSlotConfigTest {

    private final MathModel model = new MathModelLoader().loadDefault();

    @Test
    void matchesGameParameters() {
        assertThat(model.bet()).isEqualTo(10);
        assertThat(model.rows()).isEqualTo(3);
        assertThat(model.reelCount()).isEqualTo(3);
        assertThat(model.paylines()).hasSize(5);
    }

    @Test
    void matchesReelLengths() {
        assertThat(model.reelSet().reels()).extracting(ReelStrip::length).containsExactly(61, 58, 54);
    }

    /** Symbol counts per reel, PAR sheet cells Q8:S16. */
    @ParameterizedTest(name = "{0} appears {1}/{2}/{3} times")
    @CsvSource({
            "W1, 1, 1, 1",
            "H1, 2, 2, 1",
            "H2, 4, 4, 5",
            "H3, 9, 9, 10",
            "L1, 8, 5, 8",
            "L2, 13, 11, 13",
            "L3, 11, 12, 8",
            "L4, 8, 9, 5",
            "SCA, 5, 5, 3"})
    void matchesSymbolCountsPerReel(String code, int reel0, int reel1, int reel2) {
        int id = model.symbol(code).id();

        assertThat(model.reelSet().reels()).extracting(reel -> reel.count(id)).containsExactly(reel0, reel1, reel2);
    }

    /** Paytable, PAR sheet cells A31:C41. */
    @ParameterizedTest(name = "3x {0} pays {1}")
    @CsvSource({"W1, 2000", "H1, 800", "H2, 500", "H3, 80", "L1, 50", "L2, 20", "L3, 15", "L4, 10"})
    void matchesLinePays(String code, long pay) {
        assertThat(model.paytable().linePay(model.symbol(code).id(), 3)).isEqualTo(pay);
    }

    @Test
    void matchesScatterPay() {
        assertThat(model.paytable().scatterPay(3)).isEqualTo(200);
        assertThat(model.paytable().linePay(model.symbol("SCA").id(), 3)).isZero();
    }
}
