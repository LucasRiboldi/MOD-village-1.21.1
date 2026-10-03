package com.villagecolony.fabric.event;

import com.villagecolony.core.colony.service.VillageDetector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * O que a entrega entre ciclos já deu a quem — E49, 2026-09-30.
 *
 * <p><b>O defeito.</b> Quem solta a tarefa sem fazê-la (o fundidor sem
 * areia, o artesão sem ingrediente) fica ocioso com a tarefa aberta. A
 * entrega de {@link IdleHands}, uma vez por segundo, dava a mesma tarefa ao
 * mesmo aldeão, e ele a soltava de novo: no playtest de 30-09 foram 6.324
 * paradas de seis fundidores em 18 minutos. Antes do F10 essa volta era a do
 * ciclo, a cada 30 s — e é essa cadência que este registro devolve.
 *
 * <p><b>Só o par se lembra.</b> A mesma tarefa vai a outro aldeão livre na
 * hora, que é para isso que o F10 existe; e a fila só oferece a tarefa da
 * frente de cada capacidade, de modo que quem tem a dele bloqueada espera o
 * ciclo, como antes do F10. Não é descanso nem castigo — não passa pelo
 * {@code Worker.rest}, que conta desistência e tira o ofício.
 */
final class IdleHandouts {

    /** Quanto o par espera: um ciclo da colônia. */
    static final long WINDOW_TICKS = VillageDetector.CYCLE_TICKS;

    private record Handout(UUID taskId, long tick) {
    }

    /** Uma entrada por trabalhador: só a última entrega importa. */
    private final Map<UUID, Handout> last = new HashMap<>();

    /** Se a entrega entre ciclos pode dar esta tarefa a este trabalhador agora. */
    boolean mayHand(UUID workerId, UUID taskId, long now) {
        Handout handout = last.get(workerId);

        return handout == null
                || !handout.taskId().equals(taskId)
                || !isRecent(handout, now);
    }

    /** A entrega entre ciclos deu esta tarefa a este trabalhador. */
    void handed(UUID workerId, UUID taskId, long now) {
        last.put(workerId, new Handout(taskId, now));
    }

    /** Esquece as entregas de mais de um ciclo: o registro fica do tamanho da vila. */
    void forgetExpired(long now) {
        last.values().removeIf(handout -> !isRecent(handout, now));
    }

    int size() {
        return last.size();
    }

    void clear() {
        last.clear();
    }

    // Relógio que voltou (/time set) não prende o par: conta como antiga.
    private static boolean isRecent(Handout handout, long now) {
        long elapsed = now - handout.tick();

        return elapsed >= 0 && elapsed < WINDOW_TICKS;
    }
}
