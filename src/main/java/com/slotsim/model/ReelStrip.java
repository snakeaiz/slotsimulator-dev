package com.slotsim.model;

import java.util.Arrays;

/**
 * Circular sequence of symbol ids. A reel stopped at index {@code N} shows
 * {@code N, N+1, ..., N+rows-1}, wrapping around the end of the strip.
 */
public final class ReelStrip {

    private final int[] symbolIds;

    public ReelStrip(int[] symbolIds) {
        if (symbolIds == null || symbolIds.length == 0) {
            throw new IllegalArgumentException("Reel strip must contain at least one symbol");
        }
        this.symbolIds = symbolIds.clone();
    }

    public int length() {
        return symbolIds.length;
    }

    /**
     * Symbol id at the given position, wrapping around the strip in both directions.
     */
    public int symbolAt(int position) {
        return symbolIds[Math.floorMod(position, symbolIds.length)];
    }

    /**
     * Number of occurrences of the given symbol on the strip.
     */
    public int count(int symbolId) {
        int count = 0;
        for (int id : symbolIds) {
            if (id == symbolId) {
                count++;
            }
        }
        return count;
    }

    int[] symbolIds() {
        return symbolIds.clone();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ReelStrip other && Arrays.equals(symbolIds, other.symbolIds);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(symbolIds);
    }

    @Override
    public String toString() {
        return "ReelStrip" + Arrays.toString(symbolIds);
    }
}
