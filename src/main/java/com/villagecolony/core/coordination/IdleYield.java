package com.villagecolony.core.coordination;

import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.worker.model.ProfessionType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Ofício parado cede vaga — ADR-038 P1: mais de {@link #IDLE_SHARE}% de ócio
 * em {@link #WINDOWS} janelas seguidas do {@code WorkTime}, com pelo menos
 * {@link #MIN_WORKERS} pessoas, e o ofício solta uma. Quem sai fica sem
 * ofício e a contratação por demanda o leva aonde a obra espera.
 */
public final class IdleYield {

    static {
        ServerMemory.register(IdleYield.class, IdleYield::clearAll);
    }

    public static final int IDLE_SHARE = 60;

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
     * @param idleShare ócio em %, de 0 a 100
     * @param workers quantos aldeões têm o ofício
     * @return se o ofício deve ceder uma pessoa agora
     */
    public static boolean observe(UUID colonyId, ProfessionType profession, int idleShare, int workers) {
        Key key = new Key(colonyId, profession);

        if (idleShare <= IDLE_SHARE || workers < MIN_WORKERS) {
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
