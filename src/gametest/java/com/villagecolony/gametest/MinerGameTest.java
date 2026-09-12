package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.integration.StructureBlueprintReader;
import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.VillagePalette;
import com.villagecolony.core.construction.model.MineArm;
import com.villagecolony.core.construction.model.MineShaft;
import com.villagecolony.core.coordination.WorkAssignment;
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
import com.villagecolony.fabric.integration.WorkerEquipment;
import com.villagecolony.fabric.work.WorkMaterials;
import com.villagecolony.fabric.work.HousePlans;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.core.task.model.TaskState;
import com.villagecolony.fabric.brain.WorkHours;
import com.villagecolony.fabric.integration.MineFlooding;
import com.villagecolony.fabric.integration.OreVein;
import com.villagecolony.fabric.work.MineClaims;
import com.villagecolony.fabric.work.MineDigging;
import com.villagecolony.fabric.work.MineMarks;
import com.villagecolony.fabric.work.MineRock;
import com.villagecolony.fabric.work.MineSite;
import com.villagecolony.fabric.integration.MineMouth;
import com.villagecolony.fabric.integration.OreVein;
import com.villagecolony.fabric.integration.RingSweep;
import com.villagecolony.fabric.integration.StonePatch;
import com.villagecolony.fabric.work.BuilderApproach;
import com.villagecolony.fabric.work.MinerReport;
import com.villagecolony.fabric.work.MinerReach;
import com.villagecolony.fabric.work.MinerWork;
import com.villagecolony.fabric.work.SandGathering;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.Schedule;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
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
     * A altura da verga do arco, contada da boca — quatro desde
     * 2026-09-11, decisão do autor.
     *
     * <p>Escrito aqui e não em {@code MineMouth}: a constante de lá é
     * privada, e um teste que a importasse passaria a concordar com o
     * código por construção em vez de afirmar o número que o autor
     * pediu. Se os dois divergirem, é isto que precisa cair.
     */
    private static final int ARCH_TOP = 4;

    /**
     * A fronteira gravada: já além de {@link MineShaft#CARVED}, que é
     * onde acabam os dois lances e as duas salas e começa a galeria.
     */
    private static final int FRONTIER = MineShaft.CARVED + 48;

    /**
     * A boca da mina do teste do E30, no chão da arena.
     *
     * <p>Um bloco a leste dela está o primeiro corte, que é o alvo:
     * {@code MineShaft.positionAt(0)} para {@link Side#EAST}.
     */
    private static final BlockPos DEEP_MOUTH = new BlockPos(2, 1, 5);

    /**
     * O poleiro do E30: seis blocos acima do alvo, na mesma coluna.
     *
     * <p>Zero de distância no plano, seis de distância de verdade. É a
     * superfície de onde o mineiro da sessão de 2026-08-26 cavou a mina
     * inteira sem descer nela.
     */
    private static final BlockPos PERCH = new BlockPos(3, 7, 5);

    /**
     * Passagens de mineiro congelado antes de se perguntar o que o
     * contador marcava — E36, 2026-09-04.
     *
     * <p>Um terço do {@code STILL_LIMIT}, e de propósito longe dele: o
     * que se afirma aqui não é que o guarda estourou, e sim que ele
     * <b>lembra</b>. Encostar no limite mediria as duas coisas juntas e
     * deixaria a falha ambígua.
     */
    private static final int FROZEN_PASSES = 100;

    /**
     * O alcance de braço, e ele espelha {@code MinerWork.REACH}.
     *
     * <p>Escrito aqui e não lido de lá de propósito: abrir a constante
     * de produção para o teste esconderia a pergunta que este teste faz,
     * que é justamente <b>em que medida</b> aquele quatro é medido.
     */
    private static final int ARM_REACH = 4;

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

        WorkerEquipment.equip(context.getWorld(), List.of(worker));

        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chest));

        owned.owning(villager.getUuid());

        Task task = VillageColonyMod.TASKS.create(
                colony.id(),
                TaskType.COLLECT_STONE,
                TaskPriority.PRODUCTION,
                ResourceType.COBBLESTONE,
                16);

        task.reserveFor(villager.getUuid());

        // A boca posta à mão, e é a mesma razão que o teste da galeria já
        // dava: o lado da descida sai do identificador da colônia, que é
        // sorteado, e um teste não pode depender de sorte para saber onde
        // a escada passa. Enquanto o alcance era medido no plano isso não
        // aparecia — o mineiro furava de longe e a pedra caía de qualquer
        // geometria. Com o alcance honesto do E30 a sorte passou a decidir
        // o resultado, e este teste vermelhava em 42% das rodadas.
        //
        // Boca no STAND e descida ao norte: MineShaft.positionAt(0) cai
        // exatamente no ROCK, ao alcance do braço de quem está ali.
        ColonyPos mouth = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(STAND));

        VillageColonyMod.MINES.restore(
                Mine.restore(colony.id(), MineShaft.from(mouth, Side.NORTH), 0));

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

            try {
                context.assertTrue(stored > 0, "a pedra não chegou ao baú");
            } finally {
                owned.cleanUp();

                MineDigging.restoreMineDistance();
            }

            context.complete();
        });
    }

    /**
     * Atendido o pedido, o mineiro para — 2026-09-09, o E3.
     *
     * <p><b>A tarefa não tinha como terminar.</b> Nada em produção
     * comparava o que o mineiro trouxe com o que a tarefa pediu:
     * {@code task.amount()} era lido por um lugar só no mod inteiro, o
     * {@code MinerReport}, para escrever a linha do log. A sessão de
     * 2026-09-06 mostrou o número virando enfeite — <b>496 amostras com
     * a meta ultrapassada</b>, 442 delas no mesmo mineiro em
     * <i>"105 of 32 so far"</i>, cavando pedra que a colônia já tinha.
     *
     * <p>O ciclo da colônia sabia parar e não alcançava: ele tira da fila
     * o pedido que perdeu o motivo, mas só o que ainda não começou.
     *
     * <p>Pedido de um: o teste quer a fronteira, e não a resistência.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "miner",
            tickLimit = 400)
    public void theMinerStopsOnceTheOrderIsFilled(TestContext context) {
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

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, STAND);
        villager.setBreedingAge(0);

        Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
        worker.assign(ProfessionType.MINER);

        WorkerEquipment.equip(context.getWorld(), List.of(worker));

        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chest));

        owned.owning(villager.getUuid());

        Task task = VillageColonyMod.TASKS.create(
                colony.id(),
                TaskType.COLLECT_STONE,
                TaskPriority.PRODUCTION,
                ResourceType.COBBLESTONE,
                1);

        task.reserveFor(villager.getUuid());

        ColonyPos mouth = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(STAND));

        VillageColonyMod.MINES.restore(
                Mine.restore(colony.id(), MineShaft.from(mouth, Side.NORTH), 0));

        MineDigging.shortenMineDistanceTo(NEARBY);

        MinerWork.run(world, colony);

        context.runAtTick(320, () -> {
            try {
                context.assertTrue(
                        task.state() == TaskState.COMPLETED,
                        "o pedido de um pedregulho foi atendido e a tarefa ficou em "
                                + task.state() + " — o mineiro não para de cavar");
            } finally {
                owned.cleanUp();

                MineDigging.restoreMineDistance();
            }

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

        WorkerEquipment.equip(context.getWorld(), List.of(worker));

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

            try {
                context.assertTrue(
                        stored == 0,
                        "o mineiro cavou " + stored + " de dentro da própria colônia");
            } finally {
                owned.cleanUp();

                VillageColonyMod.BUILDINGS.removeOfColony(colony.id());

                MineDigging.restoreMineDistance();
            }

            context.complete();
        });
    }

    /**
     * A boca do save é respeitada; a fronteira dele, conferida —
     * 2026-08-20, reescrito em 2026-08-28.
     *
     * <p><b>A boca continua sendo a de ontem</b>, e é metade do que este
     * teste sempre afirmou: sem ela a sessão seguinte reprocurava uma
     * entrada e achava outra alguns blocos abaixo, porque a de ontem
     * tinha sido cavada.
     *
     * <p><b>A outra metade virou o contrário, e foi o mundo que ensinou.</b>
     * Ele afirmava que a fronteira gravada era obedecida — e foi
     * justamente isso que quebrou a mina do autor. O cursor marchou
     * dezenas de blocos por dentro da rocha, o número foi para o save, e
     * a mina ficou presa apontando para um lugar que ninguém alcança.
     *
     * <p>Hoje a fronteira é <b>lida do mundo</b>: a primeira posição
     * ainda fechada na ordem de cavar. Aqui o túnel não existe — a arena
     * é rasa e nada foi aberto —, então o número do save é corrigido para
     * baixo, e é isso que se afirma. <b>Não custa cavar de novo</b>: o que
     * já está aberto é pulado com uma leitura de bloco, não com uma
     * picareta.
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

        WorkerEquipment.equip(context.getWorld(), List.of(worker));

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

            try {
                context.assertTrue(
                        mine.entry().equals(mouth),
                        "a colônia trocou de boca: " + mine.entry() + " em vez de " + mouth);

                context.assertTrue(
                        mine.arm(0).cut() < FRONTIER,
                        "acreditou num número que o mundo não confirma — ficou em "
                                + mine.arm(0).cut() + " com o túnel fechado");

                context.assertTrue(
                        mine.arm(0).cut() > 0,
                        "a picareta não andou a partir da frente de verdade");
            } finally {
                owned.cleanUp();

                MineDigging.restoreMineDistance();
            }

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

        WorkerEquipment.equip(context.getWorld(), List.of(worker));

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

            try {
                context.assertTrue(stored > 0, "a areia não chegou ao baú");
            } finally {
                owned.cleanUp();

                SandGathering.restoreSandRadius();
            }

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

        WorkerEquipment.equip(context.getWorld(), List.of(worker));

        // <b>A arena passa a equipar, como a colônia equipa</b> —
        // 2026-09-04. O tempo de quebra deixou de ser a constante de
        // diamante e passou a ser o da ferramenta na mão, e a mão deste
        // aldeão estava vazia: a pedra saltou de 6 ticks para 150 e o
        // teste estourou o limite. A constante escondia que a bateria
        // media uma picareta que ninguém segurava.
        WorkerEquipment.equip(world, List.of(worker));

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

            try {
                context.assertTrue(coal > 0, "o carvão da escada não chegou ao baú");

                context.assertTrue(
                        coal > 1,
                        "só veio um carvão — o mineiro voltou ao túnel e deixou a veia pela metade");
            } finally {
                owned.cleanUp();

            }

            context.complete();
        });
    }

    /**
     * O mineiro desce até a pedra em vez de cavá-la de longe — o E30.
     *
     * <p><b>O que a sessão de 2026-08-26 mostrou.</b> A primeira mina da
     * história do mod abriu às 23:20:18, rendeu 43 blocos e parou. O
     * relatório tem a linha que explica:
     * {@code digging Pedra at 721, 54, 897, 9 blocks away, 1/6 ticks} —
     * picareta em movimento a <b>nove blocos</b> do bloco. Nove é
     * exatamente a queda até a superfície acima dele.
     *
     * <p>{@code MinerWork.isWithinReach} mede só o plano: {@code dx} e
     * {@code dz}, nunca {@code dy}. O mineiro cava a mina inteira de pé
     * lá em cima, furando o chão para baixo, e <b>nunca entra nela</b>.
     * Funciona enquanto a escada desce debaixo dele; morre quando a
     * galeria corre na horizontal, porque aí ele precisaria ter descido.
     *
     * <p>A Regra 29 pediu o contrário, e por escrito: <i>"o mineiro anda
     * até o fim da vila e <b>desce cavando em escada</b>, para poder
     * voltar a subir"</i> — degraus de dois blocos de altura, <i>"os que
     * o aldeão precisa para caber de pé"</i>. A escada foi desenhada para
     * ser andada, e nunca foi andada.
     *
     * <p><b>Por que 171 testes não pegaram.</b> Todo teste de mina desta
     * classe põe o alvo em {@code MineShaft.positionAt(0)}, colado no
     * aldeão. No primeiro degrau o alvo está dentro dos quatro blocos nas
     * duas medidas, e a diferença entre alcance no plano e alcance de
     * verdade não existe. A galeria de vinte blocos não cabe numa arena
     * de oito, e é por isso que ela nunca foi exercitada aqui.
     *
     * <p><b>O que este teste faz, então.</b> Não finge a profundidade:
     * reproduz o <b>mecanismo</b> em escala de arena. O aldeão nasce num
     * poleiro seis blocos acima da pedra, na mesma coluna, com uma escada
     * de terra que desce até o chão. Distância no plano: zero. Distância
     * de verdade: seis.
     *
     * <p>A medida é tirada <b>no tique da quebra</b>, e não no fim: a
     * pedra cai depressa — seis tiques com a picareta de diamante que o
     * mineiro tinha até 2026-09-04, vinte e três com a de madeira que
     * ele passou a ter —, e num teste que só olhasse no fim o aldeão
     * teria descido sozinho depois e a prova passaria por engano.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "miner_descent",
            tickLimit = 400)
    public void theMinerGoesDownToTheStoneInsteadOfDiggingItFromAbove(TestContext context) {
        ServerWorld world = context.getWorld();

        ground(context);

        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());

        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));

        Block rock = MinecraftTypeAdapter
                .toBlock(HousePlans.paletteOf(world, chest).stone())
                .orElseThrow();

        // A boca no chão da arena, e o primeiro corte um bloco a leste
        // dela: é o que MineShaft.positionAt(0) dá para Side.EAST.
        BlockPos target = DEEP_MOUTH.east();

        context.setBlockState(target, rock.getDefaultState());

        // O poleiro: seis blocos acima do alvo, na mesma coluna. É a
        // superfície da sessão, em escala de arena.
        context.setBlockState(PERCH.down(), Blocks.DIRT.getDefaultState());

        // <b>E um parapeito em volta dele</b> — 2026-09-11, e ele é a
        // correção de uma instabilidade medida, não enfeite.
        //
        // O poleiro era <b>um bloco só</b>, seis no ar. O aldeão nasce em
        // cima dele e a IA dele mexe — olhar em volta, ajustar a posição
        // — antes de a primeira ordem chegar. Um passo para qualquer lado
        // que não a escada e ele <b>cai</b>, e cai para fora da estrutura,
        // no terreno natural do mundo de teste, que a arena não controla.
        // Daí ele não volta, e o caso falhava dizendo apenas que a pedra
        // não saiu do mundo.
        //
        // A medição que fechou o diagnóstico: quando dá certo, a pedra cai
        // no <b>tique 45</b> de uma janela de 320 — margem de sete vezes,
        // com travamento, imobilidade e deriva todos em zero. Não era
        // lentidão; era ele não chegar a trabalhar.
        //
        // O parapeito fecha os três lados que não levam a lugar nenhum e
        // deixa o leste aberto, que é por onde a escada desce. A prova
        // continua inteira: ele ainda precisa <b>descer</b> para alcançar
        // a pedra, que é a única coisa que este caso afirma.
        for (Direction side : Direction.Type.HORIZONTAL) {
            if (side == Direction.EAST) {
                continue;
            }

            context.setBlockState(PERCH.offset(side), Blocks.DIRT.getDefaultState());
        }


        // E a escada que desce dele até o chão, um bloco por degrau. Ela
        // existe para que a versão certa TENHA como descer: um teste que
        // o mineiro não pudesse passar não provaria nada.
        for (int step = 1; step <= 4; step++) {
            context.setBlockState(
                    PERCH.add(step, -step - 1, 0), Blocks.DIRT.getDefaultState());
        }

        Colony colony = Colony.create(UUID.randomUUID(), chest);

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, PERCH);
        villager.setBreedingAge(0);

        Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
        worker.assign(ProfessionType.MINER);

        WorkerEquipment.equip(context.getWorld(), List.of(worker));

        // Os seis tiques do javadoc são os da picareta de diamante, e
        // desde 2026-09-04 o tempo sai da ferramenta na mão em vez de
        // uma constante. Sem equipar, esta pedra pede 150.
        WorkerEquipment.equip(world, List.of(worker));

        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chest));

        owned.owning(villager.getUuid());

        Task task = VillageColonyMod.TASKS.create(
                colony.id(),
                TaskType.COLLECT_STONE,
                TaskPriority.PRODUCTION,
                ResourceType.COBBLESTONE,
                16);

        task.reserveFor(villager.getUuid());

        ColonyPos mouth = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(DEEP_MOUTH));

        VillageColonyMod.MINES.restore(
                Mine.restore(colony.id(), MineShaft.from(mouth, Side.EAST), 0));

        MineDigging.shortenMineDistanceTo(NEARBY);

        MinerWork.run(world, colony);

        BlockPos stone = context.getAbsolutePos(target);

        // A distância no tique em que a pedra sai do mundo. -1 enquanto
        // ela estiver de pé, e escrita uma vez só.
        int[] whenBroken = { -1 };

        // <b>O que este caso via quando falhava: nada</b> — 2026-09-11. Ele
        // dizia "não chegou nela de jeito nenhum" e mais nada, e com isso
        // "o mineiro não recebeu alvo", "recebeu e não achou caminho" e
        // "desceu e a picareta foi lenta" saíam com a mesma frase. É o §11
        // cobrado num teste em vez de em produção: trabalho mudo não se
        // diagnostica, e teste mudo menos ainda.
        int[] closest = { Integer.MAX_VALUE };
        int[] lowest = { Integer.MAX_VALUE };
        int[] tick = { 0 };

        context.runAtEveryTick(() -> {
            tick[0]++;

            BlockPos at = villager.getBlockPos();

            closest[0] = Math.min(
                    closest[0], (int) Math.sqrt(at.getSquaredDistance(stone)));
            lowest[0] = Math.min(lowest[0], at.getY());

            if (whenBroken[0] >= 0 || context.getBlockState(target).isOf(rock)) {
                return;
            }

            whenBroken[0] = (int) Math.sqrt(at.getSquaredDistance(stone));
        });

        context.runAtTick(320, () -> {
            try {
                context.assertTrue(
                        whenBroken[0] >= 0,
                        "a pedra não saiu do mundo — chegou a "
                                + closest[0] + " blocos dela, desceu até y=" + lowest[0]
                                + " (a pedra está em y=" + stone.getY()
                                + ", o poleiro em y=" + context.getAbsolutePos(PERCH).getY()
                                + "); alvo=" + MinerWork.targetOf(villager.getUuid())
                                + " travamento=" + MinerWork.stallOf(villager.getUuid())
                                + " imobilidade=" + MinerWork.stillnessOf(villager.getUuid())
                                + " deriva=" + MinerWork.adriftOf(villager.getUuid())
                                + " trabalhos abertos=" + MinerWork.activeJobs());

                context.assertTrue(
                        whenBroken[0] <= ARM_REACH,
                        "o mineiro quebrou a pedra de " + whenBroken[0]
                                + " blocos de distância, e o braço dele tem " + ARM_REACH
                                + ": ele cavou de cima sem descer — é o E30");
            } finally {
                owned.cleanUp();

                MineDigging.restoreMineDistance();
            }

            context.complete();
        });
    }

    /**
     * Fora do expediente, o guarda de travamento não conta — 2026-08-27.
     *
     * <p><b>O defeito.</b> {@code GoToWorkTargetTask} só anda em horário
     * de trabalho: fora dele o aldeão dorme, come e socializa, e o
     * destino da colônia espera. Mas o {@code MinerWork} contava os
     * tiques do guarda de qualquer jeito — e o guarda existe para punir
     * <b>quem anda sem chegar</b>, não quem está proibido de andar.
     *
     * <p>A sessão de 2026-08-26 pagou por isso: o contador foi de 886 a
     * 2086 com o relatório dizendo {@code off hours}, e metade do
     * orçamento de dois minutos queimou com o aldeão dormindo. O
     * {@code STALL_LIMIT} promete no javadoc <i>"tiques de expediente"</i>
     * e o código contava todos.
     *
     * <p><b>Os outros já fazem certo</b>, e o lenhador é o molde — o
     * desenho do mineiro é o dele, por decisão: {@code LumberjackWork}
     * põe {@code isWorkTime} antes do {@code ++job.stalled}, e construtor
     * e fabricante nem trabalham fora da hora.
     *
     * <p><b>Por que criança e não noite.</b> {@code WorkHours} responde
     * não para bebê, sem depender do relógio. Mexer na hora do mundo é
     * global e vaza para os testes vizinhos do mesmo lote — a
     * interferência que já custou um ciclo inteiro a esta bateria.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "miner_off_hours",
            tickLimit = 100)
    public void theStallGuardDoesNotCountOutsideWorkHours(TestContext context) {
        ServerWorld world = context.getWorld();

        ground(context);

        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());

        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));

        Block rock = MinecraftTypeAdapter
                .toBlock(HousePlans.paletteOf(world, chest).stone())
                .orElseThrow();

        // O alvo longe do aldeão, e sem escada até ele: é o arranjo do
        // E30 sem a saída, porque aqui o que se afirma é o contador.
        context.setBlockState(DEEP_MOUTH.east(), rock.getDefaultState());

        context.setBlockState(PERCH.down(), Blocks.DIRT.getDefaultState());

        Colony colony = Colony.create(UUID.randomUUID(), chest);

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);

        // Criança: WorkHours diz não sem que o relógio do mundo mude.
        VillagerEntity child = context.spawnEntity(EntityType.VILLAGER, PERCH);
        child.setBreedingAge(-24_000);

        Worker worker = VillageColonyMod.WORKERS.register(child.getUuid(), colony.id());
        worker.assign(ProfessionType.MINER);

        WorkerEquipment.equip(context.getWorld(), List.of(worker));

        VillageColonyMod.STORAGES.register(WorkerStorage.of(child.getUuid(), chest));

        owned.owning(child.getUuid());

        Task task = VillageColonyMod.TASKS.create(
                colony.id(),
                TaskType.COLLECT_STONE,
                TaskPriority.PRODUCTION,
                ResourceType.COBBLESTONE,
                16);

        task.reserveFor(child.getUuid());

        ColonyPos mouth = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(DEEP_MOUTH));

        VillageColonyMod.MINES.restore(
                Mine.restore(colony.id(), MineShaft.from(mouth, Side.EAST), 0));

        MineDigging.shortenMineDistanceTo(NEARBY);

        MinerWork.run(world, colony);

        context.runAtTick(60, () -> {
            int stalled = MinerWork.stallOf(child.getUuid());

            try {
                context.assertTrue(
                        stalled == 0,
                        "o guarda contou " + stalled + " tiques fora do expediente, e fora dele"
                                + " o aldeão está proibido de andar até a pedra");
            } finally {
                owned.cleanUp();

                MineDigging.restoreMineDistance();
            }

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

        WorkerEquipment.equip(context.getWorld(), List.of(worker));

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

        try {
            context.assertTrue(
                    line.isPresent(),
                    "mineiro com trabalho aberto e nenhuma linha — é o defeito de 08-22 de volta");

            context.assertTrue(
                    line.get().contains("wants "),
                    "a linha não diz o que o mineiro procura: " + line.get());

            context.assertTrue(
                    line.get().contains(" of 16 so far"),
                    "a linha não diz quanto ele já juntou do que a tarefa pede: " + line.get());
        } finally {
            owned.cleanUp();
        }

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

        // Chão sólido em volta, até dois blocos: o baú nasce ao lado do
        // arco desde 2026-09-11, e sem piso lá o teste mediria a
        // ausência de lugar em vez da regra.
        for (Direction side : Direction.Type.HORIZONTAL) {
            for (int out = 1; out <= 2; out++) {
                context.setBlockState(
                        ROCK.offset(side, out).down(), Blocks.STONE.getDefaultState());
            }
        }

        Optional<BlockPos> chest = MineMouth.furnish(world, mouth, Direction.SOUTH, false).chest();

        context.assertTrue(chest.isPresent(), "a boca da mina não ganhou baú");

        context.assertTrue(
                world.getBlockState(chest.get()).isOf(Blocks.CHEST),
                "o que a boca ganhou não é um baú");

        // Em cima da verga, e é a única — 2026-09-11. Eram duas: uma
        // pendurada no vão do arco e outra no chão ao lado do buraco.
        context.assertTrue(
                world.getBlockState(mouth.up(ARCH_TOP + 1)).isOf(Blocks.LANTERN),
                "a boca da mina ficou sem a lanterna em cima do arco");

        // E o baú abre: bloco sólido em cima é baú que o jogador não
        // consegue olhar por dentro, e era o que acontecia quando ele
        // nascia debaixo de um pilar.
        BlockPos over = chest.get().up();

        context.assertFalse(
                world.getBlockState(over).isSolidBlock(world, over),
                "o baú da boca ficou tapado, e baú tapado não abre: "
                        + world.getBlockState(over).getBlock());

        Optional<BlockPos> again = MineMouth.furnish(world, mouth, Direction.SOUTH, false).chest();

        context.assertTrue(
                again.isPresent() && again.get().equals(chest.get()),
                "mobiliar de novo mudou o baú de lugar — e ela roda a cada passagem");

        context.complete();
    }

    /**
     * Boca que já tem baú também ganha lanterna — visto em jogo, 08-27.
     *
     * <p>A frase do autor: <i>"faltou o lampião na entrada da mina, eu
     * mesmo botei"</i>. A mobília saía toda de dentro do mesmo {@code if}
     * — quem já tinha baú voltava na primeira linha, e a lanterna nunca
     * chegava a ser tentada.
     *
     * <p>Duas bocas caem nesse caso, e as duas são comuns: a mina que
     * volta de um save anterior à Regra 30, e a boca em que a primeira
     * tentativa achou lugar para o baú e não para a lanterna — chunk na
     * borda, encosta, água. Nas duas a lanterna nunca vinha, porque a
     * segunda chance não existia.
     *
     * <p>O baú é posto à mão aqui, e não pelo {@code furnish}: é
     * exatamente o estado de quem chega mobiliado pela metade.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_mouth",
            tickLimit = 20)
    public void aMouthThatAlreadyHasAChestStillGetsItsLantern(TestContext context) {
        ServerWorld world = context.getWorld();

        BlockPos mouth = context.getAbsolutePos(ROCK);

        // Chão sólido em volta, até dois blocos: o baú nasce ao lado do
        // arco desde 2026-09-11, e sem piso lá o teste mediria a
        // ausência de lugar em vez da regra.
        for (Direction side : Direction.Type.HORIZONTAL) {
            for (int out = 1; out <= 2; out++) {
                context.setBlockState(
                        ROCK.offset(side, out).down(), Blocks.STONE.getDefaultState());
            }
        }

        // Mobiliada pela metade: o baú está lá, a lanterna não.
        context.setBlockState(ROCK.offset(Direction.NORTH), Blocks.CHEST.getDefaultState());

        MineMouth.furnish(world, mouth, Direction.SOUTH, false);

        context.assertTrue(
                world.getBlockState(mouth.up(ARCH_TOP + 1)).isOf(Blocks.LANTERN),
                "a boca que já tinha baú ficou sem lanterna para sempre");

        // <b>E o baú encostado na boca continua sendo o dela</b>: é o
        // arranjo de toda mina aberta antes de 2026-09-11, e o chestAt
        // alcança um bloco e dois justamente para não as deserdar.
        context.assertTrue(
                MineMouth.chestAt(world, mouth).orElseThrow()
                        .equals(context.getAbsolutePos(ROCK.offset(Direction.NORTH))),
                "a boca perdeu de vista o baú que já era dela");

        context.complete();
    }

    /**
     * Arco quebrado fica quebrado — visto em jogo, 2026-09-11.
     *
     * <p>A frase do autor: <i>"deve permitir que seja destruído
     * normalmente e não reaparecendo infinitamente"</i>. Ele quebrava o
     * arco, e na passagem seguinte o arco estava de volta.
     *
     * <p><b>O que falhava, e por quê.</b> O único portão do
     * {@code raiseArch} era {@code isReplaceable()}: pedra quebrada deixa
     * ar, ar é substituível, e o mod repunha. <i>"O dono do mundo
     * desfez"</i> e <i>"ainda não construí"</i> eram o mesmo estado do
     * mundo, e nenhum teste distinguia os dois porque todos mediam a
     * primeira passagem.
     *
     * <p><b>Por que ele passa a pergunta em vez de ler o mundo.</b> É o
     * caso que prova a necessidade do campo no save: este teste derruba a
     * verga e os dois pilares, deixando a boca <b>idêntica</b> a uma que
     * nunca foi mobiliada. Nenhuma inspeção do mundo poderia acertar aqui
     * — só a memória de que o arco já subiu uma vez.
     *
     * <p>O baú segue fora da regra, e o caso confere isso: ele é lido do
     * mundo por {@code ColonyChests} e {@code MinerHaul}, e tem de
     * continuar renascendo quando falta.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_mouth",
            tickLimit = 20)
    public void anArchTheOwnerBrokeIsNotRaisedAgain(TestContext context) {
        ServerWorld world = context.getWorld();

        BlockPos mouth = context.getAbsolutePos(ROCK);

        // Chão sólido em volta, até dois blocos: o baú nasce ao lado do
        // arco desde 2026-09-11, e sem piso lá o teste mediria a
        // ausência de lugar em vez da regra.
        for (Direction side : Direction.Type.HORIZONTAL) {
            for (int out = 1; out <= 2; out++) {
                context.setBlockState(
                        ROCK.offset(side, out).down(), Blocks.STONE.getDefaultState());
            }
        }

        MineMouth.furnish(world, mouth, Direction.SOUTH, false);

        // A verga do topo, que é o que a lanterna pisa.
        BlockPos lintel = mouth.up(ARCH_TOP);

        context.assertTrue(
                world.getBlockState(lintel).isOf(Blocks.COBBLESTONE),
                "o arco não subiu, e sem ele este caso não mede nada");

        // <b>O jogador derruba o arco inteiro</b> — verga, lanterna e os
        // dois pilares. Deixa a boca como se nunca tivesse sido
        // mobiliada, que é justamente o ponto: só o save sabe a diferença.
        Direction side = Direction.SOUTH.rotateYClockwise();

        world.setBlockState(lintel.up(), Blocks.AIR.getDefaultState());

        for (int up = 1; up <= ARCH_TOP; up++) {
            world.setBlockState(mouth.offset(side).up(up), Blocks.AIR.getDefaultState());
            world.setBlockState(
                    mouth.offset(side.getOpposite()).up(up), Blocks.AIR.getDefaultState());
        }

        world.setBlockState(lintel, Blocks.AIR.getDefaultState());

        // A passagem seguinte, com a mina lembrando que o arco já subiu.
        MineMouth.furnish(world, mouth, Direction.SOUTH, true);

        context.assertFalse(
                world.getBlockState(lintel).isOf(Blocks.COBBLESTONE),
                "o arco voltou depois de o jogador o derrubar");

        context.assertFalse(
                world.getBlockState(lintel.up()).isOf(Blocks.LANTERN),
                "a lanterna voltou sozinha, boiando sobre o buraco");

        context.assertFalse(
                world.getBlockState(mouth.offset(side).up(1)).isOf(Blocks.COBBLESTONE),
                "um pilar do arco voltou");

        // <b>E o baú não é governado pelo arco</b>: quebrá-lo tem de o
        // trazer de volta, porque o resto do mod o procura no mundo.
        BlockPos chest = MineMouth.chestAt(world, mouth).orElseThrow();

        world.setBlockState(chest, Blocks.AIR.getDefaultState());

        MineMouth.furnish(world, mouth, Direction.SOUTH, true);

        context.assertTrue(
                MineMouth.chestAt(world, mouth).isPresent(),
                "o baú da boca não voltou, e o mod o procura no mundo");

        context.complete();
    }

    /**
     * O arco é marcado mesmo quando o baú não acha lugar — 2026-09-12.
     *
     * <p><b>Achado do {@code gauntlet-verifier}</b>, que o provou com um
     * teste próprio e o apagou ao terminar. Este é o permanente: a
     * regressão merece ficar guardada, porque ela é o defeito original
     * sobrevivendo num canto.
     *
     * <p>A primeira correção do arco exigia {@code chest.isPresent()}
     * para marcar a mina. Boca cercada — nenhum vizinho livre para o baú
     * — nunca satisfaz isso, então nunca era marcada, e o arco derrubado
     * <b>voltava para sempre</b> ali. O arco e o baú não têm relação, e
     * amarrá-los foi conveniência minha, não desenho.
     *
     * <p>Mede pelo {@code archRaisedNow}, que é o sinal que o chamador
     * usa: se ele vem falso na primeira passagem de uma boca sem baú, a
     * mina nunca será marcada e o defeito está de volta.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_mouth",
            tickLimit = 20)
    public void theArchIsMarkedEvenWhenNoChestFits(TestContext context) {
        ServerWorld world = context.getWorld();

        BlockPos mouth = context.getAbsolutePos(ROCK);

        // <b>Boca cercada</b>: bedrock em tudo o que o baú poderia ocupar
        // — os dois alcances, as quatro direções, e um abaixo, que é o
        // DROP do MineMouth. Bedrock porque não é substituível e o
        // placeChest não o derruba.
        for (Direction side : Direction.Type.HORIZONTAL) {
            for (int out = 1; out <= 2; out++) {
                for (int dy = -1; dy <= 0; dy++) {
                    world.setBlockState(
                            mouth.offset(side, out).up(dy), Blocks.BEDROCK.getDefaultState());
                }
            }
        }

        MineMouth.Furnished first = MineMouth.furnish(world, mouth, Direction.SOUTH, false);

        context.assertTrue(
                first.chest().isEmpty(),
                "o cerco falhou e o baú achou lugar — o caso não mede o que devia");

        context.assertTrue(
                first.archRaisedNow(),
                "boca sem baú não marcou o arco, e ele voltará para sempre aqui");

        context.complete();
    }

    /**
     * Posição do arco já ocupada é deixada em paz, e conta — 2026-09-12.
     *
     * <p><b>Aqui dois achados do {@code gauntlet-verifier} se contradizem,
     * e o requisito do autor decide.</b> A iteração 2 pediu que boca com
     * as nove posições bloqueadas <b>não</b> fosse marcada, para voltar a
     * tentar quando o lugar abrisse. A iteração 3 pediu que pedra natural
     * nas mesmas posições <b>fosse</b> marcada, senão o mod repõe o que o
     * jogador cava ali. Os dois casos são o mesmo estado do mundo — bloco
     * firme que o {@code layStone} respeita —, e os dois pedidos não cabem
     * juntos.
     *
     * <p>Vale o do autor: <i>"deve permitir que seja destruído normalmente
     * e não reaparecendo infinitamente"</i>. <b>Não repor o que o jogador
     * desfaz ganha de erguer arco num caso raro</b>, e o preço aceito está
     * escrito: boca que nasceu com as posições tomadas não ganha arco de
     * pedregulho mais tarde.
     *
     * <p>Mede a Regra 3 junto: o bloco de quem estava lá primeiro não é
     * substituído.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_mouth",
            tickLimit = 20)
    public void anArchPositionAlreadyTakenIsLeftAloneAndCounts(TestContext context) {
        ServerWorld world = context.getWorld();

        BlockPos mouth = context.getAbsolutePos(ROCK);
        Direction side = Direction.SOUTH.rotateYClockwise();

        // As nove posições do arco: dois pilares de três e a verga de
        // três. Bedrock porque não é substituível e o layStone o respeita.
        List<BlockPos> arch = new ArrayList<>();

        for (int up = 1; up <= ARCH_TOP; up++) {
            arch.add(mouth.offset(side).up(up));
            arch.add(mouth.offset(side.getOpposite()).up(up));
        }

        arch.add(mouth.up(ARCH_TOP));

        for (BlockPos at : arch) {
            world.setBlockState(at, Blocks.BEDROCK.getDefaultState());
        }

        MineMouth.Furnished taken = MineMouth.furnish(world, mouth, Direction.SOUTH, false);

        context.assertTrue(
                taken.archRaisedNow(),
                "posições tomadas não contaram como arco, e o mod reporá o que o jogador"
                        + " cavar ali");

        // <b>A Regra 3</b>: nada do que já estava lá foi trocado por
        // pedregulho.
        for (BlockPos at : arch) {
            context.assertTrue(
                    world.getBlockState(at).isOf(Blocks.BEDROCK),
                    "o arco passou por cima do bloco que já estava em " + at.toShortString());
        }

        context.complete();
    }

    /**
     * Pedra natural já é o arco — 2026-09-12.
     *
     * <p><b>Terceiro achado do {@code gauntlet-verifier}</b>, e o mais
     * provável dos três em jogo. O critério <i>"uma pedra posta"</i> media
     * a substituição ter dado certo, e pedra natural ocupando a posição
     * nunca passa por substituição: boca cavada dentro de rocha intacta —
     * o caso comum, porque o {@code MineSite} só valida a coluna da
     * própria boca e não os nove vizinhos do arco — não marcava nada.
     *
     * <p>E o preço era o defeito relatado, de volta com outra roupa: o
     * jogador cava uma pedra lateral para abrir espaço, e a passagem
     * seguinte <b>repõe pedregulho no buraco</b>, porque a mina nunca
     * considerou aquele arco erguido.
     *
     * <p>O critério passou a ser <i>"há pedra firme aqui"</i>, de quem
     * quer que seja: o pedregulho do mod, a rocha do mundo, ou o bloco do
     * jogador.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_mouth",
            tickLimit = 20)
    public void naturalStoneAlreadyCountsAsTheArch(TestContext context) {
        ServerWorld world = context.getWorld();

        BlockPos mouth = context.getAbsolutePos(ROCK);
        Direction side = Direction.SOUTH.rotateYClockwise();

        // Rocha intacta nas nove posições — pedra, e não bedrock: é o que
        // o mineiro encontra ao abrir a boca dentro de um morro.
        for (int up = 1; up <= ARCH_TOP; up++) {
            world.setBlockState(mouth.offset(side).up(up), Blocks.STONE.getDefaultState());
            world.setBlockState(
                    mouth.offset(side.getOpposite()).up(up), Blocks.STONE.getDefaultState());
        }

        world.setBlockState(mouth.up(ARCH_TOP), Blocks.STONE.getDefaultState());

        MineMouth.Furnished inRock = MineMouth.furnish(world, mouth, Direction.SOUTH, false);

        context.assertTrue(
                inRock.archRaisedNow(),
                "a rocha que já fazia o papel do arco não contou, e o mod vai repor o que"
                        + " o jogador cavar ali");

        // <b>E a prova do que aquilo custava</b>: com o arco dado por
        // erguido, cavar uma pedra lateral é definitivo.
        BlockPos dug = mouth.offset(side).up(1);

        world.setBlockState(dug, Blocks.AIR.getDefaultState());

        MineMouth.furnish(world, mouth, Direction.SOUTH, true);

        context.assertFalse(
                world.getBlockState(dug).isOf(Blocks.COBBLESTONE),
                "o mod repôs pedra onde o jogador cavou");

        context.complete();
    }

    /**
     * Mobiliar de novo não põe uma segunda lanterna.
     *
     * <p>A outra metade da idempotência: agora que a lanterna é
     * conferida a cada passagem, ela precisa reconhecer a que já está
     * lá — inclusive a que o <b>jogador</b> pôs, que foi como a de
     * 08-27 apareceu.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_mouth",
            tickLimit = 20)
    public void aLanternThatIsAlreadyThereIsNotDoubled(TestContext context) {
        ServerWorld world = context.getWorld();

        BlockPos mouth = context.getAbsolutePos(ROCK);

        // Chão sólido em volta, até dois blocos: o baú nasce ao lado do
        // arco desde 2026-09-11, e sem piso lá o teste mediria a
        // ausência de lugar em vez da regra.
        for (Direction side : Direction.Type.HORIZONTAL) {
            for (int out = 1; out <= 2; out++) {
                context.setBlockState(
                        ROCK.offset(side, out).down(), Blocks.STONE.getDefaultState());
            }
        }

        MineMouth.furnish(world, mouth, Direction.SOUTH, false);
        MineMouth.furnish(world, mouth, Direction.SOUTH, false);
        MineMouth.furnish(world, mouth, Direction.SOUTH, false);

        // Num cubo em volta da boca, e não só no chão: a lanterna mudou
        // de lugar em 2026-09-11 e contar onde ela estava antes deixaria
        // este caso cego para a duplicata que ele existe para pegar.
        int lanterns = 0;

        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -1; dy <= ARCH_TOP + 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (world.getBlockState(mouth.add(dx, dy, dz)).isOf(Blocks.LANTERN)) {
                        lanterns++;
                    }
                }
            }
        }

        context.assertTrue(lanterns == 1, "a boca ficou com " + lanterns + " lanternas");

        context.complete();
    }

    /**
     * A mobília da boca não entra na escada — visto no log de 21:39.
     *
     * <p>O primeiro degrau é {@code mouth.offset(descent)}, na mesma
     * altura da boca. O {@code freeSpotNear} percorria os quatro lados e
     * o pegava como qualquer outro — e na sessão das 21:39 o lampião foi
     * parar exatamente ali:
     *
     * <pre>
     * Mine mouth at 732, 63, 898 got its lantern at 731, 63, 898
     * miners: 68f4dcde digging Lanterna at 731, 63, 898, 48 blocks away
     * </pre>
     *
     * <p>O mineiro recebeu ordem de cavar a própria lanterna. E desde que
     * a mobília virou idempotente, o mod a repõe na passagem seguinte:
     * põe, o mineiro quebra, põe de novo — para sempre.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_mouth",
            tickLimit = 20)
    public void theMouthFurnitureStaysOutOfTheStaircase(TestContext context) {
        ServerWorld world = context.getWorld();

        BlockPos mouth = context.getAbsolutePos(ROCK);

        // Chão sólido em volta, até dois blocos: o baú nasce ao lado do
        // arco desde 2026-09-11, e sem piso lá o teste mediria a
        // ausência de lugar em vez da regra.
        for (Direction side : Direction.Type.HORIZONTAL) {
            for (int out = 1; out <= 2; out++) {
                context.setBlockState(
                        ROCK.offset(side, out).down(), Blocks.STONE.getDefaultState());
            }
        }

        MineMouth.furnish(world, mouth, Direction.WEST, false);

        BlockPos firstStep = mouth.offset(Direction.WEST);

        context.assertTrue(
                world.getBlockState(firstStep).isAir(),
                "a mobília ocupou o primeiro degrau: "
                        + world.getBlockState(firstStep).getBlock());

        context.assertFalse(
                world.getBlockState(firstStep.down()).isOf(Blocks.CHEST)
                        || world.getBlockState(firstStep.down()).isOf(Blocks.LANTERN),
                "a mobília ocupou a coluna da descida um abaixo");

        // E as duas peças continuam aparecendo, cada uma no lugar novo:
        // o baú ao lado do arco, a lanterna em cima dele.
        context.assertTrue(
                MineMouth.chestAt(world, mouth).isPresent(), "a boca ficou sem baú");

        context.assertTrue(
                world.getBlockState(mouth.up(ARCH_TOP + 1)).isOf(Blocks.LANTERN),
                "a boca ficou sem lanterna");

        context.complete();
    }

    /** Uma mina desta colônia, com a boca posta à mão. */
    private static Colony mineOwner(TestContext context) {
        Colony colony = Colony.create(
                UUID.randomUUID(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(6, 2, 6))));

        VillageColonyMod.COLONIES.register(colony);

        VillageColonyMod.MINES.restore(Mine.restore(
                colony.id(),
                MineShaft.from(
                        MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(ROCK)),
                        Side.EAST),
                0));

        return colony;
    }

    /** Onde o veio está agora, e o minério colado embaixo dele. */
    private static Colony veinGoingDown(TestContext context, BlockPos taken) {
        Colony colony = mineOwner(context);

        context.setBlockState(taken.down(), Blocks.COPPER_ORE.getDefaultState());

        VillageColonyMod.MINES.of(colony.id()).orElseThrow()
                .arm(0)
                .followVein(MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(taken)));

        return colony;
    }

    private static Optional<BlockPos> veinTarget(TestContext context, Colony colony) {
        return MineDigging.nextTarget(
                context.getWorld(),
                UUID.randomUUID(),
                colony.id(),
                context.getAbsolutePos(ROCK));
    }

    /**
     * O veio que desce abre o degrau antes — decisão do autor, 2026-08-27.
     *
     * <p>A frase dele: <i>"o mineiro deve sempre manter um local que
     * consiga escapar para voltar, ou que destrua bloco para poder
     * subir"</i>, e a escolha foi abrir o bloco.
     *
     * <p>A escada da Regra 29 é subível por construção desde os três
     * blocos por degrau. O <b>veio</b> não era: {@code OreVein.beside}
     * olha as seis faces, e a de baixo é a primeira da lista. Minério
     * empilhado abre um poço de um bloco de largura, e de poço não se
     * sobe — o aldeão não pula dois.
     *
     * <p>O que falta é sempre o mesmo bloco: o teto do nível de onde ele
     * veio. Com ele aberto, esse nível passa a ter os dois blocos de ar
     * que um degrau para cima pede, e a subida se faz um degrau de cada
     * vez até a boca do poço.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_vein",
            tickLimit = 20)
    public void aVeinGoingDownOpensTheStepFirst(TestContext context) {
        BlockPos taken = new BlockPos(2, 3, 2);

        Colony colony = veinGoingDown(context, taken);

        // O teto do nível de onde ele veio, fechado.
        context.setBlockState(taken.up(), Blocks.STONE.getDefaultState());

        Optional<BlockPos> next = veinTarget(context, colony);

        context.assertTrue(next.isPresent(), "o veio não devolveu alvo nenhum");

        context.assertTrue(
                next.get().equals(context.getAbsolutePos(taken.up())),
                "desceu sem abrir por onde voltar — foi para "
                        + next.get().toShortString());

        context.complete();
    }

    /**
     * Aberto o degrau, o minério de baixo é o alvo seguinte.
     *
     * <p>A outra metade, e ela é o que impede o conserto de virar
     * travamento: a regra <b>atrasa</b> a descida em uma passagem, não a
     * cancela.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_vein",
            tickLimit = 20)
    public void withTheStepOpenTheVeinGoesDown(TestContext context) {
        BlockPos taken = new BlockPos(2, 3, 2);

        Colony colony = veinGoingDown(context, taken);

        // Já aberto: nada a fazer antes de descer.
        context.setBlockState(taken.up(), Blocks.AIR.getDefaultState());

        Optional<BlockPos> next = veinTarget(context, colony);

        context.assertTrue(
                next.isPresent() && next.get().equals(context.getAbsolutePos(taken.down())),
                "o veio não desceu mesmo com o degrau aberto");

        context.complete();
    }

    /**
     * Degrau que não se abre encerra o veio, em vez de virar poço.
     *
     * <p>Bedrock, lava, ou a casa da vila por cima. Sem saída possível a
     * colônia prefere perder o minério a perder o mineiro — a escada
     * volta a mandar, e ela é subível por construção.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_vein",
            tickLimit = 20)
    public void aStepThatCannotBeOpenedEndsTheVein(TestContext context) {
        BlockPos taken = new BlockPos(2, 3, 2);

        Colony colony = veinGoingDown(context, taken);

        context.setBlockState(taken.up(), Blocks.BEDROCK.getDefaultState());

        Optional<BlockPos> next = veinTarget(context, colony);

        context.assertFalse(
                next.isPresent() && next.get().equals(context.getAbsolutePos(taken.down())),
                "desceu para um poço sem saída");

        context.assertFalse(
                next.isPresent() && next.get().equals(context.getAbsolutePos(taken.up())),
                "mandou cavar bedrock");

        context.complete();
    }

    /**
     * Veio que anda de lado não paga degrau nenhum.
     *
     * <p>O custo é do que desce, e só dele: exigir o teto de todo minério
     * faria a galeria cavar cinquenta por cento a mais para andar reto.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_vein",
            tickLimit = 20)
    public void aVeinGoingSidewaysPaysNothing(TestContext context) {
        BlockPos taken = new BlockPos(2, 3, 2);

        Colony colony = mineOwner(context);

        context.setBlockState(taken.north(), Blocks.COPPER_ORE.getDefaultState());
        context.setBlockState(taken.up(), Blocks.STONE.getDefaultState());

        VillageColonyMod.MINES.of(colony.id()).orElseThrow()
                .arm(0)
                .followVein(MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(taken)));

        Optional<BlockPos> next = veinTarget(context, colony);

        context.assertTrue(
                next.isPresent() && next.get().equals(context.getAbsolutePos(taken.north())),
                "o veio de lado foi desviado para um degrau que ninguém precisa");

        context.complete();
    }

    /**
     * Todo tipo de minério — decisão do autor, 2026-08-27.
     *
     * <p>A frase dele: <i>"ele deve minerar todo tipo de minério"</i>.
     * Havia uma lista de dezesseis nomes escrita no código, e cada
     * minério novo pedia uma linha; até alguém escrevê-la, o mineiro
     * passava por cima dele como se fosse pedra.
     *
     * <p>Quem responde agora é a etiqueta {@code c:ores} do jogo, pelo
     * mesmo caminho da Regra 27. Este teste roda dentro do jogo de
     * propósito: etiqueta só existe com registro carregado, e afirmá-la
     * fora dele seria afirmar a lista de novo.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "ore_kinds",
            tickLimit = 20)
    public void everyKindOfOreCounts(TestContext context) {
        for (Block ore : List.of(
                Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE,
                Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE,
                Blocks.COPPER_ORE, Blocks.GOLD_ORE, Blocks.REDSTONE_ORE,
                Blocks.LAPIS_ORE, Blocks.EMERALD_ORE, Blocks.DIAMOND_ORE,
                Blocks.DEEPSLATE_DIAMOND_ORE,
                // Os que a lista escrita à mão não tinha.
                Blocks.NETHER_QUARTZ_ORE, Blocks.NETHER_GOLD_ORE,
                Blocks.ANCIENT_DEBRIS)) {

            context.assertTrue(
                    OreVein.isOre(ore.getDefaultState()),
                    ore + " não foi reconhecido como minério");
        }

        for (Block plain : List.of(
                Blocks.STONE, Blocks.DEEPSLATE, Blocks.COBBLESTONE,
                Blocks.DIRT, Blocks.GRAVEL, Blocks.SANDSTONE)) {

            context.assertFalse(
                    OreVein.isOre(plain.getDefaultState()),
                    plain + " foi tratado como minério");
        }

        context.complete();
    }

    /**
     * O carvão continua não sendo tesouro, com a etiqueta no lugar da
     * lista.
     *
     * <p>A Regra 30 separa os dois, e a troca de mecanismo não podia
     * mexer nisso: carvão vai para o baú do mineiro, na vila, porque é
     * lá que a tocha e a fornalha o consomem.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "ore_kinds",
            tickLimit = 20)
    public void coalIsOreButNotTreasure(TestContext context) {
        context.assertTrue(
                OreVein.isOre(Blocks.COAL_ORE.getDefaultState()),
                "carvão deixou de ser minério");

        context.assertFalse(
                OreVein.isTreasure(Blocks.COAL_ORE.getDefaultState()),
                "carvão virou tesouro");

        context.assertFalse(
                OreVein.isTreasure(Blocks.DEEPSLATE_COAL_ORE.getDefaultState()),
                "carvão de deepslate virou tesouro");

        context.assertTrue(
                OreVein.isTreasure(Blocks.DIAMOND_ORE.getDefaultState()),
                "diamante deixou de ser tesouro");

        context.complete();
    }

    /**
     * Um pedaço de galeria de verdade: rocha maciça, e nela um túnel de
     * dois de altura que para numa parede.
     *
     * <p>Maciça de propósito. Sem teto nem paredes o {@code approachTo}
     * acha lugar de ficar de pé <b>em cima</b> da coluna da frente, e o
     * teste passa sem provar nada — foi o que a primeira versão dele
     * fez. Numa mina de verdade esse lugar é rocha.
     *
     * <pre>
     * y=4   teto
     * y=3   túnel · túnel · túnel · túnel · PAREDE   ← o bloco de cima
     * y=2   túnel · túnel · túnel · túnel · PAREDE   ← o bloco do chão
     * y=1   chão
     * </pre>
     */
    private static void galleryFace(TestContext context) {
        for (int x = 0; x <= 6; x++) {
            for (int y = 1; y <= 4; y++) {
                for (int z = 2; z <= 4; z++) {
                    context.setBlockState(
                            new BlockPos(x, y, z), Blocks.STONE.getDefaultState());
                }
            }
        }

        for (int x = 1; x <= 4; x++) {
            context.setBlockState(new BlockPos(x, 2, 3), Blocks.AIR.getDefaultState());
            context.setBlockState(new BlockPos(x, 3, 3), Blocks.AIR.getDefaultState());
        }
    }

    /**
     * O bloco de cima da galeria também tem onde se ficar de pé —
     * 2026-08-27.
     *
     * <p><b>A sessão das 22:38 não cavou um bloco.</b> Os dois mineiros
     * passaram a sessão inteira parados, e o relatório novo disse
     * exatamente onde:
     *
     * <pre>
     * digging Pedra at 729, 45, 878, 7,9 blocks away (out of reach), stall 1938/2400
     * </pre>
     *
     * <p>Sete vírgula nove, <b>congelado</b> em oito relatórios seguidos.
     * Ele não andava.
     *
     * <p><b>A geometria.</b> A galeria é de dois de altura: chão sólido,
     * ar em cima dele, ar mais um. O alvo era o bloco <b>de cima</b> da
     * coluna da frente, e o {@code approachTo} olhava só as seis faces:
     *
     * <pre>
     * atrás, mesma altura   ar, mas o teto acima é pedra — não se fica de pé
     * embaixo               ar, mas o de cima é o próprio alvo, maciço
     * os outros quatro      rocha
     * </pre>
     *
     * <p>Nenhuma servia, e o método caía no <i>"fica a própria pedra"</i>
     * — mandar o aldeão para dentro da rocha. A navegação não cumpre
     * isso, e ele fica onde está até o guarda devolver a tarefa.
     *
     * <p>O lugar de ficar de pé existia: é <b>atrás e um abaixo</b>, o
     * chão do túnel, a um metro e oito do alvo. Diagonal, e por isso
     * invisível para as seis faces.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_approach",
            tickLimit = 20)
    public void theTopBlockOfTheGalleryHasSomewhereToStand(TestContext context) {
        ServerWorld world = context.getWorld();

        galleryFace(context);

        BlockPos target = context.getAbsolutePos(new BlockPos(5, 3, 3));

        BlockPos stand = MinerWork.approachTo(world, target);

        context.assertFalse(
                stand.equals(target),
                "o destino virou a própria pedra — o aldeão não tem como chegar lá");

        context.assertTrue(
                MinerReach.isWithinReach(
                        stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, target),
                "o lugar escolhido está fora de alcance do alvo: "
                        + stand.toShortString());

        context.assertTrue(
                BuilderApproach.standable(world, stand),
                "o lugar escolhido não cabe um aldeão: " + stand.toShortString());

        context.complete();
    }

    /**
     * O bloco de baixo continua sendo alcançado de lado.
     *
     * <p>A outra metade: o conserto não podia trocar um destino bom por
     * um pior. Para o bloco do chão da frente, o lugar é atrás dele na
     * mesma altura — dentro do túnel, como se anda numa mina.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_approach",
            tickLimit = 20)
    public void theFloorBlockIsStillReachedFromTheSide(TestContext context) {
        ServerWorld world = context.getWorld();

        galleryFace(context);

        BlockPos target = context.getAbsolutePos(new BlockPos(5, 2, 3));

        BlockPos stand = MinerWork.approachTo(world, target);

        context.assertTrue(
                stand.equals(context.getAbsolutePos(new BlockPos(4, 2, 3))),
                "o bloco do chão deixou de ser alcançado de dentro do túnel: "
                        + stand.toShortString());

        context.complete();
    }

    /**
     * <b>E o lugar de ficar de pé tem de ser um que ele alcance</b> —
     * E40, 2026-09-09.
     *
     * <p>Sessão de 09-09, três vezes em dois minutos, sempre a mesma
     * pedra:
     *
     * <pre>
     * gave up the stone at 2427,48,-1437 — 2 blocks below it and unable to climb
     * </pre>
     *
     * <p><b>A geometria, e ela é a lista de deslocamentos.</b> O
     * {@code APPROACH_OFFSETS} está ordenado por distância, e o primeiro
     * de todos é {@code (0, +1, 0)} — <b>em cima da própria pedra</b>, a
     * meio bloco. Quando o teto acima dela está aberto, esse lugar é
     * pisável e a resposta sai dali sem mais nenhuma leitura.
     *
     * <p>Só que em cima da pedra é <b>dois</b> acima de quem está de pé
     * no chão do túnel ao lado dela, e aldeão sobe um. A navegação não
     * cumpre o destino, ele não anda, o guarda devolve a tarefa — e o
     * cursor da galeria <b>segura a posição</b> ({@code couldNotReach} →
     * {@code holdPositionAt}, e com razão: pular a pedra por uma
     * desistência custou três sessões com a galeria intacta). A mesma
     * pedra volta na passagem seguinte, e o laço fecha.
     *
     * <p>O lugar bom existia — o próprio chão onde ele está, atrás e um
     * abaixo do alvo, a 1,8. A busca é que não perguntava de onde ele
     * vem.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_approach",
            tickLimit = 20)
    public void theStandingPlaceIsOneTheMinerCanClimbTo(TestContext context) {
        ServerWorld world = context.getWorld();

        galleryFace(context);

        // O teto acima da frente sai, e é isso que torna "em cima da
        // pedra" pisável — a diferença entre este caso e o do vizinho.
        context.setBlockState(new BlockPos(5, 4, 3), Blocks.AIR.getDefaultState());
        context.setBlockState(new BlockPos(5, 5, 3), Blocks.AIR.getDefaultState());

        BlockPos target = context.getAbsolutePos(new BlockPos(5, 3, 3));

        BlockPos miner = context.getAbsolutePos(new BlockPos(4, 2, 3));

        BlockPos stand = MinerWork.approachTo(world, target, miner);

        context.assertTrue(
                stand.getY() - miner.getY() <= 1,
                "o mineiro foi mandado para " + (stand.getY() - miner.getY())
                        + " blocos acima dos pés dele, e aldeão sobe um: "
                        + stand.toShortString());

        context.assertTrue(
                BuilderApproach.standable(world, stand),
                "o lugar escolhido não cabe um aldeão: " + stand.toShortString());

        context.assertTrue(
                MinerReach.isWithinReach(
                        stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, target),
                "o lugar escolhido está fora de alcance do alvo: "
                        + stand.toShortString());

        context.complete();
    }

    /**
     * E em cima da pedra continua valendo quando é de lá que ele vem.
     *
     * <p>A outra metade, e a mesma de sempre: o conserto não pode trocar
     * um destino bom por um pior. O mineiro no nível de cima alcança a
     * pedra por cima, e é o lugar mais perto que existe.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_approach",
            tickLimit = 20)
    public void fromAboveTheTopOfTheStoneIsStillTheAnswer(TestContext context) {
        ServerWorld world = context.getWorld();

        galleryFace(context);

        context.setBlockState(new BlockPos(5, 4, 3), Blocks.AIR.getDefaultState());
        context.setBlockState(new BlockPos(5, 5, 3), Blocks.AIR.getDefaultState());

        BlockPos target = context.getAbsolutePos(new BlockPos(5, 3, 3));

        BlockPos above = context.getAbsolutePos(new BlockPos(4, 4, 3));

        context.assertTrue(
                MinerWork.approachTo(world, target, above)
                        .equals(context.getAbsolutePos(new BlockPos(5, 4, 3))),
                "quem vem de cima deixou de ficar em cima da pedra: "
                        + MinerWork.approachTo(world, target, above).toShortString());

        context.complete();
    }

    /**
     * A frase de "não cheguei" diz onde ele está e para onde foi —
     * 2026-08-27.
     *
     * <p><b>Duas sessões seguidas sem um bloco cavado.</b> O relatório
     * dizia a distância — 7,9 numa, 21,5 na outra, sempre congeladas — e
     * distância sozinha não escolhe entre aldeão longe demais, destino
     * que a navegação não cumpre, túnel alagado, e aldeão do outro lado
     * de uma parede. As quatro têm correções diferentes.
     *
     * <p>Molde do {@code BuilderApproach.whyNotReached}, que existe pela
     * mesma razão do lado do construtor desde 08-22.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_approach",
            tickLimit = 20)
    public void theGiveUpLineSaysWhereHeIsAndWhereHeWasGoing(TestContext context) {
        ServerWorld world = context.getWorld();

        galleryFace(context);

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, new BlockPos(1, 2, 3));

        BlockPos target = context.getAbsolutePos(new BlockPos(5, 3, 3));

        String why = MinerReport.whyNotReached(world, villager, target);

        context.assertTrue(
                why.contains(villager.getBlockPos().toShortString()),
                "a frase não diz onde o mineiro está: " + why);

        context.assertTrue(
                why.contains(target.toShortString()),
                "a frase não diz de que pedra se trata: " + why);

        context.assertTrue(
                why.contains("blocks away"),
                "a frase não diz a distância: " + why);

        context.complete();
    }

    /**
     * Quando não há onde ficar de pé, a frase diz isso com todas as
     * letras.
     *
     * <p>É a distinção que mais importa: <i>"the stone itself"</i> quer
     * dizer que o aldeão foi mandado para dentro da rocha, e a navegação
     * nunca cumpre isso. Sem a frase, esse caso é indistinguível de
     * aldeão longe demais.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_approach",
            tickLimit = 20)
    public void aStoneWithNowhereToStandSaysSo(TestContext context) {
        ServerWorld world = context.getWorld();

        // Rocha maciça sem túnel nenhum, e funda: em cima dela é lugar
        // legítimo de ficar de pé, e com alcance 4 a superfície precisa
        // ficar fora dele.
        solidRock(context);

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, new BlockPos(1, 9, 1));

        String why = MinerReport.whyNotReached(
                world, villager, context.getAbsolutePos(new BlockPos(3, 4, 3)));

        context.assertTrue(
                why.contains("the stone itself"),
                "a frase não distinguiu 'não há onde ficar de pé': " + why);

        context.complete();
    }

    /**
     * A linha de cada ciclo diz onde ele está quando não alcança —
     * 2026-08-27.
     *
     * <p>Estava só na frase de desistência, e ela sai depois de 2400
     * tiques de expediente. A sessão das 23:18 durou três minutos, o
     * guarda parou em 1177, e ela passou inteira sem que a única linha
     * capaz de responder chegasse a ser escrita.
     *
     * <p>O estado que interessa é o do travamento, não o do fim dele.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "miner_report",
            tickLimit = 40)
    public void theCycleLineSaysWhereHeIsWhenOutOfReach(TestContext context) {
        ServerWorld world = context.getWorld();

        Colony colony = mineOwner(context);

        galleryFace(context);

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, new BlockPos(1, 2, 3));
        villager.setBreedingAge(0);

        Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
        worker.assign(ProfessionType.MINER);

        WorkerEquipment.equip(context.getWorld(), List.of(worker));

        VillageColonyMod.STORAGES.register(WorkerStorage.of(
                villager.getUuid(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(1, 2, 1)))));

        Task task = VillageColonyMod.TASKS.create(
                colony.id(), TaskType.COLLECT_STONE, TaskPriority.PRODUCTION,
                ResourceType.COBBLESTONE, 8);

        task.reserveFor(villager.getUuid());

        MinerWork.run(world, colony);
        MinerWork.tick(world);

        Optional<String> line = MinerReport.report(world, colony);

        context.assertTrue(line.isPresent(), "o relatório do mineiro não saiu");

        if (line.get().contains("out of reach")) {
            context.assertTrue(
                    line.get().contains("he is at"),
                    "a linha não diz onde ele está: " + line.get());

            context.assertTrue(
                    line.get().contains("walking to"),
                    "a linha não diz para onde ele foi mandado: " + line.get());
        }

        context.complete();
    }

    /** Rocha maciça na arena, para se cavar um vazio dentro dela. */
    private static void solidRock(TestContext context) {
        for (int x = 0; x <= 6; x++) {
            for (int y = 1; y <= 8; y++) {
                for (int z = 0; z <= 6; z++) {
                    context.setBlockState(
                            new BlockPos(x, y, z), Blocks.STONE.getDefaultState());
                }
            }
        }
    }

    /**
     * O degrau seguinte da escada é alcançado de onde ele já está —
     * 2026-08-27.
     *
     * <p><b>É a geometria da própria Regra 29</b>, e o defeito estava
     * nela desde sempre. Um degrau anda um para a frente e um para
     * baixo:
     *
     * <pre>
     * degrau 1   (1, 64, 0)   onde ele está de pé
     * degrau 2   (2, 63, 0)   o alvo — DIAGONAL, não encosta em face nenhuma
     * </pre>
     *
     * <p>O {@code approachTo} olhava as seis faces e, desde 08-27, um
     * bloco abaixo de cada uma. Nenhuma alcança a diagonal. Ele caía no
     * <i>"fica a própria pedra"</i> e mandava o aldeão para dentro da
     * rocha, que a navegação não cumpre — e o aldeão estacionava.
     *
     * <p><b>E o aldeão alcançava o tempo todo:</b> de pé no degrau 1 ele
     * está a 1,1 bloco do centro do degrau 2, e o braço dele é 4. O
     * lugar existia; o método é que não sabia procurá-lo.
     *
     * <p>Explica também por que algumas sessões cavaram e outras não: a
     * galeria é reta, e blocos consecutivos dela <b>encostam</b>. Os onze
     * blocos da sessão das 22:23 foram todos de galeria; a escada e a
     * frente do túnel nunca saíram.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_approach",
            tickLimit = 20)
    public void theNextStairStepIsReachedFromTheOneBefore(TestContext context) {
        ServerWorld world = context.getWorld();

        solidRock(context);

        // O degrau já aberto: dois de ar sobre chão sólido.
        context.setBlockState(new BlockPos(2, 3, 3), Blocks.AIR.getDefaultState());
        context.setBlockState(new BlockPos(2, 4, 3), Blocks.AIR.getDefaultState());

        // O degrau seguinte: um à frente e um abaixo. Diagonal.
        BlockPos target = context.getAbsolutePos(new BlockPos(3, 2, 3));

        BlockPos stand = MinerWork.approachTo(world, target);

        context.assertFalse(
                stand.equals(target),
                "o destino virou a própria pedra — o aldeão não tem como chegar lá");

        context.assertTrue(
                BuilderApproach.standable(world, stand),
                "o lugar escolhido não cabe um aldeão: " + stand.toShortString());

        context.assertTrue(
                MinerReach.isWithinReach(
                        stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, target),
                "o lugar escolhido está fora de alcance: " + stand.toShortString());

        context.complete();
    }

    /**
     * Não havendo lugar nenhum ao alcance, continua sendo a própria
     * pedra.
     *
     * <p>Pior destino, e nunca pior que nenhum: é o que o método já
     * fazia, e o alargamento da busca não podia inventar um lugar onde
     * não há. Quem trata este caso é o guarda de travamento, e agora
     * também o recuo da galeria.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_approach",
            tickLimit = 20)
    public void buriedStoneStillFallsBackToItself(TestContext context) {
        ServerWorld world = context.getWorld();

        solidRock(context);

        // No meio da rocha, e não perto de uma face dela: em cima é
        // lugar legítimo de ficar de pé, e fora da caixa há o chão da
        // arena. As duas primeiras versões deste teste acharam
        // justamente esses, e estavam certas em achar.
        BlockPos target = context.getAbsolutePos(new BlockPos(3, 4, 3));

        context.assertTrue(
                MinerWork.approachTo(world, target).equals(target),
                "inventou um lugar de ficar de pé dentro da rocha");

        context.complete();
    }

    /**
     * O lugar escolhido cabe um aldeão de verdade — 2026-08-28.
     *
     * <p><b>Havia duas definições de "dá para ficar de pé aqui", e a
     * sessão da meia-noite as pegou discordando</b> na mesma linha:
     *
     * <pre>
     * it was walking to 732, 46, 878, which is not standable
     * </pre>
     *
     * <p>Quem escolheu o lugar achou que cabia; quem relatou achou que
     * não. A do mineiro pedia <i>qualquer coisa que não fosse ar</i>
     * embaixo — água, lava, folha, tapete servem —, e a do construtor
     * pede <b>chão sólido</b>. A do construtor é a certa: um aldeão não
     * fica de pé sobre água.
     *
     * <p>É a mesma falha que a distância tinha antes de ontem: duas
     * contas para a mesma pergunta, e o log podendo contradizer a
     * decisão. Uma conta só, e é a do construtor.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_approach",
            tickLimit = 20)
    public void theChosenSpotHoldsAVillager(TestContext context) {
        ServerWorld world = context.getWorld();

        solidRock(context);

        // Um bolsão de dois de ar sobre ÁGUA: passa na regra frouxa
        // — não é ar embaixo — e não segura ninguém de pé.
        context.setBlockState(new BlockPos(2, 3, 3), Blocks.WATER.getDefaultState());
        context.setBlockState(new BlockPos(2, 4, 3), Blocks.AIR.getDefaultState());
        context.setBlockState(new BlockPos(2, 5, 3), Blocks.AIR.getDefaultState());

        BlockPos target = context.getAbsolutePos(new BlockPos(3, 4, 3));

        BlockPos stand = MinerWork.approachTo(world, target);

        context.assertFalse(
                stand.equals(context.getAbsolutePos(new BlockPos(2, 4, 3))),
                "escolheu o bolsão sobre água — ninguém fica de pé ali");

        context.complete();
    }

    /**
     * O E33: o mineiro cava a escada dentro da rocha — 2026-08-28.
     *
     * <p><b>Este teste faltava, e é por isso que a bateria ficava verde
     * com o jogo quebrado.</b> Todos os outros testes do mineiro montam
     * um <b>piso de terra plano</b> e plantam uma pedra nele. Numa arena
     * assim não há escada, não há teto, não há degrau diagonal e não há
     * frente de galeria — nada do que o mundo de verdade tem, e nada do
     * que quebrou sete sessões seguidas.
     *
     * <p>Aqui a arena é <b>rocha maciça</b>, com um bolsão só na boca. O
     * que se afirma é a coisa que nunca foi provada em lugar nenhum:
     * <b>o mineiro tira blocos da escada, em ordem, descendo.</b>
     *
     * <p>A escada cabe: cinco degraus a partir de uma boca em
     * {@code 6,5,4} descendo para oeste ficam dentro dos oito blocos da
     * arena.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "miner_e33",
            tickLimit = 400)
    public void theMinerDigsTheStaircaseThroughSolidRock(TestContext context) {
        ServerWorld world = context.getWorld();

        // Rocha maciça, e não um piso de terra: é a diferença entre esta
        // bateria e o mundo do jogador.
        for (int x = 0; x <= 7; x++) {
            for (int y = 0; y <= 7; y++) {
                for (int z = 0; z <= 7; z++) {
                    context.setBlockState(
                            new BlockPos(x, y, z), Blocks.STONE.getDefaultState());
                }
            }
        }

        BlockPos mouth = new BlockPos(6, 5, 4);

        // O bolsão da boca: onde o aldeão cabe de pé, e o baú ao lado.
        context.setBlockState(mouth, Blocks.AIR.getDefaultState());
        context.setBlockState(mouth.up(), Blocks.AIR.getDefaultState());
        context.setBlockState(new BlockPos(7, 6, 4), Blocks.AIR.getDefaultState());
        context.setBlockState(new BlockPos(7, 5, 4), Blocks.CHEST.getDefaultState());

        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(
                context.getAbsolutePos(new BlockPos(7, 5, 4)));

        Colony colony = Colony.create(UUID.randomUUID(), chest);

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, mouth);
        villager.setBreedingAge(0);

        Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
        worker.assign(ProfessionType.MINER);

        WorkerEquipment.equip(context.getWorld(), List.of(worker));

        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chest));

        owned.owning(villager.getUuid());

        Task task = VillageColonyMod.TASKS.create(
                colony.id(), TaskType.COLLECT_STONE, TaskPriority.PRODUCTION,
                ResourceType.COBBLESTONE, 16);

        task.reserveFor(villager.getUuid());

        // A boca posta à mão: o lado da descida sai do id da colônia, que
        // é sorteado, e um teste não pode depender de sorte.
        VillageColonyMod.MINES.restore(Mine.restore(
                colony.id(),
                MineShaft.from(MinecraftTypeAdapter.toColonyPos(
                        context.getAbsolutePos(mouth)), Side.WEST),
                0));

        MinerWork.run(world, colony);

        // <b>Passagens, e não tiques de relógio</b> — 2026-09-04. O
        // guarda conta uma vez por passagem do mineiro pelo ramo "andando,
        // fora de alcance", e não uma vez por tique do servidor: o
        // relatório da falha instável mostrou {@code stall 219/2400} com
        // novecentos tiques gastos, porque nos outros seiscentos e tantos
        // ele estava reprocurando alvo. Afirmar em tique de relógio era
        // apostar numa razão que muda a cada rodada — e o teste falhava
        // uma vez em três.
        //
        // Aqui as passagens são dadas à mão, no mesmo tique: o aldeão não
        // muda de bloco entre uma e outra, o expediente não vira, e o
        // contador chega a 300 por construção. Nada do que se mede muda —
        // continua sendo "o detector de imobilidade devolve a tarefa antes
        // do guarda de travamento".
        for (int pass = 0; pass <= MinerWork.STILL_LIMIT + 20; pass++) {
            MinerWork.tick(world);
        }

        context.runAtTick(5, () -> {
            try {
                // O primeiro degrau: um bloco à frente, na altura da boca.
                context.assertTrue(
                        context.getBlockState(new BlockPos(5, 5, 4)).isAir(),
                        "o primeiro degrau da escada continua fechado — "
                                + "o mineiro não cavou nada");

                // E a cabeça dele, que é o que a escada de três abriu.
                context.assertTrue(
                        context.getBlockState(new BlockPos(5, 6, 4)).isAir(),
                        "o degrau saiu sem altura para o aldeão passar");

                int stone = ChestInventoryReader
                        .read(world, context.getAbsolutePos(new BlockPos(7, 5, 4)))
                        .amountOf(ResourceType.COBBLESTONE);

                context.assertTrue(
                        stone > 0,
                        "a pedra saiu do mundo e não chegou ao baú");
            } finally {
                owned.cleanUp();
            }

            context.complete();
        });
    }

    /**
     * A mina do save, com a fronteira adiantada, volta a cavar —
     * o E33 como ele apareceu no mundo do autor.
     *
     * <p><b>É a forma exata do defeito.</b> O cursor marchou por dentro
     * da rocha enquanto o mineiro não alcançava nada, o número foi para o
     * save, e a mina ficou apontando dezenas de posições à frente do
     * túnel de verdade. De lá nada é alcançável, e sete sessões
     * terminaram com zero blocos.
     *
     * <p>Aqui a mina entra com a fronteira no meio da rocha fechada e
     * <b>nada aberto</b>. O que se afirma é que ela se conserta sozinha:
     * a frente é lida do mundo, e o mineiro cava o primeiro degrau.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "miner_e33",
            tickLimit = 400)
    public void aMineWhoseCursorRanAheadDigsAgain(TestContext context) {
        ServerWorld world = context.getWorld();

        for (int x = 0; x <= 7; x++) {
            for (int y = 0; y <= 7; y++) {
                for (int z = 0; z <= 7; z++) {
                    context.setBlockState(
                            new BlockPos(x, y, z), Blocks.STONE.getDefaultState());
                }
            }
        }

        BlockPos mouth = new BlockPos(6, 5, 4);

        context.setBlockState(mouth, Blocks.AIR.getDefaultState());
        context.setBlockState(mouth.up(), Blocks.AIR.getDefaultState());
        context.setBlockState(new BlockPos(7, 6, 4), Blocks.AIR.getDefaultState());
        context.setBlockState(new BlockPos(7, 5, 4), Blocks.CHEST.getDefaultState());

        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(
                context.getAbsolutePos(new BlockPos(7, 5, 4)));

        Colony colony = Colony.create(UUID.randomUUID(), chest);

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, mouth);
        villager.setBreedingAge(0);

        Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
        worker.assign(ProfessionType.MINER);

        WorkerEquipment.equip(context.getWorld(), List.of(worker));

        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chest));

        owned.owning(villager.getUuid());

        Task task = VillageColonyMod.TASKS.create(
                colony.id(), TaskType.COLLECT_STONE, TaskPriority.PRODUCTION,
                ResourceType.COBBLESTONE, 16);

        task.reserveFor(villager.getUuid());

        // A fronteira do save, adiantada e mentirosa: nada disso foi
        // aberto no mundo.
        VillageColonyMod.MINES.restore(Mine.restore(
                colony.id(),
                MineShaft.from(MinecraftTypeAdapter.toColonyPos(
                        context.getAbsolutePos(mouth)), Side.WEST),
                MineShaft.CARVED + 64));

        MinerWork.run(world, colony);

        context.runAtTick(360, () -> {
            try {
                context.assertTrue(
                        context.getBlockState(new BlockPos(5, 5, 4)).isAir(),
                        "a mina ficou presa na fronteira que o save trouxe — "
                                + "é o E33 como ele apareceu em jogo");
            } finally {
                owned.cleanUp();
            }

            context.complete();
        });
    }

    /** As duas perguntas da perna, respondidas pelos blocos deste teste. */
    /**
     * O piso de verdade, e é <b>o de produção</b>.
     *
     * <p>Eram duas linhas copiadas do {@code MinerWork}, e a cópia ficou
     * para trás em 2026-09-05: a correção da escada do jogador entrou lá
     * e o teste do E32 continuou medindo o predicado antigo. Teste que
     * valida uma cópia da regra não valida a regra.
     */
    private static MinerReach.Footing realFooting(ServerWorld world) {
        return MinerWork.footingIn(world);
    }

    /**
     * <b>O E32, em rocha de verdade.</b> A perna não aponta para pedra.
     *
     * <p>Os dois testes unitários do E32 afirmam a regra sobre uma escada
     * <b>modelada por predicado</b>: eles dizem o que fazer com um mundo
     * imaginário em que só o piso é pisável. Este aqui pergunta ao mundo.
     *
     * <p>A montagem é a forma exata do defeito: o cursor entregou a escada
     * inteira — {@code cut} de trinta, o primeiro lance completo — e a
     * picareta parou no <b>terceiro degrau</b>. Do quarto em diante a ordem
     * de cavar aponta para rocha maciça que ninguém abriu.
     *
     * <pre>
     * degraus 1 a 3   abertos, três camadas cada
     * degraus 4 a 6   ainda maciços, e todos dentro da perna de oito
     * </pre>
     *
     * <p><b>Sem o filtro, o passo é o degrau 6</b> — o ponto mais avançado
     * da ordem dentro da perna, e pedra sólida. Entregue à navegação, o
     * {@code MobNavigation.findPathTo} <b>sobe</b> alvo sólido até sair da
     * rocha, e numa mina isso é a superfície. Com o filtro, o passo é o
     * piso do degrau 3, que é o ponto mais avançado onde um aldeão fica de
     * pé.
     *
     * <p><b>O que este teste não prova:</b> que o mineiro <i>anda</i> até
     * lá. Ele afirma o destino, não a caminhada — a caminhada continua
     * dependendo de sessão de jogo.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "miner_e32",
            tickLimit = 60)
    public void theLegNeverAimsAtRockNobodyDug(TestContext context) {
        ServerWorld world = context.getWorld();

        for (int x = 0; x <= 7; x++) {
            for (int y = 0; y <= 7; y++) {
                for (int z = 0; z <= 7; z++) {
                    context.setBlockState(
                            new BlockPos(x, y, z), Blocks.STONE.getDefaultState());
                }
            }
        }

        BlockPos mouth = new BlockPos(6, 5, 4);

        context.setBlockState(mouth, Blocks.AIR.getDefaultState());
        context.setBlockState(mouth.up(), Blocks.AIR.getDefaultState());

        // Os três primeiros degraus, abertos como a ordem de cavar os abre:
        // três camadas cada, descendo um e andando um para o oeste.
        for (int step = 1; step <= 3; step++) {
            for (int layer = 0; layer < MineShaft.STAIR_HEADROOM; layer++) {
                context.setBlockState(
                        new BlockPos(mouth.getX() - step, mouth.getY() - step + layer, mouth.getZ()),
                        Blocks.AIR.getDefaultState());
            }
        }

        Mine mine = Mine.restore(
                UUID.randomUUID(),
                MineShaft.from(
                        MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(mouth)),
                        Side.WEST),
                30);

        BlockPos standing = context.getAbsolutePos(mouth);

        // Um destino fora da perna, que é a condição para o passo existir:
        // dentro dos oito blocos o destino vale por si e a perna nem corre.
        BlockPos far = context.getAbsolutePos(new BlockPos(0, 0, 0));

        BlockPos leg = MinerReach.legTowards(
                standing, far, Optional.of(mine.arm(0)), realFooting(world));

        context.assertTrue(
                BuilderApproach.standable(world, leg),
                "a perna apontou para um bloco onde o aldeão não fica de pé: "
                        + leg.toShortString()
                        + " — é o E32, e o jogo levaria esse alvo para a superfície");

        context.complete();
    }

    /**
     * E ele desce: cava o degrau que não se alcança da boca.
     *
     * <p>A segunda metade, e a que separa <i>cavar</i> de <i>descer</i>.
     * O degrau 4 fica a 4,7 blocos da boca — fora do braço de quatro. Se
     * ele sair, o aldeão andou escada abaixo, que é o que sete sessões
     * de jogo não conseguiram mostrar.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "miner_e33",
            tickLimit = 600)
    public void theMinerWalksDownTheStaircaseAsItDigs(TestContext context) {
        ServerWorld world = context.getWorld();

        for (int x = 0; x <= 7; x++) {
            for (int y = 0; y <= 7; y++) {
                for (int z = 0; z <= 7; z++) {
                    context.setBlockState(
                            new BlockPos(x, y, z), Blocks.STONE.getDefaultState());
                }
            }
        }

        BlockPos mouth = new BlockPos(6, 5, 4);

        context.setBlockState(mouth, Blocks.AIR.getDefaultState());
        context.setBlockState(mouth.up(), Blocks.AIR.getDefaultState());
        context.setBlockState(new BlockPos(7, 6, 4), Blocks.AIR.getDefaultState());
        context.setBlockState(new BlockPos(7, 5, 4), Blocks.CHEST.getDefaultState());

        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(
                context.getAbsolutePos(new BlockPos(7, 5, 4)));

        Colony colony = Colony.create(UUID.randomUUID(), chest);

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, mouth);
        villager.setBreedingAge(0);

        Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
        worker.assign(ProfessionType.MINER);

        WorkerEquipment.equip(context.getWorld(), List.of(worker));

        // <b>A arena passa a equipar, como a colônia equipa</b> —
        // 2026-09-04. O tempo de quebra deixou de ser a constante de
        // diamante e passou a ser o da ferramenta na mão, e a mão deste
        // aldeão estava vazia: a pedra saltou de 6 ticks para 150 e o
        // teste estourou o limite. A constante escondia que a bateria
        // media uma picareta que ninguém segurava.
        WorkerEquipment.equip(world, List.of(worker));

        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chest));

        owned.owning(villager.getUuid());

        Task task = VillageColonyMod.TASKS.create(
                colony.id(), TaskType.COLLECT_STONE, TaskPriority.PRODUCTION,
                ResourceType.COBBLESTONE, 32);

        task.reserveFor(villager.getUuid());

        VillageColonyMod.MINES.restore(Mine.restore(
                colony.id(),
                MineShaft.from(MinecraftTypeAdapter.toColonyPos(
                        context.getAbsolutePos(mouth)), Side.WEST),
                0));

        MinerWork.run(world, colony);

        context.runAtTick(560, () -> {
            try {
                context.assertTrue(
                        context.getBlockState(new BlockPos(2, 2, 4)).isAir(),
                        "o degrau 4 continua fechado — ele cava da boca e não desce");
            } finally {
                owned.cleanUp();
            }

            context.complete();
        });
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

        Optional<BlockPos> mouth = MineSite.mouthOf(world, center, Side.NORTH);

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

        // O afloramento, fora de toda coluna que a boca tenta — inclusive
        // as da segunda passagem, que vão a 200% da distância.
        BlockPos outcrop = context.getAbsolutePos(new BlockPos(7, 6, 7));

        world.setBlockState(outcrop, Blocks.STONE.getDefaultState());

        UUID worker = UUID.randomUUID();
        UUID colony = UUID.randomUUID();

        // Todas as colunas da boca tapadas de uma vez — as três distâncias
        // da boca boa e as duas da ruim. Com a distância encurtada a mais
        // longe delas fica a 200% de NEARBY, e construção da colônia não
        // serve de boca em passagem nenhuma. A caixa desce fundo porque a
        // segunda passagem olha vinte e quatro blocos abaixo do centro.
        int reach = NEARBY * 2;

        VillageColonyMod.BUILDINGS.register(new Building(
                UUID.randomUUID(),
                colony,
                ResourceId.vanilla("village/plains/houses/plains_small_house_1"),
                MinecraftTypeAdapter.toColonyPos(center.add(-reach, -26, -reach)),
                MinecraftTypeAdapter.toColonyPos(center.add(reach, 14, reach))));

        MineDigging.shortenMineDistanceTo(NEARBY);
        MineDigging.shortenSurfaceRadiusTo(6);

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
     * <b>E a pedra de superfície também tem prazo</b> — E44, o segundo
     * achado do {@code gauntlet-verifier} em 2026-09-10.
     *
     * <p>O {@code MinerWork.giveUp} marca <b>toda</b> pedra largada, e a
     * primeira versão do conserto só tinha ensinado o lado da escada a
     * perguntar pela marca. Numa colônia sem boca de mina viável — o caso
     * que o teste acima monta — o E44 continuava inteiro: mesma pedra
     * exposta, mesma desistência, todo ciclo, sem prazo nenhum.
     *
     * <p><b>E aqui o prazo é de mão dupla</b>, ao contrário do cursor do
     * túnel: esta busca é por proximidade e refaz a volta a cada passagem,
     * então vencido o castigo o afloramento volta a ser candidato sozinho.
     * É a metade da promessa que se cumpre de verdade, e as duas fases
     * deste teste são as duas metades — sem a segunda, um filtro que
     * recusasse o afloramento <b>para sempre</b> passaria igual.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "miner_surface",
            tickLimit = 20)
    public void theRefusedOutcropSitsOutAndThenComesBack(TestContext context) {
        ServerWorld world = context.getWorld();

        BlockPos center = context.getAbsolutePos(new BlockPos(2, 6, 2));
        BlockPos outcrop = context.getAbsolutePos(new BlockPos(7, 6, 7));

        world.setBlockState(outcrop, Blocks.STONE.getDefaultState());

        UUID worker = UUID.randomUUID();
        UUID colony = UUID.randomUUID();

        int reach = NEARBY * 2;

        VillageColonyMod.BUILDINGS.register(new Building(
                UUID.randomUUID(),
                colony,
                ResourceId.vanilla("village/plains/houses/plains_small_house_1"),
                MinecraftTypeAdapter.toColonyPos(center.add(-reach, -26, -reach)),
                MinecraftTypeAdapter.toColonyPos(center.add(reach, 14, reach))));

        MineDigging.shortenMineDistanceTo(NEARBY);
        MineDigging.shortenSurfaceRadiusTo(6);

        MineMarks.refuse(world, outcrop);

        try {
            Optional<BlockPos> during =
                    MineDigging.nextTarget(world, worker, colony, center);

            context.assertFalse(
                    during.isPresent() && during.get().equals(outcrop),
                    "a pedra de castigo foi servida de novo — o E44 pela porta"
                            + " da superfície");

            // O prazo vence, e a volta seguinte da varredura a reencontra.
            MineMarks.dug(outcrop);

            RingSweep.forget(worker);

            Optional<BlockPos> after =
                    MineDigging.nextTarget(world, worker, colony, center);

            context.assertTrue(
                    after.isPresent() && after.get().equals(outcrop),
                    "vencido o prazo o afloramento não voltou: o filtro virou exílio");
        } finally {
            MineDigging.restoreMineDistance();
            MineDigging.restoreSurfaceRadius();

            RingSweep.forget(worker);

            MineMarks.clearAll();

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

    /**
     * Sem boca boa, a colônia aceita uma ruim e procura mais longe.
     *
     * <p>Decisão do autor em 2026-08-26, com estas palavras: <i>ela aceita
     * uma boca ruim, procura mais longe</i>. Até aqui "o fim da vila" era
     * teto absoluto — se nenhuma das colunas de perto servisse, não havia
     * mina, e a colônia ficava sem a raiz de pedra, carvão e ferro.
     *
     * <p>O cenário tapa <b>todas</b> as colunas da primeira passagem com
     * construção da colônia, que a Regra 3 nunca deixa virar boca. O que
     * sobra é a segunda passagem, que vai a 150% e 200% da distância.
     *
     * <p>A afirmação é a distância: a boca achada está <b>além</b> do que
     * a primeira passagem alcança. Não se afirma qual coluna — isso
     * depende do terreno do mundo da bateria, e o que a decisão manda é
     * ir mais longe, não ir a um lugar específico.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_mouth",
            tickLimit = 20)
    public void withoutAGoodMouthTheSearchGoesFartherAndSettles(TestContext context) {
        ServerWorld world = context.getWorld();

        BlockPos center = context.getAbsolutePos(new BlockPos(3, 6, 3));

        UUID colony = UUID.randomUUID();

        int near = 3;

        // Tudo o que a primeira passagem alcança é construção da colônia:
        // ela vai no máximo a `near` do centro, e a caixa desce fundo
        // porque a janela de altura também olha para baixo.
        VillageColonyMod.BUILDINGS.register(new Building(
                UUID.randomUUID(),
                colony,
                ResourceId.vanilla("village/plains/houses/plains_small_house_1"),
                MinecraftTypeAdapter.toColonyPos(center.add(-near, -26, -near)),
                MinecraftTypeAdapter.toColonyPos(center.add(near, 14, near))));

        MineDigging.shortenMineDistanceTo(near);

        try {
            Optional<BlockPos> mouth = MineSite.mouthOf(world, center, Side.NORTH);

            context.assertTrue(
                    mouth.isPresent(),
                    "todas as colunas de perto estavam tapadas e a busca desistiu —"
                            + " é a colônia sem pedra por causa do terreno em volta");

            int away = Math.max(
                    Math.abs(mouth.get().getX() - center.getX()),
                    Math.abs(mouth.get().getZ() - center.getZ()));

            context.assertTrue(
                    away > near,
                    "a boca saiu a " + away + " blocos, dentro do alcance da primeira"
                            + " passagem — era para ela ter ido mais longe");
        } finally {
            MineDigging.restoreMineDistance();

            VillageColonyMod.BUILDINGS.removeOfColony(colony);
        }

        context.complete();
    }

    /**
     * A escada é de um mineiro só — 2026-08-28.
     *
     * <p><b>A sessão de 2026-08-26, 23:23:08.</b> A colônia tinha dois
     * mineiros e duas tarefas de pedra — o {@code ColonyCycle} abre uma
     * por recurso pedido, e pedregulho e carvão são dois —, e as duas
     * apontavam para a mesma escada. Havia reserva, e ela era da
     * <b>tarefa</b>: cada um tinha a sua, e nenhuma delas falava da mina.
     *
     * <p>O cursor da galeria mora no {@code Mine} e é um. Os dois
     * recebiam a mesma posição na mesma passagem e escreviam
     * {@code could not reach the stone} no mesmo tique — e esse aviso
     * recua o cursor, que recuava duas vezes por um bloco só.
     *
     * <p>O que este teste trava é o seam onde o cursor é lido: quem não
     * é o dono sai sem alvo, e não com o alvo do outro.
     *
     * <p><b>E ele continua valendo depois dos ramais de 2026-09-04</b>,
     * para a fase que esta arena consegue hospedar. Os índices abaixo de
     * {@link MineShaft#CARVED} — a escada e as duas salas — apontam para
     * as <b>mesmas</b> posições nos quatro ramais, e por isso o poço é de
     * um mineiro só até a galeria começar. Ver {@code Mine.branchesOpenNow}.
     *
     * <p><b>A fase da galeria não cabe aqui</b>, e fica dito: a arena
     * assenta no fundo do mundo — o bedrock aparece em {@code y=-64} —, e
     * a segunda sala fica vinte blocos abaixo dela, em {@code y=-76}.
     * Fora do limite de construção, medido em 2026-09-04. Quem afirma a
     * divergência dos ramais é o {@code MineTest}, na geometria, e o
     * {@code MineClaimsTest}, na repartição.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_one_digger",
            tickLimit = 20)
    public void theSharedPitStillTakesOneMinerAtATime(TestContext context) {
        solidRock(context);

        Colony colony = mineOwner(context);

        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        BlockPos center = context.getAbsolutePos(ROCK);

        try {
            Optional<BlockPos> his =
                    MineDigging.nextTarget(context.getWorld(), first, colony.id(), center);

            Optional<BlockPos> hers =
                    MineDigging.nextTarget(context.getWorld(), second, colony.id(), center);

            context.assertTrue(
                    his.isPresent(),
                    "o primeiro mineiro saiu sem alvo, e sem isso este teste não mede nada");

            context.assertTrue(
                    hers.isEmpty(),
                    "o segundo mineiro recebeu "
                            + hers.map(BlockPos::toShortString).orElse("")
                            + " — os dois estão cavando o mesmo poço");

            context.assertTrue(
                    MineClaims.diggersIn(colony.id()) == 1,
                    "o poço aceitou " + MineClaims.diggersIn(colony.id()) + " mineiros");
        } finally {
            MineClaims.clearAll();
        }

        context.complete();
    }

    /**
     * O mineiro que espera a escada diz que espera — 2026-08-28.
     *
     * <p><b>Sem isto ele mente por omissão.</b> A linha de quem não tem
     * alvo é <i>"looking for stone"</i>, e ela é indistinguível de quem
     * está de fato procurando. O segundo mineiro não procura nada: ele
     * está barrado, e uma sessão inteira dele "procurando" mandaria o
     * autor investigar a busca.
     *
     * <p>É a lição do E31 aplicada antes de custar sessão: relatório que
     * afirma o que não mediu é pior que relatório que cala.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_one_digger",
            tickLimit = 40)
    public void theMinerWaitingForTheShaftSaysSoInTheLog(TestContext context) {
        ServerWorld world = context.getWorld();

        solidRock(context);

        Colony colony = mineOwner(context);

        ColonyFixture owned = ColonyFixture.create().owning(colony);

        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));

        // <b>São cinco mineiros desde 2026-09-04</b>, e não dois: a mina
        // tem quatro ramais, então o primeiro que espera é o quinto. Era
        // o segundo enquanto a escada era uma só.
        UUID first = miner(context, colony, chest, owned, new BlockPos(1, 9, 1));
        UUID second = miner(context, colony, chest, owned, new BlockPos(5, 9, 5));

        // Duas tarefas, uma por recurso pedido: é assim que a colônia as
        // abre, e é a forma exata do defeito de 08-26.
        reserveStone(colony, ResourceType.COBBLESTONE, first);
        reserveStone(colony, ResourceType.COAL, second);

        MinerWork.run(world, colony);

        // O primeiro desce, e o poço passa a ter dono. Enquanto ele for
        // rocha é de um só — ver Mine.branchesOpenNow.
        MineDigging.nextTarget(world, first, colony.id(), context.getAbsolutePos(ROCK));

        try {
            String line = MinerReport.report(world, colony).orElseThrow();

            context.assertTrue(
                    line.contains("waiting for a branch"),
                    "o mineiro barrado aparece procurando pedra, e não esperando: " + line);

            context.assertTrue(
                    line.contains("looking for stone"),
                    "o dono da mina devia estar procurando pedra: " + line);
        } finally {
            MineClaims.clearAll();
            owned.cleanUp();
        }

        context.complete();
    }

    /** Um mineiro desta colônia, de pé onde se mandar. */
    /**
     * Quem não achou pedra larga a escada — 2026-09-02.
     *
     * <p><b>O impasse que a sessão de 2026-09-02 mostrou inteiro.</b> A
     * reserva segue o <i>trabalho aberto</i>, e não o trabalho <i>que
     * anda</i>: o mineiro {@code 45617d43} pegou a mina, cavou um bloco,
     * e passou os dezesseis minutos seguintes em {@code looking for
     * stone} sem soltá-la. O outro passou os mesmos dezesseis em
     * {@code waiting for the shaft}. A colônia recebeu duas pedras.
     *
     * <p>Nenhuma das três saídas da mina servia. O {@code retainOnly} só
     * tira quem perdeu o trabalho, e o dele estava aberto; o
     * {@code stepAside} é chamado pelo guarda de travamento, que conta
     * tiques <b>andando até a pedra</b> — e quem não tem alvo não anda,
     * então o contador ficou em {@code stall 0/2400} nos dois. O de fora
     * também não estourava, pela mesma razão.
     *
     * <p>A regra que faltava é a mais simples: passagem que não achou
     * pedra não está usando o cursor, e escada que ninguém usa volta a
     * ser de quem quiser.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_one_digger",
            tickLimit = 40)
    public void theMinerWhoFoundNoStoneLetsTheShaftGo(TestContext context) {
        ServerWorld world = context.getWorld();

        Colony colony = mineOwner(context);

        // A galeria inteira já aberta, que é a forma do impasse: o dono
        // da escada procura e não acha. Escrito posição a posição a
        // partir da própria mina, e não com um bloco de ar sobre a
        // arena, porque a ordem de cavar sai dela em poucos degraus.
        hollowGallery(world, colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);

        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));

        UUID first = miner(context, colony, chest, owned, new BlockPos(1, 9, 1));
        UUID second = miner(context, colony, chest, owned, new BlockPos(5, 9, 5));

        reserveStone(colony, ResourceType.COBBLESTONE, first);
        reserveStone(colony, ResourceType.COAL, second);

        try {
            Optional<BlockPos> found = MineDigging.nextTarget(
                    world, first, colony.id(), context.getAbsolutePos(ROCK));

            context.assertTrue(
                    found.isEmpty(),
                    "este teste precisa de uma galeria sem pedra, e ela achou " + found);

            context.assertTrue(
                    MineClaims.diggerIn(colony.id()).isEmpty(),
                    "quem não achou pedra continua com a escada na mão — é o impasse"
                            + " de 2026-09-02, e o segundo mineiro espera para sempre");
        } finally {
            MineClaims.clearAll();
            owned.cleanUp();
        }

        context.complete();
    }

    /**
     * Pedra sem onde pisar não é alvo — 2026-09-02, sessão das 21:44.
     *
     * <p><b>O código já sabia, e falava tarde.</b> A frase saiu seis
     * vezes em dezessete minutos, sempre depois de o mineiro ter andado
     * dois minutos de expediente até lá:
     *
     * <pre>
     * could not reach the stone at 763, 45, 878 ... the place to stand is
     * the stone itself (no free neighbour to stand on)
     * </pre>
     *
     * <p>Zero pedra na sessão inteira. O {@code approachTo} devolve a
     * própria pedra quando não acha vizinho onde caiba um aldeão, e o
     * javadoc dele delegava o caso ao guarda de travamento — que cobra
     * 2.400 tiques por vez para descobrir o que a escolha já podia ter
     * visto.
     *
     * <p>Aqui a arena é rocha maciça: não há um bloco onde um aldeão
     * caiba, em lugar nenhum. Então não há pedra que valha a picareta, e
     * a busca precisa dizer isso em vez de apontar para dentro da rocha.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_one_digger",
            tickLimit = 40)
    public void stoneWithNowhereToStandIsNotATarget(TestContext context) {
        ServerWorld world = context.getWorld();

        solidRock(context);

        Colony colony = mineOwner(context);

        // O vão: a galeria aberta e sem chão por baixo, que é o que uma
        // caverna cortando o túnel faz. Ar não é onde se pisa.
        Mine mine = VillageColonyMod.MINES.of(colony.id()).orElseThrow();

        // A fresta de um bloco só, e é ela que reproduz o defeito: o
        // aldeão precisa de dois blocos livres para caber, então um vão
        // de altura um não é lugar de pisar em canto nenhum dele.
        //
        // Abre <b>só o pé da primeira pista</b> de cada degrau, e a
        // conta sai da forma em vez de vir escrita: um degrau são
        // {@link MineShaft#STAIR_HEADROOM} camadas vezes
        // {@link MineShaft#STAIR_LANES} colunas, e passou de dois para
        // seis índices em 2026-09-05. A versão anterior abria um índice
        // sim outro não, o que naquela forma calhava de deixar só os
        // pés — e nesta deixava meia escada aberta, com onde pisar ao
        // lado.
        int perStep = MineShaft.STAIR_HEADROOM * MineShaft.STAIR_LANES;

        for (int step = 0; step < 6; step++) {
            world.setBlockState(
                    MinecraftTypeAdapter.toBlockPos(mine.shaft().positionAt(step * perStep)),
                    Blocks.AIR.getDefaultState());
        }

        // O peito de um degrau <b>fundo</b>, e não o do primeiro: perto
        // da boca o próprio vão da entrada é lugar de pisar.
        BlockPos walled = MinecraftTypeAdapter.toBlockPos(
                mine.shaft().positionAt(perStep * 4 + 1));

        // <b>E a vizinhança é selada à mão.</b> Abrir posições da ordem
        // de cavar e torcer para que nada mais fique pisável amarra o
        // cenário à forma da mina — e a forma mudou em 2026-09-05, com a
        // segunda pista da escada e o bloco a mais de altura. Selar e
        // abrir exatamente um vão de altura um faz o cenário valer por
        // construção, em qualquer forma que a mina venha a ter.
        for (int dx = -ARM_REACH - 1; dx <= ARM_REACH + 1; dx++) {
            for (int dy = -ARM_REACH - 1; dy <= ARM_REACH + 1; dy++) {
                for (int dz = -ARM_REACH - 1; dz <= ARM_REACH + 1; dz++) {
                    world.setBlockState(
                            walled.add(dx, dy, dz), Blocks.STONE.getDefaultState());
                }
            }
        }

        world.setBlockState(walled.down(), Blocks.AIR.getDefaultState());

        ColonyFixture owned = ColonyFixture.create().owning(colony);

        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));

        UUID digger = miner(context, colony, chest, owned, new BlockPos(1, 9, 1));

        reserveStone(colony, ResourceType.COBBLESTONE, digger);

        try {
            context.assertTrue(
                    MinerWork.approachTo(world, walled).equals(walled),
                    "o cenário não reproduz o defeito: " + walled.toShortString()
                            + " tem onde pisar ao lado");

            Optional<BlockPos> found = MineDigging.nextTarget(
                    world, digger, colony.id(), context.getAbsolutePos(ROCK));

            context.assertTrue(
                    found.isEmpty()
                            || !MinerWork.approachTo(world, found.get()).equals(found.get()),
                    "a busca mandou o mineiro para " + found.orElseThrow().toShortString()
                            + ", que não tem um bloco em volta onde ele caiba");

        } finally {
            MineClaims.clearAll();
            owned.cleanUp();
        }

        context.complete();
    }

    /**
     * Veio sem onde pisar se larga, em vez de ser servido para sempre —
     * 2026-09-03.
     *
     * <p><b>É a guarda de 2026-09-02 vazando pela porta do minério.</b>
     * Aquele ciclo ensinou o {@code nextCut} a recusar pedra emparedada,
     * e o {@code followingTheVein} nunca soube da regra: ele roda
     * <b>antes</b> do túnel a cada passagem, e devolvia o minério colado
     * no último sem perguntar se havia de onde bater nele.
     *
     * <p>E a veia mora no {@code Mine}, que é da colônia. Um minério
     * dentro da rocha era servido de volta na passagem seguinte, ao mesmo
     * mineiro e ao que herdasse a escada pelo
     * {@code MineClaims.stepAside}. O {@code couldNotReach} não alcançava
     * o caso — ele recua o cursor do <b>túnel</b>, e diz por escrito que é
     * silencioso quando a pedra é do veio.
     *
     * <p>O ciclo fechado era: mira o minério, anda dois minutos de
     * expediente contra a rocha, o guarda de travamento devolve a tarefa,
     * a passagem seguinte mira o mesmo minério. A colônia inteira parada
     * num bolsão de carvão que ninguém alcança.
     *
     * <p>A arena é rocha maciça — a mesma do
     * {@code stoneWithNowhereToStandIsNotATarget} —, então não há um
     * bloco onde um aldeão caiba em lugar nenhum, e o minério enterrado
     * nela é inalcançável por construção.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_vein",
            tickLimit = 20)
    public void aVeinWithNowhereToStandIsDropped(TestContext context) {
        ServerWorld world = context.getWorld();

        solidRock(context);

        Colony colony = mineOwner(context);

        BlockPos taken = new BlockPos(3, 3, 3);
        BlockPos buried = taken.north();

        context.setBlockState(buried, Blocks.COPPER_ORE.getDefaultState());

        Mine mine = VillageColonyMod.MINES.of(colony.id()).orElseThrow();

        mine.arm(0).followVein(MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(taken)));

        BlockPos ore = context.getAbsolutePos(buried);

        try {
            context.assertTrue(
                    MinerWork.approachTo(world, ore).equals(ore),
                    "o cenário não reproduz o defeito: " + ore.toShortString()
                            + " tem onde pisar ao lado");

            Optional<BlockPos> next = veinTarget(context, colony);

            context.assertFalse(
                    next.isPresent() && next.get().equals(ore),
                    "o veio mandou o mineiro para dentro da rocha, atrás do minério");

            context.assertTrue(
                    mine.arm(0).vein().isEmpty(),
                    "a veia inalcançável continua guardada, e a passagem seguinte a serve"
                            + " de novo — é o laço que prendia a colônia");
        } finally {
            MineClaims.clearAll();
        }

        context.complete();
    }

    /**
     * Desistir do minério larga a veia junto — 2026-09-03.
     *
     * <p>A guarda da entrada recusa o que já se sabe inalcançável; esta é
     * a que fecha o resto, porque <b>nem toda desistência é por falta de
     * lugar</b>: chunk descarregado, caminho que a navegação não traçou, o
     * jogador tapando o buraco enquanto o mineiro anda até lá.
     *
     * <p>Sem ela, qualquer uma dessas fecha o mesmo laço: o
     * {@code couldNotReach} recuava só o cursor do túnel, e a veia
     * apontando para o minério que ele acabou de largar continuava lá
     * para a passagem seguinte devolvê-lo.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_vein",
            tickLimit = 20)
    public void givingUpOnTheOreDropsTheVein(TestContext context) {
        Colony colony = mineOwner(context);

        BlockPos ore = context.getAbsolutePos(new BlockPos(3, 3, 3));

        Mine mine = VillageColonyMod.MINES.of(colony.id()).orElseThrow();

        mine.arm(0).followVein(MinecraftTypeAdapter.toColonyPos(ore));

        MineDigging.couldNotReach(colony.id(), ore);

        context.assertTrue(
                mine.arm(0).vein().isEmpty(),
                "o mineiro desistiu deste minério e a veia continua apontando para ele");

        context.complete();
    }

    /**
     * E ela larga <b>essa</b> veia, e não qualquer uma.
     *
     * <p>O {@code couldNotReach} recebe toda pedra largada — a do túnel, a
     * da areia, a que o outro mineiro já ultrapassou. Largar a veia em
     * todas seria jogar fora o minério que ainda está bom, que é
     * exatamente o trabalho que a veia existe para poupar.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_vein",
            tickLimit = 20)
    public void givingUpOnAnotherStoneKeepsTheVein(TestContext context) {
        Colony colony = mineOwner(context);

        BlockPos ore = context.getAbsolutePos(new BlockPos(3, 3, 3));

        Mine mine = VillageColonyMod.MINES.of(colony.id()).orElseThrow();

        mine.arm(0).followVein(MinecraftTypeAdapter.toColonyPos(ore));

        MineDigging.couldNotReach(colony.id(), context.getAbsolutePos(new BlockPos(5, 6, 5)));

        context.assertFalse(
                mine.arm(0).vein().isEmpty(),
                "largou a veia por causa de uma pedra que não era ela");

        context.complete();
    }

    /**
     * Mineiro congelado larga a pedra em quinze segundos, e não em dois
     * minutos — 2026-09-03.
     *
     * <p><b>O guarda de travamento conta tique de expediente andando até
     * a pedra, e nunca pergunta se o aldeão andou.</b> Um mineiro travado
     * paga os 2.400 inteiros antes de a tarefa voltar para a fila, e é o
     * que aparece em toda sessão registrada — <i>"seis vezes a mesma
     * frase, dois minutos de expediente cada, e zero pedra em dezessete
     * minutos"</i>.
     *
     * <p>E travado é a assinatura de todas elas: parado no mesmo bloco,
     * com destino posto. {@code he is at 718, 44, 878, walking to 718, 44,
     * 878}.
     *
     * <p>Aqui o alvo está longe e não há caminho até ele — o arranjo do
     * {@code theStallGuardDoesNotCountOutsideWorkHours}, com um adulto no
     * lugar da criança. O que se afirma é <b>quando</b> a tarefa volta: se
     * ela voltou antes do {@code STALL_LIMIT}, quem a devolveu foi o
     * detector de imobilidade, porque o outro guarda ainda nem chegou lá.
     *
     * <p><b>E "voltou" é evento, não estado</b> — KF-001 fechado em
     * 2026-09-09. A afirmação antiga lia {@code task.state()} no tique
     * 360, e esse estado não é só do mineiro: a fronteira do ciclo da
     * colônia, quando calha de cair na janela, devolve a tarefa a quem
     * acabou de largá-la, e o teste reprovava a produção por ter feito
     * exatamente o que ele promete medir. Eram ~10% das execuções — 3 em
     * 30 —, porque a janela entre a devolução e a afirmação é de umas
     * cinco dezenas de tiques nos 600 do ciclo, e a fase do ciclo depende
     * de quanto a bateria andou nos lotes anteriores. Agora o instante da
     * devolução é guardado quando acontece, e o ciclo é forçado de
     * propósito depois dele.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "miner_stillness",
            tickLimit = 400)
    public void aFrozenMinerGivesUpLongBeforeTheStallGuard(TestContext context) {
        // O relógio é do mundo inteiro e a bateria o faz andar: sem
        // fixá-lo, onde este teste cai no dia depende de quantos
        // tiques a bateria gastou antes dele. Ver 2026-09-04.
        context.getWorld().setTimeOfDay(Schedule.WORK_TIME);

        ServerWorld world = context.getWorld();

        ground(context);

        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());

        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));

        Block rock = MinecraftTypeAdapter
                .toBlock(HousePlans.paletteOf(world, chest).stone())
                .orElseThrow();

        context.setBlockState(DEEP_MOUTH.east(), rock.getDefaultState());

        context.setBlockState(PERCH.down(), Blocks.DIRT.getDefaultState());

        Colony colony = Colony.create(UUID.randomUUID(), chest);

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, PERCH);
        villager.setBreedingAge(0);

        // <b>Emparedado, e não só longe</b> — 2026-09-05. O cenário
        // apostava que o alvo ficaria fora de alcance por causa da
        // geometria da mina, e a geometria mudou: com a segunda pista da
        // escada o mineiro passou a <b>alcançar</b> a pedra
        // ({@code 0,6 blocks away, 163/200 ticks}), trabalhar, e zerar o
        // guarda — o teste media um mineiro ocupado e o chamava de
        // congelado.
        //
        // Congelar por construção não depende de forma nenhuma: seis
        // paredes em volta dos dois blocos que ele ocupa. Ele não anda, e
        // é isso que o teste promete medir.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 2; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos at = PERCH.add(dx, dy, dz);

                    if (at.equals(PERCH) || at.equals(PERCH.up())) {
                        continue;
                    }

                    context.setBlockState(at, Blocks.STONE.getDefaultState());
                }
            }
        }

        Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
        worker.assign(ProfessionType.MINER);

        WorkerEquipment.equip(context.getWorld(), List.of(worker));

        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chest));

        owned.owning(villager.getUuid());

        Task task = VillageColonyMod.TASKS.create(
                colony.id(),
                TaskType.COLLECT_STONE,
                TaskPriority.PRODUCTION,
                ResourceType.COBBLESTONE,
                16);

        task.reserveFor(villager.getUuid());

        ColonyPos mouth = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(DEEP_MOUTH));

        VillageColonyMod.MINES.restore(
                Mine.restore(colony.id(), MineShaft.from(mouth, Side.EAST), 0));

        MineDigging.shortenMineDistanceTo(NEARBY);

        MinerWork.run(world, colony);

        // <b>O que se afirma é o evento, e não o estado no tique 360</b>
        // — KF-001, 2026-09-09.
        //
        // Estado de tarefa é global, e outra coisa o escreve: a fronteira
        // do ciclo da colônia caindo dentro da janela deste teste devolve
        // a mesma tarefa ao mesmo mineiro que acabou de largá-la. É a 2ª
        // passagem do {@code WorkAssignment}, que existe de propósito e
        // tem teste — {@code theRestNeverLeavesTheWorkerIdle}: descanso
        // não deixa trabalhador parado quando não há mais nada da
        // profissão dele. O contador de 600 tiques é do servidor inteiro,
        // e a fase dele depende de quantos tiques a bateria gastou nos
        // lotes anteriores; a janela entre a devolução (~tique 305) e a
        // afirmação (360) é de umas cinco dezenas em 600, que são os
        // ~10% medidos — 3 falhas em 30 execuções.
        //
        // O guarda de imobilidade devolve a tarefa <b>uma vez</b>, e
        // nada pode desfazer que ela tenha voltado. É esse instante que
        // se guarda, e é dele que a promessa fala.
        int[] passes = { 0 };

        int[] releasedAt = { -1 };

        context.runAtEveryTick(() -> {
            passes[0]++;

            if (releasedAt[0] >= 0) {
                return;
            }

            if (task.state() != TaskState.RESERVED
                    && task.state() != TaskState.EXECUTING) {

                releasedAt[0] = passes[0];
            }
        });

        // <b>E a interferência entra de propósito.</b> Ela era sorteio —
        // ora a fronteira do ciclo caía na janela, ora não —, e sorteio
        // num teste é o que o KF-001 foi. Aqui o ciclo é forçado depois
        // de o guarda ter falado: se a afirmação sobrevive a ele, ela
        // sobrevive a qualquer fase.
        context.runAtTick(340, () -> {
            WorkAssignment.assign(
                    colony.id(), VillageColonyMod.WORKERS, VillageColonyMod.TASKS);

            MinerWork.run(world, colony);
        });

        context.runAtTick(360, () -> {
            // <b>KF-001, e esta linha existe porque a falha é rara.</b>
            //
            // A mensagem de uma asserção de gametest <b>não aparece no
            // log da bateria</b> — o console imprime só o nome do teste
            // que falhou. Numa falha de uma em doze, isso é a diferença
            // entre diagnosticar e adivinhar: a sessão de 2026-09-09
            // gastou catorze execuções tentando reproduzir e voltou sem
            // nada porque o estado do momento não estava escrito em
            // lugar nenhum.
            //
            // Fala só quando algo está errado, para não somar ruído às
            // 269 passagens de uma bateria verde.
            if (releasedAt[0] < 0 || !WorkHours.isWorkTime(world, villager)) {
                VillageColonyMod.LOGGER.warn(
                        "KF-001 — a tarefa do mineiro emparedado nunca voltou para a fila."
                                + " task={}, expediente={}, relatório={}",
                        task.state(),
                        WorkHours.isWorkTime(world, villager),
                        MinerReport.report(world, colony).orElse("(sem relatório)"));
            }

            try {
                context.assertTrue(
                        WorkHours.isWorkTime(world, villager),
                        "a arena não está em horário de expediente, e fora dele nenhum dos"
                                + " dois guardas conta — este teste não mede o que promete");

                context.assertTrue(
                        MinerWork.STILL_LIMIT < MinerWork.STALL_LIMIT,
                        "o detector de imobilidade precisa disparar ANTES do guarda de"
                                + " travamento, senão ele não adianta nada");

                context.assertTrue(
                        releasedAt[0] >= 0,
                        "o mineiro passou " + (MinerWork.STILL_LIMIT + 20) + " passagens parado"
                                + " e a tarefa nunca voltou para a fila —"
                                + " ela só voltaria no tique " + MinerWork.STALL_LIMIT
                                + ", que é o preço que toda sessão pagou. O relatório diz o"
                                + " que o contador marcava: "
                                + MinerReport.report(world, colony).orElse("(sem relatório)"));

                context.assertTrue(
                        releasedAt[0] < MinerWork.STALL_LIMIT,
                        "a tarefa voltou no tique " + releasedAt[0] + ", e o guarda de"
                                + " travamento só falaria no " + MinerWork.STALL_LIMIT
                                + " — quem a devolveu não foi o detector de imobilidade");
            } finally {
                owned.cleanUp();

                MineClaims.clearAll();

                MineDigging.restoreMineDistance();
            }

            context.complete();
        });
    }

    /**
     * O detector de imobilidade não volta a zero porque o alvo mudou —
     * E36, 2026-09-04.
     *
     * <p><b>A pergunta que o guarda faz não é sobre o alvo.</b> Ele
     * pergunta <i>o aldeão saiu do bloco?</i>, e a resposta não muda
     * quando a pedra à frente dele some: quem estava congelado continua
     * congelado. Mas o {@code release} e o {@code startNextStone} zeravam
     * o contador ao trocar de alvo, e com isso quem troca de alvo com
     * frequência ficava <b>imune</b> aos dois guardas — foi o que os
     * mineiros travados da sessão de 09-04 exibiram por vinte e cinco
     * minutos, com {@code stall 0/2400, still 0/300} e nenhum passo dado.
     *
     * <p>É a forma do erro que a pergunta 20 desta casa já nomeia:
     * <b>pendurar a limpeza num momento em vez de conferir uma
     * invariante</b>. O momento certo de zerar não é "peguei alvo novo" —
     * é "ele andou" (que o {@code WorkStall} já vê sozinho) ou "ele
     * trabalhou", que é o que o construtor e o fabricante sempre fizeram.
     *
     * <p>Aqui o alvo some do jeito mais banal do mundo: o jogador cavou a
     * pedra. O mineiro não se mexeu um bloco, e o contador não pode ter
     * esquecido disso.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "miner_e36",
            tickLimit = 100)
    public void theStillnessGuardSurvivesTheTargetChanging(TestContext context) {
        // O relógio é do mundo inteiro e a bateria o faz andar — 09-04.
        context.getWorld().setTimeOfDay(Schedule.WORK_TIME);

        ServerWorld world = context.getWorld();

        ground(context);

        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());

        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));

        Block rock = MinecraftTypeAdapter
                .toBlock(HousePlans.paletteOf(world, chest).stone())
                .orElseThrow();

        context.setBlockState(DEEP_MOUTH.east(), rock.getDefaultState());

        context.setBlockState(PERCH.down(), Blocks.DIRT.getDefaultState());

        Colony colony = Colony.create(UUID.randomUUID(), chest);

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, PERCH);
        villager.setBreedingAge(0);

        Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
        worker.assign(ProfessionType.MINER);

        WorkerEquipment.equip(context.getWorld(), List.of(worker));

        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chest));

        owned.owning(villager.getUuid());

        Task task = VillageColonyMod.TASKS.create(
                colony.id(),
                TaskType.COLLECT_STONE,
                TaskPriority.PRODUCTION,
                ResourceType.COBBLESTONE,
                16);

        task.reserveFor(villager.getUuid());

        ColonyPos mouth = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(DEEP_MOUTH));

        VillageColonyMod.MINES.restore(
                Mine.restore(colony.id(), MineShaft.from(mouth, Side.EAST), 0));

        MineDigging.shortenMineDistanceTo(NEARBY);

        MinerWork.run(world, colony);

        // Passagens à mão, no mesmo tique — 09-04. O guarda conta uma vez
        // por passagem pelo ramo "andando, fora de alcance", e não uma vez
        // por tique do servidor. Dadas assim, o aldeão não muda de bloco
        // entre uma e outra e o expediente não vira.
        for (int pass = 0; pass <= FROZEN_PASSES; pass++) {
            MinerWork.tick(world);
        }

        int before = MinerWork.stillnessOf(villager.getUuid());

        // O jogador cavou a pedra. Nada mais mudou — e ele não andou.
        context.setBlockState(DEEP_MOUTH.east(), Blocks.AIR.getDefaultState());

        MinerWork.tick(world);

        int after = MinerWork.stillnessOf(villager.getUuid());

        context.runAtTick(5, () -> {
            try {
                context.assertTrue(
                        before > 0,
                        "o contador não subiu em " + FROZEN_PASSES + " passagens com o"
                                + " mineiro congelado — este teste não mede o que promete");

                context.assertTrue(
                        after >= before,
                        "o mineiro não saiu do lugar e o contador caiu de " + before
                                + " para " + after + ": a pedra sumir zerou o guarda."
                                + " Quem troca de alvo o tempo todo fica imune aos dois,"
                                + " que é o E36");
            } finally {
                owned.cleanUp();

                MineClaims.clearAll();

                MineDigging.restoreMineDistance();
            }

            context.complete();
        });
    }

    /**
     * E o detector de imobilidade também não conta fora do expediente.
     *
     * <p>É a mesma armadilha do contador irmão, e ela já custou uma
     * sessão: fora da hora a {@code GoToWorkTargetTask} nem começa, então
     * o aldeão está <b>proibido</b> de andar. Punir quem não pode andar é
     * queimar o orçamento com ele dormindo — o contador foi de 886 a 2086
     * com o relatório dizendo {@code off hours}.
     *
     * <p>Criança pelo mesmo motivo do outro teste: {@code WorkHours}
     * responde não para bebê sem que o relógio do mundo mude, e mexer na
     * hora vaza para os testes vizinhos do mesmo lote.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "miner_off_hours",
            tickLimit = 100)
    public void theStillnessGuardDoesNotCountOutsideWorkHours(TestContext context) {
        ServerWorld world = context.getWorld();

        ground(context);

        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());

        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));

        Block rock = MinecraftTypeAdapter
                .toBlock(HousePlans.paletteOf(world, chest).stone())
                .orElseThrow();

        context.setBlockState(DEEP_MOUTH.east(), rock.getDefaultState());

        context.setBlockState(PERCH.down(), Blocks.DIRT.getDefaultState());

        Colony colony = Colony.create(UUID.randomUUID(), chest);

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);

        VillagerEntity child = context.spawnEntity(EntityType.VILLAGER, PERCH);
        child.setBreedingAge(-24_000);

        Worker worker = VillageColonyMod.WORKERS.register(child.getUuid(), colony.id());
        worker.assign(ProfessionType.MINER);

        WorkerEquipment.equip(context.getWorld(), List.of(worker));

        VillageColonyMod.STORAGES.register(WorkerStorage.of(child.getUuid(), chest));

        owned.owning(child.getUuid());

        Task task = VillageColonyMod.TASKS.create(
                colony.id(),
                TaskType.COLLECT_STONE,
                TaskPriority.PRODUCTION,
                ResourceType.COBBLESTONE,
                16);

        task.reserveFor(child.getUuid());

        ColonyPos mouth = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(DEEP_MOUTH));

        VillageColonyMod.MINES.restore(
                Mine.restore(colony.id(), MineShaft.from(mouth, Side.EAST), 0));

        MineDigging.shortenMineDistanceTo(NEARBY);

        MinerWork.run(world, colony);

        context.runAtTick(60, () -> {
            int still = MinerWork.stillnessOf(child.getUuid());

            try {
                context.assertTrue(
                        still == 0,
                        "o detector contou " + still + " tiques parado fora do expediente,"
                                + " e fora dele o aldeão está proibido de andar");
            } finally {
                owned.cleanUp();

                MineClaims.clearAll();

                MineDigging.restoreMineDistance();
            }

            context.complete();
        });
    }

    /**
     * Saiu água por ali, e o mineiro tapa — decisão do autor, 2026-09-03.
     *
     * <p>A frase dele: <i>"quando quebrar uma pedra e sair água por ali
     * ele deve rapidamente colocar um bloco no lugar para encerrar o fluxo
     * da água"</i>.
     *
     * <p>Sem isto a galeria inunda: a água corre pelo túnel, desce a
     * escada, e a mina passa a ser um lugar onde o aldeão não fica de pé —
     * {@code standable} pede dois blocos livres, e coluna de água não é
     * livre para quem anda.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_flooding",
            tickLimit = 20)
    public void thePickThatOpensWaterSealsItAtOnce(TestContext context) {
        ServerWorld world = context.getWorld();

        solidRock(context);

        BlockPos dug = new BlockPos(3, 4, 3);
        BlockPos spring = dug.north();

        context.setBlockState(dug, Blocks.AIR.getDefaultState());
        context.setBlockState(spring, Blocks.WATER.getDefaultState());

        BlockPos absolute = context.getAbsolutePos(dug);

        int sealed = MineFlooding.seal(world, absolute);

        context.assertTrue(sealed == 1, "tapou " + sealed + " faces, e havia uma nascente");

        context.assertTrue(
                world.getBlockState(context.getAbsolutePos(spring)).getFluidState().isEmpty(),
                "a nascente continua correndo depois de tapada");

        context.complete();
    }

    /**
     * E rocha seca não ganha bloco nenhum.
     *
     * <p>O outro lado, e ele importa: um mineiro que tapasse toda face
     * cavaria e reconstruiria a mina ao mesmo tempo, e a galeria nunca
     * abriria.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_flooding",
            tickLimit = 20)
    public void dryRockIsNotSealed(TestContext context) {
        ServerWorld world = context.getWorld();

        solidRock(context);

        BlockPos dug = new BlockPos(3, 4, 3);

        context.setBlockState(dug, Blocks.AIR.getDefaultState());

        int sealed = MineFlooding.seal(world, context.getAbsolutePos(dug));

        context.assertTrue(sealed == 0, "tapou " + sealed + " faces de rocha seca");

        context.complete();
    }

    /**
     * E o ramal é abandonado, que é a outra metade do pedido.
     *
     * <p><i>"...e seguir por outro caminho a continuidade de
     * mineração"</i>. Insistir na mesma direção é cavar de volta para
     * dentro do lençol.
     *
     * <p><b>Este teste media a curva da galeria até 2026-09-04</b>, e
     * media certo: com um cursor só, "seguir por outro caminho" era
     * girar o rumo dele. Com um ramal por mineiro o rumo é fixo, e seguir
     * por outro caminho é <b>trocar de ramal</b> — o inundado fecha, e o
     * aldeão recebe um dos três que sobraram.
     *
     * <p>O que se afirma é o mesmo de sempre: depois da água, aquele
     * mineiro não volta a ser mandado para dentro dela.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_flooding",
            tickLimit = 20)
    public void theBranchTurnsAwayFromTheWater(TestContext context) {
        Colony colony = mineOwner(context);

        Mine mine = VillageColonyMod.MINES.of(colony.id()).orElseThrow();

        // O mineOwner monta colônia e mina, e nenhum trabalhador: quem
        // cava aqui é este identificador, e o ramal dele é o primeiro
        // livre — o primeiro.
        UUID worker = UUID.randomUUID();

        try {
            MineDigging.flooded(
                    colony.id(), worker, context.getAbsolutePos(new BlockPos(3, 4, 3)));

            context.assertTrue(
                    mine.arm(0).isDone(),
                    "o ramal inundado continua aceitando picareta");

            context.assertTrue(
                    mine.firstArmStillOpen().isPresent(),
                    "a água fechou a mina inteira, e não só o ramal dela");

            context.complete();
        } finally {
            MineClaims.clearAll();
        }
    }

    /**
     * Entre duas faces com minério, ganha o que a vila usa antes —
     * decisão do autor, 2026-09-04.
     *
     * <p><b>Este teste afirmava o contrário até hoje</b>, e afirmava
     * certo: em 2026-09-03 a ordem era a raridade — <i>"deve sempre
     * priorizar os minerais diferentes e mais raros"</i> — e o diamante
     * da parede ganhava do carvão do chão. O autor trocou o critério
     * para a utilidade, e o carvão passou a ser o primeiro de dez: é
     * dele que sai a tocha que impede monstro de nascer na fundação.
     *
     * <p>O que a inversão <b>não</b> desfaz é o defeito que o teste
     * original pegou: o laço devolvia a primeira das seis faces, e o
     * {@code Direction.values()} começa em {@code DOWN}. A geometria
     * continua a mesma de propósito — o vencedor é que mudou de lado, e
     * com ele a prova de que a escolha é da lista e não da ordem das
     * faces.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "ore_rarity",
            tickLimit = 20)
    public void theMoreUsefulOreWinsOverTheRarerOne(TestContext context) {
        ServerWorld world = context.getWorld();

        solidRock(context);

        BlockPos at = new BlockPos(3, 4, 3);

        // O diamante embaixo, que é a face que o laço olha primeiro: se a
        // escolha viesse da ordem das faces em vez da lista, ele venceria
        // por acidente e o teste passaria sem provar nada.
        context.setBlockState(at.down(), Blocks.DIAMOND_ORE.getDefaultState());
        context.setBlockState(at.north(), Blocks.COAL_ORE.getDefaultState());

        Optional<BlockPos> chosen = OreVein.beside(world, context.getAbsolutePos(at));

        context.assertTrue(chosen.isPresent(), "não achou minério nenhum");

        context.assertTrue(
                chosen.get().equals(context.getAbsolutePos(at.north())),
                "escolheu " + chosen.get().toShortString() + " — o diamante do chão ganhou"
                        + " do carvão da parede, e a vila precisa da tocha primeiro");

        context.complete();
    }

    /**
     * E a ordem entre eles é a do uso da vila, e não a do catálogo nem a
     * da raridade.
     *
     * <p>Os dez postos da {@code BY_USE}, conferidos nas pontas e no
     * meio: o carvão abre, o ferro vem depois dele, e o diamante — que
     * era o primeiro sob a ordem antiga — cai para depois do ouro.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "ore_rarity",
            tickLimit = 20)
    public void priorityRunsFromCoalDownToTheRareOnes(TestContext context) {
        int coal = OreVein.priorityOf(Blocks.COAL_ORE.getDefaultState());
        int iron = OreVein.priorityOf(Blocks.IRON_ORE.getDefaultState());
        int copper = OreVein.priorityOf(Blocks.COPPER_ORE.getDefaultState());
        int gold = OreVein.priorityOf(Blocks.GOLD_ORE.getDefaultState());
        int diamond = OreVein.priorityOf(Blocks.DIAMOND_ORE.getDefaultState());

        context.assertTrue(
                coal < iron && iron < copper && copper < gold && gold < diamond,
                "a ordem saiu carvão=" + coal + " ferro=" + iron + " cobre=" + copper
                        + " ouro=" + gold + " diamante=" + diamond);

        // O carvão é o primeiro de todos, e é disso que o beside() se
        // aproveita para parar a varredura das seis faces.
        context.assertTrue(coal == 0, "o carvão não é o primeiro: " + coal);

        // A ardósia é o mesmo minério mais fundo, e a galeria trabalha
        // justamente onde ela está.
        context.assertTrue(
                OreVein.priorityOf(Blocks.DEEPSLATE_DIAMOND_ORE.getDefaultState()) == diamond,
                "a variante de ardósia não vale o mesmo que a de pedra");

        // O desconhecido fica depois de todos, e não no meio: a colônia
        // não tem receita para ele. Pedra não é minério e nem entra aqui,
        // então o número de fora da lista é o maior que priorityOf devolve.
        context.assertTrue(
                OreVein.priorityOf(Blocks.ANCIENT_DEBRIS.getDefaultState()) > gold,
                "os escombros antigos passaram à frente do ouro");

        context.complete();
    }

    /**
     * Abre toda a janela de busca da galeria desta colônia.
     *
     * <p>Um pouco além das 64 posições que uma passagem examina, para
     * que a busca chegue ao fim sem achar uma pedra sequer.
     */
    private static void hollowGallery(ServerWorld world, Colony colony) {
        Mine mine = VillageColonyMod.MINES.of(colony.id()).orElseThrow();

        for (int i = 0; i < 80; i++) {
            world.setBlockState(
                    MinecraftTypeAdapter.toBlockPos(mine.shaft().positionAt(i)),
                    Blocks.AIR.getDefaultState());
        }
    }

    private static UUID miner(
            TestContext context,
            Colony colony,
            ColonyPos chest,
            ColonyFixture owned,
            BlockPos stand) {

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, stand);
        villager.setBreedingAge(0);

        Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
        worker.assign(ProfessionType.MINER);

        WorkerEquipment.equip(context.getWorld(), List.of(worker));

        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chest));

        owned.owning(villager.getUuid());

        return villager.getUuid();
    }

    /** Uma tarefa de pedra deste recurso, já reservada para este mineiro. */
    private static void reserveStone(Colony colony, ResourceType resource, UUID workerId) {
        Task task = VillageColonyMod.TASKS.create(
                colony.id(),
                TaskType.COLLECT_STONE,
                TaskPriority.PRODUCTION,
                resource,
                16);

        task.reserveFor(workerId);
    }

    /** Onde a escada destes testes de luz começa, dentro da rocha. */
    private static final BlockPos LIT_ENTRY = new BlockPos(0, 6, 3);

    /**
     * Uma mina desta colônia com a escada aberta até certa posição.
     *
     * <p>A ordem de cavar é do {@code MineShaft}, e abrir por ela é o
     * que o mineiro teria feito — a arena fica com a escada de verdade,
     * e não com um corredor inventado.
     */
    private static Colony openedMine(TestContext context, int cut) {
        Colony colony = Colony.create(
                UUID.randomUUID(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(6, 2, 6))));

        VillageColonyMod.COLONIES.register(colony);

        MineShaft shaft = MineShaft.from(
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(LIT_ENTRY)), Side.EAST);

        VillageColonyMod.MINES.restore(Mine.restore(colony.id(), shaft, cut));

        for (int i = 0; i < cut; i++) {
            context.getWorld().setBlockState(
                    MinecraftTypeAdapter.toBlockPos(shaft.positionAt(i)),
                    Blocks.AIR.getDefaultState());
        }

        return colony;
    }

    /** A posição de índice {@code i} da escada destes testes. */
    private static BlockPos dug(TestContext context, Colony colony, int i) {
        return MinecraftTypeAdapter.toBlockPos(
                VillageColonyMod.MINES.of(colony.id()).orElseThrow().shaft().positionAt(i));
    }

    private static Optional<BlockPos> targetFor(TestContext context, Colony colony) {
        return MineDigging.nextTarget(
                context.getWorld(),
                UUID.randomUUID(),
                colony.id(),
                context.getAbsolutePos(LIT_ENTRY));
    }

    /**
     * A galeria ganha luz onde já foi cavada — 2026-08-28.
     *
     * <p><b>Só a boca tinha lanterna, e nem sempre.</b> Vinte blocos
     * abaixo do chão, com luz zero, é criatura nascendo <b>dentro</b> da
     * mina, ao lado de um aldeão desarmado — e a sessão de 08-26 nem a
     * lanterna da boca conseguiu pôr: {@code lantern at nowhere it fits}.
     *
     * <p>A tocha vai no <b>chão</b> da passagem, e não na posição da
     * ordem: a ordem abre a coluna inteira, e só a de baixo tem rocha
     * embaixo para apoiar a tocha.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_light",
            tickLimit = 20)
    public void theGalleryIsLitWhereItWasAlreadyDug(TestContext context) {
        solidRock(context);

        Colony colony = openedMine(context, 18);

        try {
            targetFor(context, colony);

            // <b>Procurada, e não escrita à mão.</b> As coordenadas
            // estavam fixas — (3,6,3) e (3,4,3) —, e a forma da mina
            // mudou em 2026-09-05: a escada ganhou uma segunda pista e o
            // túnel um bloco de altura, então todo índice andou de
            // lugar. Um teste que grava a coordenada mede a forma de
            // ontem; o que ele quer afirmar é que <b>a galeria acendeu</b>.
            Optional<BlockPos> lit = wallTorchIn(context);

            context.assertTrue(
                    lit.isPresent(),
                    "a galeria continuou escura: nenhuma tocha de parede na mina");

            // E o chão da coluna da tocha continua sendo degrau — a
            // tocha no piso foi o primeiro defeito desta feature.
            context.assertTrue(
                    !context.getWorld().getBlockState(lit.get().down()).isOf(Blocks.WALL_TORCH),
                    "a tocha comeu o degrau em " + lit.get().toShortString());
        } finally {
            MineClaims.clearAll();
        }

        context.complete();
    }

    /**
     * O mineiro não cava a própria tocha — 2026-08-28.
     *
     * <p>É o defeito do lampião no primeiro degrau, de 08-27, que
     * voltaria pela porta da frente: tocha tem dureza e não é da vila, e
     * o {@code nextCut} a trataria como pedra. Uma posição com luz é
     * <b>espaço aberto</b>, não rocha.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_light",
            tickLimit = 20)
    public void theMinerDoesNotDigItsOwnTorch(TestContext context) {
        solidRock(context);

        Colony colony = openedMine(context, 3);

        // A quarta posição da ordem, acesa e com rocha embaixo.
        BlockPos torch = dug(context, colony, 3);

        context.getWorld().setBlockState(torch, Blocks.TORCH.getDefaultState());

        try {
            Optional<BlockPos> next = targetFor(context, colony);

            context.assertTrue(
                    next.isPresent(),
                    "a mina não devolveu alvo nenhum, e sem isso o teste não mede nada");

            context.assertFalse(
                    next.get().equals(torch),
                    "o mineiro mirou a própria tocha em " + torch.toShortString());

            context.assertTrue(
                    next.get().equals(dug(context, colony, 4)),
                    "ele devia ter seguido para a posição seguinte, e foi para "
                            + next.get().toShortString());
        } finally {
            MineClaims.clearAll();
        }

        context.complete();
    }

    /**
     * Tocha na ordem de cavar não é o fim da galeria — 2026-08-28.
     *
     * <p>A outra metade, e é a que trancaria a mina. O
     * {@code findTheFrontier} recua o cursor até a primeira posição
     * ainda fechada; sem saber que luz é passagem aberta, ele recuaria
     * até a tocha <b>toda passagem</b>, e a mina nunca mais passaria
     * dali.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_light",
            tickLimit = 20)
    public void aTorchInTheDigOrderIsNotTheFrontier(TestContext context) {
        solidRock(context);

        Colony colony = openedMine(context, 9);

        BlockPos torch = dug(context, colony, 3);

        context.getWorld().setBlockState(torch, Blocks.TORCH.getDefaultState());

        try {
            Optional<BlockPos> next = targetFor(context, colony);

            context.assertTrue(
                    next.isPresent() && next.get().equals(dug(context, colony, 9)),
                    "a frente da galeria recuou até a tocha: foi para "
                            + next.map(BlockPos::toShortString).orElse("lugar nenhum"));
        } finally {
            MineClaims.clearAll();
        }

        context.complete();
    }

    /**
     * A linha diz para onde ele foi mandado, e não para onde ela acha
     * que ele deveria ir — 2026-08-29.
     *
     * <p><b>O relatório recomputava o destino.</b> Ele chamava
     * {@code approachTo} de novo, na hora de escrever a linha, e
     * imprimia <b>esse</b> resultado como <i>"walking to"</i> — em vez
     * de ler o destino que o aldeão de fato recebeu.
     *
     * <p>Enquanto os dois coincidem ninguém percebe. Eles deixam de
     * coincidir exatamente no caso que interessa: quando o
     * {@code MinerReach.legTowards} manda o mineiro à <b>boca da mina</b>
     * porque a pedra está longe demais para a navegação. Aí a linha
     * continua dizendo a pedra, e a sessão de 2026-08-28 saiu com o
     * segundo mineiro parado na superfície, <i>"walking to 758, 44,
     * 878"</i>, sem que desse para saber se a perna tinha sequer
     * disparado.
     *
     * <p>É a mesma família do E31 e do E30: <b>instrumento que reporta o
     * que recalculou, e não o que aconteceu</b>. E custa caro além da
     * mentira — {@code approachTo} são umas seiscentas leituras de bloco
     * por mineiro por ciclo, gastas para reimprimir um dado que já
     * estava guardado.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "miner_report",
            tickLimit = 40)
    public void theLineSaysWhereHeWasActuallySent(TestContext context) {
        ServerWorld world = context.getWorld();

        Colony colony = workingMiner(context);

        // Um destino que o approachTo nunca escolheria: quem o repõe a
        // cada tique é o legTowards, e é ele que manda para a boca.
        BlockPos sentTo = context.getAbsolutePos(new BlockPos(0, 8, 0));

        WorkTargets.set(minerOf(colony), sentTo, MinerReach.ARRIVAL);

        String line = MinerReport.report(world, colony).orElseThrow();

        try {
            context.assertTrue(
                    line.contains("out of reach"),
                    "o mineiro alcançou a pedra, e sem isso o teste não mede nada: " + line);

            context.assertTrue(
                    line.contains(sentTo.toShortString()),
                    "a linha não diz para onde ele foi mandado de verdade: " + line);
        } finally {
            MineClaims.clearAll();
            WorkTargets.clear(minerOf(colony));
        }

        context.complete();
    }

    /**
     * E ela nomeia a boca da mina, que é a resposta que faltava.
     *
     * <p>Ver o destino em números não basta: a pergunta da sessão é
     * <b>se a perna disparou</b>, e para respondê-la o leitor teria de
     * comparar coordenadas com a linha de abertura da mina, dez minutos
     * de log acima. A linha diz por extenso.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "miner_report",
            tickLimit = 40)
    public void theLineNamesTheMineMouthWhenHeWasSentThere(TestContext context) {
        ServerWorld world = context.getWorld();

        Colony colony = workingMiner(context);

        BlockPos mouth = MinecraftTypeAdapter.toBlockPos(
                VillageColonyMod.MINES.of(colony.id()).orElseThrow().shaft().entry());

        WorkTargets.set(minerOf(colony), mouth, MinerReach.ARRIVAL);

        String line = MinerReport.report(world, colony).orElseThrow();

        try {
            context.assertTrue(
                    line.contains("out of reach"),
                    "o mineiro alcançou a pedra, e sem isso o teste não mede nada: " + line);

            context.assertTrue(
                    line.contains("mine mouth"),
                    "a linha não diz que ele foi mandado à boca da mina: " + line);
        } finally {
            MineClaims.clearAll();
            WorkTargets.clear(minerOf(colony));
        }

        context.complete();
    }

    /** O mineiro desta colônia de teste, que é um só. */
    private static UUID minerOf(Colony colony) {
        return VillageColonyMod.WORKERS.ofColony(colony.id()).get(0).villagerId();
    }

    /**
     * Uma colônia com mina, um mineiro dentro dela e uma pedra em mãos.
     *
     * <p>Mesmo cenário do {@code theCycleLineSaysWhereHeIsWhenOutOfReach}:
     * o que estes testes medem é o que a linha <b>diz</b>, e para isso
     * ela precisa existir com o mineiro fora de alcance.
     */
    private static Colony workingMiner(TestContext context) {
        Colony colony = mineOwner(context);

        galleryFace(context);

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, new BlockPos(1, 2, 3));
        villager.setBreedingAge(0);

        Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
        worker.assign(ProfessionType.MINER);

        WorkerEquipment.equip(context.getWorld(), List.of(worker));

        VillageColonyMod.STORAGES.register(WorkerStorage.of(
                villager.getUuid(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(1, 2, 1)))));

        Task task = VillageColonyMod.TASKS.create(
                colony.id(), TaskType.COLLECT_STONE, TaskPriority.PRODUCTION,
                ResourceType.COBBLESTONE, 8);

        task.reserveFor(villager.getUuid());

        MinerWork.run(context.getWorld(), colony);
        MinerWork.tick(context.getWorld());

        return colony;
    }

    /**
     * O destino morre junto com a tarefa — 2026-08-29.
     *
     * <p><b>Visto em jogo:</b> <i>"mineiro e pastor rodando no mesmo
     * lugar"</i>. O destino do aldeão sobrevivia ao trabalho que o
     * criou.
     *
     * <p>Toda profissão larga o trabalho da mesma forma quando a tarefa
     * deixa de estar aberta — um {@code removeIf} sobre o mapa de
     * trabalhos —, e nenhuma delas soltava o destino junto. O
     * {@code WorkTargets} só era limpo em dois lugares: quando o
     * trabalhador é <b>dispensado</b>, e nas desistências de cada
     * trabalho. Tarefa que <b>termina bem</b> não passa por nenhum dos
     * dois.
     *
     * <p>E destino que fica é destino que manda: o
     * {@code GoToWorkTargetTask} roda enquanto houver um, e não expira.
     * O aldeão passa o resto do expediente sendo empurrado para o último
     * lugar onde trabalhou — a ovelha que já foi tosquiada, a pedra que
     * já caiu. De fora, ele fica rodando ali.
     *
     * <p>Este teste mede o mineiro, que é onde há montagem pronta. O
     * conserto é o mesmo nas sete, e a razão é uma só.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "miner_report",
            tickLimit = 40)
    public void theWorkTargetDiesWithTheTask(TestContext context) {
        ServerWorld world = context.getWorld();

        Colony colony = workingMiner(context);

        UUID miner = minerOf(colony);

        try {
            context.assertTrue(
                    WorkTargets.of(miner).isPresent(),
                    "o mineiro saiu sem destino, e sem isso este teste não mede nada");

            // A tarefa cumprida: reservada, executada, terminada. É o
            // caminho que não passava por nenhuma limpeza de destino.
            VillageColonyMod.TASKS.ofColony(colony.id()).forEach(task -> {
                task.start();
                task.complete();
            });

            MinerWork.run(world, colony);

            context.assertTrue(
                    WorkTargets.of(miner).isEmpty(),
                    "a tarefa acabou e o destino ficou em "
                            + WorkTargets.of(miner).map(BlockPos::toShortString).orElse("")
                            + " — o aldeão vai passar o expediente rodando ali");
        } finally {
            MineClaims.clearAll();
            WorkTargets.clear(miner);
        }

        context.complete();
    }

    /**
     * A linha diz quando ele está preso <b>embaixo</b> — 2026-08-29.
     *
     * <p><b>A sessão das 04:40.</b> O mineiro passou seis minutos com a
     * mesma linha, e ela dizia só <i>"out of reach"</i>:
     *
     * <pre>
     * he is at 757, 42, 877, 5,4 blocks away;
     * it was walking to 758, 44, 878;
     * the place to stand is 758, 44, 878;
     * </pre>
     *
     * <p>Oito leituras, todas na <b>mesma posição</b>, sem andar um
     * bloco. E o número que respondia estava ali sem ser lido: y=42
     * contra y=44 do lugar de ficar de pé. <b>Dois blocos abaixo</b>, e
     * aldeão sobe um. Ele não estava longe nem perdido — estava num
     * poço.
     *
     * <p>"Fora de alcance" cobre coisas com correções diferentes: longe
     * demais, caminho que a navegação não traça, e preso. A terceira
     * passou a ter nome.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "miner_report",
            tickLimit = 40)
    public void theLineSaysWhenHeIsTrappedBelowTheWayUp(TestContext context) {
        ServerWorld world = context.getWorld();

        Colony colony = workingMiner(context);

        UUID miner = minerOf(colony);

        // Um destino acima da cabeça dele: é a forma do poço.
        BlockPos above = world.getEntity(miner).getBlockPos().up(3);

        WorkTargets.set(miner, above, MinerReach.ARRIVAL);

        String line = MinerReport.report(world, colony).orElseThrow();

        try {
            context.assertTrue(
                    line.contains("out of reach"),
                    "o mineiro alcançou a pedra, e sem isso o teste não mede nada: " + line);

            context.assertTrue(
                    line.contains("below"),
                    "a linha não diz que ele está preso embaixo: " + line);
        } finally {
            MineClaims.clearAll();
            WorkTargets.clear(miner);
        }

        context.complete();
    }

    /**
     * O que espera a escada não gasta a busca de quem a tem —
     * 2026-09-04.
     *
     * <p><b>O impasse inteiro da sessão daquele dia.</b> Vinte e cinco
     * minutos com {@code 36f88641 waiting for the shaft — 0ca37494 is in
     * it} em todo ciclo, os dois contadores em {@code stall 0/2400} e
     * {@code still 0/300}, e a colônia recebendo <b>uma</b> pedra em
     * meia hora.
     *
     * <p>A correção de 2026-09-02 não alcançava isto, e o motivo é
     * cruel: ela solta a escada na passagem em que o <i>dono</i> procura
     * e não acha, e o dono nunca conseguia uma passagem. O orçamento de
     * buscas é de uma por tique para a colônia inteira; quem vinha antes
     * no mapa a gastava, e quem vinha antes era o barrado — que não
     * varre nada, porque o portão do {@code claim} o recusa antes.
     *
     * <p>A regra que faltava: <b>recusa não é busca</b>. Quem é barrado
     * no portão não olhou pedra nenhuma, e não pode cobrar do orçamento
     * o que não gastou.
     *
     * <p>A ordem aqui é escolhida, e não sorteada: o {@code JOBS} virou
     * {@code LinkedHashMap} no mesmo dia, e a tarefa do barrado é criada
     * primeiro de propósito — é essa a ordem que trava, e um teste que a
     * deixasse ao acaso passaria metade das vezes sem medir nada.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_refusal_budget",
            tickLimit = 60)
    public void theMinerWaitingForTheShaftDoesNotSpendTheOwnersSearch(TestContext context) {
        ServerWorld world = context.getWorld();

        solidRock(context);

        Colony colony = mineOwner(context);

        ColonyFixture owned = ColonyFixture.create().owning(colony);

        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));

        // A do barrado primeiro: é ela que entra antes no JOBS, e é dele
        // a primeira mão no orçamento do tique.
        UUID waiter = miner(context, colony, chest, owned, new BlockPos(1, 9, 1));
        UUID digger = miner(context, colony, chest, owned, new BlockPos(5, 9, 5));

        reserveStone(colony, ResourceType.COBBLESTONE, waiter);
        reserveStone(colony, ResourceType.COAL, digger);

        try {
            MinerWork.run(world, colony);

            // O poço passa a ser do segundo pela porta da frente, com a
            // rocha ainda inteira — é assim que o jogo o entrega, e
            // enquanto ele é rocha é de um mineiro só.
            MineDigging.nextTarget(world, digger, colony.id(), context.getAbsolutePos(ROCK));

            context.assertTrue(
                    MineDigging.armOf(colony.id(), digger).isPresent(),
                    "o teste precisa de um ramal na mão do segundo para medir alguma coisa");

            // Agora ela se esvazia: a próxima passagem dos donos não acha
            // pedra, e é nela que eles soltariam os ramais.
            hollowGallery(world, colony);

            MinerWork.tick(world);

            context.assertTrue(
                    MineDigging.armOf(colony.id(), digger).isEmpty(),
                    "o dono não teve passagem para soltar o ramal: o barrado gastou a busca"
                            + " do tique sem ter varrido nada — é o impasse de 2026-09-04");
        } finally {
            MineClaims.clearAll();
            MinerWork.clearAll();
            owned.cleanUp();
        }

        context.complete();
    }

    /**
     * <b>A escada que o jogador constrói é corredor, e não parede</b> —
     * 2026-09-05.
     *
     * <p>O autor trocou a descida da mina por uma escada de tijolos de
     * pedra, e a colônia inteira parou na porta:
     *
     * <pre>
     * miners: 6dccfd1e digging Escadas de Tijolos de Pedra at 1448, 44, 63,
     *   29,0 blocks away (out of reach, he is at 1436, 64, 81,
     *   walking to the mine mouth at 1436, 63, 81)
     * </pre>
     *
     * <p>Ele estava <b>em cima da boca</b> e era mandado para a boca.
     * {@code passable} perguntava se a caixa de colisão era vazia, degrau
     * tem colisão, e o corredor quebrava no primeiro deles: o passo não
     * achava saída e o desvio devolvia um bloco onde ele já estava.
     * Treze desistências sem um passo dado.
     *
     * <p><b>O chão era {@code isSolidBlock}</b> — cubo cheio e opaco —, e
     * por isso ninguém ficava de pé num degrau: a perna do mineiro nunca
     * escolhia um como ponto de parada. O que o pé precisa embaixo é
     * alguma coisa com colisão.
     *
     * <p>A segunda afirmação é a que segura a correção: <b>pedra maciça
     * continua parede</b>. Uma correção que abrisse o corredor por dentro
     * da rocha mandaria o mineiro atravessar o morro.
     *
     * <p>Rodado contra a correção desligada: ficar de pé no degrau
     * reprova.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "miner_player_stairs",
            tickLimit = 20)
    public void thePlayersStaircaseIsACorridorAndNotAWall(TestContext context) {
        BlockPos step = new BlockPos(2, 2, 2);
        BlockPos wall = new BlockPos(5, 2, 2);

        context.setBlockState(step.down(), Blocks.STONE.getDefaultState());
        context.setBlockState(step, Blocks.STONE_BRICK_STAIRS.getDefaultState());

        // A parede é uma <b>coluna</b> maciça, que é a forma da frente de
        // escavação: rocha com rocha em cima. Uma pedra solta com dois
        // blocos de ar acima dela seria degrau, e se anda por cima —
        // montá-la assim provaria o contrário do que este teste quer.
        for (int up = 0; up < 3; up++) {
            context.setBlockState(wall.up(up), Blocks.STONE.getDefaultState());
        }

        ServerWorld world = context.getWorld();

        MinerReach.Footing corridor = MinerWork.footingIn(world);

        context.assertTrue(
                corridor.passable(context.getAbsolutePos(step)),
                "o corredor quebra no degrau do jogador, e a colônia para na porta");

        context.assertTrue(
                BuilderApproach.standable(world, context.getAbsolutePos(step.up())),
                "ninguém fica de pé em cima do degrau do jogador");

        context.assertFalse(
                corridor.passable(context.getAbsolutePos(wall)),
                "rocha maciça virou corredor — o mineiro vai atravessar o morro");

        context.complete();
    }

    /**
     * <b>E a picareta não a derruba</b> — pedido do autor, 2026-09-05:
     * <i>"corrigir a escada que o player constrói, ou qualquer caminho
     * que o próprio player cria dentro da mina"</i>.
     *
     * <p>A outra ponta do defeito acima. O corredor deixou de quebrar no
     * degrau em 09-05; o que ficou de pé era o mineiro <b>cavando</b> o
     * degrau — {@code digging Escadas de Tijolos de Pedra}. O
     * {@code canDig} protege a vila gerada e o que a colônia construiu, e
     * a escada do jogador não é nenhuma das duas.
     */
    /**
     * <b>E o bloco cheio de tijolo também fica de pé</b> — 2026-09-10.
     *
     * <p><b>A lacuna que ele fecha foi achada refatorando</b>, não em
     * jogo: o par de testes da escada do jogador usa
     * {@code STONE_BRICK_STAIRS}, e escada não é cubo cheio — ela já é
     * barrada pelo {@code isFullCube}, <b>antes</b> de a regra do tijolo
     * ser consultada. Removida a exclusão
     * {@code !state.isIn(BlockTags.STONE_BRICKS)} de {@code MineRock},
     * <b>704 unitários e 277 testes de jogo continuavam verdes</b>.
     *
     * <p>E é essa exclusão que carrega a decisão do autor de 2026-09-05.
     * O critério é <i>forma e ferramenta</i>: cubo cheio, quebrável com
     * picareta ou pá, <b>e tijolo de pedra não</b> — porque é a única
     * família de cubo cheio que nenhuma caverna gera, e é dela que a
     * escada dele é feita. Sem o cubo cheio no cenário, a regra que
     * distingue tijolo de pedregulho nunca é exercitada.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "miner_player_stairs",
            tickLimit = 20)
    public void theMinerDoesNotDigThePlayersStoneBricks(TestContext context) {
        solidRock(context);

        Colony colony = openedMine(context, 3);

        BlockPos step = dug(context, colony, 3);

        // Cubo cheio, e não escada: é aqui que a regra do tijolo manda,
        // porque o isFullCube já não filtrou nada.
        context.getWorld().setBlockState(step, Blocks.STONE_BRICKS.getDefaultState());

        try {
            context.assertFalse(
                    MineRock.isRock(
                            context.getWorld(), step, context.getWorld().getBlockState(step)),
                    "o tijolo de pedra do jogador passou por rocha, e a picareta o abriria");

            Optional<BlockPos> next = targetFor(context, colony);

            context.assertTrue(
                    next.isPresent(),
                    "a mina não devolveu alvo nenhum, e sem isso o teste não mede nada");

            context.assertFalse(
                    next.get().equals(step),
                    "o mineiro mirou o tijolo do jogador em " + step.toShortString());
        } finally {
            MineClaims.clearAll();
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "miner_player_stairs",
            tickLimit = 20)
    public void theMinerDoesNotDigThePlayersStaircase(TestContext context) {
        solidRock(context);

        Colony colony = openedMine(context, 3);

        BlockPos step = dug(context, colony, 3);

        context.getWorld().setBlockState(step, Blocks.STONE_BRICK_STAIRS.getDefaultState());

        try {
            Optional<BlockPos> next = targetFor(context, colony);

            context.assertTrue(
                    next.isPresent(),
                    "a mina não devolveu alvo nenhum, e sem isso o teste não mede nada");

            context.assertFalse(
                    next.get().equals(step),
                    "o mineiro mirou a escada do jogador em " + step.toShortString());

            context.assertTrue(
                    next.get().equals(dug(context, colony, 4)),
                    "ele devia ter passado por ela para a posição seguinte, e foi para "
                            + next.get().toShortString());
        } finally {
            MineClaims.clearAll();
        }

        context.complete();
    }

    /**
     * E o degrau do jogador não é o fim da galeria — 2026-09-05.
     *
     * <p><b>Esta é a metade que faltava, e é a que derrubou a primeira
     * tentativa.</b> Ela mexeu só no {@code isStillClosed}, e a mina
     * emudeceu: nem fronteira, nem cursor recuado, nem picareta numa
     * sessão inteira. O motivo está escrito no {@code isOpenSpace} —
     * recuo do cursor e escolha do alvo <b>têm de concordar</b>, e
     * mexer num lado só troca um defeito por outro maior.
     *
     * <p>Os dois testes juntos são a concordância: o de cima afirma que
     * a escolha do alvo pula o degrau, e este afirma que o recuo também
     * o pula. Um sem o outro passa com a mina quebrada.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "miner_player_stairs",
            tickLimit = 20)
    public void thePlayersStepInTheDigOrderIsNotTheFrontier(TestContext context) {
        solidRock(context);

        Colony colony = openedMine(context, 9);

        // <b>A última posição já aberta</b>, e é ela que discrimina. O
        // frontierWhereRockBegins só chama de frente a posição fechada
        // cuja seguinte também é fechada — ou a última antes do cursor.
        // Um degrau no meio do corredor tem ar depois e nunca seria
        // frente; um degrau aqui é, e o cursor recua até ele.
        BlockPos step = dug(context, colony, 8);

        context.getWorld().setBlockState(step, Blocks.STONE_BRICK_STAIRS.getDefaultState());

        try {
            Optional<BlockPos> next = targetFor(context, colony);

            context.assertTrue(
                    next.isPresent() && next.get().equals(dug(context, colony, 9)),
                    "a frente da galeria recuou até o degrau do jogador: foi para "
                            + next.map(BlockPos::toShortString).orElse("lugar nenhum"));
        } finally {
            MineClaims.clearAll();
        }

        context.complete();
    }

    /**
     * <b>A pedra que ninguém alcançou sai da vez</b> — E44, 2026-09-10.
     *
     * <p>A sessão daquele dia, às 08:33, mediu o laço inteiro e ele
     * prendeu <b>os dois</b> mineiros da colônia:
     *
     * <pre>
     * 08:33     4c4171a4 mira 2442,44,-1424 — out of reach, 9,9 blocos
     * 08:34:13  desiste: "walked for 2400 ticks ... without arriving"
     * 08:34:43  d5f6de43 assume o ramal e recebe A MESMA pedra
     * 08:36:17  desiste com a mesma frase
     * </pre>
     *
     * <p>O {@code couldNotReach} devolve a posição ao cursor, e isso está
     * certo — pular por uma desistência deixou três sessões com a galeria
     * intacta em 2026-08-27. O que faltava era o prazo: segurar <i>sem
     * prazo</i> é o laço, e enquanto ele corre a picareta deve ir adiante.
     *
     * <p>É a metade da <b>escolha do alvo</b>. A do recuo é o teste
     * seguinte, e um sem o outro passa com a mina quebrada — ver o
     * {@code MineFrontier.isStillClosed}.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_refused_stone",
            tickLimit = 20)
    public void theGalleryStepsPastTheStoneNobodyCouldReach(TestContext context) {
        solidRock(context);

        Colony colony = openedMine(context, 9);

        BlockPos refused = dug(context, colony, 9);

        MineMarks.refuse(context.getWorld(), refused);

        try {
            Optional<BlockPos> next = targetFor(context, colony);

            context.assertTrue(
                    next.isPresent(),
                    "a mina não devolveu alvo nenhum, e sem isso o teste não mede nada");

            context.assertFalse(
                    next.get().equals(refused),
                    "o cursor serviu de novo a pedra de " + refused.toShortString()
                            + ", que é o E44 inteiro");

            context.assertTrue(
                    next.get().equals(dug(context, colony, 10)),
                    "ele devia ter passado para a posição seguinte, e foi para "
                            + next.get().toShortString());
        } finally {
            MineClaims.clearAll();
            MineMarks.clearAll();
        }

        context.complete();
    }

    /**
     * <b>A pedra pulada fica para trás, e isto é o comportamento, não um
     * acidente</b> — achado do {@code gauntlet-verifier}, 2026-09-10.
     *
     * <p>Ele reprovou a primeira versão do E44 por uma promessa que o
     * javadoc fazia e o código não cumpria: <i>"curto o bastante para a
     * escada que o jogador acabou de construir valer na mesma sessão"</i>.
     * Vale nos três leitores que procuram por proximidade — veia, pedra de
     * superfície, areia —, e <b>não</b> vale no cursor do túnel.
     *
     * <p>O motivo é uma regra de 2026-09-02 que veio antes desta:
     * {@code frontierWhereRockBegins} só chama de frente a posição fechada
     * cuja seguinte também está fechada, para o cursor não recuar 83
     * passos até um resto solto dentro do túnel. Uma pedra pulada por
     * castigo cujo vizinho seguinte foi cavado <b>é</b> um resto solto.
     *
     * <p>Este teste fixa isso por escrito, e é de propósito que ele afirme
     * o que afirma: um teste que exigisse o retorno estaria pedindo o laço
     * de recuo de volta. Se um dia o bloco esquecido doer em jogo, o lugar
     * de mexer é o {@code frontierWhereRockBegins} — e este teste é o que
     * vai falhar primeiro, avisando que a troca está sendo feita.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_refused_stone",
            tickLimit = 20)
    public void theSkippedStoneStaysBehindOnceTheGalleryMovedPast(TestContext context) {
        solidRock(context);

        Colony colony = openedMine(context, 9);

        BlockPos refused = dug(context, colony, 9);

        MineMarks.refuse(context.getWorld(), refused);

        try {
            Optional<BlockPos> first = targetFor(context, colony);

            context.assertTrue(
                    first.isPresent() && first.get().equals(dug(context, colony, 10)),
                    "o teste não chegou a montar o cenário: a galeria não passou adiante");

            // A picareta pega na seguinte, e o cursor fica além dela.
            context.getWorld().setBlockState(first.get(), Blocks.AIR.getDefaultState());

            // E o prazo da recusada vence.
            MineMarks.dug(refused);

            MineClaims.clearAll();

            Optional<BlockPos> second = targetFor(context, colony);

            context.assertTrue(
                    second.isPresent(),
                    "a mina parou de dar alvo depois de pular uma pedra");

            context.assertFalse(
                    second.get().equals(refused),
                    "o cursor voltou até " + refused.toShortString()
                            + " — é o recuo de 09-02 de volta, e ele custou uma sessão");
        } finally {
            MineClaims.clearAll();
            MineMarks.clearAll();
        }

        context.complete();
    }

    /**
     * <b>E a pedra de castigo não é a frente da galeria</b> — E44, e esta
     * é a metade que derruba a correção pela metade.
     *
     * <p>O {@code MineFrontier} já trazia a lição por escrito, de uma
     * tentativa de 2026-09-05 que apagou a mina inteira: <i>"uma posição
     * que o nextCut vai pular não pode ser a fronteira, senão o cursor
     * recua até ela toda passagem"</i>. Marcar só na escolha do alvo
     * devolveria o laço pela porta do recuo — a picareta pula a pedra, a
     * busca da fronteira recua o cursor de volta para ela, e o mineiro
     * volta a mirá-la na passagem seguinte.
     *
     * <p><b>E o dano do recuo não é o alvo desta passagem — é o ramal.</b>
     * Uma marca só não serviria de prova: com o cursor recuado, o
     * {@code nextCut} pula a pedra e devolve a mesma posição seguinte, e
     * o teste passaria com a mina quebrada. É o buraco que o teste irmão
     * do degrau do jogador também tem, e vale dito.
     *
     * <p>O que discrimina é a <b>curva</b>. Toda posição pulada por
     * castigo conta para o {@code BLOCKED_BEFORE_TURNING}, que existe
     * para a galeria inteiramente inalcançável não marchar pela ordem de
     * cavar — a lição de 2026-08-27. Com o cursor recuando até a primeira
     * marca, as oito são recontadas <b>a cada passagem</b> e o ramal
     * morre: {@code arm.finish()}, alvo nenhum, e a colônia sem pedra.
     *
     * <p>Por isso o arranjo é de oito: as posições 2 a 9 voltam a ser
     * rocha e recebem a marca, e o que se afirma é que a mina segue para
     * a décima em vez de recuar até a segunda e encerrar o ramal.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_refused_stone",
            tickLimit = 20)
    public void theRefusedStoneIsNotTheFrontier(TestContext context) {
        solidRock(context);

        Colony colony = openedMine(context, 10);

        // De volta a rocha: só assim elas são fronteira candidata, e é
        // exatamente o caso que o cursor recuava para pegar. Duas
        // seguidas bastam para o frontierWhereRockBegins morder; são
        // oito porque é a conta da curva que o recuo estraga.
        for (int i = 2; i <= 9; i++) {
            BlockPos refused = dug(context, colony, i);

            context.getWorld().setBlockState(refused, Blocks.STONE.getDefaultState());

            MineMarks.refuse(context.getWorld(), refused);
        }

        try {
            Optional<BlockPos> next = targetFor(context, colony);

            context.assertTrue(
                    next.isPresent(),
                    "o ramal encerrou: o cursor recuou até a primeira pedra de castigo"
                            + " e recontou as oito, que é a curva morrendo por engano");

            context.assertTrue(
                    next.get().equals(dug(context, colony, 10)),
                    "a frente da galeria recuou até a pedra de castigo: foi para "
                            + next.get().toShortString());
        } finally {
            MineClaims.clearAll();
            MineMarks.clearAll();
        }

        context.complete();
    }

    /**
     * <b>A curva vira quando as recusas se somam</b> — 2026-09-11, e este
     * teste existe porque a primeira tentativa dele não media nada.
     *
     * <p>O {@code BLOCKED_BEFORE_TURNING} existe desde 2026-08-27 para
     * uma coisa: se a frente é inalcançável, juntar as recusas e encerrar
     * o ramal, em vez de marchar pela ordem de cavar com o mundo intacto.
     * <b>E ele não virava.</b> A contagem era zerada por
     * {@code arm.digging()} no ponto em que o cursor <i>serve</i> a
     * pedra, e não no em que a picareta a quebra — e servir é uma aposta,
     * só depois se sabe se o mineiro chega nela. Bastava uma pedra nova
     * sem marca por passagem, e uma galeria atrás de um vão
     * intransponível tem pedra de sobra.
     *
     * <p>A sessão de 2026-09-11 às 00:04 mediu o preço: nove
     * desistências em oito minutos, catorze marcas <b>todas dizendo
     * {@code refused 1}</b>, nenhum {@code went one level deeper}, cinco
     * {@code no miner branch work}, e os mineiros parados nos mesmos dois
     * lugares da sessão anterior. A queixa do autor foi <i>"todos parados
     * sem trabalhar, muitos aldeões aglomerados na mina"</i>.
     *
     * <p><b>O par de chamadas aqui é o que o {@code MinerWork} faz de
     * verdade</b>, e é isso que faltava na primeira versão deste teste:
     * quem desiste chama {@code MineMarks.refuse} <b>e</b>
     * {@code MineDigging.couldNotReach}. Só com a marca, o
     * {@code findTheFrontier} reserva e reenvia a mesma pedra para
     * sempre, o ramal encerra na segunda volta por outro motivo, e o
     * teste passa com e sem o conserto — que foi o que aconteceu, e é
     * pior que não ter teste. A receita correta veio do
     * {@code gauntlet-verifier}, que a montou para provar a lacuna.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_refused_stone",
            tickLimit = 20)
    public void onlyThePickaxeResetsTheBranchCurve(TestContext context) {
        solidRock(context);

        Colony colony = openedMine(context, 5);

        UUID miner = UUID.randomUUID();

        try {
            for (int refusal = 1; refusal <= 9; refusal++) {
                Optional<BlockPos> target = MineDigging.nextTarget(
                        context.getWorld(),
                        miner,
                        colony.id(),
                        context.getAbsolutePos(LIT_ENTRY));

                if (target.isEmpty()) {
                    // Oito é o BLOCKED_BEFORE_TURNING do MineDigging,
                    // que é privado de lá. Escrito à mão e de propósito:
                    // se alguém mudar a curva, este teste tem de ser
                    // lido junto, porque é o número que ele afirma.
                    context.assertTrue(
                            refusal > 8,
                            "o ramal acabou na recusa " + refusal
                                    + ", antes da curva de oito — não foi a curva"
                                    + " que o encerrou, e o teste não mede o que promete");

                    context.complete();

                    return;
                }

                // O par que o MinerWork chama quando o mineiro não chega:
                // marca a pedra E devolve a posição ao cursor. Nenhuma
                // picareta no meio, de propósito.
                MineMarks.refuse(context.getWorld(), target.get());

                MineDigging.couldNotReach(colony.id(), target.get());
            }

            context.throwGameTestException(
                    "o ramal não acabou depois de nove recusas seguidas: a curva está"
                            + " sendo zerada por quem SERVE a pedra, e não por quem a CAVA");
        } finally {
            MineClaims.clearAll();
            MineMarks.clearAll();
        }
    }

    /**
     * <b>O beco da vizinhança não vale para o cursor do túnel</b> —
     * P1.10, e este teste existe porque o {@code gauntlet-verifier}
     * provou que a bateria inteira passava sem ele.
     *
     * <p>O {@code MineMarks} tem duas perguntas desde 2026-09-10: a
     * marca <b>do bloco</b> ({@code isOutOfReach}) e o <b>beco</b>
     * ({@code isUnreachableAround}, três recusas vivas num cubo de oito
     * blocos). O beco é pergunta dos três leitores de proximidade — veia,
     * pedra de superfície, areia — e <b>não</b> deste cursor, porque aqui
     * ele vira veto: a posição deixa de ser fronteira candidata, o cursor
     * não acha frente e o ramal morre. Na galeria a ordem de cavar sobe
     * em caracol e posições consecutivas ficam a um ou dois blocos, então
     * o cubo engole a frente legítima junto com as recusadas.
     *
     * <p><b>Por que o teste irmão não bastava.</b> O
     * {@code theRefusedStoneIsNotTheFrontier} recusa as oito posições
     * <i>uma a uma</i>, e aí a marca de bloco sozinha já produz o
     * resultado que ele afirma: trocar a pergunta daqui pela do beco
     * passava por ele sem falhar — medido pelo Verifier rodando a bateria
     * inteira duas vezes com a mutação aplicada. Ele prova que a marca de
     * bloco vale; não prova que a de região não deve valer.
     *
     * <p>O que discrimina é uma posição <b>sem marca própria</b> cercada
     * de recusadas. As três recusas ficam em posições <b>já cavadas</b>,
     * que continuam ar: elas não são fronteira candidata e não mudam nada
     * para a marca de bloco — o que fazem é uma coisa só, encher o cubo
     * em volta da fronteira legítima.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_refused_stone",
            tickLimit = 20)
    public void theNeighbourhoodDeadEndIsNotTheTunnelCursorsQuestion(TestContext context) {
        solidRock(context);

        Colony colony = openedMine(context, 10);

        try {
            // As duas últimas voltam a ser rocha: assim a 8 é resto
            // sólido com a seguinte também sólida, que é o que o
            // frontierWhereRockBegins chama de frente.
            BlockPos frontier = dug(context, colony, 8);

            context.getWorld().setBlockState(frontier, Blocks.STONE.getDefaultState());

            context.getWorld().setBlockState(
                    dug(context, colony, 9), Blocks.STONE.getDefaultState());

            // E três recusas em volta, em posições que continuam AR.
            for (int i : new int[] {5, 6, 7}) {
                MineMarks.refuse(context.getWorld(), dug(context, colony, i));
            }

            context.assertTrue(
                    !MineMarks.isOutOfReach(context.getWorld(), frontier),
                    "a montagem falhou: a fronteira não pode ter marca própria,"
                            + " senão o teste passa pelo motivo errado");

            context.assertTrue(
                    MineMarks.isUnreachableAround(context.getWorld(), frontier),
                    "a montagem falhou: as três recusas tinham de fazer beco em"
                            + " volta da fronteira — sem isso as duas perguntas"
                            + " respondem igual e o teste não discrimina nada");

            Optional<BlockPos> next = targetFor(context, colony);

            context.assertTrue(
                    next.isPresent(),
                    "o ramal encerrou: o cursor do túnel passou a perguntar pelo"
                            + " beco, e a galeria inteira ficou fechada");

            context.assertTrue(
                    next.get().equals(frontier),
                    "a fronteira sem marca própria foi descartada por causa das"
                            + " vizinhas: o cursor foi para " + next.get().toShortString());
        } finally {
            MineClaims.clearAll();
            MineMarks.clearAll();
        }

        context.complete();
    }

    /**
     * <b>A mina de save antigo conserta a forma velha</b> — 2026-09-09,
     * e a pergunta é de 09-05.
     *
     * <p>A pendência dizia: <i>"o cursor gravado aponta para a ordem de
     * antes da escada dupla e do túnel de três; o findTheFrontier lê o
     * mundo e deve se acertar sozinho, mas a escada larga e o teto alto
     * só aparecem no que ainda não foi cavado"</i>. E dizia também
     * <b>não foi visto acontecer</b> — era suspeita, e suspeita se
     * responde medindo.
     *
     * <p><b>O arranjo é o que o {@code MineSave} faz de verdade.</b>
     * Save de forma diferente não traduz a fronteira: ela volta ao
     * primeiro degrau ({@code cuts = new int[0]}, e todo ramal nasce em
     * zero). O mundo aqui tem a escavação da forma velha — as posições 0
     * a 8 abertas — com <b>um buraco</b>: a 5 continua rocha, que é o
     * bloco que a forma de hoje abre e a de ontem não abria.
     *
     * <p>O que se afirma é que a mina volta a ele em vez de marchar para
     * a frente do que já está aberto. O já aberto é pulado de graça, 64
     * por passagem, e a picareta cai no que a forma nova acrescentou.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_old_shape",
            tickLimit = 20)
    public void theMineFromAnOldShapeDigsWhatTheOldOneLeftBehind(TestContext context) {
        solidRock(context);

        Colony colony = openedMine(context, 9);

        MineShaft shaft = VillageColonyMod.MINES.of(colony.id()).orElseThrow().shaft();

        BlockPos missing = dug(context, colony, 5);

        context.getWorld().setBlockState(missing, Blocks.STONE.getDefaultState());

        // A fronteira que o save de outra forma devolve: o primeiro
        // degrau, e não o número gravado.
        VillageColonyMod.MINES.restore(Mine.restore(colony.id(), shaft, 0));

        try {
            Optional<BlockPos> next = targetFor(context, colony);

            context.assertTrue(
                    next.isPresent() && next.get().equals(missing),
                    "a mina passou por cima do que a forma velha deixou fechado —"
                            + " o buraco está em " + missing.toShortString()
                            + " e ela foi para "
                            + next.map(BlockPos::toShortString).orElse("lugar nenhum"));
        } finally {
            MineClaims.clearAll();
        }

        context.complete();
    }

    /**
     * <b>A boca da mina ganha um arco de pedra com lanterna</b> —
     * decisão do autor, 2026-09-05: <i>"colocar um arco de pedra com
     * lanterna na entrada da mina"</i>.
     *
     * <p>Dois pilares nos lados e uma verga por cima ligando os dois. Os
     * lados são os perpendiculares ao rumo da descida — é isso que
     * emoldura a entrada em vez de tapá-la.
     *
     * <p><b>Quatro blocos de altura, e a lanterna em cima</b> — decisão
     * do autor em 2026-09-11. Eram três, e a lanterna ficava pendurada
     * <b>dentro</b> do vão, roubando a altura que o arco tinha. Agora ela
     * é a única da boca e mora sobre a verga, de onde se vê de mais
     * longe sem custar passagem.
     *
     * <p>O caso afirma o vão inteiro — a boca e os três blocos acima
     * dela —, porque o arco não pode custar a passagem que ele decora.
     *
     * <p>Os pilares começam <b>um bloco acima do chão</b>, e o baú nasce
     * dois blocos ao lado: a primeira versão deste arco disputou o lugar
     * da mobília e a mina ficou sem baú, e o conserto de então deixou o
     * baú livre para nascer <b>debaixo</b> de um pilar — que é baú que
     * não abre. Ver {@code MineMouth.besideTheArch}.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_arch",
            tickLimit = 20)
    public void theMineMouthGetsAStoneArchWithALantern(TestContext context) {
        BlockPos mouth = new BlockPos(3, 2, 3);

        // Chão em volta, e não só sob a boca: a mobília da boca pede ar
        // sobre bloco sólido, e uma arena sem piso não tem onde pôr o
        // baú — foi assim que a primeira versão deste teste mediu uma
        // boca que nunca chegou a ser mobiliada.
        for (int x = 1; x <= 5; x++) {
            for (int z = 1; z <= 5; z++) {
                context.setBlockState(new BlockPos(x, 1, z), Blocks.STONE.getDefaultState());
            }
        }

        ServerWorld world = context.getWorld();

        MineMouth.furnish(world, context.getAbsolutePos(mouth), Direction.NORTH, false);

        Direction side = Direction.NORTH.rotateYClockwise();

        context.assertTrue(
                archBlockAt(context, mouth.offset(side).up()).isOf(Blocks.COBBLESTONE)
                        && archBlockAt(context, mouth.offset(side.getOpposite()).up())
                                .isOf(Blocks.COBBLESTONE),
                "a boca da mina ficou sem os pilares do arco");

        context.assertTrue(
                archBlockAt(context, mouth.up(ARCH_TOP)).isOf(Blocks.COBBLESTONE),
                "o arco ficou sem a verga por cima da boca");

        context.assertTrue(
                archBlockAt(context, mouth.up(ARCH_TOP + 1)).isOf(Blocks.LANTERN),
                "o arco ficou sem a lanterna em cima da verga");

        // O vão inteiro: a boca e os três acima dela. É a altura que o
        // autor pediu, e afirmá-la bloco a bloco é o que impede o arco de
        // encolher sem ninguém notar.
        for (int up = 0; up < ARCH_TOP; up++) {
            context.assertTrue(
                    archBlockAt(context, mouth.up(up)).isAir(),
                    "o arco tapou a passagem que ele decora, em " + up + " acima da boca");
        }

        context.complete();
    }

    /** A primeira tocha de parede da arena, se a mina acendeu alguma. */
    private static Optional<BlockPos> wallTorchIn(TestContext context) {
        for (int x = 0; x <= 15; x++) {
            for (int y = 0; y <= 15; y++) {
                for (int z = 0; z <= 15; z++) {
                    BlockPos at = context.getAbsolutePos(new BlockPos(x, y, z));

                    if (context.getWorld().getBlockState(at).isOf(Blocks.WALL_TORCH)) {
                        return Optional.of(at);
                    }
                }
            }
        }

        return Optional.empty();
    }

    /** O bloco daquela posição relativa, lido do mundo. */
    private static BlockState archBlockAt(TestContext context, BlockPos relative) {
        return context.getWorld().getBlockState(context.getAbsolutePos(relative));
    }
}
