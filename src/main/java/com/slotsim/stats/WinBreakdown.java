package com.slotsim.stats;

import com.slotsim.engine.WinListener;
import com.slotsim.engine.WinType;

/**
 * Win totals per paying symbol, to split the RTP into per-symbol contributions
 * (the "Return" column of a PAR sheet).
 * <p>
 * A symbol pays either on lines or as a scatter (the model forbids line pays for scatters), so
 * keying by symbol is unambiguous. Not thread-safe: accumulate per thread and {@link #merge merge}.
 */
public final class WinBreakdown implements WinListener {

    private final long[] winsBySymbol;
    private final long[] hitsBySymbol;

    public WinBreakdown(int symbolCount) {
        this.winsBySymbol = new long[symbolCount];
        this.hitsBySymbol = new long[symbolCount];
    }

    @Override
    public void onWin(WinType type, int paylineId, int symbolId, int count, long amount) {
        winsBySymbol[symbolId] += amount;
        hitsBySymbol[symbolId]++;
    }

    public WinBreakdown merge(WinBreakdown other) {
        if (other.winsBySymbol.length != winsBySymbol.length) {
            throw new IllegalArgumentException("Breakdowns cover different symbol sets");
        }
        for (int i = 0; i < winsBySymbol.length; i++) {
            winsBySymbol[i] += other.winsBySymbol[i];
            hitsBySymbol[i] += other.hitsBySymbol[i];
        }
        return this;
    }

    /** Total credits won by the symbol. */
    public long wins(int symbolId) {
        return winsBySymbol[symbolId];
    }

    /** Number of individual wins (line wins or scatter wins) formed by the symbol. */
    public long hits(int symbolId) {
        return hitsBySymbol[symbolId];
    }

    public int symbolCount() {
        return winsBySymbol.length;
    }

    /** Share of the total bet returned by the symbol. */
    public double rtpContribution(int symbolId, long totalBet) {
        return (double) winsBySymbol[symbolId] / totalBet;
    }
}
