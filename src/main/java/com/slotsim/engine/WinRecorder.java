package com.slotsim.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects wins into a list. Intended for inspecting single rounds, not for the simulation loop.
 */
public final class WinRecorder implements WinListener {

    private final List<Win> wins = new ArrayList<>();

    @Override
    public void onWin(WinType type, int paylineId, int symbolId, int count, long amount) {
        wins.add(new Win(type, paylineId, symbolId, count, amount));
    }

    public List<Win> wins() {
        return List.copyOf(wins);
    }

    public void clear() {
        wins.clear();
    }
}
