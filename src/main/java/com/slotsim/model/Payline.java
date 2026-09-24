package com.slotsim.model;

import java.util.Arrays;

/**
 * A path across the grid: for each reel, the row the line passes through.
 * For example the top row of a 3x3 grid is {@code [0, 0, 0]} and the
 * top-left to bottom-right diagonal is {@code [0, 1, 2]}.
 */
public final class Payline {

    private final int id;
    private final int[] rows;

    public Payline(int id, int[] rows) {
        if (rows == null || rows.length == 0) {
            throw new IllegalArgumentException("Payline " + id + " must cross at least one reel");
        }
        this.id = id;
        this.rows = rows.clone();
    }

    public int id() {
        return id;
    }

    public int length() {
        return rows.length;
    }

    public int rowOnReel(int reel) {
        return rows[reel];
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Payline other && id == other.id && Arrays.equals(rows, other.rows);
    }

    @Override
    public int hashCode() {
        return 31 * id + Arrays.hashCode(rows);
    }

    @Override
    public String toString() {
        return "Payline" + id + Arrays.toString(rows);
    }
}
