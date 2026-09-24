package com.slotsim.stats;

import java.math.BigInteger;

/**
 * RTP, volatility and hit frequency derived from {@link RoundStatistics}.
 *
 * @param rounds             number of rounds played
 * @param bet                bet per round in credits
 * @param totalBet           {@code rounds * bet}
 * @param totalWin           sum of all round wins
 * @param rtp                return to player, {@code totalWin / totalBet}
 * @param standardDeviation  standard deviation of the round win in credits
 * @param volatilityIndex    standard deviation of the round win in units of the bet
 *                           ({@code standardDeviation / bet}), the usual way slot volatility is quoted
 * @param hitFrequency       share of rounds with a win
 * @param maxWin             highest single round win
 */
public record StatisticsSummary(
        long rounds,
        long bet,
        long totalBet,
        long totalWin,
        double rtp,
        double standardDeviation,
        double volatilityIndex,
        double hitFrequency,
        long maxWin) {

    /** Two-sided 95% quantile of the standard normal distribution. */
    public static final double Z_95 = 1.959963984540054;

    static StatisticsSummary of(RoundStatistics statistics, long bet) {
        long rounds = statistics.rounds();
        if (rounds == 0) {
            throw new IllegalStateException("No rounds recorded");
        }
        long totalBet = Math.multiplyExact(rounds, bet);
        double standardDeviation = Math.sqrt(variance(statistics));
        return new StatisticsSummary(
                rounds,
                bet,
                totalBet,
                statistics.totalWin(),
                (double) statistics.totalWin() / totalBet,
                standardDeviation,
                standardDeviation / bet,
                (double) statistics.winningRounds() / rounds,
                statistics.maxWin());
    }

    /**
     * Population variance of the round win, {@code (n * Σw² - (Σw)²) / n²}.
     * <p>
     * The numerator is computed exactly with {@link BigInteger}, so there is no cancellation error.
     * For a full-cycle enumeration the population variance is the exact variance of the game;
     * for a simulation of millions of rounds it differs from the sample variance by a factor of
     * {@code n / (n - 1)}, which is negligible.
     */
    private static double variance(RoundStatistics statistics) {
        BigInteger n = BigInteger.valueOf(statistics.rounds());
        BigInteger sum = BigInteger.valueOf(statistics.totalWin());
        BigInteger numerator = n.multiply(BigInteger.valueOf(statistics.sumOfSquares())).subtract(sum.multiply(sum));
        return numerator.doubleValue() / n.multiply(n).doubleValue();
    }

    /**
     * Confidence interval of the RTP estimate by the central limit theorem:
     * {@code RTP ± z * σ / (bet * √n)}, where σ is the standard deviation of the round win.
     *
     * @param z standard normal quantile, e.g. {@link #Z_95}
     * @see <a href="https://en.wikipedia.org/wiki/Confidence_interval#Example">Confidence interval</a>
     */
    public ConfidenceInterval rtpConfidenceInterval(double z) {
        double halfWidth = z * volatilityIndex / Math.sqrt(rounds);
        return new ConfidenceInterval(rtp - halfWidth, rtp + halfWidth);
    }

    /**
     * Closed interval {@code [lower, upper]}.
     */
    public record ConfidenceInterval(double lower, double upper) {

        public boolean contains(double value) {
            return value >= lower && value <= upper;
        }

        public double halfWidth() {
            return (upper - lower) / 2;
        }
    }
}
