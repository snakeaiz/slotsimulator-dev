package com.slotsim.engine;

/**
 * A single win, as reported to a {@link WinListener}.
 */
public record Win(WinType type, int paylineId, int symbolId, int count, long amount) {
}
