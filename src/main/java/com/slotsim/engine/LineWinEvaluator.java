package com.slotsim.engine;

import com.slotsim.model.MathModel;
import com.slotsim.model.Payline;
import com.slotsim.model.Paytable;
import com.slotsim.model.Symbol;
import com.slotsim.model.SymbolType;

/**
 * Pays matching symbols on each payline, counted from the leftmost reel.
 * <p>
 * Rules per payline:
 * <ul>
 *     <li>The line symbol is the first non-wild symbol; wilds substitute for it, scatters never
 *     take part in line wins.</li>
 *     <li>A run made of wilds only pays as the wild itself.</li>
 *     <li>If both the wild run and the substituted run pay, only the higher win is awarded
 *     (one win per line). For Fruit Slot this makes {@code W1 W1 W1} pay 2000, not 3x H1, which is
 *     how the PAR sheet counts hits ({@code (W+X)^3 - W^3}).</li>
 * </ul>
 */
public final class LineWinEvaluator implements WinEvaluator {

    private static final int NO_SYMBOL = -1;

    private final int[][] lineRows;
    private final int[] lineIds;
    private final boolean[] regular;
    private final int wildId;
    private final Paytable paytable;

    public LineWinEvaluator(MathModel model) {
        this.lineRows = new int[model.paylines().size()][model.reelCount()];
        this.lineIds = new int[lineRows.length];
        for (int i = 0; i < lineRows.length; i++) {
            Payline line = model.paylines().get(i);
            lineIds[i] = line.id();
            for (int reel = 0; reel < model.reelCount(); reel++) {
                lineRows[i][reel] = line.rowOnReel(reel);
            }
        }
        this.regular = new boolean[model.symbols().size()];
        int wild = NO_SYMBOL;
        for (Symbol symbol : model.symbols()) {
            regular[symbol.id()] = symbol.type() == SymbolType.REGULAR;
            if (symbol.isWild()) {
                wild = symbol.id();
            }
        }
        this.wildId = wild;
        this.paytable = model.paytable();
    }

    @Override
    public long evaluate(ReelWindow window, WinListener listener) {
        long total = 0;
        for (int line = 0; line < lineRows.length; line++) {
            total += evaluateLine(window, line, listener);
        }
        return total;
    }

    private long evaluateLine(ReelWindow window, int line, WinListener listener) {
        int[] rows = lineRows[line];
        int reels = rows.length;

        int wildRun = 0;
        while (wildRun < reels && window.symbolAt(wildRun, rows[wildRun]) == wildId) {
            wildRun++;
        }

        int lineSymbol = NO_SYMBOL;
        int symbolRun = 0;
        if (wildRun < reels) {
            int candidate = window.symbolAt(wildRun, rows[wildRun]);
            if (regular[candidate]) {
                lineSymbol = candidate;
                symbolRun = wildRun + 1;
                while (symbolRun < reels) {
                    int symbol = window.symbolAt(symbolRun, rows[symbolRun]);
                    if (symbol != lineSymbol && symbol != wildId) {
                        break;
                    }
                    symbolRun++;
                }
            }
        }

        long wildPay = wildId == NO_SYMBOL ? 0 : paytable.linePay(wildId, wildRun);
        long symbolPay = lineSymbol == NO_SYMBOL ? 0 : paytable.linePay(lineSymbol, symbolRun);

        if (wildPay == 0 && symbolPay == 0) {
            return 0;
        }
        if (wildPay >= symbolPay) {
            listener.onWin(WinType.LINE, lineIds[line], wildId, wildRun, wildPay);
            return wildPay;
        }
        listener.onWin(WinType.LINE, lineIds[line], lineSymbol, symbolRun, symbolPay);
        return symbolPay;
    }
}
