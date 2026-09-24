package com.slotsim.model;

/**
 * Role a symbol plays during win evaluation.
 */
public enum SymbolType {
    /** Pays when enough identical symbols land on a payline. */
    REGULAR,
    /** Substitutes for any {@link #REGULAR} symbol on a payline; pays on its own when the line is all wilds. */
    WILD,
    /** Pays by count anywhere in the window, independent of paylines; never substituted by a wild. */
    SCATTER
}
