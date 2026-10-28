package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.fabric.integration.ColonyChests;
import net.minecraft.block.BlockState;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * O pedreiro sem tarefa recolhe neve com a pá — item 5 das escolhas do autor (2026-10-27), uma das ajudas
 * da Regra 127. Só quando a obra aberta pede peça de neve (camada ou bloco): ele quebra a neve natural da
 * caixa da vila com a pá e guarda as bolas de neve no baú da colônia mais perto; a obra faz o bloco e a
 * camada pela receita do jogo. Um pedreiro por colônia por vez. O diorito vem da mina, como já vinha.
 */
final class SnowShovel {

    static {
        ServerMemory.register(SnowShovel.class, SnowShovel::clearAll);
    }

    /** As peças de neve que fazem o pedreiro sair com a pá. */
    static final java.util.Set<String> SNOW_PIECES = java.util.Set.of("snow", "snow_block");

    /** Colunas da caixa da vila olhadas por passagem atrás de neve. */
    static final int SNOW_SCAN = 1024;

    private static final class Help {
        final UUID worker;
        final BlockPos target;
        int walked;

        Help(UUID worker, BlockPos target) {
            this.worker = worker;
            this.target = target;
        }
    }

    private static final Map<UUID, Help> SNOW = new HashMap<>();

    private static final Map<UUID, Integer> SNOW_CURSOR = new HashMap<>();

    private SnowShovel() {
    }

    static boolean isShoveling(UUID workerId) {
        return SNOW.values().stream().anyMatch(help -> help.worker.equals(workerId));
    }

    /** O pedreiro sem tarefa recolhe neve com a pá quando a obra aberta pede peça de neve. */
    static void pass(ServerWorld world, Colony colony) {
        Help help = SNOW.get(colony.id());

        if (help != null && !(world.getEntity(help.worker) instanceof VillagerEntity helper && IdleHelps.isFree(world, helper))) {
            stopSnow(colony.id());
            help = null;
        }

        if (help == null) {
            if (!buildWantsSnow(colony.id())) {
                return;
            }

            for (Worker worker : VillageColonyMod.WORKERS.ofColony(colony.id())) {
                if (worker.profession().filter(ProfessionType.MASON::equals).isEmpty()
                        || !(world.getEntity(worker.villagerId()) instanceof VillagerEntity mason)
                        || !IdleHelps.isFree(world, mason) || PathPaving.isPaving(mason.getUuid())) {
                    continue;
                }

                Optional<BlockPos> snow = snowInTheVillage(world, colony);

                if (snow.isEmpty()) {
                    return;
                }

                help = new Help(mason.getUuid(), snow.get());
                SNOW.put(colony.id(), help);
                break;
            }
        }

        if (help == null || !(world.getEntity(help.worker) instanceof VillagerEntity mason)) {
            return;
        }

        if (!isNaturalSnow(world, help.target)) {
            stopSnow(colony.id());
            return;
        }

        if (!mason.getBlockPos().isWithinDistance(help.target, 3)) {
            help.walked += 10;

            if (help.walked > IdleHelps.GIVE_UP_AFTER) {
                stopSnow(colony.id());
                return;
            }

            WorkTargets.set(mason.getUuid(), help.target, 2);
            return;
        }

        mason.swingHand(Hand.MAIN_HAND);
        int stored = shovelSnowAt(world, colony.id(), help.target);

        ActionJournal.action(mason.getUuid(), "SHOVELED", "snowball", stored, help.target, "the build wants snow");
        VillageColonyMod.LOGGER.info("Mason {} shoveled {} snowball(s) at {} — the build wants snow",
                mason.getUuid().toString().substring(0, 8), stored, help.target.toShortString());
        stopSnow(colony.id());
    }

    /** Se a obra aberta desta colônia ainda pede camada ou bloco de neve. */
    static boolean buildWantsSnow(UUID colonyId) {
        return VillageColonyMod.CONSTRUCTIONS.openOf(colonyId)
                .map(project -> project.remainingMaterials().keySet().stream()
                        .anyMatch(id -> SNOW_PIECES.contains(id.path())))
                .orElse(false);
    }

    /**
     * Quebra a neve com a pá e guarda as bolas de neve no baú da colônia mais perto: uma por camada, quatro
     * por bloco, como a pá dá no jogo.
     *
     * @return quantas bolas de neve foram guardadas
     */
    static int shovelSnowAt(ServerWorld world, UUID colonyId, BlockPos snow) {
        BlockState state = world.getBlockState(snow);
        int snowballs = state.isOf(net.minecraft.block.Blocks.SNOW_BLOCK) ? 4
                : state.isOf(net.minecraft.block.Blocks.SNOW) ? state.get(net.minecraft.block.SnowBlock.LAYERS) : 0;

        world.breakBlock(snow, false);

        List<ColonyPos> chests = ColonyChests.nearestFirst(world, colonyId, MinecraftTypeAdapter.toColonyPos(snow));

        return snowballs - ColonyChests.deposit(world, chests, net.minecraft.item.Items.SNOWBALL, snowballs);
    }

    /** A primeira neve natural na superfície da caixa da vila, a partir de onde a varredura parou. */
    static Optional<BlockPos> snowInTheVillage(ServerWorld world, Colony colony) {
        Optional<com.villagecolony.core.colony.model.VillageBounds> box = colony.bounds();

        if (box.isEmpty()) {
            return Optional.empty();
        }

        int width = box.get().maxX() - box.get().minX() + 1;
        int depth = box.get().maxZ() - box.get().minZ() + 1;
        int columns = width * depth;
        int start = SNOW_CURSOR.getOrDefault(colony.id(), 0) % columns;

        for (int seen = 0; seen < Math.min(SNOW_SCAN, columns); seen++) {
            int index = (start + seen) % columns;
            int x = box.get().minX() + index % width;
            int z = box.get().minZ() + index / width;

            if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                continue;
            }

            BlockPos top = new BlockPos(x, world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z) - 1, z);

            for (BlockPos at : new BlockPos[] {top, top.up()}) {
                if (isNaturalSnow(world, at)) {
                    SNOW_CURSOR.put(colony.id(), index + 1);
                    return Optional.of(at.toImmutable());
                }
            }
        }

        SNOW_CURSOR.put(colony.id(), start + Math.min(SNOW_SCAN, columns));

        return Optional.empty();
    }

    /** Camada ou bloco de neve que é chão do lugar: não é peça de obra nem de vila, nem do jogador. */
    static boolean isNaturalSnow(ServerWorld world, BlockPos at) {
        BlockState state = world.getBlockState(at);

        return (state.isOf(net.minecraft.block.Blocks.SNOW) || state.isOf(net.minecraft.block.Blocks.SNOW_BLOCK))
                && com.villagecolony.fabric.integration.BlockProtection.mayDigOut(world, at, state);
    }

    private static void stopSnow(UUID colonyId) {
        Help help = SNOW.remove(colonyId);

        if (help != null) {
            WorkTargets.clear(help.worker);
        }
    }

    static void clearAll() {
        SNOW.clear();
        SNOW_CURSOR.clear();
    }
}
