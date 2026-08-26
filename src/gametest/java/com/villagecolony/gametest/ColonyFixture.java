package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.fabric.work.BuilderWork;
import com.villagecolony.fabric.work.LumberjackWork;
import com.villagecolony.fabric.work.ManufacturerWork;
import com.villagecolony.fabric.work.MinerWork;
import com.villagecolony.fabric.work.ShepherdWork;
import com.villagecolony.fabric.work.SmelterWork;

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
            // As seis profissões, na ordem do VillagerLifecycleHandler.
            //
            // O do construtor entrou em 2026-08-25, pelo mesmo motivo que
            // ele entrou lá: trabalho de obra que sobrevive ao teste é
            // trabalho que o teste seguinte herda.
            //
            // Mineiro, fundidor e pastor entraram em 2026-08-26. Faltavam
            // aqui, e cada teste dessas três profissões chamava o `forget`
            // à mão — sempre DEPOIS do `cleanUp`, e às vezes depois do
            // `assertTrue`, que lança. Uma afirmação que caísse deixava o
            // trabalhador vivo para o resto da bateria.
            MinerWork.forget(worker);
            SmelterWork.forget(worker);
            ShepherdWork.forget(worker);
            LumberjackWork.forget(worker);
            ManufacturerWork.forget(worker);
            BuilderWork.forget(worker);
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

            // A mina também é da colônia e também é global. Deixá-la
            // atrás faria o teste seguinte herdar uma escada aberta em
            // outra arena, e o mineiro dele desceria por ela.
            VillageColonyMod.MINES.removeOfColony(colony.id());

            VillageColonyMod.COLONIES.remove(colony.id());
        }

        workers.clear();

        colony = null;
    }
}
