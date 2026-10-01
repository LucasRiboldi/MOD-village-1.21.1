package com.villagecolony.core.coordination;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.ColonyLifecycle;
import com.villagecolony.core.colony.service.ColonyService;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.service.BuildingRegistry;
import com.villagecolony.core.construction.service.ConstructionService;
import com.villagecolony.core.construction.service.MineRegistry;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.service.TaskService;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.core.worker.service.WorkerService;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Duas colônias viram uma — ADR-007, implementada em 2026-09-30.
 *
 * <p>O gatilho mora no Fabric, que enxerga o mundo; aqui está só a regra: quem
 * sobrevive (§2.1) e o que muda de dono (§3). Nada é cancelado nem demitido:
 * a vila resultante fica com os trabalhadores, as tarefas e as construções das
 * duas, e o teto de profissão volta a valer só na contratação seguinte (§2.2).
 */
public final class ColonyMerge {

    private ColonyMerge() {
    }

    /** O que a fusão moveu, para o log. */
    public record Result(
            UUID survivor, UUID absorbed, int workers, int tasks, int projects,
            int buildings, boolean mineInherited) {
    }

    /**
     * Quem sobrevive: a de mais trabalhadores, que é a vila estabelecida;
     * depois a regra das camas — emenda de 2026-09-30 à ADR-007 §2.1.
     *
     * <p>Só as camas observadas decidiam, e as camas são uma leitura do
     * instante: no playtest de 30-09 a colônia recém-nascida viu 26 camas da
     * vila e absorveu a antiga, com 41 trabalhadores, 9 construções e a mina.
     */
    public static Colony survivorOf(Colony a, Colony b, WorkerService workers) {
        Objects.requireNonNull(workers, "workers");

        int ofA = workers.ofColony(a.id()).size();
        int ofB = workers.ofColony(b.id()).size();

        if (ofA != ofB) {
            return ofA > ofB ? a : b;
        }

        return survivorOf(a, b);
    }

    /**
     * Entre colônias do mesmo tamanho: a de mais camas observadas; no empate,
     * a de id menor, para que a escolha não dependa da ordem do mapa —
     * ADR-007 §2.1.
     */
    public static Colony survivorOf(Colony a, Colony b) {
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");

        if (a.observedBeds() != b.observedBeds()) {
            return a.observedBeds() > b.observedBeds() ? a : b;
        }

        return a.id().toString().compareTo(b.id().toString()) <= 0 ? a : b;
    }

    /**
     * Funde as duas colônias na sobrevivente e tira a absorvida do registro.
     *
     * <p>O centro e as camas observadas são os da sobrevivente (§2.3); o
     * ciclo de vida é ativo se qualquer uma das duas era.
     */
    public static Result merge(
            Colony a, Colony b,
            ColonyService colonies, WorkerService workers, TaskService tasks,
            ConstructionService constructions, BuildingRegistry buildings, MineRegistry mines) {

        Colony survivor = survivorOf(a, b, workers);
        Colony absorbed = survivor == a ? b : a;
        UUID to = survivor.id();
        UUID from = absorbed.id();

        if (absorbed.isActive()) {
            survivor.setLifecycle(ColonyLifecycle.ACTIVE);
        }

        List<Worker> theirWorkers = workers.ofColony(from);
        theirWorkers.forEach(worker -> worker.joinColony(to));

        List<Task> theirTasks = tasks.ofColony(from);
        theirTasks.forEach(task -> task.joinColony(to));

        List<ConstructionProject> theirProjects = constructions.ofColony(from);
        theirProjects.forEach(project -> project.joinColony(to));

        constructions.pendingOf(from).ifPresent(pending -> {
            constructions.dropPending(from);

            if (constructions.pendingOf(to).isEmpty()) {
                constructions.registerPending(new ConstructionService.Pending(
                        pending.id(), to, pending.blueprint(), pending.origin(),
                        pending.state(), pending.deferredPieces()));
            }
        });

        int movedBuildings = buildings.reassign(from, to);
        boolean mineInherited = mines.absorb(from, to);

        colonies.remove(from);

        return new Result(
                to, from, theirWorkers.size(), theirTasks.size(), theirProjects.size(),
                movedBuildings, mineInherited);
    }
}
