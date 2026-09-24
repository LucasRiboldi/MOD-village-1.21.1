package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionOutcome;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.construction.model.SkipReason;
import com.villagecolony.core.coordination.PatienceClock;
import com.villagecolony.core.coordination.WorkClock;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskState;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.work.BuilderWork;
import com.villagecolony.fabric.work.ConstructionPlanner;
import com.villagecolony.fabric.work.WaitingWork;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.Schedule;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.level.ServerWorldProperties;

import java.util.List;
import java.util.UUID;

/**
 * A obra aberta que não anda também larga a vaga — sessão de jogo de
 * 2026-09-19.
 *
 * <p><b>O buraco que isto fecha.</b> O {@link PatienceClock} tirava da
 * frente a obra parada em {@code WAITING_RESOURCES}. A obra em
 * {@code BUILDING} que simplesmente não coloca peça não tinha relógio
 * nenhum — e a vaga de obra é <b>única</b>.
 *
 * <p><b>A medição.</b> Casa de {@code -847, 87, 1310}:
 * {@code 174 blocks left} em <b>41 das 60 leituras</b>, meia hora
 * segurando a fila, sempre em {@code BUILDING} e nunca em
 * {@code WAITING_RESOURCES} — zero linhas daquele estado na sessão
 * inteira. Enquanto isso o planejador voltava em {@code ALREADY_OPEN} e
 * a varredura de lote mal era chamada: nove passagens em noventa
 * minutos.
 *
 * <p>A causa raiz foi o rodízio de ofícios, e ela está consertada. Este
 * guarda é o <b>fundo de poço</b>: seja qual for o motivo de a conta não
 * descer, a colônia volta a planejar.
 */
public class BuildProgressGameTest implements FabricGameTest {

    /** A obra fica no ar, como nos outros testes de obra desta bateria. */
    private static final BlockPos ORIGIN = new BlockPos(1, 4, 1);

    private static final BlockPos UNSUPPORTED_CHEST = new BlockPos(2, 4, 2);

    private static final BlockPos UNSUPPORTED_STAND = new BlockPos(1, 4, 2);

    /**
     * Peça sem apoio não pode sair da planta nem acordar outro construtor
     * sem mudança no mundo.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "build_progress",
            tickLimit = 100)
    public void anUnsupportedPieceStaysPartialWithoutReopeningTheBuildTask(TestContext context) {
        ServerWorld world = context.getWorld();
        context.setBlockState(UNSUPPORTED_CHEST, Blocks.CHEST.getDefaultState());
        context.setBlockState(UNSUPPORTED_STAND.down(), Blocks.STONE.getDefaultState());
        world.setTimeOfDay(Schedule.WORK_TIME);

        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(UNSUPPORTED_CHEST));
        ColonyPos origin = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(ORIGIN));
        Colony colony = Colony.create(UUID.randomUUID(), chest);
        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, UNSUPPORTED_STAND);
        villager.setBreedingAge(0);

        VillageColonyMod.COLONIES.register(colony);
        Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
        worker.assign(ProfessionType.BUILDER);
        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chest));
        ChestDepositor.deposit(world, chest, Items.TORCH, 1);

        ConstructionProject project = ConstructionProject.plan(
                colony.id(),
                Blueprint.of(
                        ResourceId.vanilla("village/plains/houses/unsupported_torch"),
                        List.of(new BlueprintBlock(new ColonyPos(0, 0, 0), ResourceId.vanilla("torch")))),
                origin);
        VillageColonyMod.CONSTRUCTIONS.register(project);
        project.moveTo(ConstructionState.PREPARING);
        project.moveTo(ConstructionState.BUILDING);

        Task task = VillageColonyMod.TASKS.create(
                colony.id(), TaskType.BUILD, TaskPriority.CONSTRUCTION, ResourceType.OAK_PLANKS, 1);
        task.reserveFor(villager.getUuid());
        BuilderWork.run(world, colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony).owning(villager.getUuid());

        context.runAtTick(50, () -> {
            try {
                context.assertTrue(world.getBlockState(context.getAbsolutePos(ORIGIN)).isAir(),
                        "a tocha foi colocada sem um bloco de apoio");
                context.assertTrue(project.remainingCount() == 1,
                        "a peça sem apoio foi riscada como se tivesse sido construída");
                context.assertTrue(project.deferredPieces().size() == 1,
                        "a obra parcial não guardou a razão para não repetir a mesma tentativa");
                context.assertTrue(task.state() == TaskState.COMPLETED,
                        "o construtor continuou segurando uma tarefa sem nenhuma peça colocável");

                ConstructionPlanner.plan(world, colony);

                context.assertFalse(VillageColonyMod.TASKS.ofColony(colony.id()).stream()
                                .anyMatch(candidate -> candidate.type() == TaskType.BUILD
                                        && candidate.isOpen()),
                        "o planejador reabriu a mesma tarefa sem uma mudança no apoio");

                context.setBlockState(ORIGIN.down(), Blocks.STONE.getDefaultState());
                ConstructionPlanner.plan(world, colony);

                context.assertTrue(VillageColonyMod.TASKS.ofColony(colony.id()).stream()
                                .anyMatch(candidate -> candidate.type() == TaskType.BUILD
                                        && candidate.isOpen()),
                        "a mudança no apoio nao devolveu a peca pendente ao construtor");
            } finally {
                owned.cleanUp();
            }

            context.complete();
        });
    }

    /** Uma espera por apoio fisico nao e ausencia de progresso da obra. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "build_progress")
    public void aDeferredPieceIsNotAbandonedByTheProgressClock(TestContext context) {
        ColonyPos origin = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(ORIGIN));
        Colony colony = Colony.create(UUID.randomUUID(), origin);
        BlueprintBlock torch = new BlueprintBlock(new ColonyPos(0, 0, 0), ResourceId.vanilla("torch"));
        ConstructionProject project = ConstructionProject.plan(
                colony.id(),
                Blueprint.of(ResourceId.vanilla("village/plains/houses/deferred_torch"), List.of(torch)),
                origin);
        ColonyFixture owned = ColonyFixture.create().owning(colony);

        VillageColonyMod.COLONIES.register(colony);
        VillageColonyMod.CONSTRUCTIONS.register(project);
        project.defer(
                torch,
                ConstructionOutcome.skipped(origin, SkipReason.UNSUPPORTED),
                "unchanged-support");
        project.moveTo(ConstructionState.PREPARING);
        project.moveTo(ConstructionState.BUILDING);

        ServerWorldProperties clock = (ServerWorldProperties) context.getWorld().getLevelProperties();
        long time = clock.getTime();
        long day = clock.getTimeOfDay();
        try {
            clock.setTimeOfDay(1_000);
            context.assertFalse(
                    WaitingWork.giveUpIfStalled(context.getWorld(), colony, project),
                    "a primeira leitura abandonou a peca adiada");

            clock.setTime(time + PatienceClock.TICKS + 1);

            context.assertFalse(
                    WaitingWork.giveUpIfStalled(context.getWorld(), colony, project),
                    "o relogio abandonou uma peca que esperava uma mudanca no apoio");
            context.assertTrue(VillageColonyMod.CONSTRUCTIONS.openOf(colony.id()).isPresent(),
                    "a obra parcial saiu do registro antes de o apoio fisico mudar");
        } finally {
            clock.setTime(time);
            clock.setTimeOfDay(day);
            owned.cleanUp();
        }

        context.complete();
    }

    /**
     * A obra que não coloca peça nenhuma sai da frente.
     *
     * <p>O relógio é o mesmo da espera por material, e aqui ele é
     * atravessado sem esperar os dez minutos: a bateria adianta a idade
     * do mundo e a restaura na mesma chamada, antes do próximo tique.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "build_progress")
    public void theBuildThatPlacesNothingLetsGoOfTheSlot(TestContext context) {
        ColonyPos origin = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(ORIGIN));

        Colony colony = Colony.create(UUID.randomUUID(), origin);

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);

        ConstructionProject project = ConstructionProject.plan(
                colony.id(), aWallOfPlanks(), origin);

        VillageColonyMod.CONSTRUCTIONS.register(project);

        project.moveTo(ConstructionState.PREPARING);
        project.moveTo(ConstructionState.BUILDING);

        ServerWorldProperties clock = (ServerWorldProperties) context.getWorld().getLevelProperties();
        long time = clock.getTime();
        long day = clock.getTimeOfDay();
        try {
            clock.setTimeOfDay(1_000);
            // A primeira leitura só anota a conta: obra recém-aberta não
            // é obra parada, e puni-la aqui seria o defeito oposto.
            context.assertFalse(
                    WaitingWork.giveUpIfStalled(context.getWorld(), colony, project),
                    "a obra foi largada na primeira leitura, sem nenhuma chance de andar");

            // O mundo envelhece a paciência inteira, e a conta de peças
            // não mudou uma vez.
            clock.setTime(time + PatienceClock.TICKS + 1);

            context.assertTrue(
                    WaitingWork.giveUpIfStalled(context.getWorld(), colony, project),
                    "a obra ficou " + PatienceClock.TICKS + " tiques sem assentar peça"
                            + " e continuou segurando a vaga única — é a casa de 09-19,"
                            + " 174 peças paradas por meia hora");

            context.assertTrue(
                    VillageColonyMod.CONSTRUCTIONS.openOf(colony.id()).isEmpty(),
                    "a obra largada continua aberta, e a colônia segue travada nela");

            context.assertTrue(VillageColonyMod.BUILDINGS.isColonyInfrastructure(origin),
                    "o lote da obra abandonada ficou livre para sobreposição");

            context.complete();
        } finally {
            clock.setTime(time);
            clock.setTimeOfDay(day);
            owned.cleanUp();

            VillageColonyMod.BUILDINGS.removeOfColony(colony.id());
        }
    }

    /**
     * <b>E a obra que anda não é punida por demorar.</b>
     *
     * <p>É a outra metade, e sem ela o guarda seria pior que o defeito:
     * obra grande leva muito mais que dez minutos, e derrubá-la no meio
     * jogaria fora o que já está de pé. O relógio mede <b>peça
     * assentada</b>, e recomeça a cada uma.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "build_progress")
    public void theBuildThatKeepsPlacingIsLeftAlone(TestContext context) {
        ColonyPos origin = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(ORIGIN));

        Colony colony = Colony.create(UUID.randomUUID(), origin);

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);

        ConstructionProject project = ConstructionProject.plan(
                colony.id(), aWallOfPlanks(), origin);

        VillageColonyMod.CONSTRUCTIONS.register(project);

        project.moveTo(ConstructionState.PREPARING);
        project.moveTo(ConstructionState.BUILDING);

        ServerWorldProperties clock = (ServerWorldProperties) context.getWorld().getLevelProperties();
        long time = clock.getTime();
        long day = clock.getTimeOfDay();
        try {
            clock.setTimeOfDay(1_000);
            WaitingWork.giveUpIfStalled(context.getWorld(), colony, project);

            // Uma peça assentada, e o mundo envelhece a paciência
            // inteira depois dela.
            project.markPlaced(project.remaining().get(0));

            clock.setTime(time + PatienceClock.TICKS + 1);

            context.assertFalse(
                    WaitingWork.giveUpIfStalled(context.getWorld(), colony, project),
                    "a obra assentou peça e mesmo assim foi largada — o guarda está"
                            + " medindo tempo aberto em vez de progresso, e obra grande"
                            + " nunca mais terminaria");

            clock.setTime(time + 2L * PatienceClock.TICKS);
            context.assertFalse(WaitingWork.giveUpIfStalled(context.getWorld(), colony, project),
                    "a peça colocada não renovou a janela inteira de paciência");

            clock.setTime(time + 2L * PatienceClock.TICKS + 1);
            context.assertTrue(WaitingWork.giveUpIfStalled(context.getWorld(), colony, project),
                    "a obra parou de novo e nunca mais perdeu a vaga");

            context.complete();
        } finally {
            clock.setTime(time);
            clock.setTimeOfDay(day);
            owned.cleanUp();

            VillageColonyMod.BUILDINGS.removeOfColony(colony.id());
        }
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "build_progress")
    public void theNightDoesNotSpendTheBuildersPatience(TestContext context) {
        ColonyPos origin = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(ORIGIN));
        Colony colony = Colony.create(UUID.randomUUID(), origin);
        ColonyFixture owned = ColonyFixture.create().owning(colony);
        ConstructionProject project = ConstructionProject.plan(colony.id(), aWallOfPlanks(), origin);
        VillageColonyMod.CONSTRUCTIONS.register(project);
        project.moveTo(ConstructionState.PREPARING);
        project.moveTo(ConstructionState.BUILDING);
        ServerWorldProperties clock = (ServerWorldProperties) context.getWorld().getLevelProperties();
        long time = clock.getTime();
        long day = clock.getTimeOfDay();
        try {
            clock.setTimeOfDay(WorkClock.DUSK);
            WaitingWork.giveUpIfStalled(context.getWorld(), colony, project);

            long night = WorkClock.DAY - WorkClock.DUSK;
            clock.setTime(time + night);
            clock.setTimeOfDay(WorkClock.DAY);
            context.assertFalse(WaitingWork.giveUpIfStalled(context.getWorld(), colony, project),
                    "a obra foi abandonada durante o descanso previsto pela Regra 18");

            clock.setTime(time + night + WorkClock.DAY);
            clock.setTimeOfDay(2L * WorkClock.DAY);
            context.assertFalse(WaitingWork.giveUpIfStalled(context.getWorld(), colony, project),
                    "um expediente de 11000 tiques gastou a paciência de 12000");

            clock.setTime(time + night + WorkClock.DAY + 1_000);
            clock.setTimeOfDay(2L * WorkClock.DAY + 1_000);
            context.assertTrue(WaitingWork.giveUpIfStalled(context.getWorld(), colony, project),
                    "o descanso zerou a espera, permitindo que a obra prendesse a vaga para sempre");
            context.complete();
        } finally {
            clock.setTime(time);
            clock.setTimeOfDay(day);
            owned.cleanUp();
        }
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "build_progress")
    public void changingTheSunDoesNotAgeTheBuild(TestContext context) {
        ColonyPos origin = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(ORIGIN));
        Colony colony = Colony.create(UUID.randomUUID(), origin);
        ColonyFixture owned = ColonyFixture.create().owning(colony);
        ConstructionProject project = ConstructionProject.plan(colony.id(), aWallOfPlanks(), origin);
        VillageColonyMod.CONSTRUCTIONS.register(project);
        project.moveTo(ConstructionState.PREPARING);
        project.moveTo(ConstructionState.BUILDING);
        ServerWorldProperties clock = (ServerWorldProperties) context.getWorld().getLevelProperties();
        long time = clock.getTime();
        long day = clock.getTimeOfDay();
        try {
            clock.setTimeOfDay(1_000);
            WaitingWork.giveUpIfStalled(context.getWorld(), colony, project);
            clock.setTimeOfDay(10L * WorkClock.DAY + 1_000);
            context.assertFalse(WaitingWork.giveUpIfStalled(context.getWorld(), colony, project),
                    "/time consumiu paciência sem passar um tique de simulação");

            clock.setTime(time + PatienceClock.TICKS);
            context.assertTrue(WaitingWork.giveUpIfStalled(context.getWorld(), colony, project),
                    "o dia congelado impediu a recuperação da obra parada");
            context.complete();
        } finally {
            clock.setTime(time);
            clock.setTimeOfDay(day);
            owned.cleanUp();
        }
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "build_progress")
    public void abandoningTheProjectCancelsOnlyItsBuildTasks(TestContext context) {
        ColonyPos origin = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(ORIGIN));
        Colony colony = Colony.create(UUID.randomUUID(), origin);
        Colony neighbor = Colony.create(UUID.randomUUID(), origin);
        UUID builder = UUID.randomUUID();
        UUID activeBuilder = UUID.randomUUID();
        ColonyFixture owned = ColonyFixture.create().owning(colony)
                .owning(builder).owning(activeBuilder);
        ColonyFixture other = ColonyFixture.create().owning(neighbor);
        ConstructionProject project = ConstructionProject.plan(colony.id(), aWallOfPlanks(), origin);
        VillageColonyMod.CONSTRUCTIONS.register(project);
        project.moveTo(ConstructionState.PREPARING);
        project.moveTo(ConstructionState.BUILDING);
        try {
            Task reserved = buildTask(colony);
            reserved.reserveFor(builder);
            Task available = buildTask(colony);
            Task executing = buildTask(colony);
            executing.reserveFor(activeBuilder);
            executing.start();
            Task completed = buildTask(colony);
            completed.reserveFor(UUID.randomUUID());
            completed.start();
            completed.complete();
            Task foreign = buildTask(neighbor);
            Task supply = VillageColonyMod.TASKS.create(colony.id(), TaskType.COLLECT_WOOD,
                    TaskPriority.PRODUCTION, ResourceType.OAK_LOG, 2);
            BuilderWork.run(context.getWorld(), colony);
            WorkTargets.set(builder, context.getAbsolutePos(ORIGIN));

            WaitingWork.giveUp(colony, project, false);

            for (Task task : List.of(reserved, available, executing)) {
                context.assertTrue(task.state() == TaskState.CANCELLED,
                        "a obra saiu do registro, mas deixou tarefa aberta: " + task.state());
                context.assertTrue(task.executor().isEmpty(), "a tarefa reteve o construtor");
            }
            context.assertTrue(WorkTargets.of(builder).isEmpty(),
                    "o construtor continua andando para a obra abandonada");
            context.assertTrue(completed.state() == TaskState.COMPLETED,
                    "o abandono alterou uma tarefa concluída");
            context.assertTrue(foreign.isOpen() && supply.isOpen(),
                    "o abandono cancelou tarefa de outra colônia ou pedido de material");
            context.assertTrue(VillageColonyMod.BUILDINGS.isColonyInfrastructure(origin),
                    "o abandono liberou o lote parcial");
            context.complete();
        } finally {
            owned.cleanUp();
            other.cleanUp();
        }
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "build_progress")
    public void aCancelledBuilderJobDropsItsWalkingTargetOnTheNextTick(TestContext context) {
        ColonyPos origin = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(ORIGIN));
        Colony colony = Colony.create(UUID.randomUUID(), origin);
        UUID builder = UUID.randomUUID();
        ColonyFixture owned = ColonyFixture.create().owning(colony).owning(builder);
        ConstructionProject project = ConstructionProject.plan(colony.id(), aWallOfPlanks(), origin);
        VillageColonyMod.CONSTRUCTIONS.register(project);
        project.moveTo(ConstructionState.PREPARING);
        project.moveTo(ConstructionState.BUILDING);
        try {
            Task task = buildTask(colony);
            task.reserveFor(builder);
            BuilderWork.run(context.getWorld(), colony);
            WorkTargets.set(builder, context.getAbsolutePos(ORIGIN));
            task.cancel();

            BuilderWork.tick(context.getWorld());

            context.assertTrue(WorkTargets.of(builder).isEmpty(),
                    "o job foi removido, mas seu destino ficou ativo até outro ciclo");
            context.complete();
        } finally {
            owned.cleanUp();
        }
    }

    private static Task buildTask(Colony colony) {
        return VillageColonyMod.TASKS.create(colony.id(), TaskType.BUILD,
                TaskPriority.CONSTRUCTION, ResourceType.OAK_PLANKS, 2);
    }

    private static Blueprint aWallOfPlanks() {
        return Blueprint.of(
                ResourceId.vanilla("village/plains/houses/plains_small_house_1"),
                List.of(
                        new BlueprintBlock(
                                new ColonyPos(0, 0, 0), ResourceId.vanilla("oak_planks")),
                        new BlueprintBlock(
                                new ColonyPos(1, 0, 0), ResourceId.vanilla("oak_planks"))));
    }
}
