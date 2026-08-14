package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.fabric.work.LumberjackWork;
import com.villagecolony.fabric.work.ManufacturerWork;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * O estado que um teste criou, para ele desfazer só o que é dele.
 *
 * <p><b>Por que isto existe.</b> Até 2026-08-13 cada teste começava e
 * terminava chamando {@code COLONIES.clear()} e companhia. Parecia
 * higiene e era sabotagem: **a bateria roda testes concorrentes**. Um
 * teste que atravessa noventa ticks continua vivo enquanto os batches
 * seguintes começam, e o `clear` de um apagava a colônia do outro no meio
 * do caminho.
 *
 * <p>O sintoma era um teste que passava ou falhava conforme o vizinho —
 * o `cycle_deficit` acusando "a colônia não pediu madeira" quando a
 * colônia dele tinha sido apagada por um teste que rodava junto. Dois
 * testes gastaram uma sessão por isso.
 *
 * <p>A regra que fica: **nenhum teste apaga o que não criou.** Os
 * registros do mod são estáticos e compartilhados; quem os usa se limpa
 * pelo identificador.
 */
final class ColonyFixture {

    private final List<UUID> workers = new ArrayList<>();

    private Colony colony;

    private ColonyFixture() {
    }

    static ColonyFixture create() {
        return new ColonyFixture();
    }

    /** Guarda a colônia deste teste, para removê-la no fim. */
    ColonyFixture owning(Colony colony) {
        this.colony = colony;

        return this;
    }

    /** Guarda um trabalhador deste teste. */
    ColonyFixture owning(UUID workerId) {
        workers.add(workerId);

        return this;
    }

    /**
     * Desfaz o que este teste criou, e nada mais.
     *
     * <p>Chamado no fim de cada teste. As tarefas saem pela colônia, os
     * trabalhadores e baús pelo identificador de cada um, e o trabalho em
     * curso pelo mesmo caminho que a morte de um aldeão usa.
     */
    void cleanUp() {
        for (UUID worker : workers) {
            LumberjackWork.forget(worker);
            ManufacturerWork.forget(worker);
            WorkTargets.clear(worker);

            VillageColonyMod.WORKERS.remove(worker);
            VillageColonyMod.STORAGES.remove(worker);
        }

        if (colony != null) {
            for (Task task : VillageColonyMod.TASKS.ofColony(colony.id())) {
                VillageColonyMod.TASKS.remove(task.id());
            }

            // Obra e construção saem junto: desde 2026-08-14 um teste de
            // construção deixa canteiro e casa no registro, e os dois são
            // globais. Um lote que a colônia de outro teste escolhesse
            // poderia cair sobre a casa deste.
            VillageColonyMod.CONSTRUCTIONS.removeOfColony(colony.id());
            VillageColonyMod.BUILDINGS.removeOfColony(colony.id());

            VillageColonyMod.COLONIES.remove(colony.id());
        }

        workers.clear();

        colony = null;
    }
}
