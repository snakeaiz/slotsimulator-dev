package com.slotsim.model;

import java.util.Arrays;

/**
 * Pay values in credits, indexed for constant-time lookup in the hot path.
 * <p>
 * Line pays are keyed by symbol and the number of matching symbols counted from the leftmost reel.
 * Scatter pays are keyed by the number of scatter symbols anywhere in the window. A missing entry
 * pays zero.
 */
public final class Paytable {

    private final long[][] linePays;
    private final long[] scatterPays;

    /**
     * @param linePays    {@code linePays[symbolId][count]} pay for {@code count} of a kind on a line
     * @param scatterPays {@code scatterPays[count]} pay for {@code count} scatters in the window
     */
    public Paytable(long[][] linePays, long[] scatterPays) {
        this.linePays = deepCopy(linePays);
        this.scatterPays = scatterPays.clone();
    }

    public long linePay(int symbolId, int count) {
        if (symbolId >= linePays.length) {
            return 0;
        }
        long[] pays = linePays[symbolId];
        return count < pays.length ? pays[count] : 0;
    }

    public long scatterPay(int count) {
        return count < scatterPays.length ? scatterPays[count] : 0;
    }

    /**
     * Highest count that has a line pay for the symbol, or 0 if the symbol never pays on a line.
     */
    int maxLineCount(int symbolId) {
        if (symbolId >= linePays.length) {
            return 0;
        }
        long[] pays = linePays[symbolId];
        for (int count = pays.length - 1; count > 0; count--) {
            if (pays[count] > 0) {
                return count;
            }
        }
        return 0;
    }

    boolean hasLinePays(int symbolId) {
        return maxLineCount(symbolId) > 0;
    }

    boolean hasScatterPays() {
        return Arrays.stream(scatterPays).anyMatch(pay -> pay > 0);
    }

    int maxScatterCount() {
        return scatterPays.length - 1;
    }

    int symbolCapacity() {
        return linePays.length;
    }

    private static long[][] deepCopy(long[][] source) {
        long[][] copy = new long[source.length][];
        for (int i = 0; i < source.length; i++) {
            copy[i] = source[i] == null ? new long[0] : source[i].clone();
        }
        return copy;
    }
}
