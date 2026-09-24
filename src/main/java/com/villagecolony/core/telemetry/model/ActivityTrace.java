package com.villagecolony.core.telemetry.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * O traço circular de atividade de uma colônia — decisão 7B, 2026-09-24.
 *
 * <p><b>Um por colônia, e limitado.</b> A colônia mais antiga tem meses
 * de sessão; sem teto, um evento por tique simulado acumularia sem fim.
 * Dezesseis mil trezentos e oitenta e quatro é o teto que a Task 7
 * definiu — grande o bastante para um diagnóstico de endurance olhar
 * bem para trás, pequeno o bastante para caber inteiro na memória sem
 * gravação em disco por tique.
 *
 * <p><b>Mutável de propósito</b>, como {@code Colony} e {@code Mine}: um
 * traço que se reconstruísse inteiro a cada evento pagaria o buffer
 * inteiro por tique, que é exatamente o custo que a Task 7 existe para
 * evitar.
 */
public final class ActivityTrace {

    /** Dezesseis mil trezentos e oitenta e quatro, decidido pela Task 7. */
    public static final int CAPACITY = 16_384;

    private final Deque<ActivityTraceEvent> events = new ArrayDeque<>(CAPACITY);

    private long overflowCount;

    /**
     * Acrescenta um evento, expulsando o mais antigo quando o buffer
     * está cheio.
     */
    public void append(ActivityTraceEvent event) {
        Objects.requireNonNull(event, "event");

        if (events.size() == CAPACITY) {
            events.removeFirst();
            overflowCount++;
        }

        events.addLast(event);
    }

    /**
     * Os {@code count} eventos mais recentes, do mais novo ao mais
     * antigo.
     *
     * <p>Nunca devolve mais do que {@code count}, e nunca mais do que o
     * traço realmente guarda.
     */
    public List<ActivityTraceEvent> newestFirst(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count must not be negative");
        }

        List<ActivityTraceEvent> newestFirst = new ArrayList<>(Math.min(count, events.size()));

        Iterator<ActivityTraceEvent> fromNewest = events.descendingIterator();

        while (fromNewest.hasNext() && newestFirst.size() < count) {
            newestFirst.add(fromNewest.next());
        }

        return Collections.unmodifiableList(newestFirst);
    }

    /**
     * Quantos eventos foram expulsos por falta de espaço.
     *
     * <p>Cresce sem teto — não é limitado pela {@link #CAPACITY} do
     * buffer, porque é a contagem de quem <b>saiu</b> dele, não de quem
     * está dentro. Uma sessão de meses com milhões de tiques deve
     * mostrar esse número inteiro, e não um valor truncado que minta
     * sobre quanto histórico já se perdeu.
     */
    public long overflowCount() {
        return overflowCount;
    }

    /** Quantos eventos o traço guarda agora — nunca mais que {@link #CAPACITY}. */
    public int size() {
        return events.size();
    }

    /**
     * Aplica o total de estouro que o disco já contava, sem reprocessar
     * um evento por vez.
     *
     * <p>Chamado só por {@code ActivityTraceSave.read}, depois de todo
     * evento salvo já ter entrado por {@link #append}. Uma colônia de
     * meses pode ter estourado milhões de vezes; incrementar um a um
     * pagaria esse número inteiro toda vez que o mundo carrega.
     */
    public void restoreOverflow(long total) {
        if (total < 0) {
            throw new IllegalArgumentException("overflow total must not be negative");
        }

        this.overflowCount = total;
    }
}
