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
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.fabric.work.MineClaims;
import com.villagecolony.fabric.work.MineDigging;
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
                        mine.cut() < FRONTIER,
                        "acreditou num número que o mundo não confirma — ficou em "
                                + mine.cut() + " com o túnel fechado");

                context.assertTrue(
                        mine.cut() > 0,
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
     * pedra cai em seis tiques, e num teste que só olhasse no fim o
     * aldeão teria descido sozinho depois e a prova passaria por engano.
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

        context.runAtEveryTick(() -> {
            if (whenBroken[0] >= 0 || context.getBlockState(target).isOf(rock)) {
                return;
            }

            whenBroken[0] = (int) Math.sqrt(villager.getBlockPos().getSquaredDistance(stone));
        });

        context.runAtTick(320, () -> {
            try {
                context.assertTrue(
                        whenBroken[0] >= 0,
                        "a pedra não saiu do mundo — o mineiro não chegou nela de jeito nenhum");

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

        // Chão sólido em volta: sem ele nenhum vizinho serve, e o teste
        // mediria a ausência de lugar em vez da regra.
        for (Direction side : Direction.Type.HORIZONTAL) {
            context.setBlockState(ROCK.offset(side).down(), Blocks.STONE.getDefaultState());
        }

        Optional<BlockPos> chest = MineMouth.furnish(world, mouth, Direction.SOUTH);

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

        Optional<BlockPos> again = MineMouth.furnish(world, mouth, Direction.SOUTH);

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

        for (Direction side : Direction.Type.HORIZONTAL) {
            context.setBlockState(ROCK.offset(side).down(), Blocks.STONE.getDefaultState());
        }

        // Mobiliada pela metade: o baú está lá, a lanterna não.
        context.setBlockState(ROCK.offset(Direction.NORTH), Blocks.CHEST.getDefaultState());

        MineMouth.furnish(world, mouth, Direction.SOUTH);

        boolean lantern = false;

        for (Direction side : Direction.Type.HORIZONTAL) {
            if (world.getBlockState(mouth.offset(side)).isOf(Blocks.LANTERN)) {
                lantern = true;
            }
        }

        context.assertTrue(lantern, "a boca que já tinha baú ficou sem lanterna para sempre");

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

        for (Direction side : Direction.Type.HORIZONTAL) {
            context.setBlockState(ROCK.offset(side).down(), Blocks.STONE.getDefaultState());
        }

        MineMouth.furnish(world, mouth, Direction.SOUTH);
        MineMouth.furnish(world, mouth, Direction.SOUTH);
        MineMouth.furnish(world, mouth, Direction.SOUTH);

        int lanterns = 0;

        for (Direction side : Direction.Type.HORIZONTAL) {
            for (int drop = 0; drop <= 1; drop++) {
                if (world.getBlockState(mouth.offset(side).down(drop)).isOf(Blocks.LANTERN)) {
                    lanterns++;
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

        for (Direction side : Direction.Type.HORIZONTAL) {
            context.setBlockState(ROCK.offset(side).down(), Blocks.STONE.getDefaultState());
        }

        MineMouth.furnish(world, mouth, Direction.WEST);

        BlockPos firstStep = mouth.offset(Direction.WEST);

        context.assertTrue(
                world.getBlockState(firstStep).isAir(),
                "a mobília ocupou o primeiro degrau: "
                        + world.getBlockState(firstStep).getBlock());

        context.assertFalse(
                world.getBlockState(firstStep.down()).isOf(Blocks.CHEST)
                        || world.getBlockState(firstStep.down()).isOf(Blocks.LANTERN),
                "a mobília ocupou a coluna da descida um abaixo");

        // E as duas peças continuam aparecendo, nos três lados que sobram.
        boolean chest = false;
        boolean lantern = false;

        for (Direction side : Direction.Type.HORIZONTAL) {
            for (int drop = 0; drop <= 1; drop++) {
                BlockPos at = mouth.offset(side).down(drop);

                chest |= world.getBlockState(at).isOf(Blocks.CHEST);
                lantern |= world.getBlockState(at).isOf(Blocks.LANTERN);
            }
        }

        context.assertTrue(chest, "a boca ficou sem baú");
        context.assertTrue(lantern, "a boca ficou sem lanterna");

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

        context.runAtTick(360, () -> {
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
            Optional<BlockPos> mouth = MineDigging.mouthOf(world, center, Side.NORTH);

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
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_one_digger",
            tickLimit = 20)
    public void theShaftTakesOneMinerAndTurnsTheOtherAway(TestContext context) {
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
                            + " — os dois estão cavando a mesma escada");

            context.assertTrue(
                    MineClaims.diggerIn(colony.id()).orElseThrow().equals(first),
                    "a mina não ficou com quem chegou primeiro");
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

        UUID first = miner(context, colony, chest, owned, new BlockPos(1, 9, 1));
        UUID second = miner(context, colony, chest, owned, new BlockPos(5, 9, 5));

        // Duas tarefas, uma por recurso pedido: é assim que a colônia as
        // abre, e é a forma exata do defeito de 08-26.
        reserveStone(colony, ResourceType.COBBLESTONE, first);
        reserveStone(colony, ResourceType.COAL, second);

        MinerWork.run(world, colony);

        // O primeiro desce, e a mina passa a ter dono.
        MineDigging.nextTarget(world, first, colony.id(), context.getAbsolutePos(ROCK));

        try {
            String line = MinerReport.report(world, colony).orElseThrow();

            context.assertTrue(
                    line.contains("waiting for the shaft"),
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

            // O alto da coluna do índice 8: o cursor está em 18, e a luz
            // fica um espaçamento inteiro atrás dele.
            BlockPos lit = context.getAbsolutePos(new BlockPos(3, 6, 3));

            context.assertTrue(
                    context.getWorld().getBlockState(lit).isOf(Blocks.WALL_TORCH),
                    "a galeria continuou escura: "
                            + context.getWorld().getBlockState(lit).getBlock().getName()
                                    .getString()
                            + " em " + lit.toShortString());

            // E o chão da mesma coluna continua sendo degrau — a tocha
            // no piso foi o primeiro defeito desta feature.
            BlockPos floor = context.getAbsolutePos(new BlockPos(3, 4, 3));

            context.assertTrue(
                    context.getWorld().getBlockState(floor).isAir(),
                    "a tocha comeu o degrau em " + floor.toShortString());
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
}
