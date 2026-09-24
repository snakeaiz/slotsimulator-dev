package com.slotsim.config;

import com.slotsim.model.SymbolType;

import java.util.List;

/**
 * JSON shape of a math model file. Symbols are referenced by code so the file can be
 * compared with the PAR sheet by eye; {@link MathModelLoader} resolves codes to dense ids.
 */
record MathModelDefinition(
        String name,
        long bet,
        int rows,
        List<SymbolDefinition> symbols,
        List<List<String>> reels,
        List<PaylineDefinition> paylines,
        PaytableDefinition paytable) {

    record SymbolDefinition(String code, String name, SymbolType type) {
    }

    record PaylineDefinition(int id, int[] rows) {
    }

    record PaytableDefinition(List<LinePayDefinition> lines, List<ScatterPayDefinition> scatter) {
    }

    record LinePayDefinition(String symbol, int count, long pay) {
    }

    record ScatterPayDefinition(int count, long pay) {
    }
}
