package com.slotsim.rng;

import java.util.SplittableRandom;

/**
 * Fast, seedable generator for simulation, backed by {@link SplittableRandom}.
 * <p>
 * Not thread-safe: each simulation thread must own its instance. {@code SplittableRandom.nextInt(bound)}
 * is unbiased (it rejects values that would skew the modulo), which matters for reel stops.
 */
public final class SplittableRandomGenerator implements RandomNumberGenerator {

    private final SplittableRandom random;

    public SplittableRandomGenerator(long seed) {
        this.random = new SplittableRandom(seed);
    }

    @Override
    public int nextInt(int bound) {
        return random.nextInt(bound);
    }
}
