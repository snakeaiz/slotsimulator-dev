package com.slotsim.report;

import com.slotsim.model.Symbol;
import com.slotsim.simulation.SimulationConfig;
import com.slotsim.stats.StatisticsSummary;
import com.slotsim.stats.StatisticsSummary.ConfidenceInterval;
import com.slotsim.stats.WinBreakdown;

import java.util.Locale;

/**
 * Renders a {@link SimulationReport} as Markdown, readable both in a terminal and on GitHub.
 */
public final class MarkdownReportRenderer {

    public String render(SimulationReport report) {
        StatisticsSummary simulated = report.simulated();
        ConfidenceInterval interval = simulated.rtpConfidenceInterval(StatisticsSummary.Z_95);
        SimulationConfig config = report.simulation().config();

        StringBuilder out = new StringBuilder();
        line(out, "# Simulation report: %s", report.model().name());
        line(out, "");
        line(out, "Generated %s", report.createdAt());
        line(out, "");
        line(out, "## Run");
        line(out, "");
        line(out, "| Parameter | Value |");
        line(out, "|---|---|");
        line(out, "| Rounds | %,d |", config.rounds());
        line(out, "| Bet per round | %,d |", report.model().bet());
        line(out, "| Threads | %d |", config.threads());
        line(out, "| Seed | %d |", config.seed());
        line(out, "| Execution time | %.3f s |", report.simulation().elapsed().toNanos() / 1e9);
        line(out, "| Throughput | %,.0f rounds/s |", report.simulation().roundsPerSecond());
        line(out, "");
        line(out, "## Results");
        line(out, "");
        report.theoretical().ifPresentOrElse(
                theoretical -> renderComparison(out, simulated, theoretical.summary()),
                () -> renderSimulatedOnly(out, simulated));
        line(out, "");
        line(out, "RTP 95%% confidence interval: [%s, %s] (±%s)",
                percent(interval.lower()), percent(interval.upper()), percent(interval.halfWidth()));
        report.theoretical().ifPresent(theoretical -> line(out, "Theoretical RTP inside the interval: **%s**",
                interval.contains(theoretical.summary().rtp()) ? "yes" : "no"));
        line(out, "");
        renderBreakdown(out, report);
        return out.toString();
    }

    private static void renderComparison(StringBuilder out, StatisticsSummary simulated, StatisticsSummary exact) {
        line(out, "| Metric | Simulated | Theoretical (full cycle of %,d combinations) |", exact.rounds());
        line(out, "|---|---:|---:|");
        line(out, "| Total bet | %,d | %,d |", simulated.totalBet(), exact.totalBet());
        line(out, "| Total win | %,d | %,d |", simulated.totalWin(), exact.totalWin());
        line(out, "| RTP | %s | %s |", percent(simulated.rtp()), percent(exact.rtp()));
        line(out, "| Standard deviation (credits) | %.4f | %.4f |",
                simulated.standardDeviation(), exact.standardDeviation());
        line(out, "| Standard deviation (bets) | %.4f | %.4f |",
                simulated.volatilityIndex(), exact.volatilityIndex());
        line(out, "| Hit frequency | %s | %s |", percent(simulated.hitFrequency()), percent(exact.hitFrequency()));
        line(out, "| Max win | %,d | %,d |", simulated.maxWin(), exact.maxWin());
    }

    private static void renderSimulatedOnly(StringBuilder out, StatisticsSummary simulated) {
        line(out, "| Metric | Simulated |");
        line(out, "|---|---:|");
        line(out, "| Total bet | %,d |", simulated.totalBet());
        line(out, "| Total win | %,d |", simulated.totalWin());
        line(out, "| RTP | %s |", percent(simulated.rtp()));
        line(out, "| Standard deviation (credits) | %.4f |", simulated.standardDeviation());
        line(out, "| Standard deviation (bets) | %.4f |", simulated.volatilityIndex());
        line(out, "| Hit frequency | %s |", percent(simulated.hitFrequency()));
        line(out, "| Max win | %,d |", simulated.maxWin());
    }

    private static void renderBreakdown(StringBuilder out, SimulationReport report) {
        WinBreakdown simulated = report.simulation().breakdown();
        long simulatedBet = report.simulated().totalBet();
        line(out, "## RTP by symbol");
        line(out, "");
        if (report.theoretical().isPresent()) {
            line(out, "| Symbol | Simulated hits | Simulated RTP | Theoretical RTP |");
            line(out, "|---|---:|---:|---:|");
        } else {
            line(out, "| Symbol | Simulated hits | Simulated RTP |");
            line(out, "|---|---:|---:|");
        }
        for (Symbol symbol : report.model().symbols()) {
            String row = String.format(Locale.ROOT, "| %s (%s) | %,d | %s |", symbol.code(), symbol.name(),
                    simulated.hits(symbol.id()), percent(simulated.rtpContribution(symbol.id(), simulatedBet)));
            if (report.theoretical().isPresent()) {
                SimulationReport.Theoretical theoretical = report.theoretical().get();
                row += " " + percent(theoretical.breakdown()
                        .rtpContribution(symbol.id(), theoretical.summary().totalBet())) + " |";
            }
            line(out, "%s", row);
        }
    }

    private static String percent(double value) {
        return String.format(Locale.ROOT, "%.4f%%", value * 100);
    }

    private static void line(StringBuilder out, String format, Object... args) {
        out.append(String.format(Locale.ROOT, format, args)).append('\n');
    }
}
