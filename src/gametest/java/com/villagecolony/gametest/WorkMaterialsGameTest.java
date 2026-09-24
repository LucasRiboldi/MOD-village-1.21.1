package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.construction.model.VillagePalette;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.fabric.work.BuilderWork;
import com.villagecolony.fabric.work.BuilderMaterials;
import java.util.UUID;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.work.PottedPlant;
import com.villagecolony.fabric.integration.CraftingLookup;
import com.villagecolony.fabric.integration.StructureBlueprintReader;
import com.villagecolony.fabric.work.HousePlans;
import com.villagecolony.fabric.work.MaterialChoice;
import com.villagecolony.fabric.work.WorkMaterials;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Map;
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

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "work_materials",
            tickLimit = 20)
    public void whiteTerracottaAcceptsPlainTerracotta(TestContext context) {
        List<Item> choices = MaterialChoice.forBlock(Blocks.WHITE_TERRACOTTA);

        context.assertTrue(choices.getFirst() == Items.WHITE_TERRACOTTA,
                "a cor exata deve continuar sendo a primeira escolha");
        context.assertTrue(choices.contains(Items.TERRACOTTA),
                "terracota branca deve aceitar terracota neutra para nao bloquear a obra por cor");
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "work_materials",
            tickLimit = 20)
    public void whiteTerracottaRequestsClayForTheSmelter(TestContext context) {
        ServerWorld world = context.getWorld();
        ColonyPos origin = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(HERE));
        Colony colony = Colony.create(UUID.randomUUID(), origin);
        ColonyFixture owned = ColonyFixture.create().owning(colony);
        VillageColonyMod.COLONIES.register(colony);

        Blueprint plan = Blueprint.of(
                ResourceId.vanilla("village/plains/houses/white_terracotta_test"),
                List.of(new BlueprintBlock(
                        new ColonyPos(0, 0, 0), MinecraftTypeAdapter.toResourceId(Blocks.WHITE_TERRACOTTA))));
        ConstructionProject project = ConstructionProject.plan(colony.id(), plan, origin);
        VillageColonyMod.CONSTRUCTIONS.register(project);
        project.moveTo(ConstructionState.PREPARING);
        project.moveTo(ConstructionState.BUILDING);

        try {
            context.assertTrue(
                    WorkMaterials.smeltedNeeds(world, colony)
                            .getOrDefault(ResourceType.TERRACOTTA, 0) == 1,
                    "a obra de terracota branca deve pedir uma terracota ao fundidor");
            context.assertTrue(
                    WorkMaterials.surfaceGatheredNeeds(world, colony)
                            .getOrDefault(ResourceType.CLAY, 0) == 1,
                    "a terracota pedida pela obra deve abrir coleta de bloco de argila");
            context.assertTrue(
                    CraftingLookup.smeltingInputsFor(world, Items.TERRACOTTA).contains(Items.CLAY),
                    "a cadeia deve usar a receita de fornalha do Minecraft, argila para terracota");
        } finally {
            owned.cleanUp();
        }

        context.complete();
    }

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

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "work_materials",
            tickLimit = 20)
    public void smoothStoneSlabsRequestSmeltedSmoothStone(TestContext context) {
        ServerWorld world = context.getWorld();
        int smoothStone = WorkMaterials.through(
                world, ResourceId.vanilla("smooth_stone_slab"),
                ResourceType.SMOOTH_STONE, 6);

        context.assertTrue(smoothStone == 3,
                "seis lajes de pedra lisa devem pedir três pedras lisas pela receita Vanilla; veio "
                        + smoothStone);
        context.assertTrue(
                CraftingLookup.smeltingInputsFor(world, Items.SMOOTH_STONE).contains(Items.STONE),
                "a fornalha Vanilla precisa transformar stone em smooth_stone");
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

    /**
     * A casa de deserto é de arenito <b>liso</b>, e a conta precisa vê-lo.
     *
     * <p><b>Este é o teste que faltava em 2026-08-21.</b> Naquele dia a
     * vila de deserto planejou a primeira casa da história do mod e o
     * mineiro ficou parado a sessão inteira: a conta perguntava por
     * {@code minecraft:sandstone} e a casa é feita de
     * {@code smooth_sandstone}, {@code sandstone_slab} e
     * {@code smooth_sandstone_stairs}. Quase zero, e zero é o mineiro
     * sem tarefa.
     *
     * <p>A afirmação é sobre a casa que a Regra 27 manda construir de
     * verdade, e ela vale nos dois sentidos: o arenito puro sozinho não
     * paga a parede, e a família paga.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "work_materials",
            tickLimit = 20)
    public void theDesertHouseIsMadeOfSmoothSandstone(TestContext context) {
        Optional<Blueprint> plan = StructureBlueprintReader.read(
                context.getWorld(),
                ResourceId.vanilla("village/desert/houses/desert_small_house_1"));

        context.assertTrue(plan.isPresent(), "a casa de deserto do catálogo não carregou");

        Map<ResourceId, Integer> materials = plan.get().materials();

        int pure = materials.getOrDefault(ResourceId.vanilla("sandstone"), 0);

        int family = 0;

        for (Map.Entry<ResourceId, Integer> entry : materials.entrySet()) {
            if (entry.getKey().path().contains("sandstone")) {
                family += entry.getValue();
            }
        }

        context.assertTrue(
                family > 0,
                "a casa de deserto não listou arenito nenhum — a paleta dela mudou");

        context.assertTrue(
                family > pure * 2,
                "o arenito puro passou a responder pela parede (" + pure + " de " + family
                        + ") — se a casa mudou, a conta por família pode voltar a ser por bloco");

        context.complete();
    }

    /**
     * A fornalha assa arenito, e quem diz isso é o jogo — 2026-08-22.
     *
     * <p><b>É o elo que faltava para a vila de deserto.</b> A casa do
     * catálogo é feita de arenito <b>liso</b> aos sessenta blocos, e a
     * colônia só sabia cavar o cru: a obra ficava em
     * {@code WAITING_RESOURCES} com o baú cheio de arenito e a parede
     * pedindo outro bloco. Visto em jogo sete vezes numa sessão.
     *
     * <p>O fundidor tinha uma tabela de duas linhas escritas à mão —
     * areia dá vidro, ferro cru dá lingote — e o arenito liso seria a
     * terceira. A ADR-009 pede o contrário, e agora a pergunta vai ao
     * livro de receitas do jogo.
     *
     * <p>Este teste é o elo que pode quebrar em silêncio: se a receita
     * mudar de forma, {@code smeltingInputsFor} devolve vazio, o fundidor
     * encerra a tarefa e <b>nada acusa</b> — só uma casa de deserto que
     * nunca sobe.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "work_materials",
            tickLimit = 20)
    public void theFurnaceTurnsSandstoneIntoTheSmoothKind(TestContext context) {
        ServerWorld world = context.getWorld();

        List<Item> raws = CraftingLookup.smeltingInputsFor(world, Items.SMOOTH_SANDSTONE);

        context.assertTrue(
                raws.contains(Items.SANDSTONE),
                "a fornalha deste jogo não faz arenito liso de arenito — veio " + raws);

        // E o caminho de volta, que é o que o fundidor executa.
        Optional<ItemStack> made = CraftingLookup.smelted(
                world, new ItemStack(Items.SANDSTONE, 1));

        context.assertTrue(
                made.isPresent() && made.get().isOf(Items.SMOOTH_SANDSTONE),
                "assar arenito não deu arenito liso");

        // O vidro e o lingote continuam saindo pela mesma porta — a
        // tabela de duas linhas morreu e ninguém pode ter ido junto.
        context.assertTrue(
                CraftingLookup.smeltingInputsFor(world, Items.GLASS).contains(Items.SAND),
                "o vidro perdeu a areia quando a tabela do fundidor saiu");

        context.assertTrue(
                CraftingLookup.smeltingInputsFor(world, Items.IRON_INGOT)
                        .contains(Items.RAW_IRON),
                "o lingote perdeu o ferro cru quando a tabela do fundidor saiu");

        context.complete();
    }
    /**
     * O arenito cortado é recurso que o JOGO sabe nomear — 2026-09-19.
     *
     * <p><b>O que isto tranca, medido na sessão de 14:01.</b> A obra
     * parou em {@code waiting for minecraft:cut_sandstone} com <b>178
     * blocos</b> por pôr, e o log dizia {@code no mason work: no task
     * open for it — 1 able to}. Das dez variantes de arenito que a casa
     * do deserto pede, só <b>duas</b> eram {@code ResourceType}.
     *
     * <p><b>Declarar sem mapear é pior que não declarar</b>: pareceria
     * conserto e a obra continuaria esperando. A classificação está no
     * {@code DesertMasonryTest}, que não precisa de mundo; aqui fica só o
     * mapeamento, que precisa do registro do jogo.
     *
     * <p><b>Mora neste batch de propósito</b>, e isso custou uma rodada:
     * a primeira versão criou o batch {@code desert_masonry} e o
     * {@code ColonyDetectionGameTest} passou a falhar — <i>"esperava ao
     * menos 30 trabalhadores, achei 24"</i>. Aquele cenário conta aldeões
     * registrados dentro de um orçamento de três passagens, e um batch a
     * mais muda o escalonamento. Isolado: os 364 passam com todo o código
     * novo e <b>sem</b> a anotação nova.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "work_materials",
            tickLimit = 100)
    public void theCutSandstoneIsAResourceTheGameCanName(TestContext context) {
        if (MinecraftTypeAdapter.toResourceType(Items.CUT_SANDSTONE)
                .filter(ResourceType.CUT_SANDSTONE::equals)
                .isEmpty()) {

            throw new AssertionError(
                    "o arenito cortado nao vira recurso: declarado e invisivel para o"
                            + " jogo — a obra continua esperando");
        }

        // E a volta: o recurso acha o item. É por este caminho que a meta
        // vira pedido de material.
        if (MinecraftTypeAdapter.toItem(ResourceType.SANDSTONE_WALL).isEmpty()) {
            throw new AssertionError("o recurso nao acha o item — a meta nunca vira pedido");
        }

        context.complete();
    }

    /**
     * O vaso com planta não tem item, e por isso a obra travava —
     * 2026-09-19.
     *
     * <p><b>Pergunta do autor:</b> <i>"algum aldeão produz cacto num
     * vaso? cria vaso? colhe cacto e replanta?"</i>. Não, não e não — e a
     * obra pagou: ela parou em {@code waiting for minecraft:potted_cactus}
     * com 148 blocos por pôr, esperando um item que <b>não pode
     * existir</b>. No Minecraft o vaso com cacto só existe como bloco.
     *
     * <p>São oito blocos assim no catálogo: os cinco vasos, a água, a
     * lava e o caldeirão.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "work_materials",
            tickLimit = 100)
    public void thePottedPlantHasNoItemAndIsMadeOfTwo(TestContext context) {
        if (Blocks.POTTED_CACTUS.asItem() != Items.AIR) {
            throw new AssertionError(
                    "o vaso com cacto passou a ter item — o conserto partia de ele NAO"
                            + " ter, e essa premissa mudou");
        }

        if (!PottedPlant.isPotted(Blocks.POTTED_CACTUS)) {
            throw new AssertionError("o vaso com cacto nao foi reconhecido como vaso");
        }

        // Os dois ingredientes de verdade, que é o que a colônia busca.
        if (PottedPlant.plantOf(Blocks.POTTED_CACTUS).filter(Items.CACTUS::equals).isEmpty()) {
            throw new AssertionError(
                    "o vaso com cacto nao achou o cacto: "
                            + PottedPlant.plantOf(Blocks.POTTED_CACTUS));
        }

        if (PottedPlant.pot() != Items.FLOWER_POT) {
            throw new AssertionError("o vaso deixou de ser o vaso");
        }

        // E um bloco comum NAO e vaso: sem esta metade a regra viraria
        // "tudo e montado de graca", e a casa sairia sem custo nenhum.
        if (PottedPlant.isPotted(Blocks.SANDSTONE)) {
            throw new AssertionError("o arenito foi tratado como vaso com planta");
        }

        context.complete();
    }

    /**
     * Bloco sem item não segura a obra — 2026-09-19.
     *
     * <p><b>É o defeito exato de 14:47</b>, e ele merece afirmação
     * própria: a obra ficou em {@code waiting for minecraft:potted_cactus}
     * com 148 blocos por pôr, esperando um item que não existe. A
     * pergunta que decide é <i>"a obra tem material para o próximo
     * bloco?"</i> — e para bloco sem item a resposta tem de ser <b>sim</b>,
     * porque ele é montado, não trazido.
     *
     * <p>A primeira versão deste conserto passou sem teste que o medisse:
     * desligar {@code hasNoItemOfItsOwn} deixava os 366 verdes. Foi a
     * mutação que cobrou.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "work_materials",
            tickLimit = 100)
    public void theBlockWithNoItemNeverHoldsTheBuild(TestContext context) {
        UUID colonyId = UUID.randomUUID();

        ColonyPos origin = MinecraftTypeAdapter.toColonyPos(
                context.getAbsolutePos(new BlockPos(1, 1, 1)));

        // Uma obra de um bloco só, e esse bloco é a lava — que não tem
        // item e nunca sairá de baú nenhum.
        Blueprint plan = Blueprint.of(
                ResourceId.vanilla("village/desert/houses/desert_small_house_1"),
                List.of(new BlueprintBlock(new ColonyPos(0, 0, 0), ResourceId.vanilla("lava"))));

        ConstructionProject project = ConstructionProject.plan(colonyId, plan, origin);

        if (!BuilderMaterials.hasMaterialForNextBlock(context.getWorld(), project)) {
            throw new AssertionError(
                    "a obra disse que falta material para um bloco SEM ITEM — ela vai"
                            + " esperar para sempre por algo que ninguem pode trazer,"
                            + " que e o defeito de 14:47 com o potted_cactus");
        }

        context.complete();
    }

    /**
     * O cut_sandstone tem receita neste jogo — 2026-09-19.
     *
     * <p><b>O que isto tranca.</b> A obra de 16:10 parou <b>22 vezes</b>
     * esperando {@code cut_sandstone} com <b>190 arenitos no baú</b>, e o
     * {@code ColonySupply.craft} devolvia falso <b>em silêncio</b>. Antes
     * de instrumentar o silêncio, vale afirmar a metade que se pode
     * afirmar sem jogo nenhum: <b>a receita existe</b>.
     *
     * <p>Se ela não existisse, o conserto seria outro — a planta, e não a
     * produção. É a distinção que o {@code CraftReasons} passa a dizer no
     * log.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "work_materials",
            tickLimit = 100)
    public void theCutSandstoneHasARecipeInThisGame(TestContext context) {
        if (CraftingLookup.billFor(context.getWorld(), Items.CUT_SANDSTONE, any -> true)
                .isEmpty()) {

            throw new AssertionError(
                    "este jogo nao tem receita para cut_sandstone — a obra esperaria"
                            + " para sempre e o conserto seria a planta, nao a producao");
        }

        // E a receita fecha com arenito, que e o que a colonia tem aos
        // cento e noventa.
        if (CraftingLookup.billFor(
                        context.getWorld(),
                        Items.CUT_SANDSTONE,
                        item -> item == Items.SANDSTONE)
                .isEmpty()) {

            throw new AssertionError(
                    "a receita de cut_sandstone nao fecha so com arenito — a colonia tem"
                            + " 190 e mesmo assim nao fabrica");
        }

        context.complete();
    }

}
