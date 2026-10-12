package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.integration.BlockProtection;
import com.villagecolony.fabric.integration.ColonyChests;
import com.villagecolony.fabric.integration.ConstructionZone;
import com.villagecolony.fabric.integration.PlayerClearedPlants;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;

import java.util.List;
import java.util.Optional;

/**
 * O lenhador ocioso planta mudas em volta da vila — regra do autor, 2026-10-12:
 * <i>"lenhadores com tempo ocioso devem plantar rebentos de tipos diferentes
 * aleatoriamente pelo lado de fora da borda do terreno da vila até 10 blocos de
 * distância fora"</i>. No playtest 0.3.35 os lenhadores ficaram 99% ociosos.
 *
 * <p>A cada {@link #EVERY} tiques, um lenhador sem tarefa planta uma muda de tipo sorteado
 * num ponto sorteado da faixa de 1 a {@link #BAND} blocos fora da caixa da vila: chão
 * natural, céu aberto, longe de outra árvore ({@link #SPACING}), fora da faixa de
 * construção e do que é da colônia ou da vila original. A muda sai do baú quando há
 * dela; sem nenhuma, a muda é plantada assim mesmo (é planta que a natureza dá).
 */
final class OutskirtsPlanting {

    /** Até quantos blocos fora da borda da caixa. */
    static final int BAND = 10;

    /** Distância mínima até outra muda ou tronco. */
    static final int SPACING = 3;

    /** Quantos pontos sorteados se tentam por muda. */
    static final int TRIES = 12;

    /**
     * Uma muda por vila a cada cinco minutos — autor, 2026-10-12: no playtest da 0.3.41 foram
     * 44 em 21 minutos, uma por lenhador ocioso a cada ciclo.
     */
    static final long EVERY = 6_000;

    private static final java.util.Map<java.util.UUID, Long> LAST = new java.util.HashMap<>();

    static {
        com.villagecolony.core.type.ServerMemory.register(OutskirtsPlanting.class, LAST::clear);
    }

    /** Os tipos sorteados: os que crescem sozinhos, de uma muda só. */
    static final List<Block> SAPLINGS = List.of(
            Blocks.OAK_SAPLING, Blocks.BIRCH_SAPLING, Blocks.SPRUCE_SAPLING,
            Blocks.JUNGLE_SAPLING, Blocks.ACACIA_SAPLING, Blocks.CHERRY_SAPLING);

    private OutskirtsPlanting() {
    }

    /**
     * Uma muda por lenhador sem tarefa.
     *
     * @return quantas foram plantadas
     */
    static int plantForIdleLumberjacks(ServerWorld world, Colony colony) {
        Optional<VillageBounds> box = colony.bounds();

        if (box.isEmpty()) {
            return 0;
        }

        int planted = 0;
        long now = world.getTime();

        if (now - LAST.getOrDefault(colony.id(), Long.MIN_VALUE / 2) < EVERY) {
            return 0;
        }

        for (Worker worker : VillageColonyMod.WORKERS.ofColony(colony.id())) {
            if (worker.profession().filter(ProfessionType.LUMBERJACK::equals).isEmpty()
                    || !VillageColonyMod.TASKS.assignedTo(worker.villagerId()).isEmpty()) {
                continue;
            }

            if (plantOne(world, colony, box.get(), world.getRandom())) {
                planted++;
                LAST.put(colony.id(), now);
                break;
            }
        }

        if (planted > 0) {
            VillageColonyMod.LOGGER.info("Colony {} — idle lumberjacks planted {} saplings outside the village",
                    colony.id().toString().substring(0, 8), planted);
        }

        return planted;
    }

    /** Uma muda de tipo sorteado num ponto sorteado da faixa de fora. */
    static boolean plantOne(ServerWorld world, Colony colony, VillageBounds box, Random random) {
        Block sapling = SAPLINGS.get(random.nextInt(SAPLINGS.size()));

        for (int attempt = 0; attempt < TRIES; attempt++) {
            Optional<BlockPos> ground = spotOutside(world, box, random);

            if (ground.isEmpty() || !fits(world, ground.get(), sapling)) {
                continue;
            }

            ColonyChests.withdraw(world, ColonyChests.nearestFirst(world, colony.id(), colony.center()),
                    sapling.asItem(), 1);
            world.setBlockState(ground.get().up(), sapling.getDefaultState(), Block.NOTIFY_ALL);

            return true;
        }

        return false;
    }

    /** O chão de um ponto sorteado entre 1 e {@link #BAND} blocos fora da caixa. */
    static Optional<BlockPos> spotOutside(ServerWorld world, VillageBounds box, Random random) {
        int out = 1 + random.nextInt(BAND);
        int x;
        int z;

        switch (random.nextInt(4)) {
            case 0 -> {
                x = box.minX() - out;
                z = box.minZ() - BAND + random.nextInt(box.sizeZ() + 2 * BAND);
            }
            case 1 -> {
                x = box.maxX() + out;
                z = box.minZ() - BAND + random.nextInt(box.sizeZ() + 2 * BAND);
            }
            case 2 -> {
                z = box.minZ() - out;
                x = box.minX() - BAND + random.nextInt(box.sizeX() + 2 * BAND);
            }
            default -> {
                z = box.maxZ() + out;
                x = box.minX() - BAND + random.nextInt(box.sizeX() + 2 * BAND);
            }
        }

        if (box.containsColumn(x, z) || !world.isChunkLoaded(new BlockPos(x, 0, z))) {
            return Optional.empty();
        }

        int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);

        return Optional.of(new BlockPos(x, top - 1, z));
    }

    /** Chão natural, ar em cima, céu aberto, longe de árvore, obra e do que é da vila. */
    static boolean fits(ServerWorld world, BlockPos ground, Block sapling) {
        BlockState state = world.getBlockState(ground);

        if (!state.isIn(BlockTags.DIRT) || !world.getBlockState(ground.up()).isAir()
                || !world.getBlockState(ground.up(2)).isAir()
                || !sapling.getDefaultState().canPlaceAt(world, ground.up())
                || PlayerClearedPlants.isCleared(ground)
                || ConstructionZone.nearRoad(world, ground)
                || BlockProtection.isColonyBuilt(ground)
                || BlockProtection.isVillageOriginal(world, ground)) {
            return false;
        }

        for (BlockPos near : BlockPos.iterate(ground.add(-SPACING, 0, -SPACING), ground.add(SPACING, 3, SPACING))) {
            BlockState there = world.getBlockState(near);

            if (there.isIn(BlockTags.SAPLINGS) || there.isIn(BlockTags.LOGS)) {
                return false;
            }
        }

        return true;
    }
}
