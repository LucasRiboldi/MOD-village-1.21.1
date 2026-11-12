package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.LeashKnotEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Com curral na vila o pastor não amarra animal em cerca, e o animal já amarrado vai para o curral antes do solto —
 * Regra 255 (autor, 2026-11-12: "animais presos por cordas devem ser movidos para currais").
 */
public class ShepherdTetherGameTest implements FabricGameTest {

    private static final int Y = 150;

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "shepherd_tether", tickLimit = 40)
    public void aShepherdDoesNotTieWhileTheVillageHasAPen(TestContext context) {
        Arena arena = Arena.build(context, 6);
        CowEntity loose = arena.animal(EntityType.COW, new BlockPos(16, Y + 1, 18));

        arena.world.setBlockState(context.getAbsolutePos(new BlockPos(20, Y + 1, 20)), Blocks.OAK_FENCE.getDefaultState());

        context.waitAndRun(5, () -> {
            try {
                Optional<ShepherdHerding.Herd> herd = ShepherdHerding.start(arena.world, arena.colony, arena.shepherd);

                context.assertFalse(herd.isPresent() && herd.get().pen == null,
                        "o pastor amarrou a vaca " + loose.getUuid() + " com o curral cheio na vila");
            } finally {
                arena.cleanUp();
            }

            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "shepherd_tether_first", tickLimit = 120)
    public void aTiedAnimalGoesToThePenBeforeALooseOne(TestContext context) {
        Arena arena = Arena.build(context, 3);
        arena.animal(EntityType.COW, new BlockPos(15, Y + 1, 15));
        CowEntity tied = arena.animal(EntityType.COW, new BlockPos(24, Y + 1, 24));
        BlockPos post = context.getAbsolutePos(new BlockPos(25, Y + 1, 24));

        arena.world.setBlockState(post, Blocks.OAK_FENCE.getDefaultState());
        tied.attachLeash(LeashKnotEntity.getOrCreate(arena.world, post), true);

        // A navegação só responde com o pastor pousado (GameTest de navegação espera pousar).
        context.waitAndRun(40, () -> {
            try {
                Optional<ShepherdHerding.Herd> herd = ShepherdHerding.start(arena.world, arena.colony, arena.shepherd);

                context.assertTrue(herd.isPresent() && herd.get().pen != null, "o pastor não levou ninguém ao curral: herd="
                        + herd.map(h -> h.animal + "/" + h.pen).orElse("nenhum") + " amarrada=" + tied.getUuid()
                        + " guardada=" + ShepherdFlock.isPenned(arena.world, tied)
                        + " motivo=" + PenRefusal.reason(arena.world, arena.colony, arena.shepherd));
                context.assertTrue(herd.get().animal.equals(tied.getUuid()), "o pastor buscou o solto antes do amarrado");
            } finally {
                arena.cleanUp();
            }

            context.complete();
        });
    }

    /** Chão de 29 x 29, um cercado de 5 x 5 com portão ao norte e {@code sheep} ovelhas dentro, e o pastor. */
    private record Arena(TestContext context, ServerWorld world, Colony colony, VillagerEntity shepherd,
                         List<AnimalEntity> animals) {

        static Arena build(TestContext context, int sheep) {
            ServerWorld world = context.getWorld();

            for (int x = 0; x <= 28; x++) {
                for (int z = 0; z <= 28; z++) {
                    world.setBlockState(context.getAbsolutePos(new BlockPos(x, Y, z)), Blocks.GRASS_BLOCK.getDefaultState());

                    if ((x == 1 || x == 7 || z == 1 || z == 7) && x >= 1 && x <= 7 && z >= 1 && z <= 7) {
                        world.setBlockState(context.getAbsolutePos(new BlockPos(x, Y + 1, z)), Blocks.OAK_FENCE.getDefaultState());
                    }
                }
            }

            BlockPos gate = context.getAbsolutePos(new BlockPos(4, Y + 1, 1));
            world.setBlockState(gate, Blocks.OAK_FENCE_GATE.getDefaultState());

            BlockPos low = context.getAbsolutePos(new BlockPos(0, Y - 2, 0));
            BlockPos high = context.getAbsolutePos(new BlockPos(28, Y + 4, 28));
            Colony colony = Colony.create(UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(gate));
            colony.measure(new VillageBounds(low.getX(), low.getY(), low.getZ(), high.getX(), high.getY(), high.getZ()));

            VillagerEntity shepherd = EntityType.VILLAGER.create(world);
            BlockPos at = context.getAbsolutePos(new BlockPos(12, Y + 1, 12));
            shepherd.refreshPositionAndAngles(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
            world.spawnEntity(shepherd);

            Arena arena = new Arena(context, world, colony, shepherd, new ArrayList<>());

            for (int i = 0; i < sheep; i++) {
                arena.animal(EntityType.SHEEP, new BlockPos(3 + i % 3, Y + 1, 3 + i / 3));
            }

            return arena;
        }

        <T extends AnimalEntity> T animal(EntityType<T> type, BlockPos relative) {
            T animal = type.create(world);
            BlockPos at = context.getAbsolutePos(relative);
            animal.refreshPositionAndAngles(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
            animal.setAiDisabled(true);
            world.spawnEntity(animal);
            animals.add(animal);

            return animal;
        }

        void cleanUp() {
            animals.forEach(AnimalEntity::discard);
            shepherd.discard();
            BlockPos low = context.getAbsolutePos(new BlockPos(0, Y, 0));
            BlockPos high = context.getAbsolutePos(new BlockPos(28, Y + 2, 28));

            world.getEntitiesByClass(LeashKnotEntity.class, new Box(low.toCenterPos(), high.toCenterPos()), knot -> true)
                    .forEach(LeashKnotEntity::discard);

            for (BlockPos at : BlockPos.iterate(low, high)) {
                world.setBlockState(at, Blocks.AIR.getDefaultState(), Block.FORCE_STATE);
            }
        }
    }
}
