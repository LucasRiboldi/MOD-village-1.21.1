package com.villagecolony.fabric.work;

import com.villagecolony.core.worker.model.ProfessionType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Só o mineiro remodela o terreno para escalar — regra do autor, 2026-10-15. As outras profissões
 * sobem apenas por degrau natural (sem pôr bloco nem cavar); quando não há, procuram num raio maior
 * e, se nada, desistem. O mineiro mantém pilar e túnel ({@link ClimbOut.Mode}).
 *
 * <p>Lógica pura — no padrão de {@link MinerRerouteTest}.
 */
class ClimbReshapeTest {

    @Test
    void onlyTheMinerMayReshapeTheTerrain() {
        assertTrue(ClimbPolicy.mayReshapeTerrain(ProfessionType.MINER),
                "o mineiro pode pôr bloco e cavar para escalar");
    }

    @Test
    void otherProfessionsClimbOnlyNaturalSteps() {
        for (ProfessionType profession : ProfessionType.values()) {
            if (profession == ProfessionType.MINER) {
                continue;
            }

            assertFalse(ClimbPolicy.mayReshapeTerrain(profession),
                    profession + " não pode remodelar o terreno para escalar");
        }
    }

    @Test
    void anUnknownProfessionDoesNotReshape() {
        assertFalse(ClimbPolicy.mayReshapeTerrain(null),
                "sem profissão conhecida, não remodela o terreno");
    }
}
