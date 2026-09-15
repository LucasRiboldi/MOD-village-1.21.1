package com.villagecolony.fabric.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A vez de planejar, repartida entre as colônias — 2026-09-15.
 *
 * <p><b>O que o log do autor mediu:</b> ciclos de 98, 57 e 54 ms contra o
 * orçamento de 50, com o planejador levando 72 ms do pior deles e
 * <b>29 colônias</b> planejando todas no mesmo tique. Nada limitava
 * quantas decidiam obra por ciclo: o laço percorria a lista inteira, e o
 * custo do planejador era a soma das vinte e nove.
 *
 * <p><b>O que estes casos afirmam</b> é o rodízio, e não o relógio. Medir
 * milissegundos aqui mediria a máquina — a lição que o
 * {@link CycleCostTest} já registrou. O que dá para afirmar é que a fila
 * anda, que ninguém fura e que ninguém fica para trás.
 */
class PlannerTurnsTest {

    @BeforeEach
    void clear() {
        PlannerTurns.clearAll();
    }

    private static List<UUID> colonies(int howMany) {
        List<UUID> all = new ArrayList<>(howMany);

        for (int i = 0; i < howMany; i++) {
            all.add(UUID.randomUUID());
        }

        return all;
    }

    /**
     * Vila pequena não paga nada.
     *
     * <p>É a metade que protege quem não tem o problema: com menos
     * colônias do que a cota, todas planejam todo ciclo, exatamente como
     * antes de 2026-09-15. A degradação é proporcional ao mundo que a
     * causa.
     */
    @Test
    void everyColonyPlansWhenThereAreFewerThanTheQuota() {
        List<UUID> few = colonies(3);

        assertEquals(
                Set.copyOf(few),
                PlannerTurns.chooseFrom(few),
                "com três colônias e cota maior que isso, todas tinham de planejar");
    }

    /** Acima da cota, só ela passa por ciclo. */
    @Test
    void aCrowdedWorldOnlyLetsTheQuotaThrough() {
        List<UUID> many = colonies(29);

        Set<UUID> chosen = PlannerTurns.chooseFrom(many);

        assertEquals(
                PlannerTurns.PER_CYCLE,
                chosen.size(),
                "o ciclo deixou passar mais colônias do que a cota");
    }

    /**
     * <b>A fila anda, e ninguém fica para trás.</b>
     *
     * <p>É a afirmação que importa: uma cota que escolhesse sempre as
     * primeiras da lista deixaria as últimas sem planejar para sempre, e
     * a colônia do fim do mundo nunca levantaria casa. Em
     * {@code ceil(29/8)} ciclos toda colônia teve a sua vez, e exatamente
     * uma.
     */
    @Test
    void everyColonyGetsItsTurnWithinOneFullRound() {
        List<UUID> many = colonies(29);

        int cycles = (many.size() + PlannerTurns.PER_CYCLE - 1) / PlannerTurns.PER_CYCLE;

        List<UUID> served = new ArrayList<>();

        for (int cycle = 0; cycle < cycles; cycle++) {
            served.addAll(PlannerTurns.chooseFrom(many));
        }

        assertEquals(
                Set.copyOf(many),
                Set.copyOf(served),
                "alguma colônia passou uma volta inteira sem planejar");

        assertEquals(
                many.size(),
                new HashSet<>(served).size(),
                "alguma colônia planejou duas vezes antes de a volta fechar");
    }

    /**
     * A colônia que sai do mundo não segura a vez das outras.
     *
     * <p>Colônia é abandonada, descarregada e criada o tempo todo — o E9
     * é literalmente a marca de abandono oscilando. Um cursor preso a uma
     * lista que encolheu pularia colônias vivas em silêncio, e este caso
     * é o que impede.
     */
    @Test
    void theRotationSurvivesColoniesComingAndGoing() {
        List<UUID> many = colonies(20);

        PlannerTurns.chooseFrom(many);

        List<UUID> fewer = new ArrayList<>(many.subList(0, 9));

        Set<UUID> chosen = PlannerTurns.chooseFrom(fewer);

        assertFalse(chosen.isEmpty(), "a lista encolheu e o rodízio parou de servir alguém");

        assertTrue(
                fewer.containsAll(chosen),
                "o rodízio serviu uma colônia que já não está no mundo");
    }

    /** Mundo vazio não quebra e não guarda lixo. */
    @Test
    void anEmptyWorldChoosesNobody() {
        assertTrue(
                PlannerTurns.chooseFrom(List.of()).isEmpty(),
                "sem colônia nenhuma, alguém foi escolhido");
    }
}
