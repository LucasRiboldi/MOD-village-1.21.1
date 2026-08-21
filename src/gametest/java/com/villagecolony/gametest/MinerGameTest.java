package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.integration.StructureBlueprintReader;
import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.VillagePalette;
import com.villagecolony.core.construction.model.MineShaft;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceGroup;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.type.Side;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ChestInventoryReader;
import com.villagecolony.fabric.work.GlassDemand;
import com.villagecolony.fabric.work.HousePlans;
import com.villagecolony.fabric.work.MinerWork;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.UUID;

/**
 * O mineiro — 2026-08-20.
 *
 * <p>Ele existe para destravar duas coisas: os 43 pedregulhos da casa de
 * planície, que a Regra 24 tinha deixado por conta do jogador, e a vila
 * de deserto, que nascia e nunca construía por não haver árvore.
 */
public class MinerGameTest implements FabricGameTest {

    private static final BlockPos CHEST = new BlockPos(2, 2, 2);

    private static final BlockPos STAND = new BlockPos(4, 2, 4);

    /** A pedra exposta, ao alcance do braço de quem está no STAND. */
    private static final BlockPos ROCK = new BlockPos(4, 2, 3);

    /**
     * A boca da mina, perto da arena.
     *
     * <p>A bateria roda arenas lado a lado no mesmo mundo, e uma mina
     * aberta a quarenta blocos cava o cenário do teste vizinho.
     */
    private static final int NEARBY = 2;

    /** A boca da mina que o save trouxe, dentro da arena deste teste. */
    private static final BlockPos MOUTH = new BlockPos(6, 1, 6);

    /**
     * A fronteira gravada: já além de {@link MineShaft#CARVED}, que é
     * onde acabam os dois lances e as duas salas e começa a galeria.
     */
    private static final int FRONTIER = MineShaft.CARVED + 48;

    /**
     * A pedra sai do mundo e entra no baú.
     *
     * <p>As duas metades da mesma regra, e é a mesma do lenhador:
     * mineração nunca cria recurso, e nunca o perde. Um pedregulho que
     * entrasse no baú sem sair do mundo seria a colônia inventando
     * matéria; um que saísse do mundo sem entrar no baú seria o E3.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "miner",
            tickLimit = 400)
    public void theStoneLeavesTheWorldAndReachesTheChest(TestContext context) {
        ServerWorld world = context.getWorld();

        ground(context);

        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());

        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));

        // A pedra que ESTA vila quer, perguntada à paleta. Escrever
        // "stone" à mão amarraria o teste ao bioma da arena, que é fixo e
        // não é escolha nossa: num bioma de deserto o mineiro procuraria
        // arenito e passaria direto por uma pedra comum.
        Block rock = MinecraftTypeAdapter
                .toBlock(HousePlans.paletteOf(world, chest).stone())
                .orElseThrow();

        context.setBlockState(ROCK, rock.getDefaultState());

        Colony colony = Colony.create(UUID.randomUUID(), chest);

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, STAND);
        villager.setBreedingAge(0);

        Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
        worker.assign(ProfessionType.MINER);

        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chest));

        owned.owning(villager.getUuid());

        Task task = VillageColonyMod.TASKS.create(
                colony.id(),
                TaskType.COLLECT_STONE,
                TaskPriority.PRODUCTION,
                ResourceType.COBBLESTONE,
                16);

        task.reserveFor(villager.getUuid());

        // Despacho: abre o trabalho. Daqui em diante quem age é o tique
        // do servidor, que é o que este teste exercita.
        // Raio curto: a bateria roda arenas vizinhas no mesmo mundo, e
        // um mineiro de raio 48 comeria a pedra do teste do lado.
        MinerWork.shortenMineDistanceTo(NEARBY);

        MinerWork.run(world, colony);

        context.runAtTick(5, () -> context.assertTrue(
                context.getBlockState(ROCK).isOf(rock),
                "aos 5 tiques a pedra não podia ter caído"));

        context.runAtTick(320, () -> {
            // Afirma que pedra saiu do mundo e entrou no baú, e não que
            // saiu *daquela* posição. O mundo do teste tem rocha exposta
            // abaixo do piso da arena, mais perto do centro que a pedra
            // montada aqui, e não há como forrá-la: ela é o terreno do
            // mundo, fora da estrutura. Amarrar a prova a uma posição
            // seria amarrá-la ao relevo do mundo de teste.
            int stored = ChestInventoryReader
                    .read(world, context.getAbsolutePos(CHEST))
                    .amountOfGroup(ResourceGroup.STONE);

            context.assertTrue(stored > 0, "a pedra não chegou ao baú");

            owned.cleanUp();

            MinerWork.forget(villager.getUuid());

            MinerWork.restoreMineDistance();

            context.complete();
        });
    }

    /**
     * Pedra de vila gerada e de casa da colônia não se toca.
     *
     * <p>A Regra 3, e para o mineiro ela morde mais que para o lenhador:
     * a vila do jogo e as casas do jogador são feitas exatamente do
     * material que ele procura. Um mineiro que cavasse qualquer pedra
     * derrubaria a igreja no primeiro ciclo.
     *
     * <p>Aqui a prova é pelo lado da colônia, e vale para a mina
     * inteira: uma construção registrada cobre tudo o que o mineiro
     * alcançaria, e nada chega ao baú. Se ele cavasse, cavaria a casa.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "miner_protection",
            tickLimit = 400)
    public void theMinerDoesNotEatTheColonysOwnWalls(TestContext context) {
        ServerWorld world = context.getWorld();

        ground(context);

        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());
        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));

        Block rock = MinecraftTypeAdapter
                .toBlock(HousePlans.paletteOf(world, chest).stone())
                .orElseThrow();

        context.setBlockState(ROCK, rock.getDefaultState());

        Colony colony = Colony.create(UUID.randomUUID(), chest);

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);

        // Uma construção da colônia cobrindo tudo o que a mina alcança:
        // a boca fica a NEARBY do centro, e ela desce vinte blocos. Se o
        // mineiro cavar qualquer coisa, cavou a casa.
        ColonyPos wall = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(ROCK));

        VillageColonyMod.BUILDINGS.register(new com.villagecolony.core.construction.model.Building(
                UUID.randomUUID(),
                colony.id(),
                com.villagecolony.core.construction.model.ColonyHut.ID,
                new ColonyPos(wall.x() - 40, wall.y() - 40, wall.z() - 40),
                new ColonyPos(wall.x() + 40, wall.y() + 40, wall.z() + 40)));

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, STAND);
        villager.setBreedingAge(0);

        Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
        worker.assign(ProfessionType.MINER);

        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chest));

        owned.owning(villager.getUuid());

        Task task = VillageColonyMod.TASKS.create(
                colony.id(),
                TaskType.COLLECT_STONE,
                TaskPriority.PRODUCTION,
                ResourceType.COBBLESTONE,
                16);

        task.reserveFor(villager.getUuid());

        // Raio curto: a bateria roda arenas vizinhas no mesmo mundo, e
        // um mineiro de raio 48 comeria a pedra do teste do lado.
        MinerWork.shortenMineDistanceTo(NEARBY);

        MinerWork.run(world, colony);

        context.runAtTick(320, () -> {
            int stored = ChestInventoryReader
                    .read(world, context.getAbsolutePos(CHEST))
                    .amountOfGroup(ResourceGroup.STONE);

            context.assertTrue(
                    stored == 0,
                    "o mineiro cavou " + stored + " de dentro da própria colônia");

            owned.cleanUp();

            MinerWork.forget(villager.getUuid());

            VillageColonyMod.BUILDINGS.removeOfColony(colony.id());

            MinerWork.restoreMineDistance();

            context.complete();
        });
    }

    /**
     * A mina que o save trouxe não é reaberta — 2026-08-20.
     *
     * <p>Até esta data a mina morava num campo do trabalho do mineiro, e
     * fechar o mundo apagava as duas coisas que custam a refazer: a boca,
     * que a sessão seguinte reprocurava e achava alguns blocos <b>abaixo</b>
     * — o bloco de ontem tinha sido cavado —, e a fronteira, que voltava
     * ao primeiro degrau e revarria índice por índice tudo o que já
     * estava aberto.
     *
     * <p>Aqui a mina entra no registro como se o save a tivesse trazido,
     * com a fronteira já na galeria sem fim. O que se afirma é o que o
     * jogador veria: a colônia continua com <b>uma</b> boca, e ela é a de
     * ontem; e a picareta anda para a frente, em vez de recomeçar.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "miner_resume",
            tickLimit = 200)
    public void theMineTheSaveBroughtIsNotDugAgain(TestContext context) {
        ServerWorld world = context.getWorld();

        ground(context);

        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());

        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));

        Colony colony = Colony.create(UUID.randomUUID(), chest);

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, STAND);
        villager.setBreedingAge(0);

        Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
        worker.assign(ProfessionType.MINER);

        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chest));

        owned.owning(villager.getUuid());

        Task task = VillageColonyMod.TASKS.create(
                colony.id(),
                TaskType.COLLECT_STONE,
                TaskPriority.PRODUCTION,
                ResourceType.COBBLESTONE,
                16);

        task.reserveFor(villager.getUuid());

        // A boca de ontem, e uma fronteira já na galeria: acima de
        // MineShaft.CARVED as duas salas e os dois lances estão abertos, e
        // é justamente o trecho que revarrer custa caro.
        ColonyPos mouth = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(MOUTH));

        VillageColonyMod.MINES.restore(
                Mine.restore(colony.id(), MineShaft.from(mouth, Side.EAST), FRONTIER));

        MinerWork.shortenMineDistanceTo(NEARBY);

        MinerWork.run(world, colony);

        context.runAtTick(120, () -> {
            Mine mine = VillageColonyMod.MINES.of(colony.id()).orElseThrow();

            context.assertTrue(
                    mine.entry().equals(mouth),
                    "a colônia trocou de boca: " + mine.entry() + " em vez de " + mouth);

            context.assertTrue(
                    mine.cut() > FRONTIER,
                    "a fronteira não andou — parou em " + mine.cut());

            owned.cleanUp();

            MinerWork.forget(villager.getUuid());

            MinerWork.restoreMineDistance();

            context.complete();
        });
    }

    /**
     * A areia sai da praia e chega ao baú — 2026-08-20.
     *
     * <p>O elo que ainda dependia do jogador. O fundidor sabia fundir
     * desde a manhã deste dia, e a areia que ele fundia era a que o
     * jogador guardava no baú: ninguém a colhia.
     *
     * <p><b>Areia não desce a mina</b>, e é o que este teste separa do
     * anterior. A Regra 29 mandou o mineiro cavar fundo, e para pedra
     * isso está certo; areia mora na praia, na duna e na margem do lago,
     * e a vinte blocos de profundidade não há nenhuma fora do deserto. A
     * mesma profissão, dois caminhos, e quem decide é o que a tarefa
     * pede.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "miner_sand",
            tickLimit = 400)
    public void theSandLeavesTheWorldAndReachesTheChest(TestContext context) {
        ServerWorld world = context.getWorld();

        ground(context);

        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());
        context.setBlockState(ROCK, Blocks.SAND.getDefaultState());

        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));

        Colony colony = Colony.create(UUID.randomUUID(), chest);

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, STAND);
        villager.setBreedingAge(0);

        Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
        worker.assign(ProfessionType.MINER);

        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chest));

        owned.owning(villager.getUuid());

        // A areia entra pela mesma porta da pedra: a tarefa é de coleta
        // do mineiro, e o que muda é o recurso pedido.
        Task task = VillageColonyMod.TASKS.create(
                colony.id(),
                TaskType.COLLECT_STONE,
                TaskPriority.PRODUCTION,
                ResourceType.SAND,
                8);

        task.reserveFor(villager.getUuid());

        // Raio curto pelo mesmo motivo da mina: a bateria roda arenas
        // vizinhas no mesmo mundo, e uma varredura de 48 blocos raspa a
        // praia do teste do lado.
        MinerWork.shortenSandRadiusTo(NEARBY);

        MinerWork.run(world, colony);

        context.runAtTick(320, () -> {
            int stored = ChestInventoryReader
                    .read(world, context.getAbsolutePos(CHEST))
                    .amountOfGroup(ResourceGroup.SAND);

            context.assertTrue(stored > 0, "a areia não chegou ao baú");

            owned.cleanUp();

            MinerWork.forget(villager.getUuid());

            MinerWork.restoreSandRadius();

            context.complete();
        });
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
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "glass_demand",
            tickLimit = 20)
    public void threePanesCostOneBatchOfGlass(TestContext context) {
        ServerWorld world = context.getWorld();

        ColonyPos here = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));

        VillagePalette palette = HousePlans.paletteOf(world, here);

        context.assertTrue(
                GlassDemand.glassForPanes(world, palette, 0) == 0,
                "obra sem janela não pode pedir vidro");

        int forThree = GlassDemand.glassForPanes(world, palette, 3);

        context.assertTrue(
                forThree == 6,
                "três vidraças deviam custar uma fornada de 6 vidros, e custaram " + forThree);

        // Dezessete passam de uma fornada: a segunda entra inteira.
        int forSeventeen = GlassDemand.glassForPanes(world, palette, 17);

        context.assertTrue(
                forSeventeen == 12,
                "dezessete vidraças deviam custar duas fornadas, e custaram " + forSeventeen);

        context.complete();
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
     * <p>Por isso a afirmação é sobre o nome, e sobre a casa que a Regra
     * 27 manda construir de verdade — não sobre uma cópia no mod.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "glass_demand",
            tickLimit = 20)
    public void theCatalogueHouseAsksForPanesByThatName(TestContext context) {
        ResourceId house = ResourceId.vanilla("village/plains/houses/plains_small_house_1");

        Optional<Blueprint> plan = StructureBlueprintReader.read(context.getWorld(), house);

        context.assertTrue(plan.isPresent(), "a casa de planície do catálogo não carregou");

        int panes = plan.get().materials()
                .getOrDefault(ResourceId.vanilla("glass_pane"), 0);

        context.assertTrue(
                panes > 0,
                "a casa de planície não listou glass_pane — a chave da lista mudou,"
                        + " e com ela a colônia para de pedir areia em silêncio");

        context.complete();
    }

    /** Chão sólido, para o aldeão andar e a pedra ter em que assentar. */
    private static void ground(TestContext context) {
        for (int x = 0; x <= 7; x++) {
            for (int z = 0; z <= 7; z++) {
                context.setBlockState(new BlockPos(x, 1, z), Blocks.DIRT.getDefaultState());
            }
        }
    }
}
