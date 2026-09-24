package com.slotsim.simulation;

import com.slotsim.engine.SpinEngine;
import com.slotsim.engine.WinEvaluator;
import com.slotsim.model.MathModel;
import com.slotsim.rng.SplittableRandomGenerator;
import com.slotsim.stats.RoundStatistics;
import com.slotsim.stats.WinBreakdown;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Runs a Monte Carlo simulation of many rounds, in parallel.
 * <p>
 * The rounds are split into fixed-size chunks. Chunk seeds are drawn in order from a master
 * generator, so chunk {@code i} always plays the same rounds no matter which thread runs it.
 * Each chunk owns its engine and accumulator; only the immutable model and evaluator are shared,
 * so the hot loop needs no synchronisation. Chunk totals are exact integers, so merging them is
 * order independent and the result is identical for any thread count.
 */
public final class SimulationRunner {

    private final MathModel model;
    private final WinEvaluator evaluator;

    public SimulationRunner(MathModel model, WinEvaluator evaluator) {
        this.model = model;
        this.evaluator = evaluator;
    }

    public SimulationResult run(SimulationConfig config) {
        long start = System.nanoTime();
        List<Callable<ChunkResult>> chunks = chunks(config);

        RoundStatistics statistics = new RoundStatistics();
        WinBreakdown breakdown = new WinBreakdown(model.symbols().size());
        try (ExecutorService executor = Executors.newFixedThreadPool(config.threads())) {
            for (Future<ChunkResult> future : executor.invokeAll(chunks)) {
                ChunkResult chunk = future.get();
                statistics.merge(chunk.statistics());
                breakdown.merge(chunk.breakdown());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Simulation interrupted", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Simulation failed", e.getCause());
        }
        return new SimulationResult(config, statistics, breakdown, Duration.ofNanos(System.nanoTime() - start));
    }

    private List<Callable<ChunkResult>> chunks(SimulationConfig config) {
        SplittableRandom seeds = new SplittableRandom(config.seed());
        List<Callable<ChunkResult>> chunks = new ArrayList<>();
        for (long chunk = 0; chunk < config.chunks(); chunk++) {
            long seed = seeds.nextLong();
            int rounds = config.roundsInChunk(chunk);
            chunks.add(() -> playChunk(seed, rounds));
        }
        return chunks;
    }

    private ChunkResult playChunk(long seed, int rounds) {
        SpinEngine engine = new SpinEngine(model, evaluator, new SplittableRandomGenerator(seed));
        RoundStatistics statistics = new RoundStatistics();
        WinBreakdown breakdown = new WinBreakdown(model.symbols().size());
        for (int round = 0; round < rounds; round++) {
            statistics.record(engine.spin(breakdown));
        }
        return new ChunkResult(statistics, breakdown);
    }

    private record ChunkResult(RoundStatistics statistics, WinBreakdown breakdown) {
    }
}
