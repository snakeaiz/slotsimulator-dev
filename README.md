# Slot Simulator

A command-line simulator for a 3x3, 5-payline fruit slot. It loads the game's math model from
JSON, plays 10 million rounds in parallel, and reports the RTP and standard deviation. The
simulated figures are checked against the exact theoretical values, which the same engine
computes by enumerating every reel stop combination.

## Requirements

- JDK 21. The Gradle wrapper finds or downloads it through a toolchain.
- No other installation. Use `./gradlew`, or `gradlew.bat` on Windows.

## Build, test, run

```bash
./gradlew build                     # compile and run all unit tests
./gradlew test                      # tests only
./gradlew run                       # 10M rounds on all cores, report to the console
./gradlew run --args="--rounds 10_000_000 --threads 8 --seed 2026 --report reports"
./gradlew run --args="--benchmark --seed 2026"   # 1 thread vs all threads
./gradlew run --args="--play"                    # play rounds in the console
./gradlew run --args="--help"
```

| Option | Default | Meaning |
|---|---|---|
| `--rounds <n>` | `10000000` | Rounds to simulate (underscores allowed) |
| `--threads <n>` | available processors | Worker threads |
| `--seed <n>` | random, printed in the report | Master seed. The same seed gives the same result for any thread count |
| `--config <file>` | bundled `games/fruit-slot.json` | Math model to simulate |
| `--report <dir>` | none | Also write the report as Markdown into this directory |
| `--benchmark` | off | Time a single-threaded run against a parallel run |
| `--play` | off | Play rounds interactively: Enter = spin, `q` = quit |
| `--balance <n>` | `1000` | Starting balance for `--play` |

### Play mode

`--play` runs the same engine one round at a time, so you can watch it play real rounds.
Winning cells are shown in brackets:

```
> Round 18, stops [15, 10, 15]

   L2    H3    L2
  [W1]  [H1]  [W1]
   L3    H2    L3

  Line 2: 3x H1 = 800
  Win: 800   Balance: 1,635
```

## Results

10,000,000 rounds, seed 2026, 10 threads. Full report:
[reports/simulation-10M-seed-2026.md](reports/simulation-10M-seed-2026.md).

| Metric | Simulated (10M rounds) | Theoretical (full cycle) |
|---|---:|---:|
| Total bet | 100,000,000 | 1,910,520 |
| Total win | 93,279,725 | 1,783,625 |
| **RTP** | **93.2797%** | **93.3581%** |
| **Standard deviation** (in bets) | **4.8623** | **4.8676** |
| Standard deviation (credits) | 48.6229 | 48.6763 |
| Hit frequency | 13.3118% | 13.3205% |
| Max win | 2,500 | 2,500 |

- 95% confidence interval of the simulated RTP: **[92.978%, 93.581%]**, which is ±0.30%.
- The theoretical RTP of 93.3581% lies inside this interval. It matches the PAR sheet
  (cell `X23`) to 10 decimal places.
- The simulated RTP of each symbol matches the PAR sheet's Return column.

### Performance

`--benchmark --rounds 100_000_000 --seed 2026` on an Apple M1 Pro (8 performance and 2 efficiency
cores), after a JIT warm-up:

| Threads | Time | Rounds/s |
|---:|---:|---:|
| 1 | 8.42 s | 11.9 M |
| 10 | 1.27 s | 78.5 M |

- **Speed-up and variance.** The parallel run is 6.6x faster. Single-threaded throughput was
  stable at 11–13 M rounds/s. On a laptop with an IDE running, the parallel speed-up varied from
  3.7x to 7.6x between runs. Both runs always produce identical statistics.
- **Cold runs.** The execution time in a normal report, such as 0.67 s for 10M rounds, includes
  JIT warm-up. Warm, 10M rounds take about 0.1–0.2 s on 10 threads.

## Game rules

These rules come from the PAR sheet ("Task math model" tab). Because the full-cycle RTP
reproduces the sheet's figures exactly, each rule below is confirmed and not just assumed.

- **Reels.** There are 3 reels with 61, 58 and 54 symbols. Each reel stop is drawn uniformly from
  `[0, reel length)`. A reel stopped at `N` shows positions `N, N+1, N+2`.
- **Wrap-around.** Reels are circular, so positions wrap past the end of the strip. The sheet's
  scatter formula, `27 × hits / total`, is only correct for circular reels.
- **Paylines.** The sheet draws the five paylines as cell fills, not values:
  `[0,0,0]` top, `[1,1,1]` middle, `[2,2,2]` bottom, `[0,1,2]` diagonal down, `[2,1,0]` diagonal up.
  Each line pays 3 of a kind.
- **Pays.** Pays are credits for the whole bet of 10, not per-line multipliers. For example,
  3x L3 pays 15.
- **Wild W1.** It substitutes for every symbol except the scatter. A line of three wilds pays the
  wild pay of 2000, not 3x H1. Each line awards only its highest win. This matches the sheet's
  hit count `(W+X)^3 − W^3`.
- **Scatter SCA.** Three scatters anywhere pay a flat 200 per round, not multiplied by lines.
  Scatters never pay on a line.

### Inconsistencies found in the PAR sheet

- **Example stops.** Of the "Examples of Game play", only the non-winning example's stops produce
  the drawn picture.
  - The 3-lines example states stops 39/25/20. Reel 0 shows the drawn column at stop 43.
  - The bonus example states stops 34/39/44. The drawn picture is at 38/43/48.
  - The one-line example draws the column L3, L2, L3 for reel 0. That column does not exist on
    reel 0.

  The drawn pictures themselves pay exactly as described: 0, 15, 240 and 220. The tests evaluate
  the drawn pictures, and use the corrected stops wherever they exist. Neither issue affects the
  RTP.
- **Other artefacts.** The symbol list contains a stray symbol `ž` in cell `A27`. The reel 0
  `COUNTIF` range stops at row 89 while the others stop at 92. Neither has any effect.

## Design

```
config      JSON math model -> validated domain model
model       Symbol, ReelStrip, ReelSet, Payline, Paytable, MathModel (immutable)
engine      ReelWindow, WinEvaluator {Line, Scatter, Composite}, SpinEngine, WinListener
rng         RandomNumberGenerator (pluggable), SplittableRandomGenerator
stats       RoundStatistics (exact sums), StatisticsSummary (RTP, SD, CI), WinBreakdown
math        FullCycleCalculator (exact theoretical figures)
simulation  SimulationRunner (parallel, deterministic), SimulationConfig, SimulationResult
report      Markdown report
play        Interactive console game on the same engine
cli         Command-line options
```

### Configurable and extensible

- **Math model in JSON.** Symbols, reels, window height, paylines, the paytable and the bet all
  live in [`games/fruit-slot.json`](src/main/resources/games/fruit-slot.json). None of them is in
  code. Symbols are written by code (`"H1"`), so the file can be compared with the PAR sheet by
  eye.
- **Validation on load.** `MathModel` validates the model once, when it is loaded. It checks that
  paylines stay inside the window, that reels reference only known symbols, and that the scatter
  has no line pays. The hot path can then skip these checks.
- **Grid size and line shapes.** Any number of reels, rows and payline shapes works with no code
  change.
- **Count of a kind.** Line pays are `count`-of-a-kind counted from the leftmost reel, so a 5x3
  game paying 3, 4 and 5 of a kind needs only a new JSON file. A test runs a 4x1 game to show
  this.
- **New win types.** Each win type is a `WinEvaluator`. `CompositeWinEvaluator` sums them. The
  simulation loop depends only on the interface.

### Performance and memory

- **No allocation per round.** Each thread's `SpinEngine` reuses its stop array and its
  `ReelWindow`. The evaluators work on `int` symbol ids and precomputed arrays.
- **Detail on demand.** `WinListener` reports each individual win with primitive arguments. The
  simulation passes a per-thread `WinBreakdown` for the per-symbol RTP. Callers that need no
  detail pass `WinListener.NONE`.
- **Memory per chunk, not per round.** Nothing is stored per round. Memory grows only with the
  number of chunks: one small task and result per 100,000 rounds, so 100 chunks for 10M rounds.

### Thread safety and determinism

- **No shared mutable state.** The model and the evaluators are immutable and shared by all
  threads. Engines, RNGs and accumulators are confined to their chunk, so the hot loop has no
  locks, no `volatile` fields and no atomics.
- **Reproducible chunks.** Rounds are split into chunks of 100,000. Chunk seeds are drawn in
  order from the master seed, so chunk `i` plays the same rounds whichever thread runs it.
- **Order-independent merge.** Totals are whole-credit `long` sums, so merging chunks is exact
  and order independent. A given seed therefore produces bit-identical results on 1 or N
  threads, and a test asserts this.

### Statistics

With `n` rounds, `w` the win of a round and `b` the bet:

- **RTP** = `Σw / (n·b)`
- **Standard deviation.**
  - `σ = sqrt((n·Σw² − (Σw)²) / n²)`, the population standard deviation of the round win in
    credits. The numerator is computed exactly with `BigInteger`.
  - The headline figure is `σ / b`, the standard deviation in units of the bet, which is how slot
    volatility is usually quoted.
- **95% confidence interval of the RTP**, by the central limit theorem:
  `RTP ± 1.96 · (σ/b) / √n`
  ([reference](https://en.wikipedia.org/wiki/Confidence_interval#Example)).
  For 10M rounds the interval is ±0.30%.
- **Theoretical values.** `FullCycleCalculator` evaluates all 61 × 58 × 54 = 191,052 equally
  likely stop combinations with the same evaluator as the simulation. The result is the exact RTP
  and variance of the model.

### Random numbers

- **Pluggable generator.** `RandomNumberGenerator` is an interface. A production game server
  would plug in its certified RNG.
- **Simulation generator.** The simulation uses `SplittableRandom`, which is fast and seedable.
  Its bounded `nextInt` is unbiased, which matters for reel stops.

## Tests

`./gradlew test` runs 123 tests. They focus on the parts where mistakes hide:

- **Symbol positioning.** Reel pictures for known stops, wrap-around at the end of every reel,
  and reuse of the window.
- **Line evaluation.** Every PAR sheet example picture, and wild substitution edge cases:
  `WWW`, `WWX`, `XWX`, a wild next to a scatter, and scatters on a line. Count of a kind on a
  wider grid is also covered.
- **Scatter evaluation.** Scatters anywhere, two versus three, and a wild that is not a scatter.
- **Configuration.** Reel lengths, per-reel symbol counts and pays are cross-checked against the
  PAR sheet. Validation errors are also tested.
- **Theory.** The full cycle reproduces the sheet's RTP and every symbol's Return, and matches
  exact totals computed independently.
- **Statistics.** Merging accumulators, variance, and the confidence interval.
- **Simulation.** Identical results for 1 and 4 threads, exact round counts including a partial
  chunk, and convergence of the simulated RTP to the theoretical value.

## Possible next steps

- Use JMH for rigorous benchmarks instead of the built-in timing.
- Support several reel sets with weights, and features such as free spins, as new `WinEvaluator`s
  and feature states.
- Report percentiles of the win distribution and the probability of a win above N× the bet.
