package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.construction.service.ConstructionService;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.work.HousePlans;
import com.villagecolony.fabric.work.PlanPlacement;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

/** Regressoes para a escolha do lote da BigHouseMOD. */
public class BigHouseFoundationGameTest implements FabricGameTest {

    /** Uma fundacao nao pode ser apoiada sobre o telhado de uma construcao. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "big_house_foundation")
    public void theFoundationDoesNotStartOnTopOfAnExistingBuilding(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = context.getAbsolutePos(new BlockPos(3, 2, 3));
        Vec3i size = new Vec3i(3, 4, 3);

        // O lote fica imediatamente acima de uma construcao existente. A
        // versao defeituosa ignorava esta camada e aceitava o lote.
        for (int dx = 0; dx < size.getX(); dx++) {
            for (int dz = 0; dz < size.getZ(); dz++) {
                world.setBlockState(
                        origin.add(dx, -1, dz), Blocks.OAK_PLANKS.getDefaultState());
            }
        }

        Colony colony = Colony.create(
                UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(origin));

        context.assertFalse(
                invokesSafe(world, colony, origin, size),
                "a BigHouseMOD foi aceita sobre o topo de uma construcao existente");
        context.complete();
    }

    /** A caixa vertical inteira precisa estar livre por 25 blocos. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "big_house_foundation")
    public void theFoundationRejectsABlockTwentyFiveAboveTheFloor(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = context.getAbsolutePos(new BlockPos(3, 2, 3));
        Vec3i size = new Vec3i(3, 4, 3);

        world.setBlockState(origin.add(1, 25, 1), Blocks.STONE.getDefaultState());

        Colony colony = Colony.create(
                UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(origin));

        context.assertFalse(
                invokesSafe(world, colony, origin, size),
                "a BigHouseMOD foi aceita com bloco na folga vertical de 25 blocos");
        context.complete();
    }

    /** Um projeto pendente do save já reserva a área antes de {@code resume}. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "big_house_foundation")
    public void aPendingProjectStillReservesItsSavedBox(TestContext context) {
        UUID colonyId = UUID.randomUUID();
        ColonyPos origin = MinecraftTypeAdapter.toColonyPos(
                context.getAbsolutePos(new BlockPos(3, 4, 3)));
        ResourceId blueprintId = ResourceId.vanilla(
                "village/plains/houses/plains_small_house_1");
        UUID projectId = UUID.randomUUID();

        Optional<com.villagecolony.core.construction.model.Blueprint> blueprint =
                PlanPlacement.blueprintOf(context.getWorld(), colonyId, blueprintId, origin);

        context.assertTrue(blueprint.isPresent(), "a planta de teste nao foi encontrada");

        ColonyPos max = new ColonyPos(
                origin.x() + blueprint.get().size().x() - 1,
                origin.y() + blueprint.get().size().y() - 1,
                origin.z() + blueprint.get().size().z() - 1);

        try {
            VillageColonyMod.CONSTRUCTIONS.registerPending(new ConstructionService.Pending(
                    projectId,
                    colonyId,
                    blueprintId,
                    origin,
                    ConstructionState.BUILDING));

            context.assertTrue(
                    LotClearance.overlapsConstructionSite(
                            context.getWorld(), colonyId, origin, max, null),
                    "o projeto pendente nao protegeu sua caixa antes de resume");

        } finally {
            VillageColonyMod.CONSTRUCTIONS.removeOfColony(colonyId);
            BuildSiteScanner.clearAll();
        }

        context.complete();
    }

    /** A casa fundacional pendente reserva a pegada antes da retomada. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "big_house_foundation")
    public void aPendingBigHouseReservesItsFootprintAtAnyHeight(TestContext context) {
        UUID colonyId = UUID.randomUUID();
        ColonyPos origin = MinecraftTypeAdapter.toColonyPos(
                context.getAbsolutePos(new BlockPos(3, 4, 3)));
        UUID projectId = UUID.randomUUID();
        var blueprint = PlanPlacement.blueprintOf(
                context.getWorld(), colonyId, StructureBlueprintReader.BIG_HOUSE_MOD, origin)
                .orElseThrow(() -> new AssertionError("a planta da BigHouseMOD nao foi encontrada"));
        ColonyPos higherMin = new ColonyPos(
                origin.x(), origin.y() + blueprint.size().y(), origin.z());
        ColonyPos higherMax = new ColonyPos(
                origin.x() + blueprint.size().x() - 1,
                higherMin.y() + 3,
                origin.z() + blueprint.size().z() - 1);

        try {
            VillageColonyMod.CONSTRUCTIONS.registerPending(new ConstructionService.Pending(
                    projectId, colonyId, StructureBlueprintReader.BIG_HOUSE_MOD,
                    origin, ConstructionState.BUILDING));

            context.assertTrue(
                    LotClearance.overlapsConstructionSite(
                            context.getWorld(), colonyId, higherMin, higherMax, null),
                    "a casa pendente nao reservou a pegada em outra altura");
        } finally {
            VillageColonyMod.CONSTRUCTIONS.removeOfColony(colonyId);
            BuildSiteScanner.clearAll();
        }

        context.complete();
    }

    /** Uma BigHouseMOD pendente nao pode ser duplicada na deteccao da vila. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "big_house_foundation")
    public void aPendingBigHouseIsNotPlacedAgain(TestContext context) {
        UUID colonyId = UUID.randomUUID();
        ColonyPos origin = MinecraftTypeAdapter.toColonyPos(
                context.getAbsolutePos(new BlockPos(3, 4, 3)));

        try {
            VillageColonyMod.CONSTRUCTIONS.registerPending(new ConstructionService.Pending(
                    UUID.randomUUID(),
                    colonyId,
                    StructureBlueprintReader.BIG_HOUSE_MOD,
                    origin,
                    ConstructionState.BUILDING));

            BigHouseFoundation.Result result = BigHouseFoundation.ensure(
                    context.getWorld(),
                    Colony.create(colonyId, origin));

            context.assertFalse(result.placed(), "a fundacao pendente foi colocada novamente");
            context.assertTrue(result.building().isEmpty(),
                    "a fundacao pendente foi registrada como uma casa nova");
        } finally {
            VillageColonyMod.CONSTRUCTIONS.removeOfColony(colonyId);
            VillageColonyMod.BUILDINGS.removeOfColony(colonyId);
        }

        context.complete();
    }

    /**
     * Retalho de 5 × 5 com desnível de 2: altura livre 2, 3 ou 4 conforme a coluna.
     *
     * <p>Fora da arena, de propósito: a fundação exige 25 blocos de ar acima do piso, e o teto de
     * barreira da arena fica a 9.
     */
    private static BlockPos unevenPatch(TestContext context) {
        BlockPos corner = context.getAbsolutePos(new BlockPos(1, 0, 1)).add(1_000_000, 0, 1_300_000);
        ServerWorld world = context.getWorld();
        world.getChunk(corner);

        for (int dx = 0; dx < 5; dx++) {
            for (int dz = 0; dz < 5; dz++) {
                int step = (dx + dz) % 3;
                world.setBlockState(corner.add(dx, 0, dz), Blocks.STONE.getDefaultState());
                world.setBlockState(corner.add(dx, 1, dz), Blocks.DIRT.getDefaultState());

                if (step >= 1) {
                    world.setBlockState(corner.add(dx, 2, dz), Blocks.DIRT.getDefaultState());
                }

                if (step == 2) {
                    world.setBlockState(corner.add(dx, 3, dz), Blocks.GRASS_BLOCK.getDefaultState());
                }
            }
        }

        return corner;
    }

    /** Quais colunas o lote recusa, para a falha dizer onde. */
    private static String refusedColumns(ServerWorld world, BlockPos corner, int floor) {
        StringBuilder refused = new StringBuilder();

        for (int dx = 0; dx < 5; dx++) {
            for (int dz = 0; dz < 5; dz++) {
                if (!BigHouseFoundation.columnLevels(world, corner.getX() + dx, corner.getY() + floor,
                        corner.getZ() + dz, 25)) {
                    refused.append('(').append(dx).append(", ").append(dz).append(") ");
                }
            }
        }

        int top = world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                corner.getX(), corner.getZ());
        StringBuilder column = new StringBuilder(" coluna (0, 0): topo " + (top - corner.getY()) + ";");

        for (int dy = -1; dy <= floor + 26; dy++) {
            net.minecraft.block.BlockState state = world.getBlockState(corner.up(dy));

            if (!state.isAir()) {
                column.append(' ').append(dy).append('=').append(state.getBlock().getTranslationKey());
            }
        }

        return refused.length() == 0 ? "nenhuma coluna; foi a caixa (obra, peça de vila ou canteiro)"
                : "colunas " + refused + column;
    }

    private static void clearPatch(TestContext context, BlockPos corner) {
        for (BlockPos pos : BlockPos.iterate(corner, corner.add(4, 5, 4))) {
            context.getWorld().setBlockState(pos, Blocks.AIR.getDefaultState());
        }
    }

    /** Lote com desnível de até 2: a casa fundadora aplaina o chão na mediana — autor, 2026-10-20. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "big_house_levelling")
    public void anUnevenLotIsLevelledToTheMedianFloor(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos corner = unevenPatch(context);
        Vec3i size = new Vec3i(5, 4, 5);
        int floor = 3; // tops 2, 3, 4 em proporção quase igual: a mediana é 3
        Colony colony = Colony.create(UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(corner));

        try {
            context.assertTrue(
                    BigHouseFoundation.safeAfterLevelling(world, colony, corner.getX(), corner.getY() + floor,
                            corner.getZ(), size),
                    "o lote de chão natural com desnível de 2 foi recusado — " + refusedColumns(world, corner, floor));

            int moved = BigHouseFoundation.level(world, new BlockPos(corner.getX(), corner.getY() + floor,
                    corner.getZ()), size);

            for (int dx = 0; dx < 5; dx++) {
                for (int dz = 0; dz < 5; dz++) {
                    int top = world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                            corner.getX() + dx, corner.getZ() + dz);
                    context.assertTrue(top == corner.getY() + floor,
                            "a coluna (" + dx + ", " + dz + ") ficou com o topo em " + (top - corner.getY()));
                }
            }

            context.assertTrue(moved > 0, "nada foi mexido num chão irregular");
        } finally {
            clearPatch(context, corner);
        }

        context.complete();
    }

    /** Controle: tronco no meio do corte não é chão — o lote é recusado, e nada se corta. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "big_house_levelling")
    public void aTrunkInTheCutRefusesTheLevelledLot(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos corner = unevenPatch(context);
        // (2, 0) tem step 2: o bloco a 3 seria cortado num piso em 3.
        world.setBlockState(corner.add(2, 3, 0), Blocks.OAK_LOG.getDefaultState());
        Colony colony = Colony.create(UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(corner));

        try {
            context.assertFalse(
                    BigHouseFoundation.safeAfterLevelling(world, colony, corner.getX(), corner.getY() + 3,
                            corner.getZ(), new Vec3i(5, 4, 5)),
                    "o lote aceitou cortar um tronco");
        } finally {
            clearPatch(context, corner);
        }

        context.complete();
    }

    /** Chão plano de 5 × 5 fora da arena (céu aberto), com o chão pedido; devolve o canto no piso. */
    private static BlockPos flatGround(TestContext context, net.minecraft.block.Block ground, int offset) {
        BlockPos corner = context.getAbsolutePos(new BlockPos(1, 0, 1)).add(1_000_000, 0, 1_400_000 + offset);
        ServerWorld world = context.getWorld();
        world.getChunk(corner);

        for (int dx = 0; dx < 5; dx++) {
            for (int dz = 0; dz < 5; dz++) {
                world.setBlockState(corner.add(dx, 0, dz), Blocks.STONE.getDefaultState());
                world.setBlockState(corner.add(dx, 1, dz), ground.getDefaultState());
            }
        }

        return corner.up(2);
    }

    private static void clearAbove(TestContext context, BlockPos floor) {
        for (BlockPos pos : BlockPos.iterate(floor.add(-1, -2, -1), floor.add(5, 12, 5))) {
            context.getWorld().setBlockState(pos, Blocks.AIR.getDefaultState());
        }
    }

    /** Árvore natural no meio do lote: o lote é aceito e a árvore sai — G1, 2026-10-20. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "big_house_growth")
    public void aNaturalTreeInTheLotIsCleared(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos floor = flatGround(context, Blocks.GRASS_BLOCK, 0);
        Vec3i size = new Vec3i(5, 4, 5);
        Colony colony = Colony.create(UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(floor));

        for (int dy = 0; dy < 4; dy++) {
            world.setBlockState(floor.add(2, dy, 2), Blocks.OAK_LOG.getDefaultState());
        }

        for (BlockPos leaf : BlockPos.iterate(floor.add(1, 4, 1), floor.add(3, 5, 3))) {
            world.setBlockState(leaf, Blocks.OAK_LEAVES.getDefaultState());
        }

        try {
            context.assertTrue(BigHouseFoundation.safe(world, colony, floor.getX(), floor.getY(), floor.getZ(), size),
                    "o lote com uma árvore natural foi recusado");

            int cleared = BigHouseFoundation.clear(world, floor, size);

            for (BlockPos pos : BlockPos.iterate(floor, floor.add(4, 10, 4))) {
                net.minecraft.block.BlockState state = world.getBlockState(pos);
                context.assertFalse(state.isIn(net.minecraft.registry.tag.BlockTags.LOGS)
                                || state.isIn(net.minecraft.registry.tag.BlockTags.LEAVES),
                        "ficou " + state.getBlock() + " no lote em " + pos.toShortString());
            }

            context.assertTrue(cleared >= 4 + 18, "saíram só " + cleared + " blocos da árvore");
        } finally {
            clearAbove(context, floor);
            world.getEntitiesByClass(net.minecraft.entity.ItemEntity.class,
                    new net.minecraft.util.math.Box(floor).expand(8), item -> true).forEach(Entity::discard);
        }

        context.complete();
    }

    /** Controle: toras de cabana cobertas de tábua não são árvore — o lote é recusado e nada se corta. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "big_house_growth")
    public void aCabinOfLogsIsNotATree(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos floor = flatGround(context, Blocks.GRASS_BLOCK, 16);
        Colony colony = Colony.create(UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(floor));

        for (int dy = 0; dy < 3; dy++) {
            world.setBlockState(floor.add(2, dy, 2), Blocks.OAK_LOG.getDefaultState());
        }

        world.setBlockState(floor.add(2, 3, 2), Blocks.OAK_PLANKS.getDefaultState());

        try {
            context.assertFalse(BigHouseFoundation.safe(world, colony, floor.getX(), floor.getY(), floor.getZ(),
                    new Vec3i(5, 4, 5)), "a fundação aceitou derrubar a coluna de toras de uma cabana");
        } finally {
            clearAbove(context, floor);
        }

        context.complete();
    }

    /** Micélio, do campo de cogumelos, é chão do bioma — G1. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "big_house_growth")
    public void myceliumIsGroundForTheHouse(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos floor = flatGround(context, Blocks.MYCELIUM, 32);
        Colony colony = Colony.create(UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(floor));

        try {
            context.assertTrue(BigHouseFoundation.safe(world, colony, floor.getX(), floor.getY(), floor.getZ(),
                    new Vec3i(5, 4, 5)), "o lote de micélio foi recusado");
        } finally {
            clearAbove(context, floor);
        }

        context.complete();
    }

    private static boolean invokesSafe(
            ServerWorld world, Colony colony, BlockPos origin, Vec3i size) {
        try {
            Method safe = BigHouseFoundation.class.getDeclaredMethod(
                    "safe",
                    ServerWorld.class,
                    Colony.class,
                    int.class,
                    int.class,
                    int.class,
                    Vec3i.class);
            safe.setAccessible(true);
            return (boolean) safe.invoke(
                    null,
                    world,
                    colony,
                    origin.getX(),
                    origin.getY(),
                    origin.getZ(),
                    size);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("nao foi possivel testar a regra do lote", exception);
        }
    }
}
