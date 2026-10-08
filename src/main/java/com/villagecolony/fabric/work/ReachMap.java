package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ColonyChests;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * O mapa de alcance da vila — M2, fase 1 do plano do nivelador
 * ({@code docs/research/2026-10-08-nivelamento-do-solo-da-vila.md}), decisão do
 * autor: medir antes de criar o nivelador. <b>Só lê o mundo.</b>
 *
 * <p>Uma busca a pé a partir do centro da vila, com a regra de passo do aldeão:
 * vizinho alcançável se cabe de pé e o desnível é de no máximo um bloco, para
 * cima ou para baixo (o caminho tem de servir de volta). Porta e portão contam
 * como passagem. É diagnóstico global; a rota de cada aldeão continua sendo da
 * navegação Vanilla.
 *
 * <p>Fatiada: {@link #BUDGET} posições por passagem, uma passagem por segundo, só
 * em vila com jogador perto, e refeita a cada {@link #REMEASURE} tiques. No fim
 * diz quais lugares que a vila precisa alcançar ficaram de fora — baús da colônia
 * (Regra 45), a boca de superfície da mina ({@link MineEntrance}) e o lote da obra
 * aberta —, numa linha {@code VC_REACH} e no {@code /vc log}.
 */
public final class ReachMap {

    static {
        ServerMemory.register(ReachMap.class, ReachMap::clearAll);
    }

    /** Posições visitadas por passagem. */
    static final int BUDGET = 1_500;

    /** De quanto em quanto tempo o mapa é refeito: 10 min. */
    static final long REMEASURE = 12_000;

    /** Quanto além da caixa da vila a busca anda. */
    static final int MARGIN = 16;

    /** O que ficou de fora, e quantos lugares eram. */
    public record Result(int cells, int places, List<String> unreached, long tick) {

        public int reached() {
            return places - unreached.size();
        }
    }

    /** Uma busca em curso. */
    static final class Sweep {
        final ArrayDeque<BlockPos> frontier = new ArrayDeque<>();
        final Set<BlockPos> seen = new HashSet<>();
        final VillageBounds area;

        Sweep(BlockPos start, VillageBounds area) {
            this.area = area;
            frontier.add(start);
            seen.add(start);
        }
    }

    private static final Map<UUID, Sweep> SWEEPS = new HashMap<>();

    private static final Map<UUID, Result> RESULTS = new HashMap<>();

    private ReachMap() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ServerWorld world = server.getOverworld();

            if (world.getTime() % 20 == 0) {
                tick(world);
            }
        });
    }

    /** O último mapa desta colônia. */
    public static Optional<Result> of(UUID colonyId) {
        return Optional.ofNullable(RESULTS.get(colonyId));
    }

    private static void tick(ServerWorld world) {
        long now = world.getTime();

        for (Colony colony : List.copyOf(VillageColonyMod.COLONIES.all())) {
            if (!colony.isActive() || !colony.isAttended(now) || colony.bounds().isEmpty()) {
                continue;
            }

            Sweep sweep = SWEEPS.get(colony.id());

            if (sweep == null) {
                Result last = RESULTS.get(colony.id());

                if (last != null && now - last.tick() < REMEASURE) {
                    continue;
                }

                Optional<BlockPos> start = startOf(world, colony);

                if (start.isEmpty()) {
                    continue;
                }

                VillageBounds box = colony.bounds().get();

                sweep = new Sweep(start.get(), new VillageBounds(
                        box.minX() - MARGIN, box.minY() - 10, box.minZ() - MARGIN,
                        box.maxX() + MARGIN, box.maxY() + 10, box.maxZ() + MARGIN));
                SWEEPS.put(colony.id(), sweep);
            }

            if (step(world, sweep, BUDGET)) {
                SWEEPS.remove(colony.id());
                finish(world, colony, sweep);
            }
        }
    }

    /**
     * Anda até {@code budget} posições.
     *
     * @return se a busca terminou
     */
    static boolean step(ServerWorld world, Sweep sweep, int budget) {
        for (int done = 0; done < budget && !sweep.frontier.isEmpty(); done++) {
            BlockPos at = sweep.frontier.poll();

            for (Direction way : Direction.Type.HORIZONTAL) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos next = at.offset(way).up(dy);

                    if (sweep.seen.contains(next)
                            || !sweep.area.containsColumn(next.getX(), next.getZ())
                            || next.getY() < sweep.area.minY() || next.getY() > sweep.area.maxY()
                            || (dy > 0 && !open(world, at.up(2)))
                            || (dy < 0 && !open(world, at.offset(way).up()))
                            || !standable(world, next)) {
                        continue;
                    }

                    sweep.seen.add(next);
                    sweep.frontier.add(next);
                }
            }
        }

        return sweep.frontier.isEmpty();
    }

    /** O mapa de um lugar só, inteiro, para o teste: o que se alcança de {@code start} dentro de {@code area}. */
    static Set<BlockPos> measure(ServerWorld world, BlockPos start, VillageBounds area) {
        Sweep sweep = new Sweep(start, area);

        while (!step(world, sweep, BUDGET)) {
            // até fechar
        }

        return sweep.seen;
    }

    /** Se dá para ficar ao lado deste lugar: alguma posição visitada a um bloco dele. */
    static boolean reaches(Set<BlockPos> seen, BlockPos place, int around) {
        for (int dx = -around; dx <= around; dx++) {
            for (int dz = -around; dz <= around; dz++) {
                for (int dy = -2; dy <= 1; dy++) {
                    if (seen.contains(place.add(dx, dy, dz))) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static void finish(ServerWorld world, Colony colony, Sweep sweep) {
        Map<String, BlockPos> places = new LinkedHashMap<>();

        for (ColonyPos chest : ColonyChests.nearestFirst(world, colony.id(), colony.center())) {
            places.put("chest " + chest.x() + "," + chest.y() + "," + chest.z(),
                    MinecraftTypeAdapter.toBlockPos(chest));
        }

        VillageColonyMod.MINES.of(colony.id()).ifPresent(mine -> {
            ColonyPos mouth = MineEntrance.surfaceOf(world, colony.id(), mine);
            places.put("mine entrance " + mouth.x() + "," + mouth.y() + "," + mouth.z(),
                    MinecraftTypeAdapter.toBlockPos(mouth));
        });

        VillageColonyMod.CONSTRUCTIONS.openOf(colony.id()).ifPresent(project -> places.put(
                "build site " + project.origin().x() + "," + project.origin().y() + "," + project.origin().z(),
                MinecraftTypeAdapter.toBlockPos(project.origin())));

        List<String> unreached = new ArrayList<>();

        for (Map.Entry<String, BlockPos> place : places.entrySet()) {
            int around = place.getKey().startsWith("chest") ? 1 : 2;

            if (!reaches(sweep.seen, place.getValue(), around)) {
                unreached.add(place.getKey());
            }
        }

        Result result = new Result(sweep.seen.size(), places.size(), List.copyOf(unreached), world.getTime());

        RESULTS.put(colony.id(), result);

        VillageColonyMod.LOGGER.info(
                "VC_REACH version=1 colony={} cells={} places={} reached={} unreached={}",
                colony.id().toString().substring(0, 8), result.cells(), result.places(), result.reached(),
                String.join(";", unreached).replace(' ', '_'));
    }

    /** Onde a busca começa: o chão no centro da vila, ou o mais perto dele num raio de 4. */
    private static Optional<BlockPos> startOf(ServerWorld world, Colony colony) {
        ColonyPos centre = colony.center();

        for (int radius = 0; radius <= 4; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int x = centre.x() + dx;
                    int z = centre.z() + dz;

                    if (world.getChunkManager().getWorldChunk(x >> 4, z >> 4) == null) {
                        continue;
                    }

                    BlockPos top = new BlockPos(x, world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z), z);

                    if (standable(world, top)) {
                        return Optional.of(top);
                    }
                }
            }
        }

        return Optional.empty();
    }

    /** Cabe um aldeão de pé: chão com colisão, dois livres e seco; chunk só se carregado. */
    static boolean standable(ServerWorld world, BlockPos at) {
        if (world.getChunkManager().getWorldChunk(at.getX() >> 4, at.getZ() >> 4) == null) {
            return false;
        }

        BlockPos floor = at.down();

        return !world.getBlockState(floor).getCollisionShape(world, floor).isEmpty()
                && open(world, at)
                && open(world, at.up())
                && world.getFluidState(at).isEmpty();
    }

    /** Passa um aldeão: sem colisão, ou porta e portão de madeira, que ele abre (a de ferro não). */
    private static boolean open(ServerWorld world, BlockPos at) {
        BlockState state = world.getBlockState(at);

        return state.getCollisionShape(world, at).isEmpty()
                || state.isIn(BlockTags.WOODEN_DOORS)
                || state.isIn(BlockTags.FENCE_GATES);
    }

    public static void clearAll() {
        SWEEPS.clear();
        RESULTS.clear();
    }
}
