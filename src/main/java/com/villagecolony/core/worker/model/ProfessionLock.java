package com.villagecolony.core.worker.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * A troca de ofício achada entre duas leituras — Regra 383 (autor 26-11, K3 do jogo da 0.4.148: "eu vi o personagem
 * zelador deixar de ser o zelador e sumir a placa de identificação"). Quem tinha ofício e agora tem outro, ou nenhum, ou
 * sumiu do registro, entra na lista; quem ganhou o primeiro ofício não.
 */
public final class ProfessionLock {

    private ProfessionLock() {
    }

    /** Aldeão → o ofício de agora (vazio: sem ofício ou fora do registro), para cada um que perdeu ou trocou o seu. */
    public static Map<UUID, Optional<ProfessionType>> changes(Map<UUID, ProfessionType> before,
                                                              Map<UUID, ProfessionType> after) {
        Map<UUID, Optional<ProfessionType>> changed = new HashMap<>();

        before.forEach((villager, trade) -> {
            ProfessionType now = after.get(villager);

            if (now != trade) {
                changed.put(villager, Optional.ofNullable(now));
            }
        });

        return changed;
    }
}
