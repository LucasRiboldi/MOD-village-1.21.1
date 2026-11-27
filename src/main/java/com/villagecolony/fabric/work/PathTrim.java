package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.data.save.ColonySavedData;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.fabric.integration.EditLedger;
import com.villagecolony.fabric.integration.VillageBiomes;
import com.villagecolony.fabric.integration.VillageRoad;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * A rua é uma linha de um bloco — Regras 186, 242 e 390 (ADR-065 etapa 2, L1 🅲 do jogo da 0.4.150). O caminho de dentro da
 * caixa sem nenhuma marca do grafo (linha, nó, entrada, o que falta calçar e o que está planejado) volta a ser o solo do
 * bioma, relva na planície, só quando a colônia o calçou e ele ficou {@value #WAIT_DAYS} dias de jogo fora de toda linha; o
 * do jogador e o da vila gerada ficam (Regra 316). O caminho coberto, sem 2 livres em cima, vira terra (Regra 180). Quem
 * troca é o Zelador ({@link #work}): ele anda até a coluna e troca um bloco por passada, e cada troca vai para o diário dos
 * blocos (Regra 384). Porta e construção ficam.
 */
final class PathTrim {

    private static final int BUDGET = 16;

    /** Quantos dias de jogo o caminho da colônia fica fora de toda linha antes de voltar ao solo (Regra 390). */
    static final int WAIT_DAYS = 2;

    /** A espera em vigor; zerada nos testes da passada. */
    static int waitDays = WAIT_DAYS;

    /** Quanto o Zelador anda até uma coluna antes de desistir dela, em tiques. */
    static final int GIVE_UP_AFTER = 1_200;

    private static final Map<UUID, Deque<ColonyPos>> SURPLUS = new HashMap<>();

    /** O caminho coberto achado na releitura, à espera de quem o devolva ao solo. */
    private static final Map<UUID, java.util.LinkedHashSet<BlockPos>> COVERED = new HashMap<>();

    /** Colônia → coluna → o dia em que ela apareceu fora de toda linha (Regra 390). */
    private static final Map<UUID, Map<Long, Long>> SINCE = new HashMap<>();

    /** Colônia → a coluna em que o Zelador trabalha agora, e quanto já andou até ela. */
    private static final Map<UUID, Target> TARGET = new HashMap<>();

    /** Colônia → as colunas de que o Zelador desistiu nesta leva da releitura. */
    private static final Map<UUID, Set<BlockPos>> GAVE_UP = new HashMap<>();

    /** Teto da fila do coberto: o resto volta na releitura seguinte. */
    static final int MAX_COVERED = 1_024;

    private record Target(BlockPos at, boolean covered, int walked) {
    }

    static {
        ServerMemory.register(PathTrim.class, () -> {
            SURPLUS.clear();
            COVERED.clear();
            SINCE.clear();
            TARGET.clear();
            GAVE_UP.clear();
        });
    }

    /** Guarda o caminho coberto desta colônia para a próxima passada (Regras 180 e 242). */
    static void cover(UUID colonyId, BlockPos path) {
        java.util.LinkedHashSet<BlockPos> covered = COVERED.computeIfAbsent(colonyId, ignored -> new java.util.LinkedHashSet<>());

        if (covered.size() < MAX_COVERED) {
            covered.add(path.toImmutable());
        }
    }

    /** O caminho coberto à espera, para o teste. */
    static java.util.Set<BlockPos> coveredOf(UUID colonyId) {
        return java.util.Collections.unmodifiableSet(COVERED.getOrDefault(colonyId, new java.util.LinkedHashSet<>()));
    }

    /**
     * O caminho de dentro da caixa sem nenhuma marca do grafo: fora da linha e dos nós, fora da frente das portas, do que
     * falta calçar e do que está planejado (Regra 242).
     */
    static List<ColonyPos> unmarked(java.util.Set<ColonyPos> inside, com.villagecolony.core.construction.model.RoadGraph graph,
                                    java.util.Set<Long> atTheDoor, java.util.Collection<com.villagecolony.core.construction.model.PathStep> toPave,
                                    java.util.Collection<ColonyPos> planned) {
        java.util.Set<Long> marked = new java.util.HashSet<>(atTheDoor);

        graph.nodes().forEach(node -> marked.add(ColonyRoads.column(node.position().x(), node.position().z())));
        graph.edges().forEach(edge -> edge.columns().forEach(at -> marked.add(ColonyRoads.column(at.x(), at.z()))));
        toPave.forEach(step -> marked.add(ColonyRoads.column(step.x(), step.z())));
        planned.forEach(at -> marked.add(ColonyRoads.column(at.x(), at.z())));

        return inside.stream()
                .filter(at -> !marked.contains(ColonyRoads.column(at.x(), at.z())))
                .toList();
    }

    /**
     * A sobra que fica: o caminho da vila gerada, rua ou peça (Regra 316). O teste troca a pergunta, porque o arena não tem
     * estrutura de vila no chunk.
     */
    static java.util.function.BiPredicate<ServerWorld, BlockPos> keep =
            com.villagecolony.fabric.integration.BlockProtection::isVillageOriginal;

    /** Se a colônia calçou esta coluna (Regra 390); o teste troca a pergunta. */
    static java.util.function.BiPredicate<ServerWorld, BlockPos> laidByTheColony =
            (world, at) -> ColonySavedData.get(world.getServer()).roadGraphs().laidByAColony(at.getX(), at.getZ());

    private PathTrim() {
    }

    /** O que sobrou na última releitura do grafo; a coluna que voltou à linha recomeça a espera (Regra 390). */
    static void offer(UUID colonyId, List<ColonyPos> surplus) {
        SURPLUS.put(colonyId, new ArrayDeque<>(surplus));

        Set<Long> columns = new HashSet<>();

        surplus.forEach(at -> columns.add(ColonyRoads.column(at.x(), at.z())));
        SINCE.computeIfAbsent(colonyId, ignored -> new HashMap<>()).keySet().retainAll(columns);
        GAVE_UP.remove(colonyId);
    }

    /** A passada sem andar, até {@value #BUDGET} colunas: o coberto primeiro; devolve quantas mudaram. */
    static int pass(ServerWorld world, Colony colony) {
        int done = 0;

        while (done < BUDGET) {
            Optional<Target> next = next(world, colony);

            if (next.isEmpty()) {
                break;
            }

            done += apply(world, colony, next.get()) ? 1 : 0;
        }

        return done;
    }

    /**
     * Uma passada do Zelador (Regra 390): anda até a próxima coluna e, ao lado dela, troca um bloco. Devolve 1 com a troca,
     * -1 enquanto anda e 0 sem nada a fazer ou enquanto ele calça.
     */
    static int work(ServerWorld world, Colony colony, VillagerEntity keeper) {
        if (PathPaving.isPaving(keeper.getUuid())) {
            return 0;
        }

        Target target = TARGET.get(colony.id());

        if (target == null || !stillDue(world, colony, target)) {
            Optional<Target> next = next(world, colony);

            if (next.isEmpty()) {
                TARGET.remove(colony.id());
                return 0;
            }

            target = next.get();
        }

        if (!keeper.getBlockPos().isWithinDistance(target.at(), PathPaving.REACH + 1)) {
            if (target.walked() > GIVE_UP_AFTER) {
                TARGET.remove(colony.id());
                GAVE_UP.computeIfAbsent(colony.id(), ignored -> new HashSet<>()).add(target.at());
                WorkTargets.clear(keeper.getUuid());
                return 0;
            }

            TARGET.put(colony.id(), new Target(target.at(), target.covered(), target.walked() + GroundKeeper.EVERY));
            WorkTargets.set(keeper.getUuid(), WalkLegs.towards(world, keeper.getBlockPos(), target.at().up())
                    .orElse(target.at().up()), 2);
            return -1;
        }

        TARGET.remove(colony.id());
        WorkTargets.clear(keeper.getUuid());

        return apply(world, colony, target) ? 1 : 0;
    }

    /** A próxima coluna a trocar: o coberto primeiro, depois a sobra que a colônia calçou e já esperou os dias. */
    private static Optional<Target> next(ServerWorld world, Colony colony) {
        java.util.Iterator<BlockPos> covered = COVERED.getOrDefault(colony.id(), new java.util.LinkedHashSet<>()).iterator();

        while (covered.hasNext()) {
            BlockPos at = covered.next();

            covered.remove();

            if (world.isChunkLoaded(at.getX() >> 4, at.getZ() >> 4) && VillageRoad.isPathBlock(world.getBlockState(at))) {
                return Optional.of(new Target(at, true, 0));
            }
        }

        Deque<ColonyPos> left = SURPLUS.get(colony.id());

        while (left != null && !left.isEmpty()) {
            BlockPos at = MinecraftTypeAdapter.toBlockPos(left.poll());

            if (isDue(world, colony, at)) {
                return Optional.of(new Target(at, false, 0));
            }
        }

        return Optional.empty();
    }

    private static boolean stillDue(ServerWorld world, Colony colony, Target target) {
        return target.covered() ? VillageRoad.isPathBlock(world.getBlockState(target.at())) : isDue(world, colony, target.at());
    }

    /** A sobra que pode voltar ao solo agora: caminho da colônia, fora da vila gerada e da construção, e que já esperou. */
    private static boolean isDue(ServerWorld world, Colony colony, BlockPos at) {
        if (!world.isChunkLoaded(at.getX() >> 4, at.getZ() >> 4) || !VillageRoad.isPathBlock(world.getBlockState(at))
                || BuildingGaps.insideABuilding(world, colony, at.getX(), at.getZ()) || keep.test(world, at)
                || !laidByTheColony.test(world, at) || GAVE_UP.getOrDefault(colony.id(), Set.of()).contains(at)) {
            return false;
        }

        long today = RoadGraphRefresh.dayOf(world);
        long since = SINCE.computeIfAbsent(colony.id(), ignored -> new HashMap<>())
                .computeIfAbsent(ColonyRoads.column(at.getX(), at.getZ()), ignored -> today);

        return today - since >= waitDays;
    }

    /** Troca a coluna e registra no diário dos blocos (Regra 384). */
    private static boolean apply(ServerWorld world, Colony colony, Target target) {
        BlockPos at = target.at();

        if (target.covered()) {
            RoadGraphRefresh.coverPath(world, at.up(), world.getBlockState(at.up()));
        } else if (isDue(world, colony, at)) {
            world.setBlockState(at, VillageBiomes.foundationGroundAt(world, at).getDefaultState());
        }

        if (VillageRoad.isPathBlock(world.getBlockState(at))) {
            return false;
        }

        EditLedger.mod(world, at, world.getBlockState(at), "limpeza"); // Regras 384 e 390
        return true;
    }
}
