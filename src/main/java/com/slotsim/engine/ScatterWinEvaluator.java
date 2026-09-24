package com.slotsim.engine;

import com.slotsim.model.MathModel;
import com.slotsim.model.Paytable;
import com.slotsim.model.Symbol;

/**
 * Pays scatter symbols anywhere in the window, independently of paylines.
 * <p>
 * The scatter pay is a flat amount for the round and is not multiplied by the number of lines.
 * Counts above the highest listed count pay as the highest listed count ("3 or more").
 */
public final class ScatterWinEvaluator implements WinEvaluator {

    private final int scatterId;
    private final Paytable paytable;
    private final int maxCount;

    public ScatterWinEvaluator(MathModel model) {
        this.scatterId = model.symbols().stream()
                .filter(Symbol::isScatter)
                .mapToInt(Symbol::id)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Model " + model.name() + " has no scatter symbol"));
        this.paytable = model.paytable();
        this.maxCount = paytable.maxScatterCount();
    }

    @Override
    public long evaluate(ReelWindow window, WinListener listener) {
        int count = window.count(scatterId);
        long pay = paytable.scatterPay(Math.min(count, maxCount));
        if (pay > 0) {
            listener.onWin(WinType.SCATTER, WinListener.NO_PAYLINE, scatterId, count, pay);
        }
        return pay;
    }
}
