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

    /**
     * Quando este assunto falou pela última vez — 2026-09-11.
     *
     * <p><b>Porque "transição" não basta quando o motivo oscila.</b> A
     * regra de cima — fala quando muda, cala quando não muda — supõe que
     * quem pergunta é o ciclo da colônia, uma vez a cada trinta
     * segundos. Nem todo mundo pergunta assim: a busca de areia roda no
     * laço de trabalho do mineiro, <b>por tique</b>, e o motivo dela
     * alterna por construção — toda volta da varredura em anéis termina
     * em {@code NO_TARGET} e a seguinte recomeça em
     * {@code SWEEP_INCOMPLETE}.
     *
     * <p>Duas transições por volta, e uma volta a cada dez tiques, dá
     * <b>quatro linhas por segundo</b>. A sessão de 2026-09-11 às 02:03
     * mediu: <b>4.389 linhas de areia num log de 6.117</b> — setenta e
     * dois por cento da sessão, e a próxima fica cega para qualquer
     * outro diagnóstico.
     *
     * <p>E o comentário que pedia a defesa já estava escrito no
     * {@code SandGathering}, de quem o previu e confiou na regra de
     * transição: <i>"dizer 'não achei' em cada uma daria duas linhas por
     * segundo numa vila sem praia"</i>.
     */
    private static final Map<Key, Long> SPOKE_AT = new HashMap<>();

    /**
     * Quanto tempo um assunto fica calado depois de falar.
     *
     * <p>Um ciclo de colônia, que é a unidade de decisão do mod: entre
     * dois ciclos nada muda de verdade, então nada foi perdido. E o
     * amortecedor <b>não apaga a distinção</b> que o E14 introduziu
     * entre <i>"ainda varrendo"</i> e <i>"varri tudo"</i> — ela continua
     * inteira, só deixa de ser dita quatro vezes por segundo.
     */
    private static final int QUIET_TICKS = 600;

    private IdleLog() {
    }

    /** Colônia e assunto — "lumberjack", "carpenter", "mason", "building". */
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
    public static boolean record(
            UUID colonyId, String subject, IdleReason reason, String detail) {

        Objects.requireNonNull(colonyId, "colonyId");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(detail, "detail");

        if (reason == LAST.put(new Key(colonyId, subject), reason)) {
            return false;
        }

        VillageColonyMod.LOGGER.info(
                "Colony {} — no {} work: {}",
                colonyId,
                subject,
                reason.messageWith(detail));

        return true;
    }

    /**
     * O mesmo, para quem pergunta <b>por tique</b> — 2026-09-11.
     *
     * <p>Acrescenta o amortecedor do {@link #QUIET_TICKS} à regra de
     * transição: além de o motivo ter de mudar, o assunto precisa estar
     * calado há um ciclo. Sem isso, motivo que oscila vira enxurrada —
     * ver {@link #SPOKE_AT}.
     *
     * <p>Quem roda uma vez por ciclo continua no {@code record} sem
     * relógio, e de propósito: lá a transição já é rara por construção, e
     * o amortecedor só poderia atrasar uma notícia legítima.
     *
     * @param now o tique do mundo, que este tipo não tem como obter
     *     sozinho — o Core não conhece {@code ServerWorld} (ADR-005)
     */
    public static void recordAt(
            UUID colonyId, String subject, IdleReason reason, String detail, long now) {

        Objects.requireNonNull(colonyId, "colonyId");
        Objects.requireNonNull(subject, "subject");

        Key key = new Key(colonyId, subject);

        Long spoke = SPOKE_AT.get(key);

        if (spoke != null && now - spoke < QUIET_TICKS) {
            // Calado há menos de um ciclo. O motivo fica guardado do
            // mesmo jeito, senão a fala seguinte acharia que nada mudou.
            LAST.put(key, reason);

            return;
        }

        if (record(colonyId, subject, reason, detail)) {
            // Só quando falou de verdade: marcar sempre faria o
            // amortecedor calar a fala seguinte por um ciclo mesmo
            // quando esta não saiu.
            SPOKE_AT.put(key, now);
        }
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
        SPOKE_AT.clear();
    }
}
