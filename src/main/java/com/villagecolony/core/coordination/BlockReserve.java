package com.villagecolony.core.coordination;

import com.villagecolony.core.type.ResourceType;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A reserva de cada bloco nos baús — ADR-062, Regra 340. O bloco cai numa classe, e a classe diz quanto se guarda:
 * <ul>
 *   <li>{@link StockClass#RAW}: o que o aldeão recolhe do mundo, até {@value #RAW_CEILING};</li>
 *   <li>{@link StockClass#CRAFTED}: fabricado que não aparece nas vilas geradas, {@value #CRAFTED};</li>
 *   <li>{@link StockClass#VILLAGE}: fabricado com {@value #VILLAGE_MIN} ou mais blocos nas vilas geradas,
 *       {@value #VILLAGE};</li>
 *   <li>{@link StockClass#SPECIAL}: fabricado com menos de {@value #VILLAGE_MIN} nas vilas, {@value #SPECIAL};</li>
 *   <li>{@link StockClass#NEVER}: as bancadas do mod, fora da fabricação das profissões.</li>
 * </ul>
 * Os pedidos das profissões e da obra passam da reserva. A contagem das vilas e os naturais com receita vêm de
 * {@code data/villagecolony/stock/} ({@code scripts/village_block_counts.py}); lidos uma vez.
 */
public final class BlockReserve {

    /** A classe de estoque de um bloco. */
    public enum StockClass { RAW, CRAFTED, VILLAGE, SPECIAL, NEVER }

    public static final int RAW_CEILING = 256;

    public static final int CRAFTED = 4;

    public static final int VILLAGE = 12;

    public static final int SPECIAL = 1;

    /** A partir de quantos blocos nas vilas geradas o fabricado é da vila. */
    public static final int VILLAGE_MIN = 10;

    private static final String VILLAGE_BLOCKS = "/data/villagecolony/stock/village_blocks.json";

    private static final String NATURAL_BLOCKS = "/data/villagecolony/stock/natural_blocks.json";

    private static final Pattern ENTRY = Pattern.compile("\"([a-z0-9_:./-]+)\"\\s*:\\s*(\\d+)");

    private static final Pattern NAME = Pattern.compile("\"([a-z0-9_:./-]+)\"");

    /** Os dois arquivos, lidos na primeira consulta (a classe só carrega quando usada). */
    private static final class Data {

        static final Map<String, Integer> VILLAGE_COUNTS = counts();

        static final Set<String> NATURALS = names();
    }

    private BlockReserve() {
    }

    /** A classe do bloco; {@code crafted} diz se ele sai de receita (bancada, fornalha) e não do mundo. */
    public static StockClass classOf(String id, boolean crafted) {
        if (id.startsWith("villagecolony:") && id.endsWith("_bench")) {
            return StockClass.NEVER;
        }

        if (!crafted || Data.NATURALS.contains(id)) {
            return StockClass.RAW;
        }

        int inVillages = Data.VILLAGE_COUNTS.getOrDefault(id, 0);

        if (inVillages == 0) {
            return StockClass.CRAFTED;
        }

        return inVillages >= VILLAGE_MIN ? StockClass.VILLAGE : StockClass.SPECIAL;
    }

    /** Quanto a classe guarda. */
    public static int reserveOf(StockClass stockClass) {
        return switch (stockClass) {
            case RAW -> RAW_CEILING;
            case CRAFTED -> CRAFTED;
            case VILLAGE -> VILLAGE;
            case SPECIAL -> SPECIAL;
            case NEVER -> 0;
        };
    }

    /** Quanto deste bloco se guarda. */
    public static int reserveOf(String id, boolean crafted) {
        return reserveOf(classOf(id, crafted));
    }

    /** O mesmo para um recurso do mod: fabricado é o que sai da bancada ou da fornalha. */
    public static int reserveOf(ResourceType type) {
        boolean crafted = switch (type.production()) {
            case CRAFTED_WOOD, CRAFTED_STONE, SMELTED -> true;
            case HARVESTED, FARMED, MINED, SURFACE_GATHERED, SOIL_GATHERED, SHEARED -> false;
        };

        return reserveOf("minecraft:" + type.name().toLowerCase(Locale.ROOT), crafted);
    }

    private static Map<String, Integer> counts() {
        Map<String, Integer> counts = new HashMap<>();
        Matcher entries = ENTRY.matcher(read(VILLAGE_BLOCKS));

        while (entries.find()) {
            counts.put(entries.group(1), Integer.parseInt(entries.group(2)));
        }

        return Map.copyOf(counts);
    }

    private static Set<String> names() {
        Set<String> names = new HashSet<>();
        Matcher found = NAME.matcher(read(NATURAL_BLOCKS));

        while (found.find()) {
            names.add(found.group(1));
        }

        return Set.copyOf(names);
    }

    private static String read(String path) {
        try (InputStream in = BlockReserve.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("falta o arquivo de reserva " + path);
            }

            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
