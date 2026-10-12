package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.ColonyChests;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * O pastor e os cercados — regra do autor, 2026-10-11 ({@link ShepherdPens}): o cercado
 * pelo portão, mais de 5 animais e ele tenta outro, o animal entra pelo portão, e sem
 * cerca nenhuma ele põe um poste ao lado da casa com a cerca do baú.
 */
public class ShepherdPensGameTest implements FabricGameTest {

    private static final int Y = 60;

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "shepherd_herding", tickLimit = 40)
    public void theShepherdFindsThePenAndFillsItUpToFive(TestContext context) {
        ServerWorld world = context.getWorld();
        List<SheepEntity> sheep = new ArrayList<>();

        // Chão 29×29 (o lado de fora não fica cercado pelo vazio) e um cercado de 5×5
        // por dentro, com portão no lado norte.
        for (int x = 0; x <= 28; x++) {
            for (int z = 0; z <= 28; z++) {
                world.setBlockState(context.getAbsolutePos(new BlockPos(x, Y, z)), Blocks.GRASS_BLOCK.getDefaultState());

                boolean ring = (x == 1 || x == 7 || z == 1 || z == 7) && x >= 1 && x <= 7 && z >= 1 && z <= 7;

                if (ring) {
                    world.setBlockState(context.getAbsolutePos(new BlockPos(x, Y + 1, z)), Blocks.OAK_FENCE.getDefaultState());
                }
            }
        }

        BlockPos gate = context.getAbsolutePos(new BlockPos(4, Y + 1, 1));
        world.setBlockState(gate, Blocks.OAK_FENCE_GATE.getDefaultState());

        BlockPos low = context.getAbsolutePos(new BlockPos(0, Y - 2, 0));
        BlockPos high = context.getAbsolutePos(new BlockPos(28, Y + 4, 28));
        VillageBounds box = new VillageBounds(low.getX(), low.getY(), low.getZ(), high.getX(), high.getY(), high.getZ());
        Colony colony = Colony.create(UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(gate));
        colony.measure(box);

        List<ShepherdPens.Pen> pens = ShepherdPens.scan(world, box);

        context.assertTrue(pens.size() == 1 && pens.get(0).gate().equals(gate),
                "o cercado não foi achado pelo portão: " + pens);

        ShepherdPens.Pen pen = pens.get(0);

        for (int i = 0; i < 6; i++) {
            sheep.add(animalAt(world, EntityType.SHEEP, pen.inside().add(i % 3, 0, 1 + i / 3)));
        }

        CowEntity cow = animalAt(world, EntityType.COW, pen.outside().north());

        context.waitAndRun(5, () -> {
            try {
                context.assertTrue(ShepherdPens.animalsIn(world, pen) == 6, "contou " + ShepherdPens.animalsIn(world, pen)
                        + " ovelha0=" + sheep.get(0).getBlockPos() + " vivo=" + sheep.get(0).isAlive()
                        + " dentro=" + pen.inside() + " cercado=" + pen.reach().size()
                        + " contém=" + pen.reach().contains(sheep.get(0).getBlockPos()));
                context.assertTrue(ShepherdPens.withRoom(world, colony, gate).isEmpty(),
                        "com 6 animais o cercado não devia aceitar mais");

                sheep.remove(5).discard();
                context.assertTrue(ShepherdPens.withRoom(world, colony, gate).isPresent(),
                        "com 5 animais o cercado ainda aceita um");

                ShepherdPens.bringIn(world, pen, cow);

                context.assertTrue(pen.reach().contains(cow.getBlockPos()), "a vaca não entrou no cercado: " + cow.getBlockPos());
            } finally {
                sheep.forEach(SheepEntity::discard);
                cow.discard();
                ShepherdPens.clearAll();

                for (int x = 0; x <= 28; x++) {
                    for (int z = 0; z <= 28; z++) {
                        world.setBlockState(context.getAbsolutePos(new BlockPos(x, Y + 1, z)), Blocks.AIR.getDefaultState());
                        world.setBlockState(context.getAbsolutePos(new BlockPos(x, Y, z)), Blocks.AIR.getDefaultState());
                    }
                }
            }

            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "shepherd_herding", tickLimit = 40)
    public void withoutAFenceTheShepherdPutsAPostBesideHisHouse(TestContext context) {
        ServerWorld world = context.getWorld();
        int y = Y + 10;

        for (int x = 0; x <= 6; x++) {
            for (int z = 0; z <= 6; z++) {
                world.setBlockState(context.getAbsolutePos(new BlockPos(x, y, z)), Blocks.STONE.getDefaultState());
            }
        }

        BlockPos chestAt = context.getAbsolutePos(new BlockPos(0, y + 1, 0));
        world.setBlockState(chestAt, Blocks.CHEST.getDefaultState());
        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(chestAt);
        ChestDepositor.deposit(world, chest, Items.OAK_FENCE, 2);
        Colony colony = Colony.create(UUID.randomUUID(), chest);
        UUID shepherd = UUID.randomUUID();
        BlockPos home = context.getAbsolutePos(new BlockPos(3, y + 1, 3));

        VillageColonyMod.COLONIES.register(colony);
        VillageColonyMod.WORKERS.register(shepherd, colony.id()).assign(ProfessionType.SHEPHERD);
        VillageColonyMod.STORAGES.register(WorkerStorage.of(shepherd, chest));

        try {
            context.assertTrue(ShepherdHerding.nearestFence(world, home).isEmpty(), "o cenário já tem cerca");

            Optional<BlockPos> spot = ShepherdPens.postBesideHome(world, colony.id(), home);

            context.assertTrue(spot.isPresent(), "com cerca no baú, devia haver lugar para o poste ao lado da casa");
            // A coleta sai com o poste novo como destino.
            VillagerEntity keeper = animalFree(world, home);
            CowEntity loose = animalAt(world, EntityType.COW, home.add(2, 0, 2));

            try {
                VillageColonyMod.WORKERS.register(keeper.getUuid(), colony.id()).assign(ProfessionType.SHEPHERD);
                VillageColonyMod.STORAGES.register(WorkerStorage.of(keeper.getUuid(), chest));

                Optional<ShepherdHerding.Herd> herd = ShepherdHerding.start(world, colony, keeper);

                context.assertTrue(herd.isPresent() && herd.get().newPost && herd.get().pen == null,
                        "sem cerca nenhuma, a coleta devia ir para um poste novo: " + herd.map(h -> h.fence));
            } finally {
                VillageColonyMod.STORAGES.remove(keeper.getUuid());
                VillageColonyMod.WORKERS.remove(keeper.getUuid());
                keeper.discard();
                loose.discard();
            }

            context.assertTrue(ShepherdPens.placePost(world, colony.id(), spot.get())
                            && world.getBlockState(spot.get()).isIn(BlockTags.FENCES),
                    "o poste não foi posto");
            context.assertTrue(ColonyChests.countIn(world, List.of(chest), Items.OAK_FENCE) == 1,
                    "a cerca do poste devia sair do baú");
            // Um poste por casa: com ele de pé, não há lugar para outro, mesmo com cerca no baú.
            context.assertTrue(ShepherdPens.postBesideHome(world, colony.id(), home).isEmpty(),
                    "a casa ganharia um segundo poste");
        } finally {
            VillageColonyMod.STORAGES.remove(shepherd);
            VillageColonyMod.WORKERS.remove(shepherd);
            VillageColonyMod.COLONIES.remove(colony.id());

            for (int x = 0; x <= 6; x++) {
                for (int z = 0; z <= 6; z++) {
                    world.setBlockState(context.getAbsolutePos(new BlockPos(x, y + 1, z)), Blocks.AIR.getDefaultState());
                    world.setBlockState(context.getAbsolutePos(new BlockPos(x, y, z)), Blocks.AIR.getDefaultState());
                }
            }
        }

        context.complete();
    }

    private static <T extends net.minecraft.entity.passive.AnimalEntity> T animalAt(
            ServerWorld world, EntityType<T> type, BlockPos at) {
        T animal = type.create(world);

        animal.refreshPositionAndAngles(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        animal.setAiDisabled(true);
        world.spawnEntity(animal);

        return animal;
    }

    private static VillagerEntity animalFree(ServerWorld world, BlockPos at) {
        VillagerEntity villager = EntityType.VILLAGER.create(world);

        villager.refreshPositionAndAngles(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        villager.setAiDisabled(true);
        world.spawnEntity(villager);

        return villager;
    }
}
