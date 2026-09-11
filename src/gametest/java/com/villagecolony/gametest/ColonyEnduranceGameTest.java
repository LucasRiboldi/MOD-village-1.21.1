package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskState;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.event.VillageDetectionHandler;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/**
 * A colônia ao longo de duzentos ciclos — o E41, P1.13, 2026-09-11.
 *
 * <p><b>A maior lacuna de cobertura do projeto</b>, e está assim escrita
 * no {@code TODO.md}: <i>"nada mede degradação ao longo de muitos ciclos;
 * o teste mais longo do projeto tem centenas de tiques"</i>. É onde moram
 * vazamento de estado, tarefa abandonada, acúmulo de objetivo e perda de
 * referência — defeitos que nenhuma sessão de quarenta minutos encontra
 * porque ninguém fica olhando um número que cresce devagar.
 *
 * <p><b>Duzentos ciclos sem esperar cem minutos.</b> Um ciclo de colônia
 * é {@code VillageDetector.CYCLE_TICKS} = 600 tiques, então duzentos são
 * 120.000 — inviável dentro de um {@code tickLimit}. A costura já existia:
 * {@code VillageDetectionHandler.runCycleNow} roda um ciclo inteiro na
 * hora, na mesma ordem do ciclo longo. Um por tique, e os duzentos cabem
 * em duzentos tiques.
 *
 * <p><b>A fonte de trabalho infinita é o baú vazio</b>, e não uma classe
 * inventada para o teste. O plano de correção avisava que sem ela o caso
 * mediria <i>"nothing to work on"</i> — que é outro item — em vez de
 * degradação. Com baú vazio a colônia pede madeira todo ciclo; o lenhador
 * aceita, não acha árvore nenhuma na arena, devolve a tarefa e leva
 * falta. É o caminho real, e atravessa justamente onde o estado se
 * acumula: planejador, distribuição, despacho das sete profissões,
 * desistência, castigo de ofício, marcas de recusa e registro de
 * ociosidade.
 *
 * <p><b>Falha por tendência, e não por valor absoluto</b>, que é o que o
 * item pede. Nenhuma contagem aqui tem um valor "certo" — o que não pode
 * acontecer é ela <b>crescer</b> com o tempo. Então o caso compara uma
 * janela tardia com uma janela inicial, depois de descontar o aquecimento:
 * uma colônia que acabou de nascer legitimamente abre tarefa, e punir isso
 * seria medir o começo em vez da deriva.
 */
public class ColonyEnduranceGameTest implements FabricGameTest {

    /** Quantos ciclos a colônia atravessa. */
    private static final int CYCLES = 200;

    /**
     * Quantos ciclos iniciais não entram na conta.
     *
     * <p>Colônia nova abre tarefa, reivindica baú e planeja obra, e isso
     * é crescimento legítimo. Medir a partir do primeiro ciclo acusaria
     * deriva no que é só o começo.
     */
    private static final int WARMUP = 50;

    /** O tamanho das duas janelas comparadas. */
    private static final int WINDOW = 50;

    private static final BlockPos CHEST = new BlockPos(1, 2, 1);

    private static final BlockPos STAND = new BlockPos(3, 2, 3);

    /**
     * Fecha as tarefas que a colônia distribuiu neste ciclo.
     *
     * <p>Faz o papel do trabalhador que entrega, e existe para que o
     * ciclo de vida da tarefa <b>gire</b>: nasce, é reservada, fecha, e
     * sai do registro no ciclo seguinte. É esse giro que revela acúmulo —
     * uma tarefa que nunca fecha nunca sobra.
     *
     * <p>Só as que têm executor. Tarefa que ninguém pegou continua na
     * fila, e é assim que a colônia volta a ter o que fazer.
     */
    private static void finishWhatWasHandedOut(UUID colonyId) {
        for (Task task : VillageColonyMod.TASKS.ofColony(colonyId)) {
            if (task.state() == TaskState.RESERVED) {
                task.start();
            }

            if (task.state() == TaskState.EXECUTING) {
                task.complete();
            }
        }
    }

    /** A média de um trecho das amostras. */
    private static double meanOf(int[] samples, int from, int to) {
        long sum = 0;

        for (int i = from; i < to; i++) {
            sum += samples[i];
        }

        return (double) sum / (to - from);
    }

    /**
     * Afirma que uma contagem não derivou para cima.
     *
     * <p>O teto é multiplicativo <b>e</b> aditivo: sem o fator, uma
     * contagem que oscila entre 2 e 4 acusaria deriva; sem a parcela,
     * qualquer coisa que comece em zero acusaria no primeiro item que
     * aparecesse. Um vazamento de verdade cresce com os ciclos e passa
     * dos dois com folga — cem ciclos de diferença entre as janelas.
     */
    private static void assertDidNotDrift(TestContext context, String what, int[] samples) {
        double early = meanOf(samples, WARMUP, WARMUP + WINDOW);
        double late = meanOf(samples, CYCLES - WINDOW, CYCLES);

        context.assertTrue(
                late <= early * 1.5 + 2.0,
                what + " cresceu ao longo dos ciclos: " + early + " na janela inicial, "
                        + late + " na final — é acúmulo, não oscilação");
    }

    /**
     * Duzentos ciclos, e nada da colônia cresce sem parar.
     *
     * <p>Quatro contagens, e cada uma nomeia um defeito que este teste
     * existe para pegar: tarefa que nasce e nunca morre, fila de tarefa
     * aberta que só engorda, obra que se registra de novo a cada
     * passagem, e baú reivindicado duas vezes.
     *
     * <p><b>A montagem tem asserção própria.</b> Uma colônia que não
     * chegou a abrir tarefa nenhuma passaria neste teste com quatro zeros
     * — e não teria medido nada. O caso exige que ela tenha trabalhado.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "colony_endurance",
            tickLimit = 400)
    public void theColonyDoesNotAccumulateAcrossTwoHundredCycles(TestContext context) {
        ServerWorld world = context.getWorld();

        context.setBlockState(new BlockPos(3, 1, 3), Blocks.DIRT.getDefaultState());
        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());

        BlockPos anchor = context.getAbsolutePos(STAND);

        ColonyPos chestPos = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, STAND);
        villager.setBreedingAge(0);

        Colony colony = Colony.create(
                UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(anchor));

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create()
                .owning(colony)
                .owning(villager.getUuid());

        // Lenhador, e o baú fica vazio: a colônia pede madeira todo ciclo
        // e ninguém a entrega. É a fonte de trabalho que não acaba.
        Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
        worker.assign(ProfessionType.LUMBERJACK);

        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chestPos));

        int[] tasks = new int[CYCLES];
        int[] open = new int[CYCLES];
        int[] projects = new int[CYCLES];
        int[] storages = new int[CYCLES];

        // Um ciclo por tique, e não os duzentos de uma vez: cada ciclo
        // carrega a varredura de lote, a leitura de baú e o despacho das
        // sete profissões, e enfiá-los num tique só faria o teste medir a
        // paciência do servidor.
        for (int cycle = 0; cycle < CYCLES; cycle++) {
            int at = cycle;

            context.runAtTick(cycle, () -> {
                VillageDetectionHandler.runCycleNow(world, anchor);

                // <b>O trabalhador entrega, e é o teste quem faz isso.</b>
                // Sem esta linha nenhuma tarefa chega a COMPLETED — o
                // lenhador não acha árvore numa arena de oito blocos —, e
                // sem tarefa fechada o `purgeClosed` não tem o que remover.
                // A primeira versão deste caso passava com ele desligado:
                // media um ciclo em que nada nascia nem morria, que é o
                // contrário de medir acúmulo.
                finishWhatWasHandedOut(colony.id());

                tasks[at] = VillageColonyMod.TASKS.ofColony(colony.id()).size();
                open[at] = VillageColonyMod.TASKS.availableFor(colony.id()).size();
                projects[at] = VillageColonyMod.CONSTRUCTIONS.ofColony(colony.id()).size();
                storages[at] = VillageColonyMod.STORAGES.count();
            });
        }

        context.runAtTick(CYCLES + 5, () -> {
            try {
                // Montagem: sem trabalho aberto em ciclo nenhum, as quatro
                // contagens seriam zero e o teste passaria sem medir nada.
                int worked = 0;

                for (int cycle = WARMUP; cycle < CYCLES; cycle++) {
                    worked += tasks[cycle];
                }

                context.assertTrue(
                        worked > 0,
                        "a colônia não abriu tarefa em duzentos ciclos — o caso não chegou"
                                + " a medir degradação, e sim ociosidade");

                assertDidNotAccumulate(context, tasks, open, projects, storages);
            } finally {
                owned.cleanUp();
            }

            context.complete();
        });
    }

    /** As quatro contagens, cada uma com o defeito que ela nomeia. */
    private static void assertDidNotAccumulate(
            TestContext context, int[] tasks, int[] open, int[] projects, int[] storages) {

        // Tarefa que nasce e nunca morre: o purgeClosed existia desde a
        // Fase 7 e passou tempo sem ser chamado — era metade do E1.
        assertDidNotDrift(context, "o total de tarefas da colônia", tasks);

        // Fila que só engorda: pedido reaberto a cada ciclo por cima do
        // que já estava aberto.
        assertDidNotDrift(context, "a fila de tarefas abertas", open);

        // Canteiro é objeto em memória, e sem quem o remova o registro só
        // cresce — a razão de o purgeFinished existir.
        assertDidNotDrift(context, "as obras registradas", projects);

        // Baú reivindicado de novo a cada passagem, ou registro de
        // trabalhador morto que não sai.
        assertDidNotDrift(context, "os baús registrados", storages);
    }
}
