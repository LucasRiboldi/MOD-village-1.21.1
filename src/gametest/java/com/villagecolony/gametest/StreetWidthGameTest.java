package com.villagecolony.gametest;

import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.BuildSiteScanner;
import com.villagecolony.fabric.integration.RoadExtension;
import com.villagecolony.fabric.integration.StreetSurface;
import com.villagecolony.fabric.integration.SweepPersistence;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A rua que a colônia estica — Regra 140 (autor, 2026-10-28): três de largura, o caminho no meio e a mistura
 * gasta nas bordas, e pela ponta mais perto do centro, para a vila adensar antes de esticar.
 */
public class StreetWidthGameTest implements FabricGameTest {

    private static final int RADIUS = 3;

    private static final ColonyPos SMALL_HOUSE = new ColonyPos(2, 3, 2);

    private static final int Z = 6;

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "street_width")
    public void theNewStretchIsThreeWideWithWornEdges(TestContext context) {
        UUID colony = UUID.randomUUID();
        BlockPos start = new BlockPos(2, 1, Z);

        roadFrom(context, colony, start, 2, 4);
        BuildSiteScanner.find(context.getWorld(), colony,
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(start)), RADIUS, SMALL_HOUSE);

        // O chão por calçar vem depois da varredura: ela precisa terminar sem lote.
        for (int x = 5; x <= 7; x++) {
            for (int z = Z - 1; z <= Z + 1; z++) {
                context.setBlockState(new BlockPos(x, 1, z), Blocks.DIRT);
            }
        }

        context.assertTrue(RoadExtension.extend(context.getWorld(), colony, ResourceId.vanilla("dirt_path"))
                == RoadExtension.Outcome.EXTENDED, "a rua não cresceu");

        for (int x = 5; x <= 7; x++) {
            context.assertTrue(context.getBlockState(new BlockPos(x, 1, Z)).isOf(Blocks.DIRT_PATH),
                    "o meio da rua em x=" + x + " não é caminho");

            for (int z : new int[] {Z - 1, Z + 1}) {
                BlockPos edge = new BlockPos(x, 1, z);
                BlockState state = context.getBlockState(edge);

                context.assertTrue(StreetSurface.isStreetMaterial(state), "a borda " + edge.toShortString()
                        + " não foi calçada: " + state);
                context.assertTrue(StreetSurface.isColonyStreet(context.getAbsolutePos(edge), state),
                        "a borda " + edge.toShortString() + " não é rua da colônia");
            }
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "street_width")
    public void theRoadGrowsFromTheEndNearestTheCenter(TestContext context) {
        UUID colony = UUID.randomUUID();
        BlockPos center = new BlockPos(1, 1, Z);

        roadFrom(context, colony, center, 4, 6);
        BuildSiteScanner.find(context.getWorld(), colony,
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(center)), RADIUS + 3, SMALL_HOUSE);

        // Chão dos dois lados: a ponta de oeste está a 3 do centro e a de leste a 5.
        for (int x : new int[] {2, 3, 7, 8}) {
            context.setBlockState(new BlockPos(x, 1, Z), Blocks.DIRT);
        }

        context.assertTrue(RoadExtension.extend(context.getWorld(), colony, ResourceId.vanilla("dirt_path"))
                == RoadExtension.Outcome.EXTENDED, "a rua não cresceu");
        context.assertTrue(context.getBlockState(new BlockPos(3, 1, Z)).isOf(Blocks.DIRT_PATH),
                "a rua não cresceu pela ponta mais perto do centro");
        context.assertTrue(context.getBlockState(new BlockPos(7, 1, Z)).isOf(Blocks.DIRT),
                "a ponta mais distante cresceu antes da mais perto");

        context.complete();
    }

    /**
     * Uma faixa de rua de um bloco sobre o vidro: nada em volta é lote nem chão calçável. O vidro não é vazio
     * para o aterro nem chão para a rua (ver {@code RoadExtensionGameTest.sealTheGround}).
     */
    private static void roadFrom(TestContext context, UUID colony, BlockPos center, int fromX, int toX) {
        for (int x = -2; x <= 12; x++) {
            for (int z = -2; z <= 12; z++) {
                context.setBlockState(new BlockPos(x, 0, z), Blocks.GLASS);
            }
        }

        List<Long> columns = new ArrayList<>();

        for (int x = fromX; x <= toX; x++) {
            BlockPos road = new BlockPos(x, 1, Z);

            context.setBlockState(road, Blocks.DIRT_PATH);

            BlockPos absolute = context.getAbsolutePos(road);
            columns.add(ColonyRoads.column(absolute.getX(), absolute.getZ()));
        }

        SweepPersistence.restore(new ColonyRoads(colony,
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(center)), columns));
    }
}
