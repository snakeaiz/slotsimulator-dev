package com.slotsim.stats;

/**
 * Running totals over played rounds, from which RTP and standard deviation are derived.
 * <p>
 * Wins are whole credits, so the sums are kept as {@code long} and are exact: there is no
 * floating-point drift, and merging the totals of several threads gives the same result
 * in any order. With a maximum round win of {@code W} credits, {@code sumOfSquares} stays within
 * {@code Long.MAX_VALUE} for about {@code 9.2e18 / W^2} rounds (1.4e12 rounds for Fruit Slot).
 * <p>
 * Not thread-safe: each thread accumulates its own instance and the results are {@link #merge merged}.
 */
public final class RoundStatistics {

    private long rounds;
    private long totalWin;
    private long sumOfSquares;
    private long winningRounds;
    private long maxWin;

    public void record(long win) {
        rounds++;
        totalWin += win;
        sumOfSquares += win * win;
        if (win > 0) {
            winningRounds++;
            if (win > maxWin) {
                maxWin = win;
            }
        }
    }

    public RoundStatistics merge(RoundStatistics other) {
        rounds += other.rounds;
        totalWin += other.totalWin;
        sumOfSquares += other.sumOfSquares;
        winningRounds += other.winningRounds;
        maxWin = Math.max(maxWin, other.maxWin);
        return this;
    }

    public long rounds() {
        return rounds;
    }

    public long totalWin() {
        return totalWin;
    }

    public long sumOfSquares() {
        return sumOfSquares;
    }

    public long winningRounds() {
        return winningRounds;
    }

    public long maxWin() {
        return maxWin;
    }

    /**
     * Derives the summary figures for a game with a fixed bet per round.
     */
    public StatisticsSummary summarize(long bet) {
        return StatisticsSummary.of(this, bet);
    }
}
