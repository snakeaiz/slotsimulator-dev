package com.slotsim.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slotsim.config.MathModelDefinition.LinePayDefinition;
import com.slotsim.config.MathModelDefinition.ScatterPayDefinition;
import com.slotsim.config.MathModelDefinition.SymbolDefinition;
import com.slotsim.model.MathModel;
import com.slotsim.model.Payline;
import com.slotsim.model.Paytable;
import com.slotsim.model.ReelSet;
import com.slotsim.model.ReelStrip;
import com.slotsim.model.Symbol;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Reads a {@link MathModel} from JSON, either from the classpath or from the file system.
 */
public final class MathModelLoader {

    /** Math model bundled with the application, used when no config is given. */
    public static final String DEFAULT_RESOURCE = "games/fruit-slot.json";

    private final ObjectMapper mapper = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);

    public MathModel loadDefault() {
        return loadResource(DEFAULT_RESOURCE);
    }

    public MathModel loadResource(String resource) {
        try (InputStream in = MathModelLoader.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new MathModelLoadException("Math model resource not found: " + resource, null);
            }
            return load(in, resource);
        } catch (IOException e) {
            throw new MathModelLoadException("Cannot read math model resource " + resource, e);
        }
    }

    public MathModel load(Path file) {
        try (InputStream in = Files.newInputStream(file)) {
            return load(in, file.toString());
        } catch (IOException e) {
            throw new MathModelLoadException("Cannot read math model file " + file, e);
        }
    }

    private MathModel load(InputStream in, String source) throws IOException {
        MathModelDefinition definition = mapper.readValue(in, MathModelDefinition.class);
        try {
            return toModel(definition);
        } catch (RuntimeException e) {
            throw new MathModelLoadException("Invalid math model " + source + ": " + e.getMessage(), e);
        }
    }

    static MathModel toModel(MathModelDefinition definition) {
        List<Symbol> symbols = new ArrayList<>();
        Map<String, Integer> idsByCode = new HashMap<>();
        for (SymbolDefinition symbol : required(definition.symbols(), "symbols")) {
            int id = symbols.size();
            if (idsByCode.putIfAbsent(symbol.code(), id) != null) {
                throw new IllegalArgumentException("Duplicate symbol code: " + symbol.code());
            }
            symbols.add(new Symbol(id, symbol.code(), symbol.name(), symbol.type()));
        }

        List<ReelStrip> reels = required(definition.reels(), "reels").stream()
                .map(codes -> new ReelStrip(codes.stream().mapToInt(code -> idOf(code, idsByCode)).toArray()))
                .toList();

        List<Payline> paylines = required(definition.paylines(), "paylines").stream()
                .map(line -> new Payline(line.id(), required(line.rows(), "payline rows")))
                .toList();

        return new MathModel(
                required(definition.name(), "name"),
                definition.bet(),
                definition.rows(),
                symbols,
                new ReelSet(reels),
                paylines,
                toPaytable(required(definition.paytable(), "paytable"), idsByCode, symbols.size()));
    }

    private static Paytable toPaytable(MathModelDefinition.PaytableDefinition definition,
                                       Map<String, Integer> idsByCode, int symbolCount) {
        List<LinePayDefinition> lines = Optional.ofNullable(definition.lines()).orElse(List.of());
        List<ScatterPayDefinition> scatter = Optional.ofNullable(definition.scatter()).orElse(List.of());

        long[][] linePays = new long[symbolCount][];
        for (LinePayDefinition pay : lines) {
            int id = idOf(pay.symbol(), idsByCode);
            linePays[id] = put(linePays[id], pay.count(), pay.pay(), pay.symbol());
        }

        long[] scatterPays = new long[0];
        for (ScatterPayDefinition pay : scatter) {
            scatterPays = put(scatterPays, pay.count(), pay.pay(), "scatter");
        }
        return new Paytable(linePays, scatterPays);
    }

    private static long[] put(long[] pays, int count, long pay, String owner) {
        if (count <= 0 || pay < 0) {
            throw new IllegalArgumentException("Invalid pay for " + owner + ": count=" + count + ", pay=" + pay);
        }
        long[] result = pays == null ? new long[count + 1] : pays;
        if (result.length <= count) {
            result = Arrays.copyOf(result, count + 1);
        }
        if (result[count] != 0) {
            throw new IllegalArgumentException("Duplicate pay for " + owner + " x" + count);
        }
        result[count] = pay;
        return result;
    }

    private static int idOf(String code, Map<String, Integer> idsByCode) {
        Integer id = idsByCode.get(code);
        if (id == null) {
            throw new IllegalArgumentException("Unknown symbol code: " + code);
        }
        return id;
    }

    private static <T> T required(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException("Missing field: " + field);
        }
        return value;
    }
}
