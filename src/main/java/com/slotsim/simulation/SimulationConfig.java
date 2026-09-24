package com.slotsim.simulation;

/**
 * Parameters of a Monte Carlo run.
 *
 * @param rounds    number of rounds to play
 * @param threads   worker threads
 * @param seed      master seed; the same seed gives the same result for any number of threads
 * @param chunkSize rounds per work unit; each chunk has its own random stream derived from the seed
 */
public record SimulationConfig(long rounds, int threads, long seed, int chunkSize) {

    public static final int DEFAULT_CHUNK_SIZE = 100_000;

    public SimulationConfig {
        if (rounds <= 0) {
            throw new IllegalArgumentException("Rounds must be positive: " + rounds);
        }
        if (threads <= 0) {
            throw new IllegalArgumentException("Threads must be positive: " + threads);
        }
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("Chunk size must be positive: " + chunkSize);
        }
    }

    public SimulationConfig(long rounds, int threads, long seed) {
        this(rounds, threads, seed, DEFAULT_CHUNK_SIZE);
    }

    long chunks() {
        return (rounds + chunkSize - 1) / chunkSize;
    }

    int roundsInChunk(long chunk) {
        return (int) Math.min(chunkSize, rounds - chunk * chunkSize);
    }
}
