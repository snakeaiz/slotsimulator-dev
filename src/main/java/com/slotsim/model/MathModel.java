package com.slotsim.model;

import java.util.List;
import java.util.Objects;

/**
 * Complete, validated game configuration (the PAR sheet in code form).
 * Instances are immutable and therefore safe to share between simulation threads.
 *
 * @param name     game name
 * @param bet      cost of one round in credits
 * @param rows     number of visible rows per reel
 * @param symbols  all symbols, where {@code symbols.get(i).id() == i}
 * @param reelSet  reel strips, one per column
 * @param paylines paylines evaluated each round
 * @param paytable pays in credits for the whole {@code bet}
 */
public record MathModel(
        String name,
        long bet,
        int rows,
        List<Symbol> symbols,
        ReelSet reelSet,
        List<Payline> paylines,
        Paytable paytable) {

    public MathModel {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(reelSet, "reelSet");
        Objects.requireNonNull(paytable, "paytable");
        symbols = List.copyOf(symbols);
        paylines = List.copyOf(paylines);
        validate(bet, rows, symbols, reelSet, paylines, paytable);
    }

    public int reelCount() {
        return reelSet.size();
    }

    public Symbol symbol(int id) {
        return symbols.get(id);
    }

    public Symbol symbol(String code) {
        return symbols.stream()
                .filter(s -> s.code().equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown symbol: " + code));
    }

    private static void validate(long bet, int rows, List<Symbol> symbols, ReelSet reelSet,
                                 List<Payline> paylines, Paytable paytable) {
        require(bet > 0, "Bet must be positive: " + bet);
        require(rows > 0, "Rows must be positive: " + rows);
        require(!symbols.isEmpty(), "At least one symbol is required");
        require(!paylines.isEmpty() || paytable.hasScatterPays(), "Game has neither paylines nor scatter pays");

        for (int i = 0; i < symbols.size(); i++) {
            require(symbols.get(i).id() == i, "Symbol ids must be dense and ordered, got "
                    + symbols.get(i) + " at index " + i);
        }
        require(symbols.stream().filter(Symbol::isWild).count() <= 1, "At most one wild symbol is supported");
        require(symbols.stream().filter(Symbol::isScatter).count() <= 1, "At most one scatter symbol is supported");

        for (int reel = 0; reel < reelSet.size(); reel++) {
            ReelStrip strip = reelSet.reel(reel);
            require(strip.length() >= rows, "Reel " + reel + " is shorter than the window (" + rows + " rows)");
            for (int id : strip.symbolIds()) {
                require(id >= 0 && id < symbols.size(), "Reel " + reel + " references unknown symbol id " + id);
            }
        }

        for (Payline line : paylines) {
            require(line.length() == reelSet.size(), line + " must cross all " + reelSet.size() + " reels");
            for (int reel = 0; reel < line.length(); reel++) {
                int row = line.rowOnReel(reel);
                require(row >= 0 && row < rows, line + " is outside the " + rows + "-row window");
            }
        }
        require(paylines.stream().map(Payline::id).distinct().count() == paylines.size(), "Payline ids must be unique");

        require(paytable.symbolCapacity() <= symbols.size(), "Paytable references unknown symbols");
        for (Symbol symbol : symbols) {
            int maxCount = paytable.maxLineCount(symbol.id());
            require(!symbol.isScatter() || maxCount == 0, "Scatter " + symbol.code() + " cannot have line pays");
            require(maxCount <= reelSet.size(), symbol.code() + " pays for more symbols than there are reels");
        }
        require(paytable.maxScatterCount() <= reelSet.size() * rows, "Scatter pays exceed window capacity");
        require(!paytable.hasScatterPays() || symbols.stream().anyMatch(Symbol::isScatter),
                "Scatter pays defined but no scatter symbol exists");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
