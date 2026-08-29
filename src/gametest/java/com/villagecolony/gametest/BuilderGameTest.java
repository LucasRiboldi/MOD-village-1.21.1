package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.coordination.WorkAssignment;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.ChestInventoryReader;
import com.villagecolony.fabric.work.BuilderApproach;
import com.villagecolony.fabric.work.BuilderWork;
import com.villagecolony.fabric.work.MaterialChoice;
import com.villagecolony.fabric.work.ConstructionPlanner;
import com.villagecolony.fabric.work.TestBarrier;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.Schedule;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * O construtor levanta a casa — TASK-034 e TASK-035, e a Fase 11 junto.
 *
 * <p>É o primeiro trabalho do mod que <b>acrescenta</b> bloco ao mundo, e
 * por isso é o que mais precisa de fronteira: o Core não sabe o que é
 * colocar um bloco, e um bloco no lugar errado é dano que ninguém desfaz.
 *
 * <p>O projeto destes testes é uma parede de duas peças, e não a casa de
 * planície: cento e cinquenta blocos a um por segundo passariam de dois
 * minutos, e a bateria inteira roda em cinco segundos. A casa de verdade
 * é exercitada onde ela cabe — em {@code BlueprintReaderGameTest} —, e o
 * que estes testes provam é o que só o mundo prova: o bloco entra, o
 * material sai do baú, e a construção vira infraestrutura da colônia.
 */
public class BuilderGameTest implements FabricGameTest {

    private static final BlockPos CHEST = new BlockPos(2, 2, 2);

    private static final BlockPos STAND = new BlockPos(3, 2, 2);

    /** Onde a parede sobe: ao alcance do construtor, e sobre chão sólido. */
    private static final BlockPos SITE = new BlockPos(4, 2, 2);

    private static final ResourceId HUT = ResourceId.vanilla("village/plains/houses/test_wall");

    private record Fixture(Colony colony, ConstructionProject project, ColonyPos chest,
            ColonyFixture owned) {
    }

    /**
     * Colônia, construtor com baú abastecido, obra em andamento e tarefa
     * reservada.
     *
     * @param planks quantas tábuas o baú recebe
     */
    private static Fixture setUp(TestContext context, int planks) {
        return setUp(context, planks, wall(), 2);
    }

    /**
     * O mesmo cenário, com a planta e o tamanho da tarefa por fora.
     *
     * <p>Existe por causa da Regra 14: a torre precisa da mesma
     * montagem da parede, e só muda o que se manda construir.
     */
    private static Fixture setUp(
            TestContext context, int planks, Blueprint plan, int taskAmount) {

        ServerWorld world = context.getWorld();

        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());
        world.setTimeOfDay(Schedule.WORK_TIME);

        // Chão sólido sob a parede: sem ele o primeiro bloco não teria em
        // que se apoiar, e o teste mediria a regra errada.
        context.setBlockState(SITE.down(), Blocks.STONE.getDefaultState());
        context.setBlockState(SITE.down().east(), Blocks.STONE.getDefaultState());

        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));

        if (planks > 0) {
            ChestDepositor.deposit(world, chest, Items.OAK_PLANKS, planks);
        }

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, STAND);
        villager.setBreedingAge(0);

        Colony colony = Colony.create(UUID.randomUUID(), chest);

        VillageColonyMod.COLONIES.register(colony);

        Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
        worker.assign(ProfessionType.BUILDER);

        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chest));

        ConstructionProject project = ConstructionProject.plan(
                colony.id(),
                plan,
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(SITE)));

        VillageColonyMod.CONSTRUCTIONS.register(project);

        project.moveTo(ConstructionState.PREPARING);
        project.moveTo(ConstructionState.BUILDING);

        Task task = VillageColonyMod.TASKS.create(
                colony.id(), TaskType.BUILD, TaskPriority.PRODUCTION,
                ResourceType.OAK_PLANKS, taskAmount);

        task.reserveFor(villager.getUuid());

        BuilderWork.run(world, colony);

        return new Fixture(
                colony,
                project,
                chest,
                ColonyFixture.create().owning(colony).owning(villager.getUuid()));
    }

    /**
     * A porta do E8: uma entrada só no projeto, duas metades no mundo.
     *
     * <p>Até 2026-08-15 o projeto guardava as duas metades da porta —
     * é assim que o arquivo do jogo as grava — e a obra punha as duas
     * no estado padrão, que é a metade de <b>baixo</b>. O resultado
     * eram duas metades de baixo empilhadas: nem porta, nem bloco
     * legítimo, e o dobro do material.
     *
     * <p>O que este teste afirma é a correção inteira, nas três pontas:
     * o projeto pede uma porta só, o mundo recebe as duas metades, e as
     * duas metades estão <b>ligadas</b> — a de cima com
     * {@code half=upper}, que é a propriedade que faz o jogo tratá-las
     * como uma porta.
     *
     * <p>Rodado contra a regra desligada em 2026-08-15: sem
     * {@code BuilderWork.placeSecondHalf} a terceira afirmação falha, e
     * sem o descarte em {@code StructureBlueprintReader.isSecondHalf} a
     * conta do baú falha.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "builder_door",
            tickLimit = 300)
    public void theDoorGoesUpAsOneDoorAndNotTwoHalves(TestContext context) {
        Fixture fixture = setUpDoor(context);

        context.runAtTick(90, () -> {
            BlockState lower = stateAt(context, SITE);
            BlockState upper = stateAt(context, SITE.up());

            try {
                context.assertTrue(
                        lower.isOf(Blocks.OAK_DOOR),
                        "a metade de baixo da porta não foi colocada — veio "
                                + lower.getBlock());

                context.assertTrue(
                        upper.isOf(Blocks.OAK_DOOR),
                        "a metade de cima da porta não foi colocada — veio "
                                + upper.getBlock());

                context.assertTrue(
                        lower.get(Properties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER
                                && upper.get(Properties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER,
                        "as duas metades não estão ligadas: embaixo "
                                + lower.get(Properties.DOUBLE_BLOCK_HALF)
                                + ", em cima " + upper.get(Properties.DOUBLE_BLOCK_HALF));

                context.assertTrue(
                        doorsIn(context, fixture.chest) == 1,
                        "uma porta devia custar uma porta, e o baú ficou com "
                                + doorsIn(context, fixture.chest) + " de 2");
            } finally {
                fixture.owned.cleanUp();
            }

            context.complete();
        });
    }


    /**
     * A Regra 10: o construtor fabrica o que falta para a obra.
     *
     * <p>É o que travou a sessão de 2026-08-18. O relatório repetia
     * {@code WAITING_RESOURCES ... waiting for minecraft:oak_door} com
     * <b>154 tábuas guardadas</b> na colônia: ninguém fazia a porta, e a
     * cabana parava a um bloco do fim. A colônia tinha tudo de que
     * precisava e mesmo assim não terminava a casa.
     *
     * <p>O baú aqui tem só tábua — seis, que é o que a receita do jogo
     * pede — e o projeto pede uma porta. As três afirmações são a regra
     * inteira: a porta entrou no mundo, a tábua saiu do baú, e o que
     * sobrou da fabricação ficou guardado em vez de sumir.
     *
     * <p>Rodado contra a regra desligada: a obra vai para
     * {@code WAITING_RESOURCES} e nenhuma porta é posta.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "builder_craft",
            tickLimit = 300)
    public void theBuilderMakesTheDoorTheWorkIsWaitingFor(TestContext context) {
        Fixture fixture = setUpDoor(context, Items.OAK_PLANKS, 6);

        context.runAtTick(90, () -> {
            try {
                context.assertTrue(
                        stateAt(context, SITE).isOf(Blocks.OAK_DOOR),
                        "a Regra 10 pede que o construtor faça a porta que falta,"
                                + " e no lugar dela veio "
                                + stateAt(context, SITE).getBlock());

                context.assertTrue(
                        planksIn(context, fixture.chest) == 0,
                        "a porta tinha de custar as seis tábuas, e sobraram "
                                + planksIn(context, fixture.chest));

                // A receita do jogo dá três portas por seis tábuas. Uma foi
                // para a parede; as outras duas são da colônia, e sumir com
                // elas seria destruir material do jogador.
                context.assertTrue(
                        doorsIn(context, fixture.chest) == 2,
                        "o que sobrou da fabricação tinha de ficar no baú, e ficaram "
                                + doorsIn(context, fixture.chest));
            } finally {
                fixture.owned.cleanUp();
            }

            context.complete();
        });
    }

    /**
     * O material vem de mais de um baú, somando até dar.
     *
     * <p>A outra metade da decisão de 2026-08-15: {@code takeMaterial}
     * percorria todos os baús da colônia, mas desistia no primeiro que
     * não tivesse tudo. Três tábuas num baú e três em outro eram seis
     * tábuas que a colônia tinha e não conseguia usar.
     *
     * <p>Rodado contra a regra desligada: nenhum baú sozinho tem as seis,
     * a fabricação não acontece e a porta não sobe.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "builder_craft",
            tickLimit = 300)
    public void theMaterialIsGatheredFromMoreThanOneChest(TestContext context) {
        Fixture fixture = setUpDoorInTwoChests(context);

        context.runAtTick(90, () -> {
            try {
                context.assertTrue(
                        stateAt(context, SITE).isOf(Blocks.OAK_DOOR),
                        "três tábuas num baú e três em outro são seis tábuas, e a"
                                + " porta não subiu");
            } finally {
                fixture.owned.cleanUp();
            }

            context.complete();
        });
    }

    /**
     * Duas colônias de um baú cada, com metade da receita em cada um.
     *
     * <p>Dois trabalhadores porque é assim que a colônia tem dois baús:
     * o registro de baú é por trabalhador.
     */
    private static Fixture setUpDoorInTwoChests(TestContext context) {
        ServerWorld world = context.getWorld();

        BlockPos second = new BlockPos(2, 2, 4);

        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());
        context.setBlockState(second, Blocks.CHEST.getDefaultState());
        world.setTimeOfDay(Schedule.WORK_TIME);

        context.setBlockState(SITE.down(), Blocks.STONE.getDefaultState());

        ColonyPos near = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));
        ColonyPos far = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(second));

        ChestDepositor.deposit(world, near, Items.OAK_PLANKS, 3);
        ChestDepositor.deposit(world, far, Items.OAK_PLANKS, 3);

        VillagerEntity builder = context.spawnEntity(EntityType.VILLAGER, STAND);
        builder.setBreedingAge(0);

        VillagerEntity other = context.spawnEntity(EntityType.VILLAGER, STAND);
        other.setBreedingAge(0);

        Colony colony = Colony.create(UUID.randomUUID(), near);

        VillageColonyMod.COLONIES.register(colony);

        Worker worker = VillageColonyMod.WORKERS.register(builder.getUuid(), colony.id());
        worker.assign(ProfessionType.BUILDER);

        VillageColonyMod.WORKERS.register(other.getUuid(), colony.id())
                .assign(ProfessionType.LUMBERJACK);

        VillageColonyMod.STORAGES.register(WorkerStorage.of(builder.getUuid(), near));
        VillageColonyMod.STORAGES.register(WorkerStorage.of(other.getUuid(), far));

        ConstructionProject project = ConstructionProject.plan(
                colony.id(),
                door(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(SITE)));

        VillageColonyMod.CONSTRUCTIONS.register(project);

        project.moveTo(ConstructionState.PREPARING);
        project.moveTo(ConstructionState.BUILDING);

        Task task = VillageColonyMod.TASKS.create(
                colony.id(), TaskType.BUILD, TaskPriority.PRODUCTION, ResourceType.OAK_PLANKS, 1);

        task.reserveFor(builder.getUuid());

        BuilderWork.run(world, colony);

        return new Fixture(
                colony,
                project,
                near,
                ColonyFixture.create()
                        .owning(colony)
                        .owning(builder.getUuid())
                        .owning(other.getUuid()));
    }


    /**
     * O lote enterrado na duna: o construtor procura onde caber de pé.
     *
     * <p><b>O caso que a sessão de 2026-08-22 mostrou travado.</b> A vila
     * de deserto planejou a primeira casa da história do mod e o
     * construtor passou oito minutos em {@code walking for N ticks
     * without reaching the block}, três vezes até o guarda de dois
     * minutos, sem colocar um bloco. O alvo de caminhada era o pé da
     * coluna na altura da origem da obra — e no deserto essa altura
     * está debaixo da areia. Andar para dentro de bloco sólido é pedir um
     * caminho que não existe, e a task Vanilla simplesmente não anda.
     *
     * <p><b>O que este teste afirma, e o que ele não alcança.</b> A
     * decisão — onde o construtor deve pisar — está aqui inteira. O
     * caminho de ponta a ponta não: reproduzi-lo pede um construtor a
     * mais de {@code REACH} blocos do lote <b>e</b> uma duna por cima
     * dele, e a arena da bateria não tem esse tamanho. Um teste que
     * passasse sem a correção afirmaria menos que nada, e foi o que a
     * primeira versão deste fez.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "builder_buried",
            tickLimit = 20)
    public void theFootOfABuriedColumnIsAboveTheSand(TestContext context) {
        ServerWorld world = context.getWorld();

        BlockPos floor = context.getAbsolutePos(SITE);

        context.setBlockState(SITE.down(), Blocks.STONE.getDefaultState());

        for (int up = 0; up < 3; up++) {
            context.setBlockState(SITE.up(up), Blocks.SAND.getDefaultState());
        }

        Optional<BlockPos> spot = BuilderApproach.standingSpotNear(world, floor);

        context.assertTrue(spot.isPresent(), "a coluna enterrada não ofereceu lugar nenhum");

        context.assertTrue(
                spot.get().getY() > floor.getY(),
                "o pé ficou em " + spot.get().toShortString() + ", dentro da areia");

        context.assertTrue(
                world.getBlockState(spot.get()).isAir()
                        && world.getBlockState(spot.get().up()).isAir()
                        && !world.getBlockState(spot.get().down()).isAir(),
                "o lugar escolhido não é um lugar de pé");

        context.complete();
    }

    /** E a coluna livre continua respondendo o próprio chão. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "builder_buried",
            tickLimit = 20)
    public void theFootOfAnOpenColumnIsTheLotFloor(TestContext context) {
        context.setBlockState(SITE.down(), Blocks.STONE.getDefaultState());

        BlockPos floor = context.getAbsolutePos(SITE);

        Optional<BlockPos> spot = BuilderApproach.standingSpotNear(context.getWorld(), floor);

        context.assertTrue(spot.isPresent(), "a coluna livre não ofereceu lugar nenhum");

        context.assertTrue(
                spot.get().equals(floor),
                "o lote livre devia ser o próprio chão, e veio " + spot.get().toShortString());

        context.complete();
    }

    /**
     * A obra <b>espera</b> pela cama que a colônia não tem — Regra 27.
     *
     * <p><b>Este teste já afirmou o contrário, e vale registrar por
     * quê.</b> A Regra 27 mandou o construtor aguardar o bloco
     * específico, sem exceção. No mesmo dia o autor pôs por cima a
     * barreira provisória da Regra 28, e a cama estava nela: a obra
     * seguia sem cama, e era isto que se afirmava aqui.
     *
     * <p><b>Em 2026-08-21 a cama saiu da barreira</b>, e por uma decisão
     * de fora: a Regra 21 morreu, e com ela a passagem que repunha
     * mobília depois da obra. Riscar a cama passaria a deixar a casa sem
     * cama <b>para sempre</b>, e a demanda de lã sumiria junto — quem a
     * declara agora é a obra aberta. Então a cama voltou a ser esperada,
     * e o que segura a colônia é o {@code PatienceClock}: vinte ciclos, e
     * a obra sai da frente.
     *
     * <p>O baú aqui tem tábua e nenhuma lã.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "builder_furniture",
            tickLimit = 300)
    public void theWorkWaitsForTheFurnitureItCannotMake(TestContext context) {
        Fixture fixture = setUp(context, 8, furnishedRoom(), 1);

        context.runAtTick(90, () -> {
            try {
                context.assertTrue(
                        fixture.project.state() == ConstructionState.WAITING_RESOURCES,
                        "sem lã no baú a obra devia aguardar a cama, e ficou em "
                                + fixture.project.state());
            } finally {
                fixture.owned.cleanUp();
            }

            context.complete();
        });
    }

    /**
     * E o baú, que a colônia sabe fazer, entra — a outra metade.
     *
     * <p>Sem esta afirmação a espera poderia ser cumprida do jeito
     * errado: parar na primeira peça de mobília deixaria toda casa vazia
     * mesmo com material no baú.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "builder_furniture",
            tickLimit = 300)
    public void theFurnitureTheColonyCanMakeGoesIn(TestContext context) {
        Fixture fixture = setUp(context, 8, furnishedRoom(), 1);

        context.runAtTick(90, () -> {
            try {
                context.assertTrue(
                        stateAt(context, SITE).isOf(Blocks.CHEST),
                        "o baú sai de oito tábuas e a colônia tinha oito — veio "
                                + stateAt(context, SITE).getBlock());
            } finally {
                fixture.owned.cleanUp();
            }

            context.complete();
        });
    }

    /**
     * Uma planta de mobília só: um baú que dá para fazer, e uma cama que
     * não.
     *
     * <p>A cama fica no lugar seguinte para as duas caberem sem se
     * atrapalhar.
     */
    private static Blueprint furnishedRoom() {
        return Blueprint.of(HUT, List.of(
                BlueprintBlock.furniture(
                        new ColonyPos(0, 0, 0),
                        MinecraftTypeAdapter.toResourceId(Blocks.CHEST)),
                BlueprintBlock.furniture(
                        new ColonyPos(1, 0, 0),
                        MinecraftTypeAdapter.toResourceId(Blocks.WHITE_BED))));
    }

    /**
     * Colônia, construtor com duas portas no baú, e um projeto de uma
     * porta só.
     *
     * <p>Duas portas de propósito: uma sobra para que a afirmação da
     * conta seja "custou uma" e não "acabou o material".
     */
    private static Fixture setUpDoor(TestContext context) {
        return setUpDoor(context, Items.OAK_DOOR, 2);
    }

    /**
     * O mesmo cenário da porta, com o que o baú recebe por fora.
     *
     * <p>Existe por causa da Regra 10: o baú com tábua em vez de porta
     * é o caso que a sessão de 2026-08-18 mostrou travado.
     */
    private static Fixture setUpDoor(TestContext context, Item stock, int amount) {
        ServerWorld world = context.getWorld();

        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());
        world.setTimeOfDay(Schedule.WORK_TIME);

        context.setBlockState(SITE.down(), Blocks.STONE.getDefaultState());

        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));

        ChestDepositor.deposit(world, chest, stock, amount);

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, STAND);
        villager.setBreedingAge(0);

        Colony colony = Colony.create(UUID.randomUUID(), chest);

        VillageColonyMod.COLONIES.register(colony);

        Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
        worker.assign(ProfessionType.BUILDER);

        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chest));

        ConstructionProject project = ConstructionProject.plan(
                colony.id(),
                door(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(SITE)));

        VillageColonyMod.CONSTRUCTIONS.register(project);

        project.moveTo(ConstructionState.PREPARING);
        project.moveTo(ConstructionState.BUILDING);

        Task task = VillageColonyMod.TASKS.create(
                colony.id(), TaskType.BUILD, TaskPriority.PRODUCTION, ResourceType.OAK_PLANKS, 1);

        task.reserveFor(villager.getUuid());

        BuilderWork.run(world, colony);

        return new Fixture(
                colony,
                project,
                chest,
                ColonyFixture.create().owning(colony).owning(villager.getUuid()));
    }

    /** Uma porta, uma entrada — como o projeto passou a guardá-la. */
    private static Blueprint door() {
        return Blueprint.of(HUT, List.of(
                new BlueprintBlock(
                        new ColonyPos(0, 0, 0),
                        MinecraftTypeAdapter.toResourceId(Blocks.OAK_DOOR))));
    }

    private static BlockState stateAt(TestContext context, BlockPos relative) {
        return context.getWorld().getBlockState(context.getAbsolutePos(relative));
    }

    /**
     * Quantas portas sobraram no baú.
     *
     * <p>Contado aqui e não por {@code ChestInventoryReader}: aquele só
     * conhece os recursos que a colônia acompanha, e porta não é um
     * deles. O teste precisa da contagem crua.
     */
    private static int doorsIn(TestContext context, ColonyPos chest) {
        BlockPos pos = MinecraftTypeAdapter.toBlockPos(chest);

        if (!(context.getWorld().getBlockEntity(pos) instanceof ChestBlockEntity inventory)) {
            return -1;
        }

        int found = 0;

        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.getStack(slot);

            if (stack.isOf(Items.OAK_DOOR)) {
                found += stack.getCount();
            }
        }

        return found;
    }

    /**
     * A obra ganha tarefa sozinha — o defeito da sessão de 2026-08-15.
     *
     * <p><b>Este é o teste que faltava, e o motivo de ele faltar é a
     * lição.</b> Todos os outros desta classe criam a tarefa de
     * construção à mão, e por isso todos passavam enquanto, em jogo,
     * duas casas ficavam cinco horas e quarenta minutos em
     * {@code 151 blocks left} com {@code 0 working}.
     *
     * <p>Nada em produção criava tarefa {@code BUILD}: {@code
     * tasks.create} só era chamado de {@code ColonyCycle.requestMissing},
     * e aquele caminho só produz {@code BUILD} para recurso de categoria
     * {@code CONSTRUCTION}, que nenhum {@code ResourceType} tem. O
     * construtor não tinha o que fazer em vila nenhuma.
     *
     * <p>É o §11 pela segunda vez, com a mesma frase que o E10 rendeu: a
     * pergunta que o teste tem de responder não é "este código
     * funciona?", é <em>"quem põe esta coisa aqui, em jogo?"</em>.
     *
     * <p>Por isso este teste <b>não cria tarefa nenhuma</b>. Ele monta o
     * estado com que a sessão real começa — colônia, obra aberta em
     * BUILDING, construtor com baú abastecido — chama
     * {@code ConstructionPlanner.plan} e {@code WorkAssignment.assign}
     * como o ciclo os chama, e afirma que a parede sobe.
     *
     * <p>Rodado contra a regra desligada em 2026-08-15: sem
     * {@code ConstructionPlanner.ensureTask} nenhum bloco é posto, que é
     * exatamente o que a sessão de jogo mostrou.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "builder_task",
            tickLimit = 300)
    public void theOpenProjectGetsItsOwnTaskWithoutAnyoneCreatingIt(TestContext context) {
        Fixture fixture = setUpWithoutTask(context);

        context.runAtTick(90, () -> {
            try {
                context.assertTrue(
                        isPlanks(context, SITE),
                        "ninguém abriu tarefa para a obra: o primeiro bloco não foi posto");

                context.assertTrue(
                        isPlanks(context, SITE.east()),
                        "a obra parou no primeiro bloco");
            } finally {
                fixture.owned.cleanUp();
            }

            context.complete();
        });
    }

    /**
     * A obra que ficou sem material acorda quando o material chega.
     *
     * <p>{@code WAITING_RESOURCES} era estado terminal na prática: a
     * única transição para {@code BUILDING} estava na criação do
     * projeto, e {@code ensureTask} não abre tarefa fora de
     * {@code BUILDING}. A obra que uma vez ficasse sem material não era
     * tentada nunca mais, ainda que o baú enchesse no minuto seguinte.
     *
     * <p>A sessão das 19:44 de 2026-08-15 mostrou isso: casa parada em
     * 149 blocos, 52 tábuas guardadas, dois fabricantes ociosos, e
     * {@code builders: 0 working, WAITING_RESOURCES ... — no build task}
     * repetindo até o desligamento.
     *
     * <p>Aqui o baú começa vazio, a obra dorme por falta de tábua, o
     * baú é abastecido, e o ciclo seguinte tem de encontrar a parede de
     * pé.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "builder_wakes",
            tickLimit = 400)
    public void theProjectThatRanOutOfMaterialWakesWhenTheChestIsFilled(TestContext context) {
        ServerWorld world = context.getWorld();

        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());
        world.setTimeOfDay(Schedule.WORK_TIME);

        context.setBlockState(SITE.down(), Blocks.STONE.getDefaultState());
        context.setBlockState(SITE.down().east(), Blocks.STONE.getDefaultState());

        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));

        // Baú vazio de propósito: é a falta que põe a obra para dormir.
        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, STAND);
        villager.setBreedingAge(0);

        Colony colony = Colony.create(UUID.randomUUID(), chest);

        VillageColonyMod.COLONIES.register(colony);

        Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
        worker.assign(ProfessionType.BUILDER);

        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chest));

        ConstructionProject project = ConstructionProject.plan(
                colony.id(),
                wall(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(SITE)));

        VillageColonyMod.CONSTRUCTIONS.register(project);

        project.moveTo(ConstructionState.PREPARING);
        project.moveTo(ConstructionState.BUILDING);

        ColonyFixture owned =
                ColonyFixture.create().owning(colony).owning(villager.getUuid());

        ConstructionPlanner.plan(world, colony);

        WorkAssignment.assign(colony.id(), VillageColonyMod.WORKERS, VillageColonyMod.TASKS);

        BuilderWork.run(world, colony);

        // Sem tábua no baú, o construtor chega e desiste.
        context.runAtTick(120, () -> {
            context.assertTrue(
                    project.state() == ConstructionState.WAITING_RESOURCES,
                    "a obra precisava estar esperando material, e está em " + project.state());

            context.assertTrue(
                    !isPlanks(context, SITE),
                    "não havia tábua no baú e mesmo assim um bloco foi posto");

            ChestDepositor.deposit(world, chest, Items.OAK_PLANKS, 8);
        });

        // O ciclo seguinte, como VillageDetectionHandler o roda.
        context.runAtTick(140, () -> {
            ConstructionPlanner.plan(world, colony);

            context.assertTrue(
                    project.state() == ConstructionState.BUILDING,
                    "o material chegou e a obra continua em " + project.state());

            WorkAssignment.assign(colony.id(), VillageColonyMod.WORKERS, VillageColonyMod.TASKS);

            BuilderWork.run(world, colony);
        });

        context.runAtTick(320, () -> {
            try {
                context.assertTrue(
                        isPlanks(context, SITE),
                        "a obra acordou e mesmo assim o bloco não foi posto");
            } finally {
                owned.cleanUp();
            }

            context.complete();
        });
    }

    /**
     * O mesmo cenário de {@link #setUp}, menos a tarefa.
     *
     * <p>A diferença é a coisa inteira: aqui quem abre a tarefa é o
     * {@code ConstructionPlanner}, e quem a entrega ao construtor é o
     * {@code WorkAssignment} — as duas peças que o ciclo chama, na ordem
     * em que ele as chama.
     */
    private static Fixture setUpWithoutTask(TestContext context) {
        ServerWorld world = context.getWorld();

        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());
        world.setTimeOfDay(Schedule.WORK_TIME);

        context.setBlockState(SITE.down(), Blocks.STONE.getDefaultState());
        context.setBlockState(SITE.down().east(), Blocks.STONE.getDefaultState());

        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));

        ChestDepositor.deposit(world, chest, Items.OAK_PLANKS, 8);

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, STAND);
        villager.setBreedingAge(0);

        Colony colony = Colony.create(UUID.randomUUID(), chest);

        VillageColonyMod.COLONIES.register(colony);

        Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
        worker.assign(ProfessionType.BUILDER);

        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chest));

        ConstructionProject project = ConstructionProject.plan(
                colony.id(),
                wall(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(SITE)));

        VillageColonyMod.CONSTRUCTIONS.register(project);

        project.moveTo(ConstructionState.PREPARING);
        project.moveTo(ConstructionState.BUILDING);

        // O ciclo, na ordem em que VillageDetectionHandler o roda:
        // o planejador abre a tarefa, a atribuição a entrega, e o
        // executor a assume.
        ConstructionPlanner.plan(world, colony);

        WorkAssignment.assign(colony.id(), VillageColonyMod.WORKERS, VillageColonyMod.TASKS);

        BuilderWork.run(world, colony);

        return new Fixture(
                colony,
                project,
                chest,
                ColonyFixture.create().owning(colony).owning(villager.getUuid()));
    }

    /** Duas tábuas lado a lado. */
    /**
     * Seis tábuas empilhadas: a Regra 14 medida em altura.
     *
     * <p>A do topo fica a cinco blocos acima do pé do construtor. Isso
     * é de propósito: com o alcance esférico de raio 5, cinco de altura
     * mais um de lado dão distância maior que 5, e o bloco de cima é
     * inalcançável com o construtor <b>dentro</b> do lote.
     */
    private static Blueprint tower() {
        ResourceId planks = MinecraftTypeAdapter.toResourceId(Blocks.OAK_PLANKS);

        return Blueprint.of(HUT, List.of(
                new BlueprintBlock(new ColonyPos(0, 0, 0), planks),
                new BlueprintBlock(new ColonyPos(0, 1, 0), planks),
                new BlueprintBlock(new ColonyPos(0, 2, 0), planks),
                new BlueprintBlock(new ColonyPos(0, 3, 0), planks),
                new BlueprintBlock(new ColonyPos(0, 4, 0), planks),
                new BlueprintBlock(new ColonyPos(0, 5, 0), planks)));
    }

    /**
     * A Regra 14 — o construtor alcança o alto da obra.
     *
     * <p>Visto em jogo em 2026-08-18: parte da casa subiu, e parou. A
     * causa era {@code REACH} medido por {@code isWithinDistance}, que
     * é uma <b>esfera</b>: o bloco alto sai dela ainda que o construtor
     * esteja com o pé no lote, e a obra morre na altura do telhado sem
     * nenhuma linha dizendo por quê.
     *
     * <p>O que o teste afirma é a torre <b>inteira</b>, e não só o topo:
     * uma correção que alcançasse o alto e perdesse a base trocaria um
     * defeito por outro.
     *
     * <p>Rodado contra a regra desligada: com o alcance esférico as
     * cinco primeiras sobem e a sexta nunca, e o teste falha no bloco
     * de cima.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "builder_reach",
            tickLimit = 300)
    public void theBuilderReachesTheTopOfTheWorkFromTheGround(TestContext context) {
        Fixture fixture = setUp(context, 6, tower(), 6);

        context.runAtTick(200, () -> {
            for (int up = 0; up < 6; up++) {
                try {
                    context.assertTrue(
                            isPlanks(context, SITE.up(up)),
                            "a Regra 14 pede a torre inteira, e faltou a tábua "
                                    + up + " blocos acima do chão do lote");
                } finally {
                }

                fixture.owned.cleanUp();
                }

            context.complete();
        });
    }

    private static Blueprint wall() {
        ResourceId planks = MinecraftTypeAdapter.toResourceId(Blocks.OAK_PLANKS);

        return Blueprint.of(HUT, List.of(
                new BlueprintBlock(new ColonyPos(0, 0, 0), planks),
                new BlueprintBlock(new ColonyPos(1, 0, 0), planks)));
    }

    private static boolean isPlanks(TestContext context, BlockPos relative) {
        return context.getWorld()
                .getBlockState(context.getAbsolutePos(relative))
                .isOf(Blocks.OAK_PLANKS);
    }

    private static int planksIn(TestContext context, ColonyPos chest) {
        return ChestInventoryReader
                .read(context.getWorld(), MinecraftTypeAdapter.toBlockPos(chest))
                .amountOf(ResourceType.OAK_PLANKS);
    }

    /**
     * A parede fica de pé, e o material sai do baú.
     *
     * <p>As duas metades da mesma regra: construção nunca cria recurso.
     * Um bloco que entrasse no mundo sem sair do baú seria a colônia
     * inventando matéria.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "builder_wall",
            tickLimit = 300)
    public void theWallGoesUpAndTheChestPaysForIt(TestContext context) {
        Fixture fixture = setUp(context, 8);

        context.runAtTick(90, () -> {
            try {
                context.assertTrue(
                        isPlanks(context, SITE),
                        "o primeiro bloco da parede não foi colocado");

                context.assertTrue(
                        isPlanks(context, SITE.east()),
                        "o segundo bloco da parede não foi colocado");

                context.assertTrue(
                        planksIn(context, fixture.chest) == 6,
                        "o baú devia ter pago duas tábuas e tem "
                                + planksIn(context, fixture.chest));
            } finally {
                fixture.owned.cleanUp();
            }

            context.complete();
        });
    }

    /**
     * A barreira conta a peça que o construtor assentou de verdade — E31.
     *
     * <p><b>O teste unitário prova a conta; este prova o fio.</b> O
     * veredito só deixa de mentir se {@code laidOne} for chamado de onde
     * o bloco encosta no mundo, e nenhuma leitura de código garante
     * isso. Aqui a parede sobe, e o veredito tem de sair de
     * {@code NOTHING_BUILT} sozinho.
     *
     * <p>Tábua não é peça da barreira: numa parede de tábuas nada é
     * riscado, e é por isso que o veredito esperado é a notícia boa —
     * a única forma dela que continua valendo.
     *
     * <p>Batch próprio, e limpeza nas duas pontas: a soma da barreira é
     * da sessão inteira, e uma casa levantada em outro teste entraria
     * nesta conta.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "builder_barrier_tally",
            tickLimit = 300)
    public void theBarrierCountsThePiecesTheBuilderActuallyLays(TestContext context) {
        TestBarrier.clearAll();

        context.assertTrue(
                TestBarrier.verdict() == TestBarrier.Verdict.NOTHING_BUILT,
                "a soma zerada já se dizia construída: " + TestBarrier.verdict());

        Fixture fixture = setUp(context, 8);

        context.runAtTick(90, () -> {
            try {
                context.assertTrue(
                        isPlanks(context, SITE),
                        "a parede não subiu, e sem parede este teste não mede nada");

                context.assertTrue(
                        TestBarrier.verdict() == TestBarrier.Verdict.COVERED_FOR_NOTHING,
                        "a parede subiu e o veredito ficou em " + TestBarrier.verdict());
            } finally {
                TestBarrier.clearAll();
                fixture.owned.cleanUp();
            }

            context.complete();
        });
    }

    /**
     * A casa pronta vira infraestrutura da colônia — TASK-036 e 037.
     *
     * <p>É o registro de que a fusão de vilas depende, e o que dá sentido
     * ao "permanente" do PROJECT_CONSTITUTION.md §10.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "builder_registry",
            tickLimit = 300)
    public void theFinishedHouseBecomesColonyInfrastructure(TestContext context) {
        Fixture fixture = setUp(context, 8);

        context.runAtTick(90, () -> {
            try {
                context.assertTrue(
                        fixture.project.isFinished(),
                        "a obra não terminou: faltam " + fixture.project.remainingCount());

                ColonyPos corner = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(SITE));

                context.assertTrue(
                        VillageColonyMod.BUILDINGS.isColonyInfrastructure(corner),
                        "a casa pronta não entrou no registro de construções");

                context.assertTrue(
                        !VillageColonyMod.BUILDINGS.ofColony(fixture.colony.id()).isEmpty(),
                        "o registro não sabe de quem é a casa");
            } finally {
                fixture.owned.cleanUp();
            }

            context.complete();
        });
    }

    /**
     * Sem material, a obra espera — e não inventa bloco.
     *
     * <p>WAITING_RESOURCES é estado previsto em Construction-System.md.
     * O que não pode acontecer é a parede subir com o baú vazio.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "builder_no_material",
            tickLimit = 300)
    public void withoutMaterialNothingIsBuilt(TestContext context) {
        Fixture fixture = setUp(context, 0);

        context.runAtTick(90, () -> {
            try {
                context.assertTrue(
                        !isPlanks(context, SITE),
                        "a colônia construiu com o baú vazio — inventou matéria");

                context.assertTrue(
                        fixture.project.state() == ConstructionState.WAITING_RESOURCES,
                        "a obra devia estar esperando material, e está em "
                                + fixture.project.state());
            } finally {
                fixture.owned.cleanUp();
            }

            context.complete();
        });
    }

    /**
     * A Regra 27 abriu para pedra: o baú tem pedregulho, a planta pede
     * arenito, e a parede sobe.
     *
     * <p>Emenda do autor em 2026-08-26, com três palavras — <i>abre para
     * pedra só</i>. Até então o construtor aguardava o bloco específico
     * sem exceção, e uma vila que tivesse a pedra errada ficava com a
     * casa parada.
     *
     * <p><b>Duas afirmações, e as duas importam.</b> A parede sobe — a
     * obra não dorme. E o que entra nela é <b>pedregulho</b>, e não
     * arenito: a colônia não inventa matéria, e o que se assenta é o que
     * saiu do baú.
     *
     * <p>Rodado contra a emenda desligada: a obra fica em
     * {@code WAITING_RESOURCES} e nenhum bloco entra.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "builder_stone",
            tickLimit = 300)
    public void theWallOfSandstoneGoesUpWithTheCobblestoneTheColonyHas(TestContext context) {
        Fixture fixture = setUpStone(context);

        context.runAtTick(90, () -> {
            try {
                context.assertTrue(
                        stateAt(context, SITE).isOf(Blocks.COBBLESTONE),
                        "a planta pedia arenito, o baú tinha pedregulho, e o que subiu foi "
                                + stateAt(context, SITE).getBlock().getName().getString());

                context.assertTrue(
                        !stateAt(context, SITE).isOf(Blocks.SANDSTONE),
                        "entrou arenito que a colônia não tinha — isso é inventar matéria");
            } finally {
                fixture.owned.cleanUp();
            }

            context.complete();
        });
    }

    /**
     * Colônia com pedregulho no baú e uma parede de arenito para levantar.
     */
    private static Fixture setUpStone(TestContext context) {
        ServerWorld world = context.getWorld();

        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());
        world.setTimeOfDay(Schedule.WORK_TIME);

        context.setBlockState(SITE.down(), Blocks.STONE.getDefaultState());

        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));

        ChestDepositor.deposit(world, chest, Items.COBBLESTONE, 8);

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, STAND);
        villager.setBreedingAge(0);

        Colony colony = Colony.create(UUID.randomUUID(), chest);

        VillageColonyMod.COLONIES.register(colony);

        Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
        worker.assign(ProfessionType.BUILDER);

        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chest));

        Blueprint plan = Blueprint.of(HUT, List.of(new BlueprintBlock(
                new ColonyPos(0, 0, 0),
                MinecraftTypeAdapter.toResourceId(Blocks.SANDSTONE))));

        ConstructionProject project = ConstructionProject.plan(
                colony.id(),
                plan,
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(SITE)));

        VillageColonyMod.CONSTRUCTIONS.register(project);

        project.moveTo(ConstructionState.PREPARING);
        project.moveTo(ConstructionState.BUILDING);

        Task task = VillageColonyMod.TASKS.create(
                colony.id(), TaskType.BUILD, TaskPriority.PRODUCTION,
                ResourceType.SANDSTONE, 1);

        task.reserveFor(villager.getUuid());

        BuilderWork.run(world, colony);

        return new Fixture(
                colony,
                project,
                chest,
                ColonyFixture.create().owning(colony).owning(villager.getUuid()));
    }

    /**
     * O substituto veste o estado da planta — E28, 2026-08-26.
     *
     * <p>A Emenda 2 da Regra 27 abriu a parede para a madeira, e com ela
     * veio um risco que a pedra não tinha: <b>tronco tem eixo</b>. Uma
     * viga deitada trocada de espécie no estado padrão sairia <b>em pé</b>
     * — a casa fica torta, e é o tipo de defeito que só se vê olhando,
     * nunca no log.
     *
     * <p><b>Isto ainda não acontece em jogo, e vale dizer por quê:</b> o
     * {@code BlueprintBlock} não carrega propriedade nenhuma, e o
     * construtor assenta tudo no estado padrão mais o giro da porta. O
     * eixo do tronco já se perde hoje, com substituição ou sem — é a
     * ADR-008, decidida e por escrever.
     *
     * <p>Então este teste afirma a <b>garantia</b>, e não o sintoma: no
     * dia em que a ADR-008 fizer a planta carregar orientação, a
     * substituição não vai desfazê-la. Sem ele, essa quebra só apareceria
     * meses depois, numa casa torta que ninguém liga à troca de espécie.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "builder_stone",
            tickLimit = 20)
    public void theSubstituteWearsTheStateTheBlueprintAsked(TestContext context) {
        BlockState beam = Blocks.OAK_LOG.getDefaultState()
                .with(Properties.AXIS, Direction.Axis.X);

        BlockState dressed = MaterialChoice.dressedLike(beam, Items.BIRCH_LOG);

        context.assertTrue(
                dressed.isOf(Blocks.BIRCH_LOG),
                "o substituto não é o bloco que saiu do baú");

        context.assertTrue(
                dressed.get(Properties.AXIS) == Direction.Axis.X,
                "a viga trocada de espécie saiu no eixo " + dressed.get(Properties.AXIS)
                        + " — a planta pedia X, e a casa sai torta assim");

        // Pedra não tem eixo: copiar o que não existe não pode explodir.
        BlockState stone = MaterialChoice.dressedLike(beam, Items.COBBLESTONE);

        context.assertTrue(
                stone.isOf(Blocks.COBBLESTONE),
                "vestir pedra com o estado de um tronco não devolveu pedra");

        context.complete();
    }

    /**
     * A cama entra inteira, e a cabeceira não vai para dentro da parede
     * — 2026-08-29.
     *
     * <p><b>Visto em jogo.</b> A frase do autor foi <i>"aparece somente
     * a metade da cama e na direção errada"</i>, e o log da sessão diz
     * por quê, na letra:
     *
     * <pre>
     * Could not finish the two-part block at 769, 64, 935
     *     — Block{minecraft:cobblestone} is in the way
     * </pre>
     *
     * <p>A planta guarda o nome do bloco e não o estado (ADR-005), então
     * a cama saía no <b>padrão</b>, que olha para o norte. Na casa de
     * planície o norte da cama é a parede: a cabeceira não coube, e
     * sobrou meia cama.
     *
     * <p>A saída é perguntar ao mundo em vez de ao arquivo — e é ela que
     * a Regra 32 torna possível, porque com a casa de pé a parede já
     * está lá para ser vista. <b>A orientação fiel ao arquivo continua
     * sendo a ADR-008</b>, e vale para o tronco e o degrau também; o que
     * este teste trava é mais simples e mais urgente: cama que cabe.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "builder_bed",
            tickLimit = 300)
    public void theBedGoesInWholeAndNotIntoTheWall(TestContext context) {
        // <b>A cama no miolo da planta, e não na borda.</b> A Regra 17
        // vira para fora o que está na parede, e numa planta de um bloco
        // só tudo é parede: a primeira versão deste teste passava sem
        // conserto nenhum, porque a regra da porta apontava a cama para
        // longe do muro por acaso. Com dois cantos de pedregulho a caixa
        // fica 3x3x3, a cama cai no centro dela, e volta a valer o que a
        // casa de verdade faz — o estado padrão, que olha para o norte.
        Fixture fixture = setUp(context, 0, bedInTheMiddle(), 1);

        BlockPos bed = SITE.add(1, 1, 1);

        // A parede para onde o padrão aponta. É a forma exata do que a
        // casa de planície tem.
        context.setBlockState(bed.north(), Blocks.COBBLESTONE.getDefaultState());

        ChestDepositor.deposit(context.getWorld(), fixture.chest, Items.WHITE_BED, 1);
        ChestDepositor.deposit(context.getWorld(), fixture.chest, Items.COBBLESTONE, 2);

        context.runAtTick(120, () -> {
            try {
                BlockState foot = context.getBlockState(bed);

                context.assertTrue(
                        foot.isOf(Blocks.WHITE_BED),
                        "o pé da cama não foi posto: "
                                + foot.getBlock().getName().getString());

                context.assertTrue(
                        foot.get(Properties.BED_PART) == BedPart.FOOT,
                        "o que entrou no lugar do pé foi a cabeceira");

                BlockPos head = bed.offset(foot.get(Properties.HORIZONTAL_FACING));

                context.assertFalse(
                        head.equals(bed.north()),
                        "a cama apontou para a parede, que é o defeito visto em jogo");

                context.assertTrue(
                        context.getBlockState(head).isOf(Blocks.WHITE_BED),
                        "a cama ficou pela metade: em " + head.toShortString() + " está "
                                + context.getBlockState(head).getBlock().getName().getString());
            } finally {
                fixture.owned.cleanUp();
            }

            context.complete();
        });
    }

    /**
     * Uma cama no centro de uma caixa de 3x3x3.
     *
     * <p>Os dois cantos de pedregulho existem só para dar tamanho à
     * caixa: sem eles a cama seria borda, e a Regra 17 a viraria para
     * fora — que é o contrário do que este teste quer medir.
     */
    private static Blueprint bedInTheMiddle() {
        return Blueprint.of(
                new ResourceId("villagecolony", "test/bed"),
                List.of(
                        new BlueprintBlock(
                                new ColonyPos(0, 0, 0), ResourceId.vanilla("cobblestone")),
                        BlueprintBlock.furniture(
                                new ColonyPos(1, 1, 1), ResourceId.vanilla("white_bed")),
                        new BlueprintBlock(
                                new ColonyPos(2, 2, 2), ResourceId.vanilla("cobblestone"))));
    }
}
