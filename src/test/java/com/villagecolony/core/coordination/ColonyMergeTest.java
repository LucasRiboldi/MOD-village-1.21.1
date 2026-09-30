package com.villagecolony.core.coordination;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.ColonyLifecycle;
import com.villagecolony.core.colony.service.ColonyService;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.MineShaft;
import com.villagecolony.core.construction.service.BuildingRegistry;
import com.villagecolony.core.construction.service.ConstructionService;
import com.villagecolony.core.construction.service.MineRegistry;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskState;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.task.service.TaskService;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.type.Side;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.core.worker.service.WorkerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** ADR-007: duas colônias que se tocam ou ocupam o mesmo espaço viram uma. */
class ColonyMergeTest {

    private ColonyService colonies;
    private WorkerService workers;
    private TaskService tasks;
    private ConstructionService constructions;
    private BuildingRegistry buildings;
    private MineRegistry mines;

    @BeforeEach
    void setUp() {
        colonies = new ColonyService();
        workers = new WorkerService();
        tasks = new TaskService();
        constructions = new ConstructionService();
        buildings = new BuildingRegistry();
        mines = new MineRegistry();
    }

    private Colony colonyWithBeds(int x, int beds) {
        Colony colony = colonies.createColony(new ColonyPos(x, 64, 0));
        colony.observe(new ColonyPos(x, 64, 0), beds, true);
        return colony;
    }

    private ColonyMerge.Result merge(Colony a, Colony b) {
        return ColonyMerge.merge(a, b, colonies, workers, tasks, constructions, buildings, mines);
    }

    @Test
    void theColonyWithMoreBedsSurvivesWhicheverOrder() {
        Colony small = colonyWithBeds(0, 3);
        Colony big = colonyWithBeds(40, 10);

        assertSame(big, ColonyMerge.survivorOf(small, big));
        assertSame(big, ColonyMerge.survivorOf(big, small));
    }

    @Test
    void aTieIsBrokenByTheSmallerIdNotByTheOrder() {
        Colony a = colonyWithBeds(0, 5);
        Colony b = colonyWithBeds(40, 5);
        Colony smaller = a.id().toString().compareTo(b.id().toString()) < 0 ? a : b;

        assertSame(smaller, ColonyMerge.survivorOf(a, b));
        assertSame(smaller, ColonyMerge.survivorOf(b, a));
    }

    @Test
    void everyWorkerTaskAndBuildingJoinsTheSurvivorAndNobodyIsDismissed() {
        Colony small = colonyWithBeds(0, 3);
        Colony big = colonyWithBeds(40, 10);

        Worker mover = workers.register(UUID.randomUUID(), small.id());
        workers.register(UUID.randomUUID(), big.id());
        Task task = tasks.create(small.id(), TaskType.COLLECT_WOOD, TaskPriority.PRODUCTION,
                ResourceType.OAK_LOG, 8);
        task.reserveFor(mover.villagerId());
        buildings.register(new Building(UUID.randomUUID(), small.id(), ResourceId.vanilla("hut"),
                new ColonyPos(0, 64, 0), new ColonyPos(4, 68, 4)));

        ColonyMerge.Result result = merge(small, big);

        assertEquals(big.id(), result.survivor());
        assertEquals(2, workers.ofColony(big.id()).size(), "a fusão não reduz trabalhadores");
        assertEquals(big.id(), mover.colonyId());
        assertEquals(big.id(), task.colonyId());
        assertEquals(TaskState.RESERVED, task.state(), "nenhuma tarefa é cancelada nem solta");
        assertEquals(1, buildings.ofColony(big.id()).size());
        assertTrue(buildings.ofColony(small.id()).isEmpty());
        assertFalse(colonies.find(small.id()).isPresent(), "a absorvida sai do registro");
    }

    @Test
    void theOpenBuildOfTheAbsorbedColonyKeepsGoingUnderTheSurvivor() {
        Colony small = colonyWithBeds(0, 3);
        Colony big = colonyWithBeds(40, 10);
        ConstructionProject project = ConstructionProject.plan(small.id(),
                Blueprint.of(ResourceId.vanilla("hut"),
                        List.of(new BlueprintBlock(new ColonyPos(0, 0, 0), ResourceId.vanilla("oak_planks")))),
                new ColonyPos(0, 64, 0));
        constructions.register(project);

        merge(small, big);

        assertEquals(big.id(), project.colonyId());
        assertTrue(constructions.openOf(big.id()).isPresent());
    }

    @Test
    void theSurvivorWithoutAMineInheritsTheAbsorbedOne() {
        Colony small = colonyWithBeds(0, 3);
        Colony big = colonyWithBeds(40, 10);
        mines.open(small.id(), MineShaft.from(new ColonyPos(0, 64, 0), Side.NORTH));

        ColonyMerge.Result result = merge(small, big);

        assertTrue(result.mineInherited());
        assertTrue(mines.of(big.id()).isPresent());
        assertFalse(mines.of(small.id()).isPresent());
    }

    @Test
    void theSurvivorKeepsItsOwnMineAndTheOtherIsForgotten() {
        Colony small = colonyWithBeds(0, 3);
        Colony big = colonyWithBeds(40, 10);
        mines.open(small.id(), MineShaft.from(new ColonyPos(0, 64, 0), Side.NORTH));
        var own = mines.open(big.id(), MineShaft.from(new ColonyPos(40, 64, 0), Side.NORTH));

        ColonyMerge.Result result = merge(small, big);

        assertFalse(result.mineInherited());
        assertSame(own, mines.of(big.id()).orElseThrow());
        assertFalse(mines.of(small.id()).isPresent());
    }

    @Test
    void theMergedColonyIsActiveWhenEitherWas() {
        Colony small = colonyWithBeds(0, 3);
        Colony big = colonyWithBeds(40, 10);
        small.setLifecycle(ColonyLifecycle.ACTIVE);
        big.setLifecycle(ColonyLifecycle.DORMANT);

        merge(small, big);

        assertTrue(big.isActive());
    }
}
