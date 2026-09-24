package com.slotsim;

import com.slotsim.cli.CommandLineOptions;
import com.slotsim.config.MathModelLoader;
import com.slotsim.engine.CompositeWinEvaluator;
import com.slotsim.engine.WinEvaluator;
import com.slotsim.math.FullCycleCalculator;
import com.slotsim.model.MathModel;
import com.slotsim.report.MarkdownReportRenderer;
import com.slotsim.report.SimulationReport;
import com.slotsim.simulation.SimulationConfig;
import com.slotsim.simulation.SimulationResult;
import com.slotsim.simulation.SimulationRunner;
import com.slotsim.stats.RoundStatistics;
import com.slotsim.stats.WinBreakdown;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

/**
 * Command-line entry point: loads the math model, computes its exact full-cycle figures, runs the
 * Monte Carlo simulation and prints (and optionally saves) the report.
 */
public final class SlotSimulatorApp {

    private static final DateTimeFormatter FILE_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC);

    /** Rounds played before a benchmark so that both measured runs execute JIT-compiled code. */
    private static final long BENCHMARK_WARM_UP_ROUNDS = 2_000_000L;

    private SlotSimulatorApp() {
    }

    public static void main(String[] args) {
        CommandLineOptions options;
        try {
            options = CommandLineOptions.parse(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.err.println(CommandLineOptions.USAGE);
            System.exit(2);
            return;
        }
        if (options.help()) {
            System.out.println(CommandLineOptions.USAGE);
            return;
        }

        MathModelLoader loader = new MathModelLoader();
        MathModel model = options.config().map(loader::load).orElseGet(loader::loadDefault);
        WinEvaluator evaluator = CompositeWinEvaluator.forModel(model);
        SimulationRunner runner = new SimulationRunner(model, evaluator);

        if (options.benchmark()) {
            benchmark(runner, options);
            return;
        }

        SimulationResult result = runner.run(new SimulationConfig(options.rounds(), options.threads(), options.seed()));
        SimulationReport report = new SimulationReport(model, result, theoretical(model, evaluator), Instant.now());
        String markdown = new MarkdownReportRenderer().render(report);

        System.out.println(markdown);
        options.reportDir().ifPresent(dir -> System.out.println("Report written to " + write(dir, report, markdown)));
    }

    private static Optional<SimulationReport.Theoretical> theoretical(MathModel model, WinEvaluator evaluator) {
        if (!FullCycleCalculator.canEnumerate(model.reelSet(), FullCycleCalculator.DEFAULT_MAX_CYCLE_SIZE)) {
            return Optional.empty();
        }
        WinBreakdown breakdown = new WinBreakdown(model.symbols().size());
        RoundStatistics statistics = new FullCycleCalculator(model, evaluator).calculate(breakdown);
        return Optional.of(new SimulationReport.Theoretical(statistics.summarize(model.bet()), breakdown));
    }

    private static void benchmark(SimulationRunner runner, CommandLineOptions options) {
        runner.run(new SimulationConfig(BENCHMARK_WARM_UP_ROUNDS, options.threads(), options.seed()));

        SimulationResult single = runner.run(new SimulationConfig(options.rounds(), 1, options.seed()));
        SimulationResult parallel = runner.run(new SimulationConfig(options.rounds(), options.threads(), options.seed()));

        System.out.printf(Locale.ROOT, "Benchmark: %,d rounds, seed %d%n%n", options.rounds(), options.seed());
        System.out.println("| Threads | Time | Rounds/s |");
        System.out.println("|---:|---:|---:|");
        for (SimulationResult result : new SimulationResult[]{single, parallel}) {
            System.out.printf(Locale.ROOT, "| %d | %.3f s | %,.0f |%n", result.config().threads(),
                    result.elapsed().toNanos() / 1e9, result.roundsPerSecond());
        }
        System.out.printf(Locale.ROOT, "%nSpeed-up: %.2fx%n",
                (double) single.elapsed().toNanos() / parallel.elapsed().toNanos());
        System.out.println("Identical statistics: "
                + (single.statistics().totalWin() == parallel.statistics().totalWin()
                && single.statistics().sumOfSquares() == parallel.statistics().sumOfSquares() ? "yes" : "no"));
    }

    private static Path write(Path dir, SimulationReport report, String markdown) {
        try {
            Files.createDirectories(dir);
            return Files.writeString(dir.resolve("simulation-" + FILE_TIMESTAMP.format(report.createdAt()) + ".md"),
                    markdown);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write report to " + dir, e);
        }
    }
}
