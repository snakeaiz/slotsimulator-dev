package com.slotsim.engine;

import com.slotsim.model.MathModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Sums the wins of several evaluators, e.g. line wins and scatter wins.
 */
public final class CompositeWinEvaluator implements WinEvaluator {

    private final WinEvaluator[] evaluators;

    public CompositeWinEvaluator(List<? extends WinEvaluator> evaluators) {
        if (evaluators.isEmpty()) {
            throw new IllegalArgumentException("At least one evaluator is required");
        }
        this.evaluators = evaluators.toArray(WinEvaluator[]::new);
    }

    /**
     * Evaluator for everything the model can pay: paylines if it has any, scatters if it has a scatter pay.
     */
    public static CompositeWinEvaluator forModel(MathModel model) {
        List<WinEvaluator> evaluators = new ArrayList<>();
        if (!model.paylines().isEmpty()) {
            evaluators.add(new LineWinEvaluator(model));
        }
        if (model.paytable().maxScatterCount() > 0) {
            evaluators.add(new ScatterWinEvaluator(model));
        }
        return new CompositeWinEvaluator(evaluators);
    }

    @Override
    public long evaluate(ReelWindow window, WinListener listener) {
        long total = 0;
        for (WinEvaluator evaluator : evaluators) {
            total += evaluator.evaluate(window, listener);
        }
        return total;
    }
}
