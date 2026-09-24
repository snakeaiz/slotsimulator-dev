package com.slotsim.engine;

/**
 * Receives every individual win found during evaluation.
 * <p>
 * Arguments are primitives on purpose: the simulation passes {@link #NONE} and pays nothing for
 * detail it does not need, while tests, reports or per-symbol statistics can observe each win
 * without changing the evaluators.
 */
@FunctionalInterface
public interface WinListener {

    /** Payline id used for wins that are not tied to a payline. */
    int NO_PAYLINE = -1;

    /** Listener that ignores all wins. */
    WinListener NONE = (type, paylineId, symbolId, count, amount) -> {
    };

    /**
     * @param type      kind of win
     * @param paylineId id of the winning payline, or {@link #NO_PAYLINE}
     * @param symbolId  paying symbol
     * @param count     number of symbols that formed the win
     * @param amount    win in credits
     */
    void onWin(WinType type, int paylineId, int symbolId, int count, long amount);
}
