package com.villagecolony.core.coordination;

import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.type.Side;

import java.util.Collection;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * O plano de expansão do terreno da vila — Regra 56, escolha do autor de 2026-10-12
 * pelas opções B, C e D de {@code docs/research/2026-10-12-expansao-do-terreno.md}.
 *
 * <ul>
 *   <li><b>B</b> — cada obra reivindica em volta dela uma folga pelo tipo: casa
 *       {@link #HOUSE_CLAIM}, oficina e roça {@link #WORK_CLAIM}.</li>
 *   <li><b>C</b> — {@link #FULL_AFTER} varreduras seguidas sem lote dizem que a vila
 *       encheu: ela mede os quatro lados e anexa {@link #STRIP} blocos no melhor.</li>
 *   <li><b>D</b> — a rua cresce primeiro pelas pontas voltadas para o lado escolhido; o
 *       lote nasce na beira dela ({@code RoadExtension}).</li>
 *   <li><b>Borda</b> — uma em cada {@link #EDGE_EVERY} obras nasce na faixa da borda, para a
 *       vila crescer mais depressa (autor, 2026-10-12).</li>
 * </ul>
 */
public final class VillageExpansion {

    static {
        ServerMemory.register(VillageExpansion.class, VillageExpansion::clearAll);
    }

    /** A folga em volta de uma casa de morar. */
    public static final int HOUSE_CLAIM = 16;

    /** A folga em volta de oficina, roça e demais obras de trabalho. */
    public static final int WORK_CLAIM = 24;

    /** A largura da faixa anexada de cada vez. */
    public static final int STRIP = 16;

    /**
     * Quantas voltas completas da varredura sem lote dizem que a vila encheu. Uma: a volta
     * já percorreu o raio inteiro, e no playtest da 0.3.39 ela levou 20 minutos.
     */
    public static final int FULL_AFTER = 1;

    /**
     * Com uma faixa anexada que ainda não deu lote, quantas varreduras até a próxima.
     *
     * <p>Era {@code 3 * FULL_AFTER} (0.3.57): a vila esperava três voltas de varredura — dezenas de
     * minutos — entre anexos, e no meio tempo ficava sem lote, sem obra e com os produtores
     * ociosos (playtest `iALIIoOwEK`: lenhador 98%, pedreiro 71%, com o estoque cheio). Agora anexa
     * já na volta seguinte que confirma a falta de lote (14-10): a vila cresce no ritmo em que fica
     * ociosa. A salvaguarda contra inchaço é outra e continua — só se anexa um lado plano, seco e
     * sem outra colônia ({@link SideSurvey#usable}).
     */
    public static final int FULL_AGAIN_AFTER = FULL_AFTER;

    /** De quantas obras novas uma nasce na borda da vila (autor, 2026-10-12). */
    public static final int EDGE_EVERY = 5;

    /** A faixa da borda: até quantos blocos da linha da caixa, por dentro ou por fora. */
    public static final int EDGE_BAND = 16;

    private static final Map<UUID, Integer> MISSES = new HashMap<>();

    private static final Map<UUID, Integer> OPENED = new HashMap<>();

    private static final Set<UUID> EDGE_TURN = new HashSet<>();

    private static final Map<UUID, Side> SIDE = new HashMap<>();

    private static final Set<UUID> PENDING = new HashSet<>();

    private static Predicate<ResourceId> isHouse = id -> true;

    private VillageExpansion() {
    }

    /**
     * O que se mediu além de um lado da caixa.
     *
     * @param columns quantas colunas se mediram
     * @param flat quantas têm chão sem degrau maior que um bloco para a vizinha
     * @param water quantas têm água ou lava em cima
     * @param built quantas têm construção da colônia ou da vila gerada
     * @param foreign se a faixa encosta na caixa de outra colônia
     */
    public record SideSurvey(Side side, int columns, int flat, int water, int built, boolean foreign) {

        public SideSurvey {
            Objects.requireNonNull(side, "side");
        }

        /** Chão bom ganha; água e construção perdem. */
        public int score() {
            return flat - 2 * water - 3 * built;
        }

        /** Sem outra colônia e com ao menos metade do chão plano. */
        public boolean usable() {
            return !foreign && columns > 0 && 2 * flat >= columns;
        }
    }

    /** Quem diz o que é casa de morar: o lado do jogo, que lê as plantas. */
    public static void useHouseRule(Predicate<ResourceId> rule) {
        isHouse = Objects.requireNonNull(rule, "rule");
    }

    /** A folga que esta planta reivindica em volta dela. */
    public static int claimOf(ResourceId blueprint) {
        Objects.requireNonNull(blueprint, "blueprint");

        return isHouse.test(blueprint) ? HOUSE_CLAIM : WORK_CLAIM;
    }

    /**
     * Uma varredura terminou sem lote.
     *
     * @return se a vila encheu e deve anexar uma faixa agora
     */
    public static boolean sweptWithoutLot(UUID colonyId) {
        if (EDGE_TURN.remove(colonyId)) {
            // A vez da borda não achou lote: a próxima busca é a de sempre, e esta não conta.
            return false;
        }

        int misses = MISSES.merge(colonyId, 1, Integer::sum);

        if (misses < (PENDING.contains(colonyId) ? FULL_AGAIN_AFTER : FULL_AFTER)) {
            return false;
        }

        MISSES.remove(colonyId);

        return true;
    }

    /** Uma varredura achou lote: a faixa anexada rendeu e a contagem recomeça. */
    public static void lotFound(UUID colonyId) {
        MISSES.remove(colonyId);
        PENDING.remove(colonyId);
    }

    /** Os lados que se podem anexar, do melhor para o pior; no empate, o primeiro medido. */
    public static java.util.List<SideSurvey> ranked(Collection<SideSurvey> surveys) {
        java.util.List<SideSurvey> usable = new java.util.ArrayList<>(surveys.stream().filter(SideSurvey::usable).toList());

        usable.sort(java.util.Comparator.comparingInt(SideSurvey::score).reversed());

        return usable;
    }

    /** O melhor lado que se pode anexar; vazio se nenhum serve. No empate, o primeiro medido. */
    public static Optional<SideSurvey> choose(Collection<SideSurvey> surveys) {
        SideSurvey best = null;

        for (SideSurvey survey : surveys) {
            if (survey.usable() && (best == null || survey.score() > best.score())) {
                best = survey;
            }
        }

        return Optional.ofNullable(best);
    }

    /** A faixa que este lado ganha. */
    public static VillageBounds stripOf(VillageBounds box, Side side) {
        return box.stripBeyond(side, STRIP);
    }

    /** O lado anexado: a rua cresce primeiro por ele. */
    public static void annexed(UUID colonyId, Side side) {
        SIDE.put(colonyId, Objects.requireNonNull(side, "side"));
        PENDING.add(colonyId);
    }

    /** Se a faixa anexada desta colônia ainda não deu lote — a vez do lote na faixa (StripLot, 2026-10-22). */
    public static boolean hasPendingStrip(UUID colonyId) {
        return PENDING.contains(colonyId) && SIDE.containsKey(colonyId);
    }

    /** Para onde a vila está crescendo, se ela já escolheu. */
    public static Optional<Side> sideOf(UUID colonyId) {
        return Optional.ofNullable(SIDE.get(colonyId));
    }

    /**
     * Uma obra nova abriu num lote. Depois de {@code EDGE_EVERY - 1} obras, a seguinte é
     * a vez da borda: o lote dela sai da faixa da borda ({@link #allowsLotAt}).
     */
    public static void buildOpened(UUID colonyId) {
        EDGE_TURN.remove(colonyId);

        if (OPENED.merge(colonyId, 1, Integer::sum) % EDGE_EVERY == EDGE_EVERY - 1) {
            EDGE_TURN.add(colonyId);
        }
    }

    /** Se a próxima obra desta colônia procura lote na borda. */
    public static boolean isEdgeTurn(UUID colonyId) {
        return EDGE_TURN.contains(colonyId);
    }

    /** Se um lote nesta coluna serve agora: na vez da borda, só na faixa da borda. */
    public static boolean allowsLotAt(UUID colonyId, VillageBounds box, int x, int z) {
        return !EDGE_TURN.contains(colonyId)
                || (box.containsColumn(x, z, EDGE_BAND) && !box.containsColumn(x, z, -EDGE_BAND));
    }

    public static void clearAll() {
        OPENED.clear();
        EDGE_TURN.clear();
        MISSES.clear();
        SIDE.clear();
        PENDING.clear();
    }
}
