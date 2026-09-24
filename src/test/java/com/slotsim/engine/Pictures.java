package com.slotsim.engine;

import com.slotsim.model.MathModel;

/**
 * Builds reel pictures for tests from rows of symbol codes, top row first,
 * e.g. {@code picture(model, "H3 L2 L4", "H1 W1 L4", "H2 L3 L4")}.
 */
final class Pictures {

    private Pictures() {
    }

    static ReelWindow picture(MathModel model, String... rows) {
        String[][] cells = new String[rows.length][];
        for (int row = 0; row < rows.length; row++) {
            cells[row] = rows[row].trim().split("\\s+");
        }
        int[][] columns = new int[cells[0].length][rows.length];
        for (int reel = 0; reel < columns.length; reel++) {
            for (int row = 0; row < rows.length; row++) {
                columns[reel][row] = model.symbol(cells[row][reel]).id();
            }
        }
        return ReelWindow.ofColumns(columns);
    }
}
