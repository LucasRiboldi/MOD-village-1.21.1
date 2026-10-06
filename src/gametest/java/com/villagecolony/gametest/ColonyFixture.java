package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.fabric.work.BuilderWork;
import com.villagecolony.fabric.work.LumberjackWork;
import com.villagecolony.fabric.work.CraftingWork;
import com.villagecolony.fabric.work.FarmerWork;
import com.villagecolony.fabric.work.MinerWork;
import com.villagecolony.fabric.work.ShepherdWork;
import com.villagecolony.fabric.work.SmelterWork;
import com.villagecolony.fabric.work.SurfaceGatheringWork;

import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

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
public final class ColonyFixture {

    private final List<UUID> workers = new ArrayList<>();

    private Colony colony;

    private ColonyFixture() {
    }

    public static ColonyFixture create() {
        return new ColonyFixture();
    }

    /**
     * Uma colônia nova com centro no ponto relativo da arena, já registrada e
     * já deste teste — ADR-035 §7. Substitui o par {@code Colony.create} +
     * {@code COLONIES.register} + {@code owning} que cada teste repetia.
     */
    public static ColonyFixture colonyAt(TestContext context, BlockPos center) {
        ColonyPos at = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(center));
        Colony colony = Colony.create(UUID.randomUUID(), at);

        VillageColonyMod.COLONIES.register(colony);

        return new ColonyFixture().owning(colony);
    }

    /** A colônia que este teste possui, ou erro se ele não possui nenhuma. */
    public Colony colony() {
        if (colony == null) {
            throw new IllegalStateException("este fixture não possui colônia");
        }

        return colony;
    }

    /**
     * Um trabalhador <b>com a ferramenta</b> desta colônia, já deste teste.
     * É o padrão: na colônia real todo contratado é equipado no mesmo ciclo.
     */
    public Worker equippedWorker(TestContext context, ProfessionType profession, BlockPos at) {
        Worker worker = TestWorkers.createEquippedWorker(context, colony().id(), profession, at);
        owning(worker.villagerId());

        return worker;
    }

    /** O mesmo, de mãos vazias — só quando o assunto não é tempo de quebra. */
    public Worker emptyHandedWorker(TestContext context, ProfessionType profession, BlockPos at) {
        Worker worker = TestWorkers.createWorker(context, colony().id(), profession, at);
        owning(worker.villagerId());

        return worker;
    }

    /** Guarda a colônia deste teste, para removê-la no fim. */
    public ColonyFixture owning(Colony colony) {
        this.colony = colony;

        return this;
    }

    /** Guarda um trabalhador deste teste. */
    public ColonyFixture owning(UUID workerId) {
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
    public void cleanUp() {
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
            SurfaceGatheringWork.forget(worker);
            ShepherdWork.forget(worker);
            LumberjackWork.forget(worker);
            CraftingWork.forget(worker);
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

            // E a varredura da lavoura, pelo mesmo motivo — P1.5,
            // 2026-09-11. O cursor e o descanso são por colônia: um
            // descanso deixado para trás faria o fazendeiro do teste
            // seguinte não varrer o campo que ele acabou de plantar.
            FarmerWork.forgetColony(colony.id());

            VillageColonyMod.COLONIES.remove(colony.id());
        }

        workers.clear();

        colony = null;
    }
}
