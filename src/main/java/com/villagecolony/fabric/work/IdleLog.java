package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.coordination.IdleReason;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Diz por que uma profissão não trabalhou, e não repete enquanto o
 * motivo for o mesmo.
 *
 * <p>O ciclo da colônia roda a cada trinta segundos. Uma linha por
 * ciclo por profissão é spam, e spam esconde: o log de uma sessão de
 * vinte minutos teria oitenta linhas iguais, e a que importa — a que
 * muda — passaria despercebida no meio delas.
 *
 * <p>A regra é a que {@code ConstructionPlanner} já usava desde
 * 2026-08-14 e que este tipo generaliza para as três profissões: fala
 * na primeira vez, cala enquanto o motivo não mudar, e volta a falar
 * quando mudar. É o mesmo que dizer que o log registra **transições**,
 * não estados.
 *
 * <p>A chave inclui o assunto, e não só a colônia: um lenhador parado
 * por falta de tarefa e um construtor parado por falta de lote são dois
 * silêncios diferentes, e um não pode calar o outro.
 *
 * <p>Estado em memória, esquecido ao parar o servidor — ver
 * {@link #clearAll}. Perder isso custa uma linha repetida por colônia
 * na primeira volta depois de carregar, e é o que se quer: a sessão
 * nova deve dizer onde cada colônia está.
 */
public final class IdleLog {

    /** O último motivo registrado, por colônia e assunto. */
    private static final Map<Key, IdleReason> LAST = new HashMap<>();

    private IdleLog() {
    }

    /** Colônia e assunto — "lumberjack", "manufacturer", "building". */
    private record Key(UUID colonyId, String subject) {
    }

    /**
     * Registra que nada aconteceu, e por quê.
     *
     * <p>Silencioso quando o motivo é o mesmo da última vez.
     *
     * @param subject a profissão ou a fase que ficou parada, como
     *     aparece na linha: {@code "lumberjacks"}, {@code "building"}
     * @param detail o que muda de um ciclo para o outro — coordenada,
     *     contagem, nome de bloco. Fica **fora** da comparação de
     *     propósito: um detalhe diferente a cada ciclo faria a linha
     *     voltar toda vez, que é exatamente o que este tipo evita
     */
    public static void record(
            UUID colonyId, String subject, IdleReason reason, String detail) {

        Objects.requireNonNull(colonyId, "colonyId");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(detail, "detail");

        if (reason == LAST.put(new Key(colonyId, subject), reason)) {
            return;
        }

        VillageColonyMod.LOGGER.info(
                "Colony {} — no {} work: {}",
                colonyId,
                subject,
                reason.messageWith(detail));
    }

    /** Sem detalhe do momento. */
    public static void record(UUID colonyId, String subject, IdleReason reason) {
        record(colonyId, subject, reason, "");
    }

    /**
     * Esquece o motivo guardado de um assunto.
     *
     * <p>Chamado quando o trabalho volta a acontecer: sem isto, uma
     * colônia que ficou sem lote, construiu, e ficou sem lote de novo
     * não diria a segunda vez — o motivo guardado ainda seria o mesmo, e
     * o registrador o trataria como repetição de um silêncio que já
     * tinha acabado.
     */
    public static void clear(UUID colonyId, String subject) {
        LAST.remove(new Key(colonyId, subject));
    }

    /** Esquece tudo. Chamado ao parar o servidor. */
    public static void clearAll() {
        LAST.clear();
    }
}
