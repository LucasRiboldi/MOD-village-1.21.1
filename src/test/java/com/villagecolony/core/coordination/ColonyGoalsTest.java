package com.villagecolony.core.coordination;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.resource.model.ResourceTally;
import com.villagecolony.core.resource.service.ResourceDemand;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceType;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A Regra 1 de 2026-08-08: colher até os baús encherem.
 *
 * <p>O que estes testes guardam é a equivalência que a regra criou —
 * déficit e espaço livre passaram a ser o mesmo número. Enquanto isso
 * valer, a colônia para de pedir sozinha quando não há mais onde
 * guardar.
 */
class ColonyGoalsTest {

    private static Colony colony() {
        return Colony.create(UUID.randomUUID(), new ColonyPos(0, 64, 0));
    }

    private static ResourceTally owned(ResourceType type, int amount) {
        Map<ResourceType, Integer> counts = new EnumMap<>(ResourceType.class);
        counts.put(type, amount);

        return ResourceTally.of(counts);
    }

    /** A meta é o que se tem mais o que ainda cabe. */
    @Test
    void theGoalIsWhatIsStoredPlusWhatFits() {
        ResourceTally stock = owned(ResourceType.OAK_LOG, 20);

        Map<ResourceType, Integer> goal = ColonyGoals.of(colony(), stock, 100);

        assertEquals(120, goal.get(ResourceType.OAK_LOG));
    }

    /**
     * O déficit que sai da meta é exatamente o espaço livre.
     *
     * <p>É a regra inteira em uma linha: a colônia pede o que cabe, nem
     * mais nem menos.
     */
    @Test
    void theDeficitIsTheFreeSpace() {
        ResourceTally stock = owned(ResourceType.OAK_LOG, 20);

        Map<ResourceType, Integer> missing = ResourceDemand.deficit(
                ColonyGoals.of(colony(), stock, 100), stock);

        assertEquals(100, missing.get(ResourceType.OAK_LOG));
    }

    /**
     * Baú cheio não pede nada.
     *
     * <p>É o fim do E1: a meta constante fazia a colônia pedir a cada
     * ciclo enquanto o lenhador não a alcançasse, e a fila crescia mais
     * rápido do que esvaziava.
     */
    @Test
    void aFullChestAsksForNothing() {
        ResourceTally stock = owned(ResourceType.OAK_LOG, 64);

        Map<ResourceType, Integer> goal = ColonyGoals.of(colony(), stock, 0);

        assertTrue(ResourceDemand.isSatisfied(goal, stock));
        assertTrue(ResourceDemand.deficit(goal, stock).isEmpty());
    }

    /**
     * Madeira é madeira: o que já está guardado conta pelo grupo.
     *
     * <p>Uma colônia com o baú cheio de abeto e espaço para mais dez não
     * pede sessenta e quatro de carvalho — pede dez.
     */
    @Test
    void anyWoodCountsTowardsTheGoal() {
        ResourceTally spruce = owned(ResourceType.SPRUCE_LOG, 54);

        Map<ResourceType, Integer> missing = ResourceDemand.deficit(
                ColonyGoals.of(colony(), spruce, 10), spruce);

        assertEquals(10, missing.get(ResourceType.OAK_LOG));
    }

    /**
     * A pedra saiu da meta.
     *
     * <p>Ninguém minera no MVP, e {@code ColonyCycle.typeFor} manda todo
     * recurso NATURAL para coleta: a meta de pedra virava tarefa que só o
     * lenhador podia pegar, e ele derrubava árvore para atendê-la.
     */
    @Test
    void thereIsNoStoneGoalWhileNobodyMines() {
        Map<ResourceType, Integer> goal = ColonyGoals.of(
                colony(), ResourceTally.empty(), 64);

        assertFalse(goal.containsKey(ResourceType.COBBLESTONE));
    }

    /** Espaço negativo é erro de quem mediu, e não meta a menos. */
    @Test
    void negativeRoomIsRefused() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ColonyGoals.of(colony(), ResourceTally.empty(), -1));
    }
}
