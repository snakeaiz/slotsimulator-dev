package com.slotsim.cli;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Parsed command-line options.
 *
 * @param rounds    rounds to simulate
 * @param threads   worker threads
 * @param seed      master seed of the simulation
 * @param config    math model file; the bundled model is used when empty
 * @param reportDir directory for the Markdown report; no file is written when empty
 * @param benchmark compare a single-threaded run with a parallel one
 * @param help      print usage and exit
 */
public record CommandLineOptions(
        long rounds,
        int threads,
        long seed,
        Optional<Path> config,
        Optional<Path> reportDir,
        boolean benchmark,
        boolean help) {

    public static final long DEFAULT_ROUNDS = 10_000_000L;

    public static final String USAGE = """
            Usage: slot-simulator [options]

              --rounds <n>     rounds to simulate (default 10000000, underscores allowed)
              --threads <n>    worker threads (default: available processors)
              --seed <n>       master seed (default: random, printed in the report)
              --config <file>  math model JSON (default: bundled Fruit Slot model)
              --report <dir>   also write the report as Markdown into this directory
              --benchmark      compare 1 thread with --threads on the same seed
              --help           print this help
            """;

    public static CommandLineOptions parse(String[] args) {
        long rounds = DEFAULT_ROUNDS;
        int threads = Runtime.getRuntime().availableProcessors();
        long seed = System.nanoTime();
        Path config = null;
        Path reportDir = null;
        boolean benchmark = false;
        boolean help = false;

        for (int i = 0; i < args.length; i++) {
            String option = args[i];
            switch (option) {
                case "--rounds" -> rounds = parseLong(option, value(args, ++i, option));
                case "--threads" -> threads = Math.toIntExact(parseLong(option, value(args, ++i, option)));
                case "--seed" -> seed = parseLong(option, value(args, ++i, option));
                case "--config" -> config = Path.of(value(args, ++i, option));
                case "--report" -> reportDir = Path.of(value(args, ++i, option));
                case "--benchmark" -> benchmark = true;
                case "--help", "-h" -> help = true;
                default -> throw new IllegalArgumentException("Unknown option: " + option);
            }
        }
        if (rounds <= 0) {
            throw new IllegalArgumentException("--rounds must be positive");
        }
        if (threads <= 0) {
            throw new IllegalArgumentException("--threads must be positive");
        }
        return new CommandLineOptions(rounds, threads, seed, Optional.ofNullable(config),
                Optional.ofNullable(reportDir), benchmark, help);
    }

    private static String value(String[] args, int index, String option) {
        if (index >= args.length) {
            throw new IllegalArgumentException("Missing value for " + option);
        }
        return args[index];
    }

    private static long parseLong(String option, String value) {
        try {
            return Long.parseLong(value.replace("_", ""));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number for " + option + ": " + value, e);
        }
    }
}
