package com.villagecolony.fabric.integration;

import com.villagecolony.core.construction.model.VillageBlend;
import com.villagecolony.core.construction.model.VillageSurface;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.StructureLiquidSettings;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructurePiecesList;
import net.minecraft.structure.StructureStart;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.structure.pool.StructurePoolElement;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.structure.StructureKeys;

import java.util.List;

/**
 * Regra 296: as peças rígidas da vila descem ou sobem ao solo suavizado antes de serem postas — a caixa e a posição da
 * peça andam juntas, e o piso novo entra na conta do terreno como coluna fixa, sem pilar em volta.
 */
public class VillagePiecesGameTest implements FabricGameTest {

    private static final String HOUSE = "minecraft:village/plains/houses/plains_small_house_1";

    /** Numa encosta de 3 por bloco, duas casas que vieram altas demais descem ao chão da vila, sem pilar. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_pieces", tickLimit = 5)
    public void rigidPiecesSettleOnTheSmoothedGround(TestContext context) {
        StructureTemplateManager templates = context.getWorld().getStructureTemplateManager();
        int size = 48;
        PoolStructurePiece low = house(templates, new BlockPos(6, 150, 18));
        PoolStructurePiece high = house(templates, new BlockPos(30, 150, 18));
        StructureStart start = new StructureStart(
                context.getWorld().getRegistryManager().get(RegistryKeys.STRUCTURE).get(StructureKeys.VILLAGE_PLAINS),
                new ChunkPos(0, 0), 0, new StructurePiecesList(List.<StructurePiece>of(low, high)));
        int[][] natural = new int[size][size];
        VillageSurface.Kind[][] kinds = new VillageSurface.Kind[size][size];

        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                natural[x][z] = 60 + 3 * x;
                kinds[x][z] = VillageSurface.Kind.FREE;
            }
        }

        VillageBlend.Area core = new VillageBlend.Area(2, 2, size - 3, size - 3);
        VillageBlend.Area reach = new VillageBlend.Area(0, 0, size - 1, size - 1);
        BlockPos lowBefore = low.getPos();

        VillagePieces.settle(start, natural, kinds, core, reach, 0, 0);

        int shift = low.getPos().getY() - lowBefore.getY();
        context.assertTrue(shift < 0, "a casa que veio alta demais não desceu");
        context.assertTrue(low.getBoundingBox().getMinY() - shift == 150,
                "a caixa da casa não andou junto com a posição: " + low.getBoundingBox());

        VillageBlend.Shape shape = VillageBlend.shape(natural, kinds, core, reach);

        for (PoolStructurePiece piece : List.of(low, high)) {
            BlockBox box = piece.getBoundingBox();
            int floor = box.getMinY() + piece.getGroundLevelDelta() - 1;

            context.assertTrue(kinds[box.getMinX()][box.getMinZ()] == VillageSurface.Kind.FIXED
                    && natural[box.getMinX()][box.getMinZ()] == floor, "o piso novo da casa não ficou fixo no terreno");

            for (int z = box.getMinZ(); z <= box.getMaxZ(); z++) {
                for (int x : new int[] {box.getMinX() - 1, box.getMaxX() + 1}) {
                    int around = shape.target(x, z, natural[x][z]);
                    context.assertTrue(Math.abs(around - floor) <= 1,
                            "a casa com piso " + floor + " ficou em pilar: ao lado, " + around);
                }
            }
        }

        context.complete();
    }

    /**
     * Regra 305: a casa ganha um patamar de 2 colunas no piso em volta, e a entrada de cada porta é a primeira coluna à
     * frente dela sem bloco da estrutura, também no piso — mesmo dentro da caixa do molde.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_pieces_apron", tickLimit = 5)
    public void aHouseGetsAnApronAndAnEntryOnItsFloor(TestContext context) {
        StructureTemplateManager templates = context.getWorld().getStructureTemplateManager();
        int size = 40;
        PoolStructurePiece house = house(templates, new BlockPos(15, 80, 15));
        StructureStart start = new StructureStart(
                context.getWorld().getRegistryManager().get(RegistryKeys.STRUCTURE).get(StructureKeys.VILLAGE_PLAINS),
                new ChunkPos(0, 0), 0, new StructurePiecesList(List.<StructurePiece>of(house)));
        int[][] natural = new int[size][size];
        VillageSurface.Kind[][] kinds = new VillageSurface.Kind[size][size];

        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                natural[x][z] = 60 + x;
                kinds[x][z] = VillageSurface.Kind.FREE;
            }
        }

        VillageBlend.Area all = new VillageBlend.Area(0, 0, size - 1, size - 1);
        java.util.Map<Long, Integer> doors = new java.util.HashMap<>();
        java.util.Map<Long, Integer> aprons = new java.util.HashMap<>();

        VillagePieces.settle(start, natural, kinds, com.villagecolony.core.construction.model.VillageCore.of(all, size,
                size), all, 0, 0, templates, context.getWorld().getRegistryManager(),
                doors, aprons);

        BlockBox box = house.getBoundingBox();
        int floor = box.getMinY() + house.getGroundLevelDelta() - 1;

        context.assertFalse(doors.isEmpty(), "Regra 305: a casa ficou sem coluna de entrada");
        doors.values().forEach(level -> context.assertTrue(level == floor || level == floor - 1,
                "Regra 313: a entrada não ficou no piso " + floor + " nem um abaixo, ficou em " + level));
        // O patamar acompanha a entrada mais baixa: um abaixo do piso quando a porta tem degrau (Regra 313).
        int apron = Math.max(floor - 1, doors.values().stream().mapToInt(Integer::intValue).min().orElse(floor));

        for (int x = box.getMinX() - 2; x <= box.getMaxX() + 2; x++) {
            for (int z = box.getMinZ() - 2; z <= box.getMaxZ() + 2; z++) {
                boolean ring = x < box.getMinX() || x > box.getMaxX() || z < box.getMinZ() || z > box.getMaxZ();

                if (ring) {
                    long column = com.villagecolony.core.construction.model.ColonyRoads.column(x, z);
                    context.assertTrue(kinds[x][z] == VillageSurface.Kind.FIXED
                                    && natural[x][z] == (doors.get(column) != null ? doors.get(column) : apron)
                                    && (aprons.get(column) != null || doors.get(column) != null),
                            "Regra 305: o patamar em " + x + "," + z + " não ficou em " + apron + ": " + natural[x][z]);
                }
            }
        }

        context.complete();
    }

    private static PoolStructurePiece house(StructureTemplateManager templates, BlockPos pos) {
        StructurePoolElement element = StructurePoolElement.ofSingle(HOUSE).apply(StructurePool.Projection.RIGID);
        BlockBox box = element.getBoundingBox(templates, pos, BlockRotation.NONE);

        return new PoolStructurePiece(templates, element, pos, 1, BlockRotation.NONE, box,
                StructureLiquidSettings.APPLY_WATERLOGGING);
    }
}
