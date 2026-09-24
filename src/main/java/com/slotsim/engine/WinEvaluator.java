package com.slotsim.engine;

/**
 * Evaluates one kind of win on a reel picture. Implementations must be stateless (or immutable)
 * so a single instance can be shared by all simulation threads.
 */
public interface WinEvaluator {

    /**
     * @param window   reel picture to evaluate
     * @param listener receives each individual win
     * @return total win in credits
     */
    long evaluate(ReelWindow window, WinListener listener);

    default long evaluate(ReelWindow window) {
        return evaluate(window, WinListener.NONE);
    }
}
