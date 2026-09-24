package com.slotsim.config;

import com.slotsim.model.MathModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MathModelLoaderTest {

    private static final String VALID = """
            {
              "name": "Mini",
              "bet": 5,
              "rows": 1,
              "symbols": [
                {"code": "A", "name": "Ace", "type": "REGULAR"},
                {"code": "S", "name": "Scatter", "type": "SCATTER"}
              ],
              "reels": [["A", "S"], ["A", "A", "S"]],
              "paylines": [{"id": 1, "rows": [0, 0]}],
              "paytable": {
                "lines": [{"symbol": "A", "count": 2, "pay": 7}],
                "scatter": [{"count": 2, "pay": 30}]
              }
            }
            """;

    @TempDir
    Path tempDir;

    private final MathModelLoader loader = new MathModelLoader();

    @Test
    void loadsModelFromFile() throws IOException {
        MathModel model = loader.load(write(VALID));

        assertThat(model.name()).isEqualTo("Mini");
        assertThat(model.bet()).isEqualTo(5);
        assertThat(model.symbol("A").id()).isZero();
        assertThat(model.reelSet().reel(1).symbolAt(2)).isEqualTo(model.symbol("S").id());
        assertThat(model.paytable().linePay(0, 2)).isEqualTo(7);
        assertThat(model.paytable().scatterPay(2)).isEqualTo(30);
    }

    @Test
    void rejectsUnknownSymbolOnReel() throws IOException {
        Path file = write(VALID.replace("[\"A\", \"S\"]", "[\"A\", \"X\"]"));

        assertThatThrownBy(() -> loader.load(file))
                .isInstanceOf(MathModelLoadException.class)
                .hasMessageContaining("Unknown symbol code: X");
    }

    @Test
    void rejectsDuplicateSymbolCode() throws IOException {
        Path file = write(VALID.replace("\"code\": \"S\"", "\"code\": \"A\""));

        assertThatThrownBy(() -> loader.load(file))
                .isInstanceOf(MathModelLoadException.class)
                .hasMessageContaining("Duplicate symbol code: A");
    }

    @Test
    void rejectsUnknownJsonProperty() throws IOException {
        Path file = write(VALID.replace("\"bet\"", "\"betAmount\""));

        assertThatThrownBy(() -> loader.load(file))
                .isInstanceOf(MathModelLoadException.class);
    }

    @Test
    void surfacesDomainValidationErrors() throws IOException {
        Path file = write(VALID.replace("\"rows\": [0, 0]", "\"rows\": [0, 1]"));

        assertThatThrownBy(() -> loader.load(file))
                .isInstanceOf(MathModelLoadException.class)
                .hasMessageContaining("outside");
    }

    @Test
    void failsOnMissingResource() {
        assertThatThrownBy(() -> loader.loadResource("games/missing.json"))
                .isInstanceOf(MathModelLoadException.class)
                .hasMessageContaining("not found");
    }

    private Path write(String json) throws IOException {
        return Files.writeString(tempDir.resolve("model.json"), json);
    }
}
