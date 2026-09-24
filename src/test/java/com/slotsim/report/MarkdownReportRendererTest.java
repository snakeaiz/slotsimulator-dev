package com.slotsim.report;

import com.slotsim.config.MathModelLoader;
import com.slotsim.engine.CompositeWinEvaluator;
import com.slotsim.engine.WinEvaluator;
import com.slotsim.math.FullCycleCalculator;
import com.slotsim.model.MathModel;
import com.slotsim.simulation.SimulationConfig;
import com.slotsim.simulation.SimulationResult;
import com.slotsim.simulation.SimulationRunner;
import com.slotsim.stats.RoundStatistics;
import com.slotsim.stats.WinBreakdown;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownReportRendererTest {

    private final MathModel model = new MathModelLoader().loadDefault();
    private final WinEvaluator evaluator = CompositeWinEvaluator.forModel(model);
    private final SimulationResult simulation =
            new SimulationRunner(model, evaluator).run(new SimulationConfig(200_000, 2, 11));

    @Test
    void rendersSimulationAgainstTheory() {
        WinBreakdown breakdown = new WinBreakdown(model.symbols().size());
        RoundStatistics exact = new FullCycleCalculator(model, evaluator).calculate(breakdown);
        SimulationReport report = new SimulationReport(model, simulation,
                Optional.of(new SimulationReport.Theoretical(exact.summarize(model.bet()), breakdown)),
                Instant.parse("2026-09-24T10:00:00Z"));

        String markdown = new MarkdownReportRenderer().render(report);

        assertThat(markdown)
                .contains("# Simulation report: Fruit Slot")
                .contains("| Rounds | 200,000 |")
                .contains("| Seed | 11 |")
                .contains("| Total bet | 2,000,000 | 1,910,520 |")
                .contains("93.3581%")
                .contains("Theoretical RTP inside the interval: **yes**")
                .contains("| SCA (Scatter) |")
                .contains("21.1984%");
    }

    @Test
    void rendersSimulationOnlyWhenTheoryIsUnavailable() {
        SimulationReport report = new SimulationReport(model, simulation, Optional.empty(), Instant.now());

        String markdown = new MarkdownReportRenderer().render(report);

        assertThat(markdown)
                .contains("| Metric | Simulated |")
                .doesNotContain("Theoretical");
    }
}
