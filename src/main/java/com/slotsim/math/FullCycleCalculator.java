package com.slotsim.math;

import com.slotsim.engine.ReelWindow;
import com.slotsim.engine.WinEvaluator;
import com.slotsim.engine.WinListener;
import com.slotsim.model.MathModel;
import com.slotsim.model.ReelSet;
import com.slotsim.stats.RoundStatistics;

/**
 * Computes the exact statistics of a game by evaluating every stop combination once.
 * <p>
 * Stops are drawn uniformly and independently per reel, so each of the
 * {@code len(reel 0) * len(reel 1) * ...} combinations (the "full cycle") is equally likely and the
 * statistics over the full cycle are the theoretical values of the math model, not an estimate.
 * The same evaluator as the simulation is used, which cross-checks the engine against the PAR sheet.
 */
public final class FullCycleCalculator {

    /** Default guard against enumerating cycles that would take too long. */
    public static final long DEFAULT_MAX_CYCLE_SIZE = 500_000_000L;

    private final MathModel model;
    private final WinEvaluator evaluator;
    private final long maxCycleSize;

    public FullCycleCalculator(MathModel model, WinEvaluator evaluator) {
        this(model, evaluator, DEFAULT_MAX_CYCLE_SIZE);
    }

    public FullCycleCalculator(MathModel model, WinEvaluator evaluator, long maxCycleSize) {
        this.model = model;
        this.evaluator = evaluator;
        this.maxCycleSize = maxCycleSize;
    }

    public RoundStatistics calculate() {
        return calculate(WinListener.NONE);
    }

    /**
     * @param listener receives every win of every combination, e.g. for a per-symbol breakdown
     */
    public RoundStatistics calculate(WinListener listener) {
        ReelSet reelSet = model.reelSet();
        long cycleSize = reelSet.cycleSize();
        if (cycleSize > maxCycleSize) {
            throw new IllegalStateException("Full cycle of " + cycleSize + " combinations exceeds the limit of "
                    + maxCycleSize + "; use a simulation instead");
        }

        int reels = reelSet.size();
        int[] stops = new int[reels];
        ReelWindow window = new ReelWindow(reels, model.rows());
        RoundStatistics statistics = new RoundStatistics();

        // Odometer over all stop combinations: the last reel turns fastest.
        while (true) {
            window.fill(reelSet, stops);
            statistics.record(evaluator.evaluate(window, listener));

            int reel = reels - 1;
            while (reel >= 0 && ++stops[reel] == reelSet.reel(reel).length()) {
                stops[reel] = 0;
                reel--;
            }
            if (reel < 0) {
                return statistics;
            }
        }
    }
}
