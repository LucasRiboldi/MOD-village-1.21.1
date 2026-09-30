package com.villagecolony.fabric.integration;

import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Alternativas de lote quando a pegada cai sobre a rua — 2026-09-30. */
public final class RoadsideSitesGameTest implements FabricGameTest {

    /**
     * A rua que dobra não reprova o lote: ele desliza para o lado livre.
     *
     * <p>Na sessão das 02:45, 49% das recusas de lote foram "área reservada
     * de rua". Cada coluna de rua gerava um só candidato, ancorado numa
     * ponta; quando a rua dobrava para dentro dele, o lote inteiro era
     * recusado, embora o mesmo lote deslizado ao longo da rua coubesse.
     *
     * <p>Cenário: rua reta em z=4 e um desvio dela em (5, 5), justo onde a
     * pegada de 2×2 ancorada em x=4 cairia. Ancorada na outra ponta
     * (x=3..4), ela cabe.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "roadside_sites")
    public void aLotSlidesAlongTheRoadAwayFromABend(TestContext context) {
        UUID colony = UUID.randomUUID();
        int y = 3;

        for (int x = 0; x <= 8; x++) {
            for (int z = 0; z <= 8; z++) {
                context.setBlockState(new BlockPos(x, y, z), Blocks.GRASS_BLOCK);
                context.setBlockState(new BlockPos(x, y + 1, z), Blocks.AIR);
                context.setBlockState(new BlockPos(x, y + 2, z), Blocks.AIR);
            }
        }

        List<BlockPos> road = List.of(
                new BlockPos(2, y, 4), new BlockPos(3, y, 4), new BlockPos(4, y, 4),
                new BlockPos(5, y, 4), new BlockPos(6, y, 4), new BlockPos(5, y, 5));

        for (BlockPos at : road) {
            context.setBlockState(at, Blocks.DIRT_PATH);
        }

        List<Long> columns = road.stream()
                .map(context::getAbsolutePos)
                .map(pos -> ColonyRoads.column(pos.getX(), pos.getZ()))
                .toList();
        ColonyPos center = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(road.get(0)));

        try {
            BuildSiteScanner.restore(new ColonyRoads(colony, center, columns));

            BlockPos column = context.getAbsolutePos(new BlockPos(4, y, 4));

            Optional<BuildSiteScanner.Site> site = RoadsideSites.siteFor(
                    context.getWorld(), colony, column.getX(), column.getZ(), column.getY(),
                    column.getY(), new ColonyPos(2, 2, 2), Direction.NORTH);

            context.assertTrue(site.isPresent(),
                    "a rua dobrando para dentro da pegada reprovou o lote, sem tentar deslizá-lo");

            ColonyPos origin = site.get().origin();
            BlockPos bend = context.getAbsolutePos(new BlockPos(5, y, 5));

            context.assertTrue(
                    bend.getX() < origin.x() || bend.getX() > origin.x() + 1
                            || bend.getZ() < origin.z() || bend.getZ() > origin.z() + 1,
                    "o lote aceito ainda cobre a rua em " + bend.toShortString());
        } finally {
            BuildSiteScanner.clearAll();
        }

        context.complete();
    }
}
