package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;

import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * O que está dentro do volume da casa — P1.6, 2026-09-19.
 *
 * <p><b>A pergunta que dois playtests seguidos abriram.</b> Com o índice
 * de ruas consertado (P1.5) a varredura voltou a existir — 39.163 colunas
 * numa sessão, 20.994 na seguinte — e mesmo assim a vila aprovou
 * <b>zero</b> lotes nas duas. A recusa dominante mudou de dono:
 *
 * <pre>
 * 09-19 10:36   275 de 392 (70%)   algo dentro do volume
 * 09-19 11:36   371 de 532 (70%)   algo dentro do volume
 * </pre>
 *
 * <p><b>E a Regra 22 não diz de que é feita.</b> É a mesma cegueira que o
 * {@link ProtectionSample} desfez para a Regra 3 em 09-18, e ali a amostra
 * decidiu o P1.3 <b>numa leitura</b>: 87% de areia e arenito apontaram o
 * conserto direto.
 *
 * <p><b>Duas causas dividem uma linha só, e elas pedem consertos
 * opostos.</b> O {@code BuildSiteScanner} recusa por {@code OCCUPIED} em
 * dois pontos bem diferentes:
 *
 * <pre>
 * COLONY_BUILT   isColonyBuilt — a própria colônia já construiu ali.
 *                A vila está cheia, e o conserto é procurar mais longe
 *                ou crescer a rua.
 * IN_THE_COLUMN  isClearAbove — tem coisa física na coluna. Se for
 *                cacto e arbusto morto, o conserto é limpar vegetação;
 *                se for arenito, é terreno e a régua está errada.
 * </pre>
 *
 * <p>Do lado de fora as duas são "algo está no caminho", e escolher o
 * conserto errado custa uma sessão inteira. Esta amostra as separa e, na
 * segunda, <b>nomeia o bloco</b>.
 *
 * <p><b>Uma amostra, não um log por bloco.</b> São centenas de recusas por
 * sessão, e uma linha por bloco afogaria o log e mudaria o que se está
 * medindo — a mesma escolha do {@code ProtectionSample}.
 */
public final class VolumeSample {

    /** Por que o volume foi recusado. */
    public enum Why {

        /** A própria colônia já construiu naquele chão. */
        COLONY_BUILT,

        /** Havia bloco na coluna, acima do chão. */
        IN_THE_COLUMN
    }

    /**
     * Quantos tipos distintos guardar antes de parar de aprender.
     *
     * <p>Mesmo teto e mesma razão do {@code ProtectionSample}: o que
     * interessa é a cauda pesada, os dois ou três tipos que respondem
     * pela maioria.
     */
    private static final int MAX_KINDS = 30;

    /**
     * Teto de posições distintas, para a amostra não crescer sem fim.
     *
     * <p>O mesmo do {@code ProtectionSample}, e pela mesma razão: o
     * servidor vive horas e a varredura repete as mesmas colunas.
     */
    private static final int MAX_POSITIONS = 20_000;

    /** Quantas vezes cada causa recusou. */
    private static final Map<Why, Integer> BY_CAUSE = new HashMap<>();

    /** Quantas POSIÇÕES distintas de cada tipo de bloco barraram a coluna. */
    private static final Map<String, Integer> SEEN = new HashMap<>();

    /**
     * As posições já contadas.
     *
     * <p><b>Conta bloco, e não visita</b> — a lição de 09-18, em que o
     * {@code ProtectionSample} relatou "1548 chest" numa vila de três
     * camas porque a varredura passa pela mesma coluna a cada ciclo. Sem
     * isto o número mede a frequência da varredura, não o terreno.
     */
    private static final Set<Long> COUNTED = new HashSet<>();

    private VolumeSample() {
    }

    /** A colônia já construiu neste chão. */
    public static void colonyBuilt() {
        BY_CAUSE.merge(Why.COLONY_BUILT, 1, Integer::sum);
    }

    /**
     * Havia bloco na coluna, e este é o primeiro que barrou.
     *
     * <p>O chamador já sabe qual bloco parou a conferência; pedir que ele
     * o passe evita que esta classe repita a varredura da coluna.
     */
    public static void inTheColumn(ServerWorld world, BlockPos pos) {
        BY_CAUSE.merge(Why.IN_THE_COLUMN, 1, Integer::sum);

        if (COUNTED.size() >= MAX_POSITIONS || !COUNTED.add(pos.asLong())) {
            return;
        }

        Block block = world.getBlockState(pos).getBlock();

        String name = Registries.BLOCK.getId(block).getPath();

        if (SEEN.size() >= MAX_KINDS && !SEEN.containsKey(name)) {
            return;
        }

        SEEN.merge(name, 1, Integer::sum);
    }

    /** Diz o que se viu, junto do relatório de recusas. */
    public static void report() {
        if (BY_CAUSE.isEmpty()) {
            return;
        }

        int built = BY_CAUSE.getOrDefault(Why.COLONY_BUILT, 0);

        int column = BY_CAUSE.getOrDefault(Why.IN_THE_COLUMN, 0);

        StringBuilder said = new StringBuilder();

        SEEN.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(8)
                .forEach(entry -> {
                    if (said.length() > 0) {
                        said.append("; ");
                    }

                    said.append(entry.getValue()).append(' ').append(entry.getKey());
                });

        VillageColonyMod.LOGGER.info(
                "Rule 22 turned lots down — {} the colony had already built there,"
                        + " {} had something in the column{}",
                built,
                column,
                said.length() == 0 ? "" : " — " + said);
    }

    /** Esquece o que foi contado. Chamado ao parar o servidor. */
    public static void clearAll() {
        BY_CAUSE.clear();

        SEEN.clear();

        COUNTED.clear();
    }

    /** Quantas vezes esta causa recusou. Para a bateria. */
    public static int countOf(Why why) {
        return BY_CAUSE.getOrDefault(why, 0);
    }

    /** Quantas posições distintas deste bloco barraram. Para a bateria. */
    public static int countOf(String block) {
        return SEEN.getOrDefault(block, 0);
    }
}
