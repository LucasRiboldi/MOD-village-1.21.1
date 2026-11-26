package com.villagecolony.core.worker.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Regra 383 (autor 26-11, K3 do jogo da 0.4.148: "nenhum aldeão de profissão pode deixar de ser ela ou trocar"): o
 * ofício dado não muda nem sai; e a troca, se acontecer, é dita no log.
 */
class ProfessionLockTest {

    @Test
    @DisplayName("Regra 383: quem tem ofício não recebe outro")
    void aWorkerKeepsItsTrade() {
        Worker worker = Worker.register(UUID.randomUUID(), UUID.randomUUID());

        worker.assign(ProfessionType.GROUNDSKEEPER);
        worker.assign(ProfessionType.MASON);

        assertEquals(Optional.of(ProfessionType.GROUNDSKEEPER), worker.profession(),
                "Regra 383: o Zelador deixou de ser Zelador");
    }

    @Test
    @DisplayName("Regra 383: a troca de ofício entre duas leituras é achada")
    void aChangeBetweenTwoReadingsIsFound() {
        UUID kept = UUID.randomUUID();
        UUID changed = UUID.randomUUID();
        UUID gone = UUID.randomUUID();
        Map<UUID, ProfessionType> before = Map.of(kept, ProfessionType.MINER, changed, ProfessionType.GROUNDSKEEPER,
                gone, ProfessionType.FARMER);
        Map<UUID, ProfessionType> after = Map.of(kept, ProfessionType.MINER, changed, ProfessionType.MASON);

        assertEquals(Map.of(changed, Optional.of(ProfessionType.MASON), gone, Optional.empty()),
                ProfessionLock.changes(before, after));
    }
}
