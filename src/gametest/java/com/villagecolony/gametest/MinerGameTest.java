package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.Building;
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
import com.villagecolony.fabric.work.WorkMaterials;
import com.villagecolony.fabric.work.HousePlans;
import com.villagecolony.fabric.work.MineDigging;
import com.villagecolony.fabric.integration.MineMouth;
import com.villagecolony.fabric.integration.OreVein;
import com.villagecolony.fabric.integration.RingSweep;
import com.villagecolony.fabric.integration.StonePatch;
import com.villagecolony.fabric.work.MinerReport;
import com.villagecolony.fabric.work.MinerWork;
import com.villagecolony.fabric.work.SandGathering;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.List;
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
        MineDigging.shortenMineDistanceTo(NEARBY);

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

            MineDigging.restoreMineDistance();

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
                ResourceId.vanilla("village/plains/houses/plains_small_house_1"),
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
        MineDigging.shortenMineDistanceTo(NEARBY);

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

            MineDigging.restoreMineDistance();

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

        MineDigging.shortenMineDistanceTo(NEARBY);

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

            MineDigging.restoreMineDistance();

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
        SandGathering.shortenSandRadiusTo(NEARBY);

        MinerWork.run(world, colony);

        context.runAtTick(320, () -> {
            int stored = ChestInventoryReader
                    .read(world, context.getAbsolutePos(CHEST))
                    .amountOfGroup(ResourceGroup.SAND);

            context.assertTrue(stored > 0, "a areia não chegou ao baú");

            owned.cleanUp();

            MinerWork.forget(villager.getUuid());

            SandGathering.restoreSandRadius();

            context.complete();
        });
    }

    /**
     * O carvão da galeria chega ao baú, e a veia é seguida — 2026-08-21.
     *
     * <p>A Regra 29 mandou o mineiro descer vinte blocos, e até aqui ele
     * descia sem <b>ver</b>: passava ao lado do carvão e trazia
     * pedregulho. As três tochas da casa de planície ficavam por conta do
     * jogador.
     *
     * <p>Duas afirmações, e a segunda é a que dá o nome à coisa. Minério
     * não vem sozinho: o segundo carvão está <b>fora</b> do caminho da
     * escada, colado no primeiro. Se ele chegar ao baú, foi porque o
     * mineiro seguiu a veia em vez de voltar para o túnel.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "miner_ore",
            tickLimit = 400)
    public void theCoalInTheGalleryReachesTheChestAndTheVeinIsFollowed(TestContext context) {
        ServerWorld world = context.getWorld();

        ground(context);

        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());

        // O primeiro degrau da escada, contado da boca: entrada + um a
        // leste, na mesma altura. É o que MineShaft.positionAt(0) dá.
        context.setBlockState(new BlockPos(5, 2, 3), Blocks.COAL_ORE.getDefaultState());

        // E o vizinho dele, que a escada nunca visita.
        context.setBlockState(new BlockPos(5, 2, 2), Blocks.COAL_ORE.getDefaultState());

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
                ResourceType.COAL,
                8);

        task.reserveFor(villager.getUuid());

        // A boca posta à mão: o lado da descida sai do identificador da
        // colônia, que é sorteado, e um teste não pode depender de sorte
        // para saber onde a escada passa.
        ColonyPos mouth = MinecraftTypeAdapter.toColonyPos(
                context.getAbsolutePos(new BlockPos(4, 2, 3)));

        VillageColonyMod.MINES.restore(
                Mine.restore(colony.id(), MineShaft.from(mouth, Side.EAST), 0));

        MinerWork.run(world, colony);

        context.runAtTick(320, () -> {
            int coal = ChestInventoryReader
                    .read(world, context.getAbsolutePos(CHEST))
                    .amountOf(ResourceType.COAL);

            context.assertTrue(coal > 0, "o carvão da escada não chegou ao baú");

            context.assertTrue(
                    coal > 1,
                    "só veio um carvão — o mineiro voltou ao túnel e deixou a veia pela metade");

            owned.cleanUp();

            MinerWork.forget(villager.getUuid());

            context.complete();
        });
    }

    /** Chão sólido, para o aldeão andar e a pedra ter em que assentar. */
    private static void ground(TestContext context) {
        for (int x = 0; x <= 7; x++) {
            for (int z = 0; z <= 7; z++) {
                context.setBlockState(new BlockPos(x, 1, z), Blocks.DIRT.getDefaultState());
            }
        }
    }

    /**
     * O mineiro com trabalho aberto deixa linha no log.
     *
     * <p><b>O defeito era a ausência dela.</b> Na sessão de 2026-08-22
     * dois mineiros reivindicaram baú e passaram treze minutos sem
     * produzir uma linha sequer — nem "abriu mina", nem "pegou", nem
     * motivo de ociosidade. Do lado de fora, o mineiro que anda, o que
     * está parado, o que não tem picareta e o que não achou pedra eram a
     * mesma coisa: silêncio.
     *
     * <p>O lenhador ganhou a linha dele em 2026-08-12 e o construtor em
     * 08-18, depois do mesmo tipo de sessão perdida. Este teste existe
     * para que a terceira não precise acontecer de novo.
     *
     * <p>Afirma a existência e o <b>conteúdo mínimo</b>: sem o que ele
     * procura e sem quanto já juntou, a linha volta a não responder as
     * perguntas que a sessão fez.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "miner_report",
            tickLimit = 40)
    public void theMinerWithWorkLeavesALineInTheLog(TestContext context) {
        ServerWorld world = context.getWorld();

        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));

        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());
        context.setBlockState(ROCK, Blocks.STONE.getDefaultState());

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

        MineDigging.shortenMineDistanceTo(NEARBY);

        MinerWork.run(world, colony);

        Optional<String> line = MinerReport.report(world, colony);

        context.assertTrue(
                line.isPresent(),
                "mineiro com trabalho aberto e nenhuma linha — é o defeito de 08-22 de volta");

        context.assertTrue(
                line.get().contains("wants "),
                "a linha não diz o que o mineiro procura: " + line.get());

        context.assertTrue(
                line.get().contains(" of 16 so far"),
                "a linha não diz quanto ele já juntou do que a tarefa pede: " + line.get());

        owned.cleanUp();

        context.complete();
    }

    /**
     * A boca da mina ganha lanterna e baú — a Regra 30.
     *
     * <p><b>Regra do autor, 2026-08-22:</b> onde o mineiro decide começar
     * a cavar aparecem uma lanterna de um lado do buraco e um baú
     * marcado como do mineiro do outro.
     *
     * <p>Afirma também que <b>chamar de novo não cria de novo</b>. A
     * mobília é posta a cada passagem em que a mina existe — mina de
     * save antigo não passou pela regra, e boca em chunk descarregado
     * falha na primeira tentativa —, então repetir precisa ser de graça.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_mouth",
            tickLimit = 20)
    public void theMineMouthGetsALanternAndAChest(TestContext context) {
        ServerWorld world = context.getWorld();

        BlockPos mouth = context.getAbsolutePos(ROCK);

        // Chão sólido em volta: sem ele nenhum vizinho serve, e o teste
        // mediria a ausência de lugar em vez da regra.
        for (Direction side : Direction.Type.HORIZONTAL) {
            context.setBlockState(ROCK.offset(side).down(), Blocks.STONE.getDefaultState());
        }

        Optional<BlockPos> chest = MineMouth.furnish(world, mouth);

        context.assertTrue(chest.isPresent(), "a boca da mina não ganhou baú");

        context.assertTrue(
                world.getBlockState(chest.get()).isOf(Blocks.CHEST),
                "o que a boca ganhou não é um baú");

        boolean lantern = false;

        for (Direction side : Direction.Type.HORIZONTAL) {
            if (world.getBlockState(mouth.offset(side)).isOf(Blocks.LANTERN)) {
                lantern = true;
            }
        }

        context.assertTrue(lantern, "a boca da mina ficou sem lanterna");

        Optional<BlockPos> again = MineMouth.furnish(world, mouth);

        context.assertTrue(
                again.isPresent() && again.get().equals(chest.get()),
                "mobiliar de novo mudou o baú de lugar — e ela roda a cada passagem");

        context.complete();
    }

    /**
     * O que é tesouro e o que não é — a Regra 30, decidida pelo autor.
     *
     * <p>O baú da boca guarda <b>todo minério menos carvão</b>. O carvão
     * fica de fora porque a colônia o consome o tempo todo — a tocha sai
     * dele e a fornalha o queima —, e mandá-lo para o fundo da mina
     * seria afastá-lo de quem o usa.
     *
     * <p>Eram dois minérios reconhecidos até 08-21, carvão e ferro.
     * Seguir só esses dois era o mineiro passando ao lado de diamante sem
     * ver, e a regra manda ele ir atrás de recurso normalmente.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_mouth",
            tickLimit = 20)
    public void everyOreButCoalIsTreasure(TestContext context) {
        for (Block ore : List.of(
                Blocks.COPPER_ORE, Blocks.IRON_ORE, Blocks.GOLD_ORE, Blocks.REDSTONE_ORE,
                Blocks.LAPIS_ORE, Blocks.EMERALD_ORE, Blocks.DIAMOND_ORE,
                Blocks.DEEPSLATE_DIAMOND_ORE, Blocks.DEEPSLATE_GOLD_ORE)) {

            context.assertTrue(
                    OreVein.isTreasure(ore.getDefaultState()),
                    ore.getName().getString() + " devia ir para o baú da mina");
        }

        for (Block common : List.of(
                Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE, Blocks.STONE,
                Blocks.COBBLESTONE, Blocks.SANDSTONE, Blocks.DIRT)) {

            context.assertTrue(
                    !OreVein.isTreasure(common.getDefaultState()),
                    common.getName().getString() + " não é tesouro, e foi para o baú da mina");
        }

        context.assertTrue(
                OreVein.isOre(Blocks.COAL_ORE.getDefaultState()),
                "o carvão deixou de ser minério, e a veia dele para de ser seguida");

        context.complete();
    }

    /**
     * A busca não desiste na primeira coluna — 2026-08-22.
     *
     * <p><b>É o defeito que fez a mina nunca abrir.</b> Até aqui
     * {@code mouthOf} olhava <b>um ponto</b> — centro mais quarenta
     * blocos numa direção fixa — e devolvia vazio se ele não servisse.
     * Três sessões de jogo terminaram com {@code 0 mines} no save e
     * mineiros mudos com tarefa aberta.
     *
     * <p>A coluna ideal é tapada por construção da colônia, que a Regra 3
     * proíbe cavar. O que se afirma é que a mina <b>ainda assim</b> acha
     * onde nascer, mais perto ou de outro lado.
     *
     * <p>A montagem se confere antes de afirmar: sem a coluna ideal
     * realmente tapada, o teste passaria por acidente e não diria nada.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_mouth",
            tickLimit = 20)
    public void theSearchTriesMoreThanOneColumn(TestContext context) {
        ServerWorld world = context.getWorld();

        for (int x = 0; x <= 7; x++) {
            for (int z = 0; z <= 7; z++) {
                context.setBlockState(new BlockPos(x, 1, z), Blocks.DIRT.getDefaultState());
            }
        }

        BlockPos center = context.getAbsolutePos(new BlockPos(4, 2, 4));

        MineDigging.shortenMineDistanceTo(3);

        // A mesma conta que mouthOf faz para a coluna ideal.
        BlockPos ideal = new BlockPos(
                center.getX() + Side.NORTH.offsetX() * 3,
                center.getY(),
                center.getZ() + Side.NORTH.offsetZ() * 3);

        UUID colonyId = UUID.randomUUID();

        VillageColonyMod.BUILDINGS.register(new Building(
                UUID.randomUUID(),
                colonyId,
                ResourceId.vanilla("village/plains/houses/plains_small_house_1"),
                MinecraftTypeAdapter.toColonyPos(ideal.add(-1, -14, -1)),
                MinecraftTypeAdapter.toColonyPos(ideal.add(1, 6, 1))));

        boolean covered = VillageColonyMod.BUILDINGS.isColonyInfrastructure(
                MinecraftTypeAdapter.toColonyPos(ideal.down()));

        Optional<BlockPos> mouth = MineDigging.mouthOf(world, center, Side.NORTH);

        MineDigging.restoreMineDistance();

        VillageColonyMod.BUILDINGS.removeOfColony(colonyId);

        context.assertTrue(covered, "a montagem falhou: a coluna ideal não ficou tapada");

        context.assertTrue(
                mouth.isPresent(),
                "a coluna ideal estava tapada e a busca desistiu — é o defeito de 08-22");

        context.assertTrue(
                Math.abs(mouth.get().getX() - ideal.getX()) > 1
                        || Math.abs(mouth.get().getZ() - ideal.getZ()) > 1,
                "a boca nasceu dentro da construção, em " + mouth.get().toShortString());

        context.complete();
    }

    /**
     * Sem boca de mina, a pedra vem da superfície — 2026-08-25.
     *
     * <p>Na sessão daquele dia as vinte e quatro colunas da boca caíram
     * todas em terreno que não serve, e a colônia ficou <b>sem fonte de
     * pedra nenhuma</b>: a obra morreu de fome esperando pedregulho e a
     * vila parou de crescer por causa do terreno em volta. A escada
     * continua sendo o caminho — é ela que traz carvão e ferro —, mas
     * não ter escada não pode ser o mesmo que não ter pedra.
     *
     * <p>A arena não tem chão nenhum de propósito: é o que faz as vinte
     * e quatro colunas falharem sem precisar montar terreno ruim. O
     * único bloco sólido é o afloramento, e ele está fora das colunas
     * que a boca tenta.
     *
     * <p>Rodado contra a alternativa desligada: {@code nextTarget}
     * devolve vazio, que era o mineiro parado com tarefa aberta.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "miner_surface",
            tickLimit = 20)
    public void withoutAMineMouthTheStoneComesFromTheSurface(TestContext context) {
        ServerWorld world = context.getWorld();

        // Alto na arena de propósito: o piso do mundo é de ardósia, e a
        // janela do StonePatch o alcançaria se o centro ficasse rente ao
        // chão. Aqui só existe um bloco de pedra na janela — o que o
        // teste pôs.
        BlockPos center = context.getAbsolutePos(new BlockPos(2, 6, 2));

        // O afloramento, longe das colunas que a boca tenta.
        BlockPos outcrop = context.getAbsolutePos(new BlockPos(6, 6, 6));

        world.setBlockState(outcrop, Blocks.STONE.getDefaultState());

        UUID worker = UUID.randomUUID();
        UUID colony = UUID.randomUUID();

        // As vinte e quatro colunas tapadas de uma vez: com a distância
        // encurtada elas cabem todas numa caixa de cinco por cinco em
        // volta do centro, e construção da colônia não serve de boca.
        VillageColonyMod.BUILDINGS.register(new Building(
                UUID.randomUUID(),
                colony,
                ResourceId.vanilla("village/plains/houses/plains_small_house_1"),
                MinecraftTypeAdapter.toColonyPos(center.add(-NEARBY, -14, -NEARBY)),
                MinecraftTypeAdapter.toColonyPos(center.add(NEARBY, 6, NEARBY))));

        MineDigging.shortenMineDistanceTo(NEARBY);
        MineDigging.shortenSurfaceRadiusTo(5);

        try {
            Optional<BlockPos> target =
                    MineDigging.nextTarget(world, worker, colony, center);

            context.assertTrue(
                    VillageColonyMod.MINES.of(colony).isEmpty(),
                    "a montagem falhou: era para nenhuma coluna servir de boca");

            context.assertTrue(
                    target.isPresent(),
                    "sem boca de mina o mineiro ficou sem nada para cavar");

            context.assertTrue(
                    target.get().equals(outcrop),
                    "o alvo não foi o afloramento, e sim " + target.get().toShortString());
        } finally {
            MineDigging.restoreMineDistance();
            MineDigging.restoreSurfaceRadius();

            RingSweep.forget(worker);

            VillageColonyMod.BUILDINGS.removeOfColony(colony);
        }

        context.complete();
    }

    /**
     * O que conta como pedra exposta, e o que não conta.
     *
     * <p>As três condições do {@code StonePatch}, e as três juntas: é da
     * família da pedra, tem ar em cima, e não é de ninguém. A do arenito
     * entra junto porque é a pedra da vila de deserto — a casa de lá pede
     * noventa e três blocos dela.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "miner_surface",
            tickLimit = 20)
    public void exposedStoneIsStoneWithAirAboveAndNoOwner(TestContext context) {
        ServerWorld world = context.getWorld();

        // No alto da arena: rente ao chão, a janela alcança o piso do
        // mundo, que é de ardósia e responderia por qualquer afirmação.
        BlockPos rock = new BlockPos(4, 6, 3);

        for (Block stone : List.of(
                Blocks.STONE, Blocks.COBBLESTONE, Blocks.ANDESITE, Blocks.DIORITE,
                Blocks.GRANITE, Blocks.DEEPSLATE, Blocks.SANDSTONE, Blocks.RED_SANDSTONE)) {

            context.setBlockState(rock, stone.getDefaultState());

            context.assertTrue(
                    StonePatch.in(world, context.getAbsolutePos(rock), context.getAbsolutePos(rock).getY())
                            .isPresent(),
                    stone.getName().getString() + " exposto devia servir ao mineiro");
        }

        // Tapada: a de cima é que estaria exposta, e ela não é pedra.
        context.setBlockState(rock, Blocks.STONE.getDefaultState());
        context.setBlockState(rock.up(), Blocks.DIRT.getDefaultState());

        context.assertTrue(
                StonePatch.in(world, context.getAbsolutePos(rock), context.getAbsolutePos(rock).getY()).isEmpty(),
                "pedra enterrada não está exposta, e o mineiro não cava para chegar nela");

        // Sob água: aldeão não mergulha.
        context.setBlockState(rock.up(), Blocks.WATER.getDefaultState());

        context.assertTrue(
                StonePatch.in(world, context.getAbsolutePos(rock), context.getAbsolutePos(rock).getY()).isEmpty(),
                "pedra sob água passou por exposta");

        context.setBlockState(rock.up(), Blocks.AIR.getDefaultState());
        context.setBlockState(rock, Blocks.DIRT.getDefaultState());

        context.assertTrue(
                StonePatch.in(world, context.getAbsolutePos(rock), context.getAbsolutePos(rock).getY()).isEmpty(),
                "terra não é pedra");

        context.complete();
    }
}
