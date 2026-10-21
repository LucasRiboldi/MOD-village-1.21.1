package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.type.ColonyPos;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.WorldChunk;

import java.util.List;
import java.util.Optional;

/**
 * Toda vila que nasce ganha 2 ovelhas, 2 galinhas, 2 vacas e 2 porcos em lugares sorteados dentro
 * da caixa dela — regra do autor, 2026-10-20: no deserto o pastor passou a sessão em "no sheep
 * with wool within the radius", e a vila Vanilla do deserto não gera bicho nenhum.
 *
 * <p>Só no nascimento da colônia (como as árvores de {@link VillageForest#seedInitial}): depois,
 * o rebanho cresce pelo pastor e quem morreu fica morto.
 */
public final class VillageFarmAnimals {

    /** Quantos de cada espécie. */
    static final int EACH = 2;

    /** As quatro espécies da regra. */
    static final List<EntityType<? extends AnimalEntity>> SPECIES =
            List.of(EntityType.SHEEP, EntityType.CHICKEN, EntityType.COW, EntityType.PIG);

    /** Quantos sorteios por bicho antes de desistir dele. */
    private static final int TRIES = 40;

    /** Sem caixa medida, o quadrado em volta do centro. */
    private static final int FALLBACK_RADIUS = 24;

    private VillageFarmAnimals() {
    }

    /** Solta os bichos da fundação; devolve quantos nasceram. */
    public static int seedInitial(ServerWorld world, Colony colony) {
        Random random = world.getRandom();
        int born = 0;

        for (EntityType<? extends AnimalEntity> species : SPECIES) {
            for (int index = 0; index < EACH; index++) {
                Optional<BlockPos> spot = spotFor(world, colony, random);

                if (spot.isEmpty()) {
                    continue;
                }

                AnimalEntity animal = species.create(world, null, spot.get(), SpawnReason.EVENT, false, false);

                if (animal != null) {
                    animal.setPersistent();
                    world.spawnEntity(animal);
                    born++;
                }
            }
        }

        VillageColonyMod.LOGGER.info("Colony {} got {} farm animals at its founding",
                colony.id().toString().substring(0, 8), born);

        return born;
    }

    /** Um lugar sorteado na caixa da vila: chão firme, sem água, dois blocos de ar em cima. */
    static Optional<BlockPos> spotFor(ServerWorld world, Colony colony, Random random) {
        ColonyPos center = colony.center();
        Optional<VillageBounds> box = colony.bounds();
        int minX = box.map(VillageBounds::minX).orElse(center.x() - FALLBACK_RADIUS);
        int maxX = box.map(VillageBounds::maxX).orElse(center.x() + FALLBACK_RADIUS);
        int minZ = box.map(VillageBounds::minZ).orElse(center.z() - FALLBACK_RADIUS);
        int maxZ = box.map(VillageBounds::maxZ).orElse(center.z() + FALLBACK_RADIUS);

        for (int attempt = 0; attempt < TRIES; attempt++) {
            int x = minX + random.nextInt(maxX - minX + 1);
            int z = minZ + random.nextInt(maxZ - minZ + 1);
            WorldChunk chunk = world.getChunkManager().getWorldChunk(x >> 4, z >> 4);

            if (chunk == null) {
                continue;
            }

            BlockPos feet = new BlockPos(x, chunk.sampleHeightmap(Heightmap.Type.MOTION_BLOCKING, x, z) + 1, z);

            if (standsOn(world, feet)) {
                return Optional.of(feet);
            }
        }

        return Optional.empty();
    }

    private static boolean standsOn(ServerWorld world, BlockPos feet) {
        BlockState below = world.getBlockState(feet.down());

        return world.isInBuildLimit(feet)
                && below.isSolidBlock(world, feet.down())
                && below.getFluidState().isEmpty()
                && !below.isIn(net.minecraft.registry.tag.BlockTags.LEAVES)
                && world.getBlockState(feet).getCollisionShape(world, feet).isEmpty()
                && world.getBlockState(feet.up()).getCollisionShape(world, feet.up()).isEmpty()
                && !world.getFluidState(feet).isIn(FluidTags.WATER)
                && !world.getFluidState(feet).isIn(FluidTags.LAVA);
    }
}
