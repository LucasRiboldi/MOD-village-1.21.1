package com.villagecolony.gametest;

import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.integration.VillageRoad;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;

/**
 * De que bloco é a rua de cada vila — 2026-08-21.
 *
 * <p><b>O defeito que estes testes fecham.</b> A busca de lote
 * reconhecia rua por um nome escrito no código, {@code dirt_path}. A vila
 * de deserto calça com <b>arenito liso</b>, e por isso nunca teve beira
 * de rua nenhuma: nascia, contratava, contava recurso, recebia arenito do
 * mineiro — e terminava toda varredura com "no free lot beside a road",
 * que é a frase certa para a pergunta errada.
 *
 * <p>Nada disso aparecia em teste porque a arena da bateria tem bioma
 * fixo, e o bioma fixo é de planície.
 *
 * <p>Os números não estão no mod: quem responde é o catálogo do jogo, e
 * é a ele que estes testes perguntam.
 */
public class VillageRoadGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_road",
            tickLimit = 40)
    public void eachStylePavesWithWhatTheGameSays(TestContext context) {
        ServerWorld world = context.getWorld();

        ResourceId plains = VillageRoad.pavingFor(world, "plains").orElse(null);

        context.assertTrue(
                ResourceId.vanilla("dirt_path").equals(plains),
                "a planície devia calçar com dirt_path, e calça com " + plains);

        ResourceId desert = VillageRoad.pavingFor(world, "desert").orElse(null);

        context.assertTrue(
                ResourceId.vanilla("smooth_sandstone").equals(desert),
                "o deserto devia calçar com smooth_sandstone, e calça com " + desert);

        // As outras três seguem a planície, e é o que faz do deserto a
        // exceção que passou despercebida.
        for (String style : new String[] {"savanna", "taiga", "snowy"}) {
            ResourceId paving = VillageRoad.pavingFor(world, style).orElse(null);

            context.assertTrue(
                    ResourceId.vanilla("dirt_path").equals(paving),
                    "o estilo " + style + " calça com " + paving);
        }

        context.complete();
    }

    /**
     * A pergunta que a busca de lote faz, e o que ela precisa recusar.
     *
     * <p>Sem estilo de propósito: quem chama olha coluna por coluna e não
     * tem o bioma em mãos. A confusão que isso poderia causar não existe,
     * porque nem caminho de terra nem arenito liso nascem sozinhos.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_road",
            tickLimit = 40)
    public void bothPavingsAreRoadAndTheGroundIsNot(TestContext context) {
        ServerWorld world = context.getWorld();

        context.assertTrue(
                VillageRoad.isPaving(world, Blocks.DIRT_PATH.getDefaultState()),
                "caminho de terra não foi reconhecido como rua");

        context.assertTrue(
                VillageRoad.isPaving(world, Blocks.SMOOTH_SANDSTONE.getDefaultState()),
                "arenito liso não foi reconhecido como rua — o deserto volta a não construir");

        for (var ground : new net.minecraft.block.Block[] {
                Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.SAND, Blocks.STONE, Blocks.SANDSTONE}) {

            context.assertTrue(
                    !VillageRoad.isPaving(world, ground.getDefaultState()),
                    ground + " foi tomado por rua, e a colônia construiria em cima do nada");
        }

        context.complete();
    }
}
