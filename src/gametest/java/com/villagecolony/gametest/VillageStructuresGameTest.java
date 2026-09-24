package com.villagecolony.gametest;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.integration.StructureBlueprintReader;
import com.villagecolony.fabric.integration.VillageStructures;
import com.villagecolony.fabric.work.HousePlans;
import com.villagecolony.fabric.work.PlanPlacement;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A Regra 27 — o construtor só levanta o que está no catálogo do jogo.
 *
 * <p>Regra imutável do autor, 2026-08-20: as estruturas que o construtor
 * de cada bioma pode construir são as da pasta de estruturas, e o mod
 * não cria nenhuma outra.
 *
 * <p>Isso desfez a Regra 13, que tinha inventado a cabana do mod porque
 * a casa do jogo era impossível de levantar. A resposta passou a ser
 * outra: a casa do jogo pede pedra, então a colônia aprendeu a minerar.
 */
public class VillageStructuresGameTest implements FabricGameTest {

    /** As cinco vilas que o jogo gera. */
    private static final List<String> STYLES =
            List.of("plains", "taiga", "savanna", "snowy", "desert");

    /**
     * <b>E cada estilo tem a roça padrão dele</b> — decisão do autor,
     * 2026-09-05: <i>"precisam construir o espaço de plantação padrão e
     * idêntico aos que já vêm na vila do Minecraft"</i>.
     *
     * <p>A resposta é a Regra 27 outra vez: a roça já está no catálogo do
     * jogo, ao lado das casas. Nenhum {@code .nbt} novo, nenhum byte da
     * Mojang — o mod só precisa saber pedir por ela.
     *
     * <p>Ela mora em {@code houses} porque é lá que o gerador de vilas a
     * põe: para o Vanilla, a roça é uma das peças que um lote recebe. É
     * por isso que a busca é por <b>nome</b>.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_catalog")
    public void everyVillageStyleHasAFarmOfItsOwn(TestContext context) {
        for (String style : STYLES) {
            List<ResourceId> farms = VillageStructures.farmsFor(style);

            context.assertTrue(
                    !farms.isEmpty(),
                    "a vila de " + style + " ficou sem roça nenhuma no catálogo");

            for (ResourceId farm : farms) {
                context.assertTrue(
                        farm.path().startsWith("village/" + style + "/houses/")
                                && farm.path().contains("farm"),
                        farm + " não é roça de " + style);
            }
        }

        context.complete();
    }

    /**
     * E a barreira de teste não vale para ela.
     *
     * <p>A Regra 28 limita quantas <b>casas</b> a colônia tenta levantar,
     * para que uma sessão que falha diga qual regra falhou. A roça é uma
     * peça por estilo e não polui essa comparação — filtrá-la pelo sufixo
     * da casa pequena a apagaria do catálogo inteiro.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_catalog")
    public void theTestBarrierDoesNotHideTheFarm(TestContext context) {
        context.assertTrue(
                VillageStructures.farmsFor("plains").stream()
                        .noneMatch(farm -> farm.path().endsWith("_small_house_1")),
                "a roça de planície saiu filtrada pela barreira da casa pequena");

        context.assertTrue(
                !VillageStructures.farmsFor("plains").isEmpty(),
                "a barreira de teste apagou a roça de planície do catálogo");

        context.complete();
    }

    /** Cada estilo de vila tem casas, e o deserto tem as dele. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_catalog")
    public void everyVillageStyleHasHousesOfItsOwn(TestContext context) {
        for (String style : STYLES) {
            List<ResourceId> houses = VillageStructures.housesFor(style);

            context.assertTrue(
                    !houses.isEmpty(),
                    "a vila de " + style + " ficou sem casa nenhuma no catálogo");

            for (ResourceId house : houses) {
                context.assertTrue(
                        house.path().startsWith("village/" + style + "/houses/"),
                        house + " não é casa de " + style);
            }
        }

        context.complete();
    }

    /**
     * A vila tem mais de uma casa para cada pegada — 2026-09-18.
     *
     * <p><b>O defeito que este teste mede.</b> Até hoje a colônia
     * levantava sempre a mesma estrutura. A causa estava em
     * {@code HousePlans.catalogPlans}: ele corta as pegadas repetidas —
     * {@code sizes.add(plan.size())} — e o planejador levanta a
     * {@code get(0)}. As oito {@code small_house} de planície colapsavam
     * em <b>uma</b>, e o sorteio escrito em 09-09 sorteava entre uma só.
     *
     * <p>O corte é legítimo: ele serve à <b>busca de lote</b>, que o
     * comentário do {@code PLANS_OFFERED} registra em dez minutos. O que
     * mudou é que as irmãs voltam <b>depois</b> do lote achado, por
     * {@code PlanPlacement.siblingsOf}.
     *
     * <p><b>Por que em jogo e não no unitário.</b> Ler a pegada de uma
     * planta é ler o {@code .nbt} do jogo, e a pergunta aqui é
     * justamente sobre o catálogo real: se um dia a Mojang der uma
     * pegada distinta a cada casa, este teste cai e a variedade passa a
     * vir do próprio corte — que é o que se quer saber.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_catalog")
    public void theStyleHasSiblingsSharingAFootprint(TestContext context) {
        int stylesWithSiblings = 0;

        for (String style : STYLES) {
            List<ResourceId> dwellings = VillageStructures.housesFor(style).stream()
                    .filter(HousePlans::isDwelling)
                    .toList();

            context.assertTrue(
                    !dwellings.isEmpty(),
                    "o filtro de moradia esvaziou o catálogo de " + style);

            Map<ColonyPos, Integer> byFootprint = new HashMap<>();

            for (ResourceId id : dwellings) {
                StructureBlueprintReader.read(context.getWorld(), id)
                        .ifPresent(plan -> byFootprint.merge(plan.size(), 1, Integer::sum));
            }

            context.assertTrue(
                    !byFootprint.isEmpty(),
                    "nenhuma moradia de " + style + " pôde ser lida do catálogo");

            if (byFootprint.values().stream().anyMatch(sharing -> sharing > 1)) {
                stylesWithSiblings++;
            }
        }

        // Não se exige irmã em TODO estilo — isso seria afirmar sobre os
        // arquivos da Mojang, que não são nossos. Exige-se que a
        // variedade exista em algum lugar: se nenhum dos cinco repete
        // pegada, siblingsOf é código morto e o defeito era outro.
        context.assertTrue(
                stylesWithSiblings > 0,
                "nenhum estilo repete pegada — siblingsOf não tem o que devolver");

        context.complete();
    }

    /**
     * O filtro de moradia não come casa — 2026-09-18.
     *
     * <p>Ele tira cerca de bicho, ponto de encontro, templo, estábulo e
     * a peça decorativa; a decisão do autor é que a colônia levanta
     * <b>moradia</b>. O risco de um filtro por substring é comer demais,
     * e quem paga é a vila que para de crescer — a lição de 08-20.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_catalog")
    public void theDwellingFilterKeepsTheHouses(TestContext context) {
        for (String style : STYLES) {
            List<ResourceId> dwellings = VillageStructures.housesFor(style).stream()
                    .filter(HousePlans::isDwelling)
                    .toList();

            long houses = dwellings.stream()
                    .filter(id -> id.path().contains("_house_"))
                    .count();

            context.assertTrue(
                    houses > 0,
                    "o filtro comeu todas as casas de moradia de " + style);

            for (ResourceId kept : dwellings) {
                context.assertTrue(
                        !kept.path().contains("farm"),
                        kept + " é roça e passou pelo filtro de moradia");
            }
        }

        context.complete();
    }

    /**
     * Nenhuma casa inventada pelo mod entra na lista.
     *
     * <p>É a regra dita pelo lado de fora: a cabana escrita em código —
     * {@code villagecolony:hut} — existia e era o que a colônia
     * levantava. Ela foi apagada em 2026-08-21, e o que resta afirmar é
     * que <b>nada fora do jogo entra na lista</b>: um namespace que não
     * seja {@code minecraft} é casa que o mod inventou.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_catalog")
    public void nothingTheModInventedIsOffered(TestContext context) {
        for (String style : STYLES) {
            for (ResourceId house : VillageStructures.housesFor(style)) {
                context.assertTrue(
                        ResourceId.VANILLA.equals(house.namespace()),
                        house + " não é do jogo, e a Regra 27 só aceita o que é");
            }
        }

        context.complete();
    }

    /**
     * A vila em ruína não é modelo de construção.
     *
     * <p>As variantes zumbi são as mesmas casas com teia e tocha
     * apagada. Uma colônia que as levantasse estaria construindo a
     * própria decadência.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_catalog")
    public void theRuinedVariantsAreNotBuildable(TestContext context) {
        for (String style : STYLES) {
            for (ResourceId house : VillageStructures.housesFor(style)) {
                context.assertTrue(
                        !house.path().contains("zombie"),
                        "a colônia ia levantar uma ruína: " + house);
            }
        }

        context.complete();
    }

    /**
     * O catálogo do mod é uma lista fechada de peças Vanilla — 2026-09-20.
     *
     * <p>Os nomes vieram da lista pedida em jogo, mas foram corrigidos para
     * os ids reais do Minecraft: por exemplo,
     * {@code desert_cartographer_house_1},
     * {@code savanna_small_farm} e os postes de luz fora de {@code houses}.
     * Qualquer peça fora desta relação é uma escolha que o autor não
     * autorizou.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_catalog")
    public void onlyTheCorrectedVanillaWhitelistIsBuildable(TestContext context) {
        Map<String, Set<String>> expected = Map.of(
                "plains", Set.of(
                        "village/plains/houses/plains_small_house_1",
                        "village/plains/houses/plains_small_house_2",
                        "village/plains/houses/plains_small_house_3",
                        "village/plains/houses/plains_small_house_4",
                        "village/plains/houses/plains_small_house_5",
                        "village/plains/houses/plains_small_house_6",
                        "village/plains/houses/plains_small_house_7",
                        "village/plains/houses/plains_small_house_8",
                        "village/plains/houses/plains_medium_house_1",
                        "village/plains/houses/plains_medium_house_2",
                        "village/plains/houses/plains_big_house_1",
                        "village/plains/houses/plains_armorer_house_1",
                        "village/plains/houses/plains_butcher_shop_1",
                        "village/plains/houses/plains_butcher_shop_2",
                        "village/plains/houses/plains_cartographer_1",
                        "village/plains/houses/plains_fisher_cottage_1",
                        "village/plains/houses/plains_fletcher_house_1",
                        "village/plains/houses/plains_library_1",
                        "village/plains/houses/plains_library_2",
                        "village/plains/houses/plains_masons_house_1",
                        "village/plains/houses/plains_shepherds_house_1",
                        "village/plains/houses/plains_tannery_1",
                        "village/plains/houses/plains_temple_3",
                        "village/plains/houses/plains_temple_4",
                        "village/plains/houses/plains_tool_smith_1",
                        "village/plains/houses/plains_weaponsmith_1",
                        "village/plains/houses/plains_small_farm_1",
                        "village/plains/houses/plains_large_farm_1",
                        "village/plains/houses/plains_animal_pen_1",
                        "village/plains/houses/plains_animal_pen_2",
                        "village/plains/houses/plains_animal_pen_3",
                        "village/plains/houses/plains_stable_1",
                        "village/plains/houses/plains_stable_2",
                        "village/plains/plains_lamp_1"),
                "desert", Set.of(
                        "village/desert/houses/desert_small_house_1",
                        "village/desert/houses/desert_small_house_2",
                        "village/desert/houses/desert_small_house_3",
                        "village/desert/houses/desert_small_house_4",
                        "village/desert/houses/desert_small_house_5",
                        "village/desert/houses/desert_small_house_6",
                        "village/desert/houses/desert_small_house_7",
                        "village/desert/houses/desert_small_house_8",
                        "village/desert/houses/desert_medium_house_1",
                        "village/desert/houses/desert_medium_house_2",
                        "village/desert/houses/desert_armorer_1",
                        "village/desert/houses/desert_butcher_shop_1",
                        "village/desert/houses/desert_cartographer_house_1",
                        "village/desert/houses/desert_fisher_1",
                        "village/desert/houses/desert_fletcher_house_1",
                        "village/desert/houses/desert_library_1",
                        "village/desert/houses/desert_mason_1",
                        "village/desert/houses/desert_shepherd_house_1",
                        "village/desert/houses/desert_tannery_1",
                        "village/desert/houses/desert_temple_1",
                        "village/desert/houses/desert_temple_2",
                        "village/desert/houses/desert_tool_smith_1",
                        "village/desert/houses/desert_weaponsmith_1",
                        "village/desert/houses/desert_farm_1",
                        "village/desert/houses/desert_farm_2",
                        "village/desert/houses/desert_large_farm_1",
                        "village/desert/houses/desert_animal_pen_1",
                        "village/desert/houses/desert_animal_pen_2",
                        "village/desert/desert_lamp_1"),
                "savanna", Set.of(
                        "village/savanna/houses/savanna_small_house_1",
                        "village/savanna/houses/savanna_small_house_2",
                        "village/savanna/houses/savanna_small_house_3",
                        "village/savanna/houses/savanna_small_house_4",
                        "village/savanna/houses/savanna_small_house_5",
                        "village/savanna/houses/savanna_small_house_6",
                        "village/savanna/houses/savanna_small_house_7",
                        "village/savanna/houses/savanna_small_house_8",
                        "village/savanna/houses/savanna_medium_house_1",
                        "village/savanna/houses/savanna_medium_house_2",
                        "village/savanna/houses/savanna_armorer_1",
                        "village/savanna/houses/savanna_butchers_shop_1",
                        "village/savanna/houses/savanna_cartographer_1",
                        "village/savanna/houses/savanna_fisher_cottage_1",
                        "village/savanna/houses/savanna_fletcher_house_1",
                        "village/savanna/houses/savanna_library_1",
                        "village/savanna/houses/savanna_mason_1",
                        "village/savanna/houses/savanna_shepherd_1",
                        "village/savanna/houses/savanna_tannery_1",
                        "village/savanna/houses/savanna_temple_1",
                        "village/savanna/houses/savanna_temple_2",
                        "village/savanna/houses/savanna_tool_smith_1",
                        "village/savanna/houses/savanna_weaponsmith_1",
                        "village/savanna/houses/savanna_small_farm",
                        "village/savanna/houses/savanna_large_farm_1",
                        "village/savanna/houses/savanna_animal_pen_1",
                        "village/savanna/houses/savanna_animal_pen_2",
                        "village/savanna/savanna_lamp_post_01"),
                "taiga", Set.of(
                        "village/taiga/houses/taiga_small_house_1",
                        "village/taiga/houses/taiga_small_house_2",
                        "village/taiga/houses/taiga_small_house_3",
                        "village/taiga/houses/taiga_small_house_4",
                        "village/taiga/houses/taiga_small_house_5",
                        "village/taiga/houses/taiga_medium_house_1",
                        "village/taiga/houses/taiga_medium_house_2",
                        "village/taiga/houses/taiga_medium_house_3",
                        "village/taiga/houses/taiga_medium_house_4",
                        "village/taiga/houses/taiga_armorer_house_1",
                        "village/taiga/houses/taiga_armorer_2",
                        "village/taiga/houses/taiga_butcher_shop_1",
                        "village/taiga/houses/taiga_cartographer_house_1",
                        "village/taiga/houses/taiga_fisher_cottage_1",
                        "village/taiga/houses/taiga_fletcher_house_1",
                        "village/taiga/houses/taiga_library_1",
                        "village/taiga/houses/taiga_masons_house_1",
                        "village/taiga/houses/taiga_shepherds_house_1",
                        "village/taiga/houses/taiga_tannery_1",
                        "village/taiga/houses/taiga_temple_1",
                        "village/taiga/houses/taiga_tool_smith_1",
                        "village/taiga/houses/taiga_weaponsmith_1",
                        "village/taiga/houses/taiga_small_farm_1",
                        "village/taiga/houses/taiga_large_farm_1",
                        "village/taiga/houses/taiga_animal_pen_1",
                        "village/taiga/taiga_lamp_post_1"),
                "snowy", Set.of(
                        "village/snowy/houses/snowy_small_house_1",
                        "village/snowy/houses/snowy_small_house_2",
                        "village/snowy/houses/snowy_small_house_3",
                        "village/snowy/houses/snowy_small_house_4",
                        "village/snowy/houses/snowy_small_house_5",
                        "village/snowy/houses/snowy_small_house_6",
                        "village/snowy/houses/snowy_small_house_7",
                        "village/snowy/houses/snowy_small_house_8",
                        "village/snowy/houses/snowy_medium_house_1",
                        "village/snowy/houses/snowy_medium_house_2",
                        "village/snowy/houses/snowy_armorer_house_1",
                        "village/snowy/houses/snowy_butchers_shop_1",
                        "village/snowy/houses/snowy_cartographer_house_1",
                        "village/snowy/houses/snowy_fisher_cottage",
                        "village/snowy/houses/snowy_fletcher_house_1",
                        "village/snowy/houses/snowy_library_1",
                        "village/snowy/houses/snowy_masons_house_1",
                        "village/snowy/houses/snowy_shepherds_house_1",
                        "village/snowy/houses/snowy_tannery_1",
                        "village/snowy/houses/snowy_temple_1",
                        "village/snowy/houses/snowy_tool_smith_1",
                        "village/snowy/houses/snowy_weapon_smith_1",
                        "village/snowy/houses/snowy_farm_1",
                        "village/snowy/houses/snowy_animal_pen_1",
                        "village/snowy/houses/snowy_animal_pen_2",
                        "village/snowy/snowy_lamp_post_01"));

        for (String style : STYLES) {
            Set<String> actual = VillageStructures.buildableFor(style).stream()
                    .map(ResourceId::path)
                    .collect(java.util.stream.Collectors.toSet());
            context.assertTrue(
                    actual.equals(expected.get(style)),
                    "lista Vanilla incorreta para " + style + ": " + actual);
        }

        context.complete();
    }
}
