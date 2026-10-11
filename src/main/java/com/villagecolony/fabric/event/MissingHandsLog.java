package com.villagecolony.fabric.event;

import com.villagecolony.core.coordination.IdleReason;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.fabric.work.IdleLog;
import java.util.Locale;
import java.util.UUID;

/** A linha que diz quando a colônia não tem quem faça um material — ver {@link #report}. */
final class MissingHandsLog {

    private MissingHandsLog() {
    }

    /**
     * Diz que a colônia não tem quem faça um material — 2026-09-09.
     *
     * <p><b>O silêncio que ela quebra.</b> {@code ColonyCycle} pula o
     * pedido de material que ninguém sabe fazer, e pular está certo:
     * tarefa sem executor possível fica na fila para sempre. O que estava
     * errado é que ele pulava <b>sem uma linha</b>, e o que o autor via
     * era {@code assigned 0 tasks (0 open)} sem causa — o mesmo sintoma
     * da roça que travou a vila nesta mesma data, e que custou uma hora
     * de sessão até ser diagnosticado.
     *
     * <p><b>Assunto por tarefa, e não por material.</b> Uma colônia sem
     * fundidor não sabe fazer vidro <b>nem</b> lingote <b>nem</b> arenito
     * liso: três linhas iguais diriam a mesma coisa três vezes. O
     * {@code IdleLog} compara só o motivo, então a primeira fala e as
     * outras calam sozinhas — e o detalhe, que fica fora da comparação
     * de propósito, nomeia o material que chegou primeiro.
     *
     * <p><b>O {@code clear} é metade da correção.</b> Sem ele, uma
     * colônia que perde o fundidor, contrata outro e o perde de novo
     * ficaria muda na segunda vez: o motivo guardado ainda seria
     * {@code NO_WORKER}, e o registrador trataria como repetição de um
     * silêncio que já tinha acabado. É o caso que o javadoc de
     * {@code IdleLog.clear} descreve.
     */
    static void report(
            UUID colonyId, ResourceType resource, TaskType type, int hands) {

        String subject = type.name().toLowerCase(Locale.ROOT);
        if (hands == 0) {
            IdleLog.record(
                    colonyId,
                    subject,
                    IdleReason.NO_WORKER,
                    resource + " needs " + type.required());

            return;
        }
        IdleLog.clear(colonyId, subject);
    }
}
