package com.slotsim.simulation;

import com.slotsim.stats.RoundStatistics;
import com.slotsim.stats.WinBreakdown;

import java.time.Duration;

/**
 * Outcome of a Monte Carlo run.
 */
public record SimulationResult(
        SimulationConfig config,
        RoundStatistics statistics,
        WinBreakdown breakdown,
        Duration elapsed) {

    public double roundsPerSecond() {
        return statistics.rounds() / (elapsed.toNanos() / 1e9);
    }
}
