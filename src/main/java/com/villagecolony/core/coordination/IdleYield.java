package com.villagecolony.core.coordination;

import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.worker.model.ProfessionType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Ofício parado cede vaga — ADR-038 P1 e a regra do ócio (ADR-041 #3): mais de
 * {@link #IDLE_SHARE}% de ocioso + bloqueado + encalhado
 * em {@link #WINDOWS} janelas seguidas do {@code WorkTime}, com pelo menos
 * {@link #MIN_WORKERS} pessoas, e o ofício solta uma. Quem sai fica sem
 * ofício e a contratação por demanda o leva aonde a obra espera.
 *
 * <p><b>Vaga por demanda</b> (P5c, autossuficiência, 2026-10-11): o titular
 * único também cede quando outro ofício tem falta para a obra e o dele não. O
 * construtor fica de fora: sem ele a obra para.
 */
public final class IdleYield {

    static {
        ServerMemory.register(IdleYield.class, IdleYield::clearAll);
    }

    public static final int IDLE_SHARE = 15;

    public static final int WINDOWS = 2;

    /** Com menos que isto o ofício nunca cede: ele precisa de alguém. */
    public static final int MIN_WORKERS = 2;

    private record Key(UUID colony, ProfessionType profession) {
    }

    private static final Map<Key, Integer> STREAK = new HashMap<>();

    private IdleYield() {
    }

    public static void clearAll() {
        STREAK.clear();
    }

    /**
     * Uma janela fechada deste ofício.
     *
     * @param idleShare ocioso + bloqueado + encalhado em %, de 0 a 100
     * @param workers quantos aldeões têm o ofício
     * @return se o ofício deve ceder uma pessoa agora
     */
    public static boolean observe(UUID colonyId, ProfessionType profession, int idleShare, int workers) {
        return observe(colonyId, profession, idleShare, workers, java.util.List.of());
    }

    /**
     * O mesmo, sabendo quais ofícios têm falta para a obra ({@code ProfessionDemand}).
     *
     * @param shortTrades os ofícios com falta para a obra neste ciclo
     */
    public static boolean observe(UUID colonyId, ProfessionType profession, int idleShare, int workers,
            java.util.List<ProfessionType> shortTrades) {

        Key key = new Key(colonyId, profession);
        boolean lastOneMayGo = workers == 1
                && profession != ProfessionType.BUILDER
                && !shortTrades.contains(profession)
                && shortTrades.stream().anyMatch(other -> other != profession);

        if (idleShare <= IDLE_SHARE || (workers < MIN_WORKERS && !lastOneMayGo)) {
            STREAK.remove(key);
            return false;
        }

        int streak = STREAK.merge(key, 1, Integer::sum);

        if (streak < WINDOWS) {
            return false;
        }

        STREAK.remove(key);
        return true;
    }
}
