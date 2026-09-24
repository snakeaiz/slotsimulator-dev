package com.slotsim.report;

import com.slotsim.model.MathModel;
import com.slotsim.simulation.SimulationResult;
import com.slotsim.stats.StatisticsSummary;
import com.slotsim.stats.WinBreakdown;

import java.time.Instant;
import java.util.Optional;

/**
 * Everything a report shows: the simulated run and, when the full cycle is small enough to
 * enumerate, the exact theoretical figures to compare it with.
 *
 * @param model       simulated math model
 * @param simulation  Monte Carlo result
 * @param theoretical exact full-cycle statistics and breakdown, if computed
 * @param createdAt   report timestamp
 */
public record SimulationReport(
        MathModel model,
        SimulationResult simulation,
        Optional<Theoretical> theoretical,
        Instant createdAt) {

    public StatisticsSummary simulated() {
        return simulation.statistics().summarize(model.bet());
    }

    /**
     * Exact figures of the math model.
     */
    public record Theoretical(StatisticsSummary summary, WinBreakdown breakdown) {
    }
}
