package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.telemetry.model.ActivityKind;
import com.villagecolony.core.telemetry.model.ActivityProfession;
import com.villagecolony.core.telemetry.model.ActivityState;
import com.villagecolony.core.telemetry.model.ActivityTraceEvent;
import com.villagecolony.core.telemetry.model.ControlledReason;
import com.villagecolony.core.telemetry.model.TargetKind;
import com.villagecolony.core.type.Capability;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;

import java.util.UUID;

/**
 * A porta única por onde toda profissão avisa que desistiu — decisão do
 * autor, 2026-09-10.
 *
 * <p><b>O verificador que o autor pediu já existia pela metade.</b> Os
 * dois guardas — o de imobilidade do {@link WorkStall} (300 tiques) e o
 * de travamento (2.400) — disparam nas sete profissões desde 2026-09-03.
 * O que faltava era o <b>destino</b>: eles devolviam a tarefa para a
 * mesma fila, e só o mineiro e o lenhador chegavam a chamar
 * {@link Worker#rest}. As outras cinco desistiam <b>sem que ninguém
 * contasse</b>, e um fazendeiro que não alcança a roça repetia o mesmo
 * lote a cada ciclo sem deixar rastro no trabalhador.
 *
 * <p>Esta classe fecha isso com uma linha por profissão. Quem escala é o
 * {@code Worker}: três desistências na janela e ele larga o ofício, e a
 * atribuição de profissão o manda para outro — ver
 * {@code Worker.giveUpProfession} e
 * {@code ProfessionAssigner.vacancyFor}.
 *
 * <p><b>A capacidade vem da tarefa, e não de uma constante por
 * profissão.</b> O carpinteiro e o pedreiro compartilham implementação e
 * se separam pela tarefa que executam; ler {@link Task#requiredCapability}
 * é o que impede a contagem de somar os dois no mesmo balde.
 */
public final class WorkerStrikes {

    private WorkerStrikes() {
    }

    /**
     * Este trabalhador desistiu do que estava fazendo.
     *
     * <p>Silencioso quando o aldeão já não é trabalhador da colônia —
     * morte, zumbificação e dispensa derrubam o registro antes de o
     * trabalho ser encerrado, e desistir de um trabalho órfão não é erro.
     */
    public static void gaveUp(UUID workerId, Task task) {
        VillageColonyMod.WORKERS.find(workerId).ifPresent(worker -> {
            worker.profession().ifPresent(profession ->
                    {
                        ActivityLog.failed(profession, task.requiredCapability().name());
                        ActivityLog.abandoned(profession, task.requiredCapability().name());

                        // O traço persistido, decisão 7B — 2026-09-24.
                        // Só aqui, onde o UUID do trabalhador é real: ver
                        // ActivityTraceRegistry para o porquê de IdleLog
                        // (que fala por colônia, sem trabalhador
                        // identificável) ficar fora desta entrega.
                        VillageColonyMod.ACTIVITY_TRACES.append(
                                worker.colonyId(),
                                new ActivityTraceEvent(
                                        workerId,
                                        toActivityProfession(profession),
                                        toActivityKind(task.requiredCapability()),
                                        ActivityState.ABANDONED,
                                        ControlledReason.WORK_STALLED,
                                        toTargetKind(task.requiredCapability()),
                                        0));
                    });
            worker.rest(task.requiredCapability());

            if (!worker.hasProfession()) {
                VillageColonyMod.LOGGER.info(
                        "Worker {} gave up {} once too often and left the trade"
                                + " — the colony will hire it into something else",
                        workerId,
                        task.requiredCapability());
            }
        });
    }

    /**
     * A tradução entre o vocabulário do worker e o do traço —
     * {@code core.telemetry} não pode importar {@code core.worker} nem
     * {@code core.type} guarda esses valores; ver o javadoc de
     * {@link ActivityProfession}. Só {@code fabric}, que enxerga os dois
     * lados, faz essa ponte.
     */
    private static ActivityProfession toActivityProfession(ProfessionType profession) {
        return switch (profession) {
            case MINER -> ActivityProfession.MINER;
            case LUMBERJACK -> ActivityProfession.LUMBERJACK;
            case MASON -> ActivityProfession.MASON;
            case SMELTER -> ActivityProfession.SMELTER;
            case CARPENTER -> ActivityProfession.CARPENTER;
            case FARMER -> ActivityProfession.FARMER;
            case SHEPHERD -> ActivityProfession.SHEPHERD;
            case BUILDER -> ActivityProfession.BUILDER;
        };
    }

    private static ActivityKind toActivityKind(Capability capability) {
        return switch (capability) {
            case COLLECT_STONE -> ActivityKind.MINING;
            case COLLECT_WOOD, CRAFT_WOOD -> ActivityKind.HARVESTING;
            case COLLECT_SURFACE_RESOURCE, COLLECT_SOIL -> ActivityKind.MINING;
            case COLLECT_WOOL, MAINTAIN_FOOD -> ActivityKind.SHEPHERDING;
            case SMELT_ITEMS -> ActivityKind.SMELTING;
            case CRAFT_STONE -> ActivityKind.CRAFTING;
            case BUILD_STRUCTURE -> ActivityKind.BUILDING;
        };
    }

    private static TargetKind toTargetKind(Capability capability) {
        return switch (capability) {
            case COLLECT_STONE, CRAFT_STONE -> TargetKind.STONE;
            case COLLECT_WOOD, CRAFT_WOOD -> TargetKind.WOOD;
            case COLLECT_SURFACE_RESOURCE, COLLECT_SOIL -> TargetKind.ORE;
            case COLLECT_WOOL, MAINTAIN_FOOD -> TargetKind.ANIMAL;
            case SMELT_ITEMS -> TargetKind.ORE;
            case BUILD_STRUCTURE -> TargetKind.CONSTRUCTION;
        };
    }
}
