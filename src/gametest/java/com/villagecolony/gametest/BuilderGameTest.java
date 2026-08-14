package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.storage.model.WorkerStorage;
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
