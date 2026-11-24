package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.type.ResourceId;
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

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "shepherd_herding", tickLimit = 300)
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

        // No chão da arena, encostada no portão: ela entra andando, não por teleporte.
        CowEntity cow = animalAt(world, EntityType.COW, pen.outside());

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

                // A vaca entra andando pelo portão aberto, sem teleporte — MAE, escolha do autor de 2026-11-04.
                cow.setAiDisabled(false);
                ShepherdPens.startIn(world, pen, cow);
                walkIn(context, world, pen, cow, sheep, ShepherdPens.WALK_LIMIT);
            } catch (RuntimeException | AssertionError failure) {
                cleanUp(context, world, sheep, cow);
                throw failure;
            }
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

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "shepherd_registered_priority", tickLimit = 40)
    public void reachableLargePenWinsAndBlockedLargeFallsBackByBlueprint(TestContext context) {
        var world = context.getWorld();
        var near = context.getAbsolutePos(new BlockPos(4, Y2 + 1, 1));
        var far = near.east(10);
        var colony = twoPens(world, near, far);
        var medium = registerPen(colony, near, new ResourceId("villagecolony", "colony/medium_animal_pen_poultry"));
        var large = registerPen(colony, far, PenPlans.MODEL);
        var shepherd = animalFree(world, near.north());
        var rabbit = animalAt(world, EntityType.RABBIT, far.south(2));
        var sheep = animalAt(world, EntityType.SHEEP, far.south(3));
        try {
            context.assertTrue(ShepherdPens.forSpecies(world, colony, EntityType.CHICKEN, shepherd, null, false)
                    .map(pen -> pen.gate().equals(far)).orElse(false), "o médio próximo ganhou do grande acessível");
            context.assertTrue(PenRegroup.separate(world, colony, shepherd).isEmpty(),
                    "o pastor tentou separar o coelho e a ovelha do grande");
            world.setBlockState(far.up(), Blocks.STONE.getDefaultState());
            for (var species : List.of(EntityType.CHICKEN, EntityType.RABBIT)) {
                context.assertTrue(ShepherdPens.forSpecies(world, colony, species, shepherd, null, false)
                        .map(pen -> pen.gate().equals(near)).orElse(false), "o médio de aves não recebeu " + species);
            }
            context.assertTrue(ShepherdPens.forSpecies(world, colony, EntityType.SHEEP, shepherd, null, false).isEmpty(),
                    "o médio vazio de aves recebeu uma ovelha");
        } finally {
            VillageColonyMod.BUILDINGS.remove(medium.id());
            VillageColonyMod.BUILDINGS.remove(large.id());
            clearTwoPens(world, near, far, List.of(shepherd, rabbit, sheep));
        }
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "shepherd_registered_medium", tickLimit = 40)
    public void mediumRegroupsOnlyMisplacedAnimalsAndVanillaKeepsItsResidents(TestContext context) {
        var world = context.getWorld();
        var near = context.getAbsolutePos(new BlockPos(4, Y2 + 1, 1));
        var far = near.east(10);
        var colony = twoPens(world, near, far);
        var poultry = registerPen(colony, near, new ResourceId("villagecolony", "colony/medium_animal_pen_poultry"));
        var cattle = registerPen(colony, far, new ResourceId("villagecolony", "colony/medium_animal_pen_cattle"));
        var shepherd = animalFree(world, near.north());
        var chicken = animalAt(world, EntityType.CHICKEN, near.south(2));
        var rabbit = animalAt(world, EntityType.RABBIT, near.south(3));
        var cow = animalAt(world, EntityType.MOOSHROOM, near.south(4));
        try {
            var move = PenRegroup.separate(world, colony, shepherd);
            context.assertTrue(move.isPresent() && move.get().animal.equals(cow.getUuid())
                    && move.get().fence.equals(far), "só a coguvaca deve sair para o médio de bovinos");
            VillageColonyMod.BUILDINGS.remove(poultry.id());
            var vanilla = registerPen(colony, near, ResourceId.vanilla("village/plains/houses/plains_animal_pen_1"));
            try {
                ShepherdPens.clearAll();
                PenRegroup.clearAll();
                context.assertTrue(PenRegroup.separate(world, colony, shepherd).isEmpty(), "tentou separar o curral Vanilla");
            } finally {
                VillageColonyMod.BUILDINGS.remove(vanilla.id());
            }
        } finally {
            VillageColonyMod.BUILDINGS.remove(poultry.id());
            VillageColonyMod.BUILDINGS.remove(cattle.id());
            clearTwoPens(world, near, far, List.of(shepherd, chicken, rabbit, cow));
        }
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "shepherd_medium_groups", tickLimit = 40)
    public void looseHerdUsesOnlyTheRegisteredMediumGroup(TestContext context) {
        var world = context.getWorld();
        var near = context.getAbsolutePos(new BlockPos(4, Y2 + 1, 1));
        var far = near.east(10);
        var colony = twoPens(world, near, far);
        world.setBlockState(far.up(), Blocks.STONE.getDefaultState());
        var shepherd = animalFree(world, near.north(3));
        var pig = animalAt(world, EntityType.PIG, near.north(2));
        var horse = animalAt(world, EntityType.HORSE, near.east(5).north(3));
        var llama = animalAt(world, EntityType.LLAMA, far.north(3));
        try {
            var candidates = HerdCandidates.of(world, colony, shepherd);
            context.assertTrue(candidates.contains(horse) && candidates.contains(llama),
                    "a coleta deve incluir cavalos e lhamas soltos");
            var groups = java.util.Map.of(
                    "cattle", List.of(EntityType.COW, EntityType.MOOSHROOM),
                    "sheep", List.of(EntityType.SHEEP), "pig", List.of(EntityType.PIG),
                    "poultry", List.of(EntityType.CHICKEN, EntityType.RABBIT),
                    "horse", List.of(EntityType.HORSE), "camelid", List.of(EntityType.LLAMA, EntityType.GOAT));
            for (var group : groups.entrySet()) {
                var building = registerPen(colony, near,
                        new ResourceId("villagecolony", "colony/medium_animal_pen_" + group.getKey()));
                try {
                    ShepherdPens.clearAll();
                    for (var types : groups.values()) {
                        for (var type : types) {
                            boolean accepts = ShepherdPens.forSpecies(world, colony, type, near.north(), null, false).isPresent();
                            context.assertTrue(accepts == group.getValue().contains(type),
                                    group.getKey() + " escolheu incorretamente para " + type);
                        }
                    }
                    if (group.getKey().equals("horse")) {
                        context.assertTrue(ShepherdHerding.start(world, colony, shepherd)
                                .filter(herd -> herd.animal.equals(horse.getUuid())).isPresent(),
                                "um porco mais perto sem destino impediu o recolhimento do cavalo");
                    }
                } finally {
                    VillageColonyMod.BUILDINGS.remove(building.id());
                }
            }
        } finally {
            clearTwoPens(world, near, far, List.of(shepherd, pig, horse, llama));
        }
        context.complete();
    }

    private static Building registerPen(Colony colony, BlockPos gate, ResourceId model) {
        Building building = new Building(UUID.randomUUID(), colony.id(), model,
                MinecraftTypeAdapter.toColonyPos(gate.add(-3, -1, 0)), MinecraftTypeAdapter.toColonyPos(gate.add(3, 2, 6)));
        VillageColonyMod.BUILDINGS.register(building);
        return building;
    }

    private static Colony twoPens(ServerWorld world, BlockPos near, BlockPos far) {
        BlockPos low = near.add(-7, -1, -30);
        BlockPos high = far.add(7, -1, 11);
        for (var at : BlockPos.iterate(low, high)) world.setBlockState(at, Blocks.GRASS_BLOCK.getDefaultState());
        for (var gate : List.of(near, far)) {
            for (int x = -3; x <= 3; x++) {
                for (int z = 0; z <= 6; z++) {
                    if (Math.abs(x) == 3 || z == 0 || z == 6) {
                        world.setBlockState(gate.add(x, 0, z), Blocks.OAK_FENCE.getDefaultState());
                    }
                }
            }
            world.setBlockState(gate, Blocks.OAK_FENCE_GATE.getDefaultState());
        }
        var colony = Colony.create(UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(near));
        colony.measure(new VillageBounds(low.getX(), low.getY(), low.getZ(), high.getX(), high.getY() + 4, high.getZ()));
        return colony;
    }

    private static void clearTwoPens(ServerWorld world, BlockPos near, BlockPos far,
                                     List<? extends net.minecraft.entity.Entity> spawned) {
        spawned.forEach(net.minecraft.entity.Entity::discard);
        ShepherdPens.clearAll();
        for (var at : BlockPos.iterate(near.add(-7, -1, -30), far.add(7, 1, 11))) {
            world.setBlockState(at, Blocks.AIR.getDefaultState());
        }
    }

    /**
     * A lotação é pelo tamanho — autor, 2026-10-22: um animal a cada {@link ShepherdPens#FLOOR_PER_ANIMAL}
     * blocos de chão. O curral de 7×7 por dentro leva 12: com 8 ainda cabe (os 5 fixos de antes o davam
     * por cheio), com 12 está cheio, e sendo o único, a vila tem todos os currais cheios.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "shepherd_pens_size_the_bigger_pen_holds_more_animals", tickLimit = 40)
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

    /** Um passo da entrada andando; depois de dentro, o portão fecha e ela continua lá. */
    private static void walkIn(TestContext context, ServerWorld world, ShepherdPens.Pen pen, CowEntity cow,
                               List<SheepEntity> sheep, int left) {
        try {
            if (ShepherdPens.isInside(pen, cow)) {
                FencedIn.closeWhenClear(pen.gate());
                settle(context, world, pen, cow, sheep, 40);
                return;
            }
            context.assertTrue(left > 0, "a vaca não entrou andando: " + cow.getBlockPos().toShortString()
                    + " portão=" + pen.gate().toShortString() + " aberto=" + world.getBlockState(pen.gate()).get(net.minecraft.block.FenceGateBlock.OPEN)
                    + " dentro=" + pen.inside().toShortString() + " fora=" + pen.outside().toShortString()
                    + " parada=" + cow.getNavigation().isIdle() + " caminho=" + cow.getNavigation().getCurrentPath());
            ShepherdPens.walkThrough(world, pen, cow, ShepherdPens.deeper(pen));
            context.waitAndRun(1, () -> walkIn(context, world, pen, cow, sheep, left - 1));
        } catch (RuntimeException | AssertionError failure) {
            cleanUp(context, world, sheep, cow);
            throw failure;
        }
    }

    /** Depois da entrada, o portão fecha assim que fica livre, e a vaca, solta, não sai mais. */
    private static void settle(TestContext context, ServerWorld world, ShepherdPens.Pen pen, CowEntity cow,
                               List<SheepEntity> sheep, int left) {
        try {
            FencedIn.tick(world);
            if (left > 0) {
                context.waitAndRun(1, () -> settle(context, world, pen, cow, sheep, left - 1));
                return;
            }
            context.assertFalse(world.getBlockState(pen.gate()).get(net.minecraft.block.FenceGateBlock.OPEN),
                    "o portão ficou aberto depois da entrada");
            context.assertTrue(ShepherdPens.isInside(pen, cow), "a vaca escapou depois da entrada");
            cleanUp(context, world, sheep, cow);
            context.complete();
        } catch (RuntimeException | AssertionError failure) {
            cleanUp(context, world, sheep, cow);
            throw failure;
        }
    }

    private static void cleanUp(TestContext context, ServerWorld world, List<SheepEntity> sheep, CowEntity cow) {
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
        villager.setOnGround(true);
        world.spawnEntity(villager);

        return villager;
    }
}
