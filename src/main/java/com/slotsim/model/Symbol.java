package com.slotsim.model;

import java.util.Objects;

/**
 * A reel symbol. The {@code id} is a dense index (0..n-1) used by the hot evaluation path
 * instead of object references.
 *
 * @param id   dense numeric id, equal to the symbol's position in {@link MathModel#symbols()}
 * @param code short code used in the PAR sheet and config, e.g. {@code H1}
 * @param name human readable name, e.g. {@code Seven}
 * @param type evaluation role
 */
public record Symbol(int id, String code, String name, SymbolType type) {

    public Symbol {
        if (id < 0) {
            throw new IllegalArgumentException("Symbol id must be non-negative: " + id);
        }
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
    }

    public boolean isWild() {
        return type == SymbolType.WILD;
    }

    public boolean isScatter() {
        return type == SymbolType.SCATTER;
    }
}
