package com.villagecolony.core.coordination;

import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.type.ServerMemory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.ToIntFunction;

/**
 * O adiantamento — peça que nenhuma obra pediu — ADR-038 P5b: no máximo
 * {@link #BATCH} de uma peça por vez; feitas, troca para a seguinte, num ciclo
 * sem fim de um pouco de cada. Peça que não avança em {@link #PATIENCE} ciclos
 * (falta de material) cede a vez, para o ciclo não parar nela.
 *
 * <p><b>Com teto</b> (P1, autossuficiência, 2026-10-11): quando a colônia tem
 * tetos publicados ({@link #ceilings}), nenhuma peça passa do que a obra aberta
 * ainda pede somado ao maior uso dela numa casa do estilo da vila. Sem teto a
 * roda acumulou 318 lajes e 356 escadas enquanto a obra esperava pedregulho.
 */
public final class AdvanceStock {

    static {
        ServerMemory.register(AdvanceStock.class, AdvanceStock::clearAll);
    }

    /** Quantas de uma peça se adiantam de cada vez. */
    public static final int BATCH = 5;

    /** Ciclos sem progresso antes de a peça ceder a vez. */
    public static final int PATIENCE = 10;

    private static final class Cursor {
        int index;
        int target;
        int stalled;
    }

    private record Lane(UUID colonyId, String name) {
    }

    private static final Map<Lane, Cursor> CURSORS = new HashMap<>();

    private static final Map<UUID, Map<ResourceType, Integer>> CEILINGS = new HashMap<>();

    /** As peças que cada ofício adianta sem tarefa — ADR-038 P2a. */
    public static final List<ResourceType> SMELTER_PIECES = List.of(ResourceType.GLASS, ResourceType.SMOOTH_STONE);

    /** As do carpinteiro — ADR-039 D1: escada, laje e cerca; a viga descascada não, ela gasta a tora da obra. */
    public static final List<ResourceType> CARPENTER_PIECES =
            List.of(ResourceType.OAK_STAIRS, ResourceType.OAK_SLAB, ResourceType.OAK_FENCE);

    private AdvanceStock() {
    }

    public static void clearAll() {
        CURSORS.clear();
        VILLAGE_PIECES.clear();
        CEILINGS.clear();
    }

    /** As peças que as plantas da vila usam, por fila — ADR-039 item 4. */
    private static final Map<Lane, List<ResourceType>> VILLAGE_PIECES = new HashMap<>();

    /** Anota as peças que as estruturas desta vila pedem, para esta fila. */
    public static void piecesOfTheVillage(UUID colonyId, String lane, List<ResourceType> pieces) {
        VILLAGE_PIECES.put(new Lane(colonyId, lane), List.copyOf(pieces));
    }

    /** O teto de cada peça desta colônia; peça fora do mapa tem teto zero. */
    public static void ceilings(UUID colonyId, Map<ResourceType, Integer> ceilings) {
        CEILINGS.put(colonyId, Map.copyOf(ceilings));
    }

    private static ToIntFunction<ResourceType> ceilingOf(UUID colonyId) {
        Map<ResourceType, Integer> known = CEILINGS.get(colonyId);

        // E nunca acima do teto geral (StockCeiling): o adiantamento não é pedido da obra.
        return known == null ? StockCeiling::base : type -> Math.min(known.getOrDefault(type, 0), StockCeiling.base(type));
    }

    /** As peças da vila para esta fila, ou {@code fallback} enquanto as plantas não foram lidas. */
    public static List<ResourceType> piecesFor(UUID colonyId, String lane, List<ResourceType> fallback) {
        return VILLAGE_PIECES.getOrDefault(new Lane(colonyId, lane), fallback);
    }

    /** A vez de uma fila, como o save a guarda — ADR-039 C. */
    public record Turn(UUID colonyId, String lane, int index, int target, int stalled) {
    }

    /** A vez de cada fila, para o save. */
    public static List<Turn> snapshot() {
        return CURSORS.entrySet().stream()
                .map(entry -> new Turn(entry.getKey().colonyId(), entry.getKey().name(),
                        entry.getValue().index, entry.getValue().target, entry.getValue().stalled))
                .toList();
    }

    /** Devolve a vez de cada fila. */
    public static void restore(List<Turn> turns) {
        for (Turn turn : turns) {
            Cursor cursor = new Cursor();
            cursor.index = turn.index();
            cursor.target = turn.target();
            cursor.stalled = turn.stalled();
            CURSORS.put(new Lane(turn.colonyId(), turn.lane()), cursor);
        }
    }

    /**
     * Põe nas metas a peça da vez desta colônia.
     *
     * @param pieces as peças que a vila adianta, em ordem
     * @param stock quanto a colônia tem de cada uma
     * @return a peça da vez, ou nenhuma quando a lista é vazia
     */
    public static java.util.Optional<ResourceType> addTo(
            Map<ResourceType, Integer> goals, UUID colonyId, List<ResourceType> pieces,
            ToIntFunction<ResourceType> stock) {
        return addTo(goals, colonyId, "mason", pieces, stock);
    }

    /** O mesmo, numa fila própria: cada ofício tem o seu ciclo. */
    public static java.util.Optional<ResourceType> addTo(
            Map<ResourceType, Integer> goals, UUID colonyId, String lane, List<ResourceType> pieces,
            ToIntFunction<ResourceType> stock) {
        if (pieces.isEmpty()) {
            return java.util.Optional.empty();
        }

        Lane key = new Lane(colonyId, lane);
        Cursor cursor = CURSORS.get(key);

        if (cursor == null) {
            cursor = new Cursor();
            cursor.index = 0;
            cursor.target = stock.applyAsInt(pieces.get(0)) + BATCH;
            CURSORS.put(key, cursor);
        }

        ToIntFunction<ResourceType> ceiling = ceilingOf(colonyId);

        cursor.index = Math.floorMod(cursor.index, pieces.size());
        ResourceType piece = pieces.get(cursor.index);
        int have = stock.applyAsInt(piece);

        if (have >= cursor.target || ++cursor.stalled > PATIENCE || have >= ceiling.applyAsInt(piece)) {
            int next = nextBelowCeiling(pieces, cursor.index, stock, ceiling);

            if (next < 0) {
                cursor.stalled = 0;

                return java.util.Optional.empty();
            }

            cursor.index = next;
            piece = pieces.get(cursor.index);
            have = stock.applyAsInt(piece);
            cursor.target = have + BATCH;
            cursor.stalled = 0;
        }

        cursor.target = (int) Math.min(cursor.target, (long) ceiling.applyAsInt(piece));
        goals.merge(piece, cursor.target, Math::max);

        return java.util.Optional.of(piece);
    }

    /** A próxima peça, depois de {@code from} e em roda, ainda abaixo do teto; -1 se nenhuma. */
    private static int nextBelowCeiling(List<ResourceType> pieces, int from,
            ToIntFunction<ResourceType> stock, ToIntFunction<ResourceType> ceiling) {

        for (int step = 1; step <= pieces.size(); step++) {
            int index = Math.floorMod(from + step, pieces.size());
            ResourceType piece = pieces.get(index);

            if (stock.applyAsInt(piece) < ceiling.applyAsInt(piece)) {
                return index;
            }
        }

        return -1;
    }
}
