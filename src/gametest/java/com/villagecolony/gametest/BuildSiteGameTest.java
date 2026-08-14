package com.villagecolony.gametest;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.BuildSiteScanner;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

/**
 * O lote da próxima casa — a Regra 6 lendo o mundo.
 *
 * <p>Gametest porque é leitura de terreno: mapa de alturas, blocos e
 * chunk carregado. O Core não tem nada disso.
 *
 * <p><b>Raio curto de propósito.</b> A bateria roda concorrente e as
 * estruturas dos outros testes ficam perto. Com raio de poucos blocos, a
 * busca não alcança a rua de ninguém, e o que este teste afirma é sobre o
 * terreno que ele mesmo montou. Ver {@link ColonyFixture}.
 */
public class BuildSiteGameTest implements FabricGameTest {

    /** Curto o bastante para a busca não sair da estrutura do teste. */
    private static final int RADIUS = 3;

    /** Uma casa de dois por dois, que cabe na estrutura vazia. */
    private static final ColonyPos SMALL_HOUSE = new ColonyPos(2, 3, 2);

    /**
     * Chão liso com rua ao lado: a colônia acha onde construir.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "build_site")
    public void aLotBesideTheRoadIsFound(TestContext context) {
        BlockPos center = new BlockPos(3, 1, 3);

        paveGround(context, center);

        context.setBlockState(center, Blocks.DIRT_PATH.getDefaultState());

        Optional<ColonyPos> site = BuildSiteScanner.find(
                context.getWorld(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(center)),
                RADIUS,
                SMALL_HOUSE);

        context.assertTrue(site.isPresent(), "não achou lote ao lado da rua");

        // Assenta sobre o chão, não dentro dele: o chão está em y=1
        // relativo, então a casa começa em y=2.
        context.assertTrue(
                site.get().y() == context.getAbsolutePos(center).getY() + 1,
                "a casa assentou em " + site.get().y() + ", e o chão está em "
                        + context.getAbsolutePos(center).getY());

        context.complete();
    }

    /**
     * Sem rua, não há lote.
     *
     * <p>É a Regra 6 ao pé da letra: nunca casa isolada. Um chão liso e
     * vazio no meio do campo é exatamente o lugar onde a colônia <b>não</b>
     * pode construir.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "build_site")
    public void groundWithoutARoadIsNotALot(TestContext context) {
        BlockPos center = new BlockPos(3, 1, 3);

        paveGround(context, center);

        Optional<ColonyPos> site = BuildSiteScanner.find(
                context.getWorld(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(center)),
                RADIUS,
                SMALL_HOUSE);

        context.assertTrue(site.isEmpty(), "achou lote sem rua nenhuma por perto");

        context.complete();
    }

    /**
     * Terreno acidentado é recusado.
     *
     * <p>Um pilar dentro do lote põe o desnível acima de
     * {@link BuildSiteScanner#MAX_SLOPE}, e a casa iria para outro lugar
     * em vez de nascer enterrada de um lado.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "build_site")
    public void brokenGroundIsRefused(TestContext context) {
        BlockPos center = new BlockPos(3, 1, 3);

        paveGround(context, center);

        context.setBlockState(center, Blocks.DIRT_PATH.getDefaultState());

        // Uma torre em cada vizinho da rua: qualquer lote que encoste
        // nela passa do desnível.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }

                for (int dy = 1; dy <= BuildSiteScanner.MAX_SLOPE + 2; dy++) {
                    context.setBlockState(
                            center.add(dx, dy, dz), Blocks.GRASS_BLOCK.getDefaultState());
                }
            }
        }

        Optional<ColonyPos> site = BuildSiteScanner.find(
                context.getWorld(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(center)),
                RADIUS,
                SMALL_HOUSE);

        context.assertTrue(site.isEmpty(), "aceitou lote com desnível de quatro blocos");

        context.complete();
    }

    /**
     * Chão de grama em volta do ponto, largo o bastante para caber a
     * casa em qualquer das quatro direções.
     */
    private static void paveGround(TestContext context, BlockPos center) {
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                context.setBlockState(
                        center.add(dx, 0, dz), Blocks.GRASS_BLOCK.getDefaultState());
            }
        }
    }
}
