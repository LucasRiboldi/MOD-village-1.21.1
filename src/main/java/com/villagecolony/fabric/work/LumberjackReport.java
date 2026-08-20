package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.coordination.IdleReason;
import com.villagecolony.fabric.brain.WorkHours;
import com.villagecolony.core.coordination.WorkAssignment;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskType;
import net.minecraft.server.world.ServerWorld;

import java.util.Map;
import java.util.UUID;

/**
 * A linha que o lenhador deixa no log.
 *
 * <p>Saiu de {@code LumberjackWork} em 2026-08-20, quando ele passou de
 * mil e duzentas linhas. Contar o que está acontecendo é um assunto
 * próprio, e é um assunto que este projeto leva a sério: o §11 do
 * Project-State existe porque uma fase inteira ficou muda por quatro
 * sessões, e nenhuma delas pôde ser diagnosticada depois.
 *
 * <p>Duas linhas, e a diferença entre elas é o §11 em miniatura. Quando
 * há trabalho, diz-se <b>o que cada um está fazendo</b>; quando não há,
 * diz-se <b>por quê</b> — e uma vez por motivo, não uma por ciclo, que
 * é o {@link IdleLog}.
 */
public final class LumberjackReport {

    private LumberjackReport() {
    }

    /**
     * Diz por que nenhum lenhador desta colônia está trabalhando.
     *
     * <p>Até 2026-08-15 este caminho era mudo. {@link #report} só fala de
     * lenhador <b>com</b> trabalho aberto, então uma colônia com dois
     * lenhadores e nenhuma tarefa passava a sessão inteira sem uma linha
     * — e do lado de fora isso é idêntico a uma colônia que não tem
     * lenhador nenhum, ou a uma cujo código não está rodando.
     *
     * <p>É a lição do E14 aplicada onde ela ainda não estava: a fase de
     * construção aprendeu a dizer por que não construía, e as outras
     * duas continuaram caladas.
     *
     * <p>Três respostas, e a diferença entre elas manda em coisas
     * diferentes: sem trabalhador é a atribuição de profissão; sem
     * tarefa é a meta da colônia; e tarefa sem executor é o casamento
     * entre as duas.
     */
    static void reportIdle(Colony colony) {
        int hands = WorkAssignment.countCapableOf(
                colony.id(), TaskType.COLLECT_WOOD.required(), VillageColonyMod.WORKERS);

        if (hands == 0) {
            IdleLog.record(colony.id(), LumberjackWork.SUBJECT, IdleReason.NO_WORKER);

            return;
        }

        boolean anyTask = false;

        for (Task task : VillageColonyMod.TASKS.ofColony(colony.id())) {
            if (LumberjackWork.isWoodTask(task) && LumberjackWork.isOngoing(task)) {
                anyTask = true;

                break;
            }
        }

        IdleLog.record(
                colony.id(),
                LumberjackWork.SUBJECT,
                anyTask ? IdleReason.NO_EXECUTOR : IdleReason.NO_TASK,
                hands + " able to");
    }

    /**
     * Uma linha por ciclo dizendo o que cada lenhador está fazendo.
     *
     * <p>Existe porque a Regra 2 tirou o trabalho do ciclo de 600 ticks e
     * levou a instrumentação junto: em 2026-08-12 um servidor rodou onze
     * ciclos com seis tarefas atribuídas e nenhuma árvore derrubada, e o
     * log não sabia dizer se o aldeão estava andando, sem baú, sem árvore
     * ou dormindo. Quatro causas com quatro correções diferentes.
     *
     * <p>Uma linha a cada trinta segundos não é spam, e é a única forma
     * de saber o que acontece com um aldeão que ninguém está olhando —
     * a mesma razão da linha que existia antes. O que não pode voltar é
     * falar a cada tick.
     *
     * <p>Silenciosa quando não há lenhador com trabalho: colônia sem
     * tarefa de madeira não precisa dizer nada.
     */
    static void report(ServerWorld world, Colony colony) {
        StringBuilder line = new StringBuilder();
        int reported = 0;

        for (Map.Entry<UUID, LumberjackWork.Job> entry : LumberjackWork.JOBS.entrySet()) {
            LumberjackWork.Job job = entry.getValue();

            if (!job.task.belongsTo(colony.id())) {
                continue;
            }

            if (reported++ > 0) {
                line.append("; ");
            }

            line.append(shortId(entry.getKey()))
                    .append(" ")
                    .append(describe(world, entry.getKey(), job));
        }

        if (reported == 0) {
            return;
        }

        VillageColonyMod.LOGGER.info(
                "Colony {} lumberjacks: {}", colony.id(), line);
    }

    /**
     * O que este lenhador está fazendo, em poucas palavras.
     *
     * <p>Diz distância e horário de trabalho porque um aldeão parado ao
     * lado de uma árvore e um aldeão que nunca vai chegar são a mesma
     * linha sem eles. Foi o que a leitura de 2026-08-12 mostrou: um
     * lenhador travado em "bloco 1 de 60, 0/0 ticks" ciclo após ciclo, e
     * nenhuma forma de saber se ele estava longe, dormindo ou fora de
     * chunk carregado — três causas com três correções diferentes.
     *
     * <p>E diz o relógio de travamento, desde 2026-08-15. Naquela sessão
     * dois lenhadores ficaram dezesseis minutos a sete e nove blocos da
     * árvore sem chegar, e {@link #giveUp} — que deveria ter soltado a
     * tarefa em dois minutos de horário de trabalho — não falou uma vez
     * sequer. Três explicações cabiam no que o log mostrava: o contador
     * sobe e o limite está alto demais; o contador não sobe porque
     * {@code step} não chega à linha 534; ou alguém o zera a cada ciclo.
     * As três pedem correções diferentes e o número as separa numa
     * olhada — medir custa uma palavra na linha.
     */
    private static String describe(ServerWorld world, UUID workerId, LumberjackWork.Job job) {
        if (!(world.getEntity(workerId) instanceof net.minecraft.entity.passive.VillagerEntity villager)) {
            return "not loaded (" + job.collected + " logs so far, stall " + job.stalled + ")";
        }

        String clock = WorkHours.isWorkTime(world, villager) ? "work time" : "off hours";

        if (job.isBetweenTrees()) {
            return "looking for a tree, " + clock
                    + " (" + job.collected + " logs so far, stall "
                    + job.stalled + "/" + TreeChoice.stallLimit + ")";
        }

        int distance = (int) Math.sqrt(
                villager.getBlockPos().getSquaredDistance(job.plan.base()));

        return (distance <= LumberjackWork.REACH ? "chopping" : "walking")
                + " — tree at " + job.plan.base().toShortString()
                + ", " + distance + " blocks away, " + clock
                + ", block " + (job.index + 1) + " of " + job.plan.blocks().size()
                + ", " + job.progress + "/" + job.required + " ticks"
                + ", " + job.collected + " logs so far"
                + ", stall " + job.stalled + "/" + TreeChoice.stallLimit;
    }

    /** Os oito primeiros dígitos do UUID, como no resto do log. */
    static String shortId(UUID id) {
        return id.toString().substring(0, 8);
    }
}
