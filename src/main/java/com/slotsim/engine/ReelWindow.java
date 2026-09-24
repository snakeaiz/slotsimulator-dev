package com.slotsim.engine;

import com.slotsim.model.ReelSet;
import com.slotsim.model.ReelStrip;

import java.util.Arrays;

/**
 * The visible part of the reels ("reel picture"): {@code rows} symbol ids per reel.
 * <p>
 * The window is mutable and meant to be reused across spins by a single thread, so the
 * simulation loop does not allocate per round. It is <b>not</b> thread-safe.
 */
public final class ReelWindow {

    private final int[][] cells;

    public ReelWindow(int reels, int rows) {
        if (reels <= 0 || rows <= 0) {
            throw new IllegalArgumentException("Window must have at least one reel and one row");
        }
        this.cells = new int[reels][rows];
    }

    /**
     * Builds a window from explicit columns, one array of symbol ids per reel (top row first).
     * Useful for evaluating a known reel picture without going through reel strips.
     */
    public static ReelWindow ofColumns(int[]... columns) {
        ReelWindow window = new ReelWindow(columns.length, columns[0].length);
        for (int reel = 0; reel < columns.length; reel++) {
            if (columns[reel].length != window.rows()) {
                throw new IllegalArgumentException("All columns must have the same number of rows");
            }
            System.arraycopy(columns[reel], 0, window.cells[reel], 0, window.rows());
        }
        return window;
    }

    /**
     * Fills the window with the reel picture for the given stop positions: reel {@code r}
     * stopped at {@code stops[r]} shows strip positions {@code stops[r] .. stops[r] + rows - 1},
     * wrapping around the end of the strip.
     */
    public void fill(ReelSet reelSet, int[] stops) {
        if (stops.length != cells.length || reelSet.size() != cells.length) {
            throw new IllegalArgumentException("Expected " + cells.length + " stops and reels");
        }
        for (int reel = 0; reel < cells.length; reel++) {
            ReelStrip strip = reelSet.reel(reel);
            int[] column = cells[reel];
            for (int row = 0; row < column.length; row++) {
                column[row] = strip.symbolAt(stops[reel] + row);
            }
        }
    }

    public int symbolAt(int reel, int row) {
        return cells[reel][row];
    }

    public int reels() {
        return cells.length;
    }

    public int rows() {
        return cells[0].length;
    }

    /**
     * Number of cells showing the given symbol anywhere in the window.
     */
    public int count(int symbolId) {
        int count = 0;
        for (int[] column : cells) {
            for (int id : column) {
                if (id == symbolId) {
                    count++;
                }
            }
        }
        return count;
    }

    @Override
    public String toString() {
        return "ReelWindow" + Arrays.deepToString(cells);
    }
}
