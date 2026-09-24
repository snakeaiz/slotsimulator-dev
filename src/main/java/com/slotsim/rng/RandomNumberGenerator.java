package com.slotsim.rng;

/**
 * Source of uniformly distributed random numbers used to stop the reels.
 * <p>
 * Kept as an interface so the engine does not depend on a concrete generator: a production game
 * server would plug in its certified RNG, the simulation uses a fast seeded generator and tests
 * use scripted values.
 */
@FunctionalInterface
public interface RandomNumberGenerator {

    /**
     * @param bound exclusive upper bound, must be positive
     * @return a uniformly distributed value in {@code [0, bound)}
     */
    int nextInt(int bound);
}
