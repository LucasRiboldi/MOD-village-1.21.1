package com.villagecolony.gametest;

import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.VillagePalette;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.StructureBlueprintReader;
import com.villagecolony.fabric.work.HousePlans;
import com.villagecolony.fabric.work.WorkMaterials;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

/**
 * A obra pede peça, e a colônia produz material — 2026-08-21.
 *
 * <p><b>O elo que quebra em silêncio.</b> A casa não pede vidro: pede
 * vidraça. Não pede carvão: pede tocha. Não pede lingote: pede lampião,
 * que pede pepita. Se qualquer uma dessas pontes se romper — o nome do
 * bloco mudar, a receita sair de um datapack, o item da tocha de parede
 * deixar de ser o da tocha — a conta devolve <b>zero</b>. E zero não
 * derruba nada: a colônia apenas para de pedir a matéria-prima, para
 * sempre, sem uma linha de log.
 *
 * <p>É por isso que estes testes existem, e é por isso que rodam num
 * servidor de verdade: a única autoridade sobre quanto vidro dá uma
 * vidraça é o livro de receitas do jogo, e nenhum desses números está
 * escrito no mod.
 *
 * <p>Moravam em {@code MinerGameTest}, porque a areia e o carvão
 * chegaram pelo mineiro. Não são dele, e o arquivo passou de quinhentas
 * linhas.
 */
public class WorkMaterialsGameTest implements FabricGameTest {

    /** Uma posição qualquer da arena, para a paleta saber o bioma. */
    private static final BlockPos HERE = new BlockPos(2, 2, 2);

    /**
     * A vidraça vira vidro pela receita do próprio jogo — 2026-08-20.
     *
     * <p>É a conta que fecha a cadeia, e a que pode calar sem quebrar
     * nada: a casa de planície não pede vidro, pede <b>três vidraças</b>,
     * e perguntar ao projeto quanto vidro falta devolvia zero. Com zero a
     * colônia nunca abre tarefa de fundição, o fundidor fica parado e a
     * areia não tem para quem ser colhida.
     *
     * <p>Seis vidros dão dezesseis vidraças, e quem diz isso é o livro de
     * receitas do jogo — nenhum número desses está escrito no mod. Três
     * vidraças pedem a fornada inteira: meia fornada não existe.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "work_materials",
            tickLimit = 20)
    public void threePanesCostOneBatchOfGlass(TestContext context) {
        ServerWorld world = context.getWorld();

        ColonyPos here = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(HERE));

        VillagePalette palette = HousePlans.paletteOf(world, here);

        context.assertTrue(
                WorkMaterials.through(world, ResourceId.vanilla("glass_pane"), ResourceType.GLASS, 0) == 0,
                "obra sem janela não pode pedir vidro");

        int forThree = WorkMaterials.through(world, ResourceId.vanilla("glass_pane"), ResourceType.GLASS, 3);

        context.assertTrue(
                forThree == 6,
                "três vidraças deviam custar uma fornada de 6 vidros, e custaram " + forThree);

        // Dezessete passam de uma fornada: a segunda entra inteira.
        int forSeventeen = WorkMaterials.through(world, ResourceId.vanilla("glass_pane"), ResourceType.GLASS, 17);

        context.assertTrue(
                forSeventeen == 12,
                "dezessete vidraças deviam custar duas fornadas, e custaram " + forSeventeen);

        context.complete();
    }

    /**
     * A tocha da parede vira carvão pela receita do jogo — 2026-08-21.
     *
     * <p>A mesma conta do vidro e o mesmo silêncio possível: a casa pede
     * <b>tocha</b>, e não carvão. Um carvão dá quatro tochas, e as três
     * da casa de planície custam uma fornada — o resto fica no baú para a
     * casa seguinte.
     *
     * <p><b>O que este teste realmente guarda</b> é que
     * {@code wall_torch} chega ao item {@code torch}: a tocha de parede
     * não tem item próprio no jogo, e se essa ponte quebrar a conta
     * devolve zero e a colônia para de pedir carvão sem dizer nada.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "work_materials",
            tickLimit = 20)
    public void threeTorchesCostOneCoal(TestContext context) {
        ServerWorld world = context.getWorld();

        ResourceId wallTorch = ResourceId.vanilla("wall_torch");

        context.assertTrue(
                WorkMaterials.through(world, wallTorch, ResourceType.COAL, 0) == 0,
                "obra sem tocha não pode pedir carvão");

        int forThree = WorkMaterials.through(world, wallTorch, ResourceType.COAL, 3);

        context.assertTrue(
                forThree == 1,
                "três tochas deviam custar um carvão, e custaram " + forThree);

        // Cinco passam da fornada de quatro: a segunda entra inteira.
        int forFive = WorkMaterials.through(world, wallTorch, ResourceType.COAL, 5);

        context.assertTrue(
                forFive == 2,
                "cinco tochas deviam custar dois carvões, e custaram " + forFive);

        context.complete();
    }

    /**
     * O lampião desce dois degraus até o lingote — 2026-08-21.
     *
     * <p><b>O caso que pediu o segundo degrau.</b> O lampião não pede
     * lingote: pede oito <b>pepitas</b>, e a pepita é que sai do lingote,
     * nove de cada. Um degrau só devolvia zero — e com zero o fundidor
     * nunca recebia tarefa de ferro, que foi exatamente o que aconteceu
     * no dia em que ele aprendeu a fundi-lo.
     *
     * <p>Um lampião custa um lingote, e sobra pepita: nove saem de um, e
     * ele quer oito. Sobrar é melhor que faltar — meia fornada não
     * existe.
     *
     * <p>Pergunta a {@code through} direto desde 2026-08-21. Até ali
     * {@code iron} recebia a contagem de lampões pronta, da passagem de
     * mobília; agora ela mesma pergunta à obra aberta, e a obra precisa
     * de um mundo montado. O que este teste quer é a <b>receita</b>, e
     * ela está aqui inteira.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "work_materials",
            tickLimit = 20)
    public void oneLanternCostsOneIngot(TestContext context) {
        ServerWorld world = context.getWorld();

        context.assertTrue(
                lanternsCost(world, 0) == 0,
                "casa sem lampião não pode pedir ferro");

        int forOne = lanternsCost(world, 1);

        context.assertTrue(
                forOne == 1,
                "um lampião devia custar um lingote, e custou " + forOne);

        // Dois lampiões são dezesseis pepitas: duas fornadas de nove.
        int forTwo = lanternsCost(world, 2);

        context.assertTrue(
                forTwo == 2,
                "dois lampiões deviam custar dois lingotes, e custaram " + forTwo);

        context.complete();
    }

    private static int lanternsCost(ServerWorld world, int lanterns) {
        return WorkMaterials.through(
                world, WorkMaterials.LANTERN, ResourceType.IRON_INGOT, lanterns);
    }

    /**
     * A cama desce um degrau até a lã — 2026-08-21.
     *
     * <p>Nasceu com a morte da Regra 21. A conta de lã era {@code camas
     * × 3}, escrita à mão na passagem de mobília; passou a sair da
     * receita do jogo, como o vidro e o carvão já saíam. O número
     * continua sendo três — e a diferença é que agora quem o diz é o
     * livro de receitas, e um datapack que mude a cama muda a conta
     * junto.
     *
     * <p><b>E ele já pegou um defeito.</b> O jogo tem mais de uma receita
     * para {@code white_bed}, e uma delas é tingir uma cama preta: o
     * livro devolveu essa, e a conta de lã deu zero. Zero aqui é o
     * pastor sem tarefa e a casa sem cama, em silêncio. Ver
     * {@code WorkMaterials.sameFamily}.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "work_materials",
            tickLimit = 20)
    public void oneBedCostsThreeWool(TestContext context) {
        ServerWorld world = context.getWorld();

        int forOne = bedsCost(world, 1);

        context.assertTrue(
                forOne == 3,
                "uma cama devia custar três lãs, e custou " + forOne);

        int forTwo = bedsCost(world, 2);

        context.assertTrue(
                forTwo == 6,
                "duas camas deviam custar seis lãs, e custaram " + forTwo);

        context.assertTrue(bedsCost(world, 0) == 0, "casa sem cama não pede lã");

        context.complete();
    }

    private static int bedsCost(ServerWorld world, int beds) {
        return WorkMaterials.through(
                world, ResourceId.vanilla("white_bed"), ResourceType.WHITE_WOOL, beds);
    }

    /**
     * A casa do catálogo pede vidraça com este nome exato.
     *
     * <p>É o elo que pode quebrar em silêncio. {@code GlassDemand}
     * procura {@code minecraft:glass_pane} na lista de materiais da obra;
     * se a chave da lista fosse outra, a busca devolveria zero, a colônia
     * nunca pediria areia e <b>nada acusaria o erro</b> — nem exceção,
     * nem log, só um fundidor parado para sempre.
     *
     * <p>Vale igual para {@code wall_torch} e o carvão, que entrou em
     * 2026-08-21 pela mesma porta.
     *
     * <p>Por isso a afirmação é sobre o nome, e sobre a casa que a Regra
     * 27 manda construir de verdade — não sobre uma cópia no mod.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "work_materials",
            tickLimit = 20)
    public void theCatalogueHouseAsksForPanesAndTorchesByThoseNames(TestContext context) {
        ResourceId house = ResourceId.vanilla("village/plains/houses/plains_small_house_1");

        Optional<Blueprint> plan = StructureBlueprintReader.read(context.getWorld(), house);

        context.assertTrue(plan.isPresent(), "a casa de planície do catálogo não carregou");

        int panes = plan.get().materials()
                .getOrDefault(ResourceId.vanilla("glass_pane"), 0);

        context.assertTrue(
                panes > 0,
                "a casa de planície não listou glass_pane — a chave da lista mudou,"
                        + " e com ela a colônia para de pedir areia em silêncio");

        int torches = plan.get().materials()
                .getOrDefault(ResourceId.vanilla("wall_torch"), 0);

        context.assertTrue(
                torches > 0,
                "a casa de planície não listou wall_torch — a chave da lista mudou,"
                        + " e com ela a colônia para de pedir carvão em silêncio");

        context.complete();
    }
}
