package com.slotsim.rng;

import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class SplittableRandomGeneratorTest {

    @Test
    void coversEveryValueInRangeRoughlyUniformly() {
        RandomNumberGenerator rng = new SplittableRandomGenerator(7);
        int bound = 61;
        int draws = 610_000;
        int[] counts = new int[bound];

        for (int i = 0; i < draws; i++) {
            counts[rng.nextInt(bound)]++; // out-of-range values would throw here
        }

        // expected 10,000 per value, sigma ~ 99; 6 sigma keeps the test deterministic in practice
        assertThat(IntStream.of(counts).min().orElseThrow()).isGreaterThanOrEqualTo(9_400);
        assertThat(IntStream.of(counts).max().orElseThrow()).isLessThanOrEqualTo(10_600);
    }

    @Test
    void isReproducibleForSameSeed() {
        RandomNumberGenerator first = new SplittableRandomGenerator(123);
        RandomNumberGenerator second = new SplittableRandomGenerator(123);

        for (int i = 0; i < 100; i++) {
            assertThat(first.nextInt(1_000)).isEqualTo(second.nextInt(1_000));
        }
    }
}
