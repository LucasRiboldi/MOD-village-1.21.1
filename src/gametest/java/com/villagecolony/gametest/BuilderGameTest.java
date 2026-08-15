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
import com.villagecolony.fabric.work.BuilderWork;
import com.villagecolony.fabric.work.ConstructionPlanner;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.Schedule;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
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
                wall(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(SITE)));

        VillageColonyMod.CONSTRUCTIONS.register(project);

        project.moveTo(ConstructionState.PREPARING);
        project.moveTo(ConstructionState.BUILDING);

        Task task = VillageColonyMod.TASKS.create(
                colony.id(), TaskType.BUILD, TaskPriority.PRODUCTION, ResourceType.OAK_PLANKS, 2);

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

            fixture.owned.cleanUp();

            context.complete();
        });
    }

    /**
     * Colônia, construtor com duas portas no baú, e um projeto de uma
     * porta só.
     *
     * <p>Duas portas de propósito: uma sobra para que a afirmação da
     * conta seja "custou uma" e não "acabou o material".
     */
    private static Fixture setUpDoor(TestContext context) {
        ServerWorld world = context.getWorld();

        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());
        world.setTimeOfDay(Schedule.WORK_TIME);

        context.setBlockState(SITE.down(), Blocks.STONE.getDefaultState());

        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));

        ChestDepositor.deposit(world, chest, Items.OAK_DOOR, 2);

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
            context.assertTrue(
                    isPlanks(context, SITE),
                    "ninguém abriu tarefa para a obra: o primeiro bloco não foi posto");

            context.assertTrue(
                    isPlanks(context, SITE.east()),
                    "a obra parou no primeiro bloco");

            fixture.owned.cleanUp();

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

            fixture.owned.cleanUp();

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

            fixture.owned.cleanUp();

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
            context.assertTrue(
                    !isPlanks(context, SITE),
                    "a colônia construiu com o baú vazio — inventou matéria");

            context.assertTrue(
                    fixture.project.state() == ConstructionState.WAITING_RESOURCES,
                    "a obra devia estar esperando material, e está em "
                            + fixture.project.state());

            fixture.owned.cleanUp();

            context.complete();
        });
    }
}
