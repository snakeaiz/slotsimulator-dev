package com.slotsim.cli;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommandLineOptionsTest {

    @Test
    void usesDefaultsWithoutArguments() {
        CommandLineOptions options = CommandLineOptions.parse(new String[0]);

        assertThat(options.rounds()).isEqualTo(10_000_000L);
        assertThat(options.threads()).isEqualTo(Runtime.getRuntime().availableProcessors());
        assertThat(options.config()).isEmpty();
        assertThat(options.reportDir()).isEmpty();
        assertThat(options.benchmark()).isFalse();
    }

    @Test
    void parsesAllOptions() {
        CommandLineOptions options = CommandLineOptions.parse(new String[]{
                "--rounds", "1_000_000", "--threads", "4", "--seed", "42",
                "--config", "game.json", "--report", "reports", "--benchmark"});

        assertThat(options.rounds()).isEqualTo(1_000_000L);
        assertThat(options.threads()).isEqualTo(4);
        assertThat(options.seed()).isEqualTo(42);
        assertThat(options.config()).contains(Path.of("game.json"));
        assertThat(options.reportDir()).contains(Path.of("reports"));
        assertThat(options.benchmark()).isTrue();
    }

    @Test
    void rejectsUnknownOption() {
        assertThatThrownBy(() -> CommandLineOptions.parse(new String[]{"--spins", "5"}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown option: --spins");
    }

    @Test
    void rejectsMissingValue() {
        assertThatThrownBy(() -> CommandLineOptions.parse(new String[]{"--rounds"}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing value");
    }

    @Test
    void rejectsInvalidNumbers() {
        assertThatThrownBy(() -> CommandLineOptions.parse(new String[]{"--rounds", "ten"}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid number");
        assertThatThrownBy(() -> CommandLineOptions.parse(new String[]{"--threads", "0"}))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
