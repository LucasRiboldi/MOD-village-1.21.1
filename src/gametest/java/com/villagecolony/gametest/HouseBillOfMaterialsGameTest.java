package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
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

    /**
     * A casa de planície sabe por onde se entra, e gira para a rua.
     *
     * <p>A Regra 17 com planta lida do jogo. A cabana do mod é quadrada
     * e resolvia a porta mudando duas coordenadas; a casa do arquivo tem
     * a porta onde o gerador a pôs — a um bloco da parede oeste, com o
     * encaixe de rua do jigsaw do mesmo lado — e a única forma de
     * virá-la é girar tudo.
     *
     * <p>Prende as duas pontas: a planta responde de que lado é a porta,
     * e girar leva a porta para o lado pedido.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "hut_bill")
    public void theSmallHouseKnowsWhichWayItOpensAndTurnsToTheRoad(TestContext context) {
        Blueprint house = StructureBlueprintReader.read(
                        context.getWorld(), StructureBlueprintReader.SMALL_HOUSE)
                .orElseThrow();

        Optional<Side> door = house.doorSide();

        context.assertTrue(door.isPresent(), "a casa pequena não disse por onde se entra");

        context.assertTrue(
                door.get() == Side.WEST,
                "a porta da casa pequena fica a oeste, e a planta disse " + door.get());

        for (Side road : Side.values()) {
            Blueprint turned = house.rotated(door.get().turnsTo(road));

            context.assertTrue(
                    turned.doorSide().orElseThrow() == road,
                    "girada para " + road + ", a porta foi parar em "
                            + turned.doorSide().orElseThrow());

            context.assertTrue(
                    turned.blockCount() == house.blockCount(),
                    "girar mudou o número de blocos da casa");
        }

        context.complete();
    }

    /**
     * O leitor separa mobília de parede na casa do jogo.
     *
     * <p>A classificação sobreviveu à Regra 21, que morreu em
     * 2026-08-21: quem decide hoje o que não segura a obra é a barreira
     * da Regra 28. O que se afirma aqui é que o leitor de estrutura
     * continua marcando cama e tocha como mobília — e que pedregulho
     * continua sendo parede.
     *
     * <p>E o que <b>não</b> é mobília importa igual: pedregulho é
     * parede, e parede segura a obra.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "hut_bill")
    public void theBedAndTheTorchDoNotHoldUpTheWork(TestContext context) {
        Blueprint house = StructureBlueprintReader.read(
                        context.getWorld(), StructureBlueprintReader.SMALL_HOUSE)
                .orElseThrow();

        for (BlueprintBlock block : house.blocks()) {
            String name = block.block().path();

            boolean shouldBeFurniture = name.endsWith("_bed") || name.endsWith("torch");

            context.assertTrue(
                    block.furniture() == shouldBeFurniture,
                    name + " está do lado errado da Regra 21");
        }

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
