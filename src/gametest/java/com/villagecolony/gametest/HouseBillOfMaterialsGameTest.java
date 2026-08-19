package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ColonyHut;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.Side;
import com.villagecolony.fabric.integration.StructureBlueprintReader;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A lista de compras da casa — 2026-08-15.
 *
 * <p>Não prova regra nenhuma: responde uma pergunta que custava uma
 * sessão de jogo por vez. A obra parou em {@code WAITING_RESOURCES} com
 * 149 blocos e o log não dizia <b>de quê</b> ela estava à espera, e sem
 * isso não dá para saber se a casa está lenta ou impossível.
 *
 * <p>A diferença é a que decide o próximo trabalho. O que a colônia
 * fabrica a partir de tronco — tábua, porta, escada — ela resolve
 * sozinha quando a Regra 10 existir. O que não se fabrica a partir de
 * madeira — pedregulho, vidro — não tem produtor nenhum nesta vila, e
 * nenhuma quantidade de lenhador vai mudar isso.
 *
 * <p>Sempre passa. O que ele entrega é a lista no log.
 */
public class HouseBillOfMaterialsGameTest implements FabricGameTest {

    /**
     * O que sai de tronco, e portanto do lenhador mais o fabricante.
     *
     * <p>Lista curta de propósito: cada nome aqui é uma cadeia que o mod
     * tem, ou vai ter pela Regra 10. Acrescentar um nome sem a cadeia
     * existir é o mesmo que voltar à casa de planície.
     */
    private static final java.util.Set<String> FROM_LOGS =
            java.util.Set.of("oak_planks", "oak_door", "oak_stairs", "oak_slab", "oak_fence");


    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "house_bill")
    public void theHouseSaysWhatItIsMadeOf(TestContext context) {
        StructureBlueprintReader.read(
                        context.getWorld(), StructureBlueprintReader.PLAINS_SMALL_HOUSE)
                .ifPresentOrElse(
                        HouseBillOfMaterialsGameTest::announce,
                        () -> VillageColonyMod.LOGGER.info(
                                "BILL — this game has no {}",
                                StructureBlueprintReader.PLAINS_SMALL_HOUSE));

        context.complete();
    }

    /**
     * E a cabana da colônia, que é o alvo do MVP desde 08-15.
     *
     * <p>Este afirma, e não só relata: a <b>estrutura</b> da cabana tem
     * de ser feita só do que a colônia produz a partir de tronco. No dia
     * em que alguém puser pedregulho numa parede, a obra volta a ser
     * impossível e este teste cai antes de a sessão de jogo descobrir.
     *
     * <p><b>A mobília ficou de fora em 2026-08-19</b>, e é a Regra 21.
     * Cama e lampião pedem lã e ferro, que a colônia não produz — e
     * entram na casa mesmo assim, porque a Regra 21 os tirou do caminho
     * da obra: a casa termina sem eles e a peça entra depois, quando o
     * material aparecer num baú. Exigi-los aqui seria pedir de volta o
     * travamento que a Regra 13 corrigiu.
     *
     * <p>A distinção é a que importa: o que <b>segura</b> a obra
     * continua tendo de ser possível.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "hut_bill")
    public void theColonyCanMakeEverythingTheHutIsMadeOf(TestContext context) {
        Blueprint hut = ColonyHut.blueprint(ColonyHut.OAK_PLANKS, Side.NORTH);

        announce(hut);

        for (BlueprintBlock block : hut.blocks()) {
            if (block.furniture()) {
                continue;
            }

            context.assertTrue(
                    FROM_LOGS.contains(block.block().path()),
                    "a estrutura da cabana pede " + block.block()
                            + ", que a colônia não faz a partir de tronco");
        }

        context.complete();
    }

    /**
     * A mobília existe, e a colônia sabe fazer pelo menos o baú.
     *
     * <p>A outra ponta da Regra 21: sem esta afirmação, "a mobília não
     * segura a obra" poderia ser cumprido tirando a mobília da planta.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "hut_bill")
    public void theHutCarriesItsFurniture(TestContext context) {
        Blueprint hut = ColonyHut.blueprint(ColonyHut.OAK_PLANKS, Side.NORTH);

        long furniture = hut.blocks().stream().filter(BlueprintBlock::furniture).count();

        context.assertTrue(
                furniture == 3,
                "a Regra 21 pede cama, baú e lampião, e a cabana traz " + furniture);

        context.complete();
    }

    /**
     * O schema que o mod passou a carregar sozinho — 2026-08-19.
     *
     * <p>A casa está em {@code data/villagecolony/structure/houses/}, e
     * o que este teste prende é que ela <b>carrega</b>: o caminho de
     * dados do 1.21.1 é {@code structure} no singular, e um arquivo na
     * pasta errada não é erro em lugar nenhum — ele simplesmente não
     * existe para o jogo, e a colônia diria "o jogo não tem essa
     * estrutura" sem que ninguém soubesse por quê.
     *
     * <p>Também relata a lista de compras dela, que é o que decide se a
     * colônia pode ou não construí-la. Ver a Regra 13.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "hut_bill")
    public void theModsOwnSchemaLoads(TestContext context) {
        Optional<Blueprint> house = StructureBlueprintReader.read(
                context.getWorld(), StructureBlueprintReader.SMALL_HOUSE);

        context.assertTrue(
                house.isPresent(),
                "o schema " + StructureBlueprintReader.SMALL_HOUSE
                        + " não carregou — confira a pasta data/villagecolony/structure");

        announce(house.get());

        context.assertTrue(
                house.get().blockCount() > 100,
                "a casa pequena tem 149 blocos e veio com " + house.get().blockCount());

        context.complete();
    }

    /** Uma linha por material, do mais pedido para o menos. */
    private static void announce(Blueprint house) {
        Map<ResourceId, Integer> materials = house.materials();

        List<Map.Entry<ResourceId, Integer>> ordered = new ArrayList<>(materials.entrySet());

        ordered.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        VillageColonyMod.LOGGER.info(
                "BILL — {} needs {} blocks of {} kinds",
                house.id(),
                house.blockCount(),
                materials.size());

        for (Map.Entry<ResourceId, Integer> material : ordered) {
            VillageColonyMod.LOGGER.info(
                    "BILL   {} x {}", material.getValue(), material.getKey());
        }
    }
}
