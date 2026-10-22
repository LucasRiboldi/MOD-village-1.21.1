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
 * pelo portão, cheio ele tenta outro (a lotação é pelo tamanho, 2026-10-22), o animal entra
 * pelo portão, e sem cerca nenhuma ele põe um poste ao lado da casa com a cerca do baú.
 */
public class ShepherdPensGameTest implements FabricGameTest {

    private static final int Y = 60;

    /** Os currais por espécie, em outra altura: o chão de 29×29 passa da arena. */
    private static final int Y2 = 90;

    /** O curral grande, numa terceira altura. */
    private static final int Y3 = 120;

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

    /**
     * Um curral por espécie — Regra 72, autor de 2026-10-22: com dois currais, a vaca vai para o da vaca e
     * a ovelha para o vazio (mesmo mais longe), e a ovelha de um curral misto sai pelo portão para o dela.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "shepherd_pens_species", tickLimit = 40)
    public void eachSpeciesGetsItsOwnPen(TestContext context) {
        ServerWorld world = context.getWorld();

        for (int x = 0; x <= 28; x++) {
            for (int z = 0; z <= 28; z++) {
                world.setBlockState(context.getAbsolutePos(new BlockPos(x, Y2, z)), Blocks.GRASS_BLOCK.getDefaultState());

                boolean ringA = (x == 1 || x == 7 || z == 1 || z == 7) && x >= 1 && x <= 7 && z >= 1 && z <= 7;
                boolean ringB = (x == 11 || x == 17 || z == 1 || z == 7) && x >= 11 && x <= 17 && z >= 1 && z <= 7;

                if (ringA || ringB) {
                    world.setBlockState(context.getAbsolutePos(new BlockPos(x, Y2 + 1, z)), Blocks.OAK_FENCE.getDefaultState());
                }
            }
        }

        BlockPos gateA = context.getAbsolutePos(new BlockPos(4, Y2 + 1, 1));
        BlockPos gateB = context.getAbsolutePos(new BlockPos(14, Y2 + 1, 1));
        world.setBlockState(gateA, Blocks.OAK_FENCE_GATE.getDefaultState());
        world.setBlockState(gateB, Blocks.OAK_FENCE_GATE.getDefaultState());

        BlockPos low = context.getAbsolutePos(new BlockPos(0, Y2 - 2, 0));
        BlockPos high = context.getAbsolutePos(new BlockPos(28, Y2 + 4, 28));
        Colony colony = Colony.create(UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(gateA));
        colony.measure(new VillageBounds(low.getX(), low.getY(), low.getZ(), high.getX(), high.getY(), high.getZ()));

        List<ShepherdPens.Pen> pens = ShepherdPens.pensOf(world, colony);
        ShepherdPens.Pen penA = pens.stream().filter(pen -> pen.gate().equals(gateA)).findFirst().orElseThrow();
        ShepherdPens.Pen penB = pens.stream().filter(pen -> pen.gate().equals(gateB)).findFirst().orElseThrow();
        BlockPos home = gateA.north(2);

        CowEntity cow = animalAt(world, EntityType.COW, penA.inside().south());
        List<net.minecraft.entity.Entity> spawned = new ArrayList<>(List.of(cow));

        context.waitAndRun(5, () -> {
            try {
                context.assertTrue(ShepherdPens.forSpecies(world, colony, EntityType.COW, home, null, false)
                                .map(penA::equals).orElse(false),
                        "a vaca devia ir para o curral da vaca");
                context.assertTrue(ShepherdPens.forSpecies(world, colony, EntityType.SHEEP, home, null, false)
                                .map(penB::equals).orElse(false),
                        "a ovelha devia ir para o curral vazio, e não para o da vaca, mais perto");

                SheepEntity sheep = animalAt(world, EntityType.SHEEP, penA.inside().south().east());
                spawned.add(sheep);

                context.assertTrue(ShepherdPens.speciesIn(world, penA).size() == 2, "o curral A devia estar misto");
                context.assertTrue(ShepherdPens.forSpecies(world, colony, EntityType.SHEEP, home, penA, true)
                                .map(penB::equals).orElse(false),
                        "a separação devia mandar a ovelha do curral misto para o vazio");

                VillagerEntity shepherd = animalFree(world, penA.outside());
                spawned.add(shepherd);
                ShepherdPens.bringOut(world, penA, sheep, shepherd);

                context.assertFalse(penA.reach().contains(sheep.getBlockPos()), "a ovelha não saiu do curral misto");
                context.assertTrue(sheep.getLeashHolder() == shepherd, "a ovelha não ficou na corda do pastor");
            } finally {
                spawned.forEach(net.minecraft.entity.Entity::discard);
                ShepherdPens.clearAll();

                for (int x = 0; x <= 28; x++) {
                    for (int z = 0; z <= 28; z++) {
                        world.setBlockState(context.getAbsolutePos(new BlockPos(x, Y2 + 1, z)), Blocks.AIR.getDefaultState());
                        world.setBlockState(context.getAbsolutePos(new BlockPos(x, Y2, z)), Blocks.AIR.getDefaultState());
                    }
                }
            }

            context.complete();
        });
    }

    /**
     * A lotação é pelo tamanho — autor, 2026-10-22: um animal a cada {@link ShepherdPens#FLOOR_PER_ANIMAL}
     * blocos de chão. O curral de 7×7 por dentro leva 12: com 8 ainda cabe (os 5 fixos de antes o davam
     * por cheio), com 12 está cheio, e sendo o único, a vila tem todos os currais cheios.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "shepherd_pens_size", tickLimit = 40)
    public void theBiggerPenHoldsMoreAnimals(TestContext context) {
        ServerWorld world = context.getWorld();
        List<SheepEntity> sheep = new ArrayList<>();

        for (int x = 0; x <= 28; x++) {
            for (int z = 0; z <= 28; z++) {
                world.setBlockState(context.getAbsolutePos(new BlockPos(x, Y3, z)), Blocks.GRASS_BLOCK.getDefaultState());

                if ((x == 1 || x == 9 || z == 1 || z == 9) && x >= 1 && x <= 9 && z >= 1 && z <= 9) {
                    world.setBlockState(context.getAbsolutePos(new BlockPos(x, Y3 + 1, z)), Blocks.OAK_FENCE.getDefaultState());
                }
            }
        }

        BlockPos gate = context.getAbsolutePos(new BlockPos(5, Y3 + 1, 1));
        world.setBlockState(gate, Blocks.OAK_FENCE_GATE.getDefaultState());

        BlockPos low = context.getAbsolutePos(new BlockPos(0, Y3 - 2, 0));
        BlockPos high = context.getAbsolutePos(new BlockPos(28, Y3 + 4, 28));
        VillageBounds box = new VillageBounds(low.getX(), low.getY(), low.getZ(), high.getX(), high.getY(), high.getZ());
        Colony colony = Colony.create(UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(gate));
        colony.measure(box);

        List<ShepherdPens.Pen> pens = ShepherdPens.scan(world, box);
        ShepherdPens.Pen pen = pens.get(0);

        for (int i = 0; i < 12; i++) {
            sheep.add(animalAt(world, EntityType.SHEEP, pen.inside().add(i % 4 - 1, 0, 1 + i / 4)));
        }

        context.waitAndRun(5, () -> {
            try {
                context.assertTrue(ShepherdPens.capacityOf(pen) == 12,
                        "o curral de 49 de chão devia levar 12, leva " + ShepherdPens.capacityOf(pen));
                context.assertTrue(ShepherdPens.animalsIn(world, pen) == 12, "contou " + ShepherdPens.animalsIn(world, pen));
                context.assertTrue(ShepherdPens.allFull(world, colony), "com 12 no curral de 12 a vila devia estar cheia");

                for (int i = 0; i < 4; i++) {
                    sheep.remove(sheep.size() - 1).discard();
                }

                context.assertTrue(ShepherdPens.withRoom(world, colony, gate).isPresent(),
                        "com 8 animais o curral grande ainda aceita");
                context.assertFalse(ShepherdPens.allFull(world, colony), "com lugar, a vila não está cheia");
            } finally {
                sheep.forEach(SheepEntity::discard);
                ShepherdPens.clearAll();

                for (int x = 0; x <= 28; x++) {
                    for (int z = 0; z <= 28; z++) {
                        world.setBlockState(context.getAbsolutePos(new BlockPos(x, Y3 + 1, z)), Blocks.AIR.getDefaultState());
                        world.setBlockState(context.getAbsolutePos(new BlockPos(x, Y3, z)), Blocks.AIR.getDefaultState());
                    }
                }
            }

            context.complete();
        });
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
