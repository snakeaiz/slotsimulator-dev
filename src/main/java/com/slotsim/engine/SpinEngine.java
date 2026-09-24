package com.slotsim.engine;

import com.slotsim.model.MathModel;
import com.slotsim.model.ReelSet;
import com.slotsim.model.ReelStrip;
import com.slotsim.rng.RandomNumberGenerator;

/**
 * Plays single rounds: draws a stop for each reel, fills the reel picture and evaluates it.
 * <p>
 * Every reel stop is drawn uniformly from {@code [0, reel length)}, as the PAR sheet specifies.
 * The engine reuses its stop and window buffers, so a round allocates nothing. It is <b>not</b>
 * thread-safe: create one engine per thread and share the immutable model and evaluator.
 */
public final class SpinEngine {

    private final ReelSet reelSet;
    private final WinEvaluator evaluator;
    private final RandomNumberGenerator rng;
    private final int[] reelLengths;
    private final int[] stops;
    private final ReelWindow window;

    public SpinEngine(MathModel model, WinEvaluator evaluator, RandomNumberGenerator rng) {
        this.reelSet = model.reelSet();
        this.evaluator = evaluator;
        this.rng = rng;
        this.reelLengths = reelSet.reels().stream().mapToInt(ReelStrip::length).toArray();
        this.stops = new int[reelLengths.length];
        this.window = new ReelWindow(model.reelCount(), model.rows());
    }

    /**
     * Plays one random round.
     *
     * @return total win in credits
     */
    public long spin() {
        return spin(WinListener.NONE);
    }

    public long spin(WinListener listener) {
        for (int reel = 0; reel < stops.length; reel++) {
            stops[reel] = rng.nextInt(reelLengths[reel]);
        }
        return evaluateCurrentStops(listener);
    }

    /**
     * Plays the round for the given stops, e.g. to replay a known outcome.
     */
    public long spinAt(int[] stops, WinListener listener) {
        if (stops.length != this.stops.length) {
            throw new IllegalArgumentException("Expected " + this.stops.length + " stops");
        }
        for (int reel = 0; reel < stops.length; reel++) {
            if (stops[reel] < 0 || stops[reel] >= reelLengths[reel]) {
                throw new IllegalArgumentException("Stop " + stops[reel] + " is outside reel " + reel);
            }
        }
        System.arraycopy(stops, 0, this.stops, 0, stops.length);
        return evaluateCurrentStops(listener);
    }

    /** Stops of the last round. */
    public int[] lastStops() {
        return stops.clone();
    }

    /** Reel picture of the last round; overwritten by the next round. */
    public ReelWindow window() {
        return window;
    }

    private long evaluateCurrentStops(WinListener listener) {
        window.fill(reelSet, stops);
        return evaluator.evaluate(window, listener);
    }
}
