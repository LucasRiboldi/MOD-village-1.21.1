package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.UUID;

/**
 * As correções do deserto jogado de 2026-10-21 — pedido do autor: o mineiro que mora abaixo da mina
 * sobe até ela, a grama nasce no curral, o viveiro fica perto do baú do lenhador e terracota, feno e
 * argila aparecem quando a obra pede. Cada caso com o controle.
 */
public class DesertFixesGameTest implements FabricGameTest {

    /** Quem encalhou abaixo do caminho só sai perto da altura dele; sem caminho, como antes. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "desert_fixes")
    public void aWorkerStrandedBelowItsWayIsOutOnlyNearItsHeight(TestContext context) {
        UUID colonyId = UUID.randomUUID();
        UUID below = UUID.randomUUID();
        UUID plain = UUID.randomUUID();
        BlockPos feet = context.getAbsolutePos(new BlockPos(2, 1, 2));

        VillageColonyMod.WORKERS.register(below, colonyId);
        VillageColonyMod.WORKERS.register(plain, colonyId);

        try {
            StrandedWorkers.strandBelow(below, feet, feet.up(20), "test", 0L);
            StrandedWorkers.strandNow(plain, feet, "test");

            context.assertFalse(StrandedWorkers.reachedTheWay(below, feet),
                    "20 abaixo do caminho foi dado como fora");
            context.assertTrue(StrandedWorkers.reachedTheWay(below, feet.up(19)),
                    "a um da altura do caminho continuou preso");
            context.assertTrue(StrandedWorkers.wayOf(below).equals(Optional.of(feet.up(20))),
                    "a subida não mira o caminho dele");
            context.assertTrue(StrandedWorkers.reachedTheWay(plain, feet),
                    "controle: o encalhado sem caminho deixou de sair como antes");
        } finally {
            StrandedWorkers.forget(below);
            StrandedWorkers.forget(plain);
            VillageColonyMod.WORKERS.remove(below);
            VillageColonyMod.WORKERS.remove(plain);
        }

        context.complete();
    }

    /** A subida que não sobe tem prazo; quem subiu não larga; quem largou descansa dela. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "desert_fixes")
    public void aClimbThatGainsNothingEndsAndRests(TestContext context) {
        UUID colonyId = UUID.randomUUID();
        UUID stuck = UUID.randomUUID();
        UUID climbing = UUID.randomUUID();
        BlockPos feet = context.getAbsolutePos(new BlockPos(2, 1, 2));
        long deadline = StrandedWorkers.CLIMB_DEADLINE;

        VillageColonyMod.WORKERS.register(stuck, colonyId);
        VillageColonyMod.WORKERS.register(climbing, colonyId);

        try {
            StrandedWorkers.strandBelow(stuck, feet, feet.up(20), "test", 0L);
            StrandedWorkers.strandBelow(climbing, feet, feet.up(20), "test", 0L);

            context.assertFalse(StrandedWorkers.climbGaveUp(stuck, feet, deadline - 1),
                    "largou a subida antes do prazo");
            context.assertFalse(StrandedWorkers.climbGaveUp(climbing, feet.up(2), deadline),
                    "controle: quem subiu dois blocos largou a subida");
            context.assertTrue(StrandedWorkers.climbGaveUp(stuck, feet, deadline),
                    "no prazo, sem subir, não largou a subida");

            StrandedWorkers.release(stuck);
            context.assertFalse(StrandedWorkers.strandBelow(stuck, feet, feet.up(20), "test", deadline + 1),
                    "voltou à subida no descanso dela");
        } finally {
            StrandedWorkers.forget(stuck);
            StrandedWorkers.forget(climbing);
            VillageColonyMod.WORKERS.remove(stuck);
            VillageColonyMod.WORKERS.remove(climbing);
        }

        context.complete();
    }

    /** A ovelha no curral ganha grama no chão dele, debaixo dela. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "desert_fixes")
    public void grassGrowsOnThePenFloorUnderTheSheep(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos feet = context.getAbsolutePos(new BlockPos(1, 4, 1)).add(1_000_000, 0, 2_000_000);
        world.getChunk(feet);

        for (BlockPos pos : BlockPos.iterate(feet.add(-3, -2, -3), feet.add(3, -1, 3))) {
            world.setBlockState(pos, Blocks.SAND.getDefaultState());
        }

        try {
            Optional<BlockPos> grass = PostGrass.inPen(world, feet);

            context.assertTrue(grass.equals(Optional.of(feet.down()))
                            && world.getBlockState(feet.down()).isOf(Blocks.GRASS_BLOCK),
                    "não nasceu grama debaixo da ovelha no curral: " + grass);
        } finally {
            for (BlockPos pos : BlockPos.iterate(feet.add(-3, -2, -3), feet.add(3, 2, 3))) {
                world.setBlockState(pos, Blocks.AIR.getDefaultState());
            }
        }

        context.complete();
    }

    /** O viveiro perto do baú fica no anel e na altura dele; sem chão nessa altura, nada. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "desert_fixes")
    public void theNurseryNearTheChestStaysAtTheChestHeight(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos chest = context.getAbsolutePos(new BlockPos(1, 8, 1)).add(1_000_000, 0, 2_100_000);
        BlockPos high = chest.add(0, 0, 80);

        loadAround(world, chest);
        loadAround(world, high);
        floor(world, chest, -1);
        floor(world, high, LumberjackNursery.NEAR_CHEST_MAX);

        com.villagecolony.core.colony.model.Colony colony = com.villagecolony.core.colony.model.Colony.create(
                UUID.randomUUID(), com.villagecolony.fabric.adapter.MinecraftTypeAdapter.toColonyPos(chest));
        colony.measure(new com.villagecolony.core.colony.model.VillageBounds(chest.getX() - 20, chest.getY() - 8,
                chest.getZ() - 20, chest.getX() + 20, chest.getY() + 8, high.getZ() + 20));
        VillageColonyMod.COLONIES.register(colony);
        UUID outside = UUID.randomUUID();

        try {
            Optional<BlockPos> spot = LumberjackNursery.spotNearTheChest(world, colony.id(), chest);

            context.assertTrue(spot.isPresent(), "não achou viveiro perto do baú");
            double distance = Math.sqrt(Math.pow(spot.get().getX() - chest.getX(), 2)
                    + Math.pow(spot.get().getZ() - chest.getZ(), 2));
            context.assertTrue(distance >= LumberjackNursery.NEAR_CHEST_MIN - 1
                            && distance <= LumberjackNursery.NEAR_CHEST_MAX + 1
                            && Math.abs(spot.get().getY() - chest.getY()) <= 3,
                    "o viveiro ficou fora do anel ou da altura do baú: " + spot.get().subtract(chest));

            // Controle: o chão bem acima do baú não serve.
            context.assertTrue(LumberjackNursery.spotNearTheChest(world, colony.id(), high).isEmpty(),
                    "o viveiro nasceu longe da altura do baú");
            // Controle: colônia sem caixa medida não usa o anel.
            context.assertTrue(LumberjackNursery.spotNearTheChest(world, outside, chest).isEmpty(),
                    "o viveiro do anel nasceu sem caixa da vila");
        } finally {
            VillageColonyMod.COLONIES.remove(colony.id());
            clear(world, chest);
            clear(world, high);
        }

        context.complete();
    }

    /**
     * O gelo e a neve aparecem quando a obra os pede — T1 (autor, 2026-10-29: "a neve deve aparecer automático"; o gelo
     * só sai com toque suave e a neve travava a obra: 16 registros de obra parada por neve nas simulações).
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "desert_fixes")
    public void iceAndSnowAppearWhenTheBuildAsks(TestContext context) {
        context.assertTrue(AppearingPieces.appears(Blocks.PACKED_ICE)
                        && AppearingPieces.appears(Blocks.ICE)
                        && AppearingPieces.appears(Blocks.BLUE_ICE),
                "o gelo devia aparecer: nenhuma profissão o obtém sem toque suave");
        context.assertTrue(AppearingPieces.appears(Blocks.SNOW_BLOCK) && AppearingPieces.appears(Blocks.SNOW),
                "a neve devia aparecer quando a obra a pede");
        context.complete();
    }

    /** Terracota, feno e argila aparecem; tábua não. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "desert_fixes")
    public void terracottaHayAndClayAppearWhenAsked(TestContext context) {
        context.assertTrue(AppearingPieces.appears(Blocks.LIME_TERRACOTTA)
                        && AppearingPieces.appears(Blocks.TERRACOTTA)
                        && AppearingPieces.appears(Blocks.WHITE_GLAZED_TERRACOTTA)
                        && AppearingPieces.appears(Blocks.HAY_BLOCK)
                        && AppearingPieces.appears(Blocks.CLAY),
                "terracota, feno ou argila não aparecem");
        context.assertFalse(AppearingPieces.appears(Blocks.OAK_PLANKS)
                        || AppearingPieces.appears(Blocks.SANDSTONE),
                "controle: tábua ou arenito passaram a aparecer");
        context.complete();
    }

    private static void loadAround(ServerWorld world, BlockPos at) {
        for (int dx = -32; dx <= 32; dx += 16) {
            for (int dz = -32; dz <= 32; dz += 16) {
                world.getChunk(at.add(dx, 0, dz));
            }
        }
    }

    private static void floor(ServerWorld world, BlockPos chest, int dy) {
        int r = LumberjackNursery.NEAR_CHEST_MAX + 2;

        for (BlockPos pos : BlockPos.iterate(chest.add(-r, dy, -r), chest.add(r, dy, r))) {
            world.setBlockState(pos, Blocks.GRASS_BLOCK.getDefaultState());
        }
    }

    private static void clear(ServerWorld world, BlockPos chest) {
        int r = LumberjackNursery.NEAR_CHEST_MAX + 2;

        for (BlockPos pos : BlockPos.iterate(chest.add(-r, -2, -r), chest.add(r, LumberjackNursery.NEAR_CHEST_MAX + 2, r))) {
            world.setBlockState(pos, Blocks.AIR.getDefaultState());
        }
    }
}
