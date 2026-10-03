package com.villagecolony.core.worker.model;

import com.villagecolony.core.type.Capability;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkerTest {

    private static final UUID VILLAGER = UUID.randomUUID();
    private static final UUID COLONY = UUID.randomUUID();

    @Test
    void registeredWorkerKeepsItsIds() {
        Worker worker = Worker.register(VILLAGER, COLONY);

        assertEquals(VILLAGER, worker.villagerId());
        assertEquals(COLONY, worker.colonyId());
    }

    /** Registrar e dar função são momentos diferentes. */
    @Test
    void registeredWorkerHasNoProfessionYet() {
        Worker worker = Worker.register(VILLAGER, COLONY);

        assertEquals(Optional.empty(), worker.profession());
        assertFalse(worker.hasProfession());
    }

    @Test
    void registerRejectsNulls() {
        assertThrows(NullPointerException.class, () -> Worker.register(null, COLONY));
        assertThrows(NullPointerException.class, () -> Worker.register(VILLAGER, null));
    }

    @Test
    void assignGivesTheWorkerAProfession() {
        Worker worker = Worker.register(VILLAGER, COLONY);

        worker.assign(ProfessionType.LUMBERJACK);

        assertEquals(Optional.of(ProfessionType.LUMBERJACK), worker.profession());
        assertTrue(worker.hasProfession());
    }

    /** A colônia realoca conforme a necessidade muda. */
    @Test
    void assignReplacesThePreviousProfession() {
        Worker worker = Worker.register(VILLAGER, COLONY);

        worker.assign(ProfessionType.FARMER);
        worker.assign(ProfessionType.BUILDER);

        assertEquals(Optional.of(ProfessionType.BUILDER), worker.profession());
    }

    @Test
    void assignRejectsNull() {
        Worker worker = Worker.register(VILLAGER, COLONY);

        assertThrows(NullPointerException.class, () -> worker.assign(null));
    }

    @Test
    void unassignRemovesTheProfession() {
        Worker worker = Worker.register(VILLAGER, COLONY);
        worker.assign(ProfessionType.CARPENTER);

        worker.unassign();

        assertFalse(worker.hasProfession());
        assertEquals(Optional.empty(), worker.profession());
    }

    @Test
    void restoreKeepsTheSavedProfession() {
        Worker worker = Worker.restore(VILLAGER, COLONY, ProfessionType.BUILDER);

        assertEquals(Optional.of(ProfessionType.BUILDER), worker.profession());
    }

    /** Quem não tinha função quando o mundo fechou volta sem função. */
    @Test
    void restoreAcceptsAbsentProfession() {
        Worker worker = Worker.restore(VILLAGER, COLONY, null);

        assertFalse(worker.hasProfession());
    }

    @Test
    void belongsToAnswersAboutTheOwningColony() {
        Worker worker = Worker.register(VILLAGER, COLONY);

        assertTrue(worker.belongsTo(COLONY));
        assertFalse(worker.belongsTo(UUID.randomUUID()));
    }

    /** A identidade é o aldeão, não a função nem a colônia. */
    @Test
    void identityIsTheVillager() {
        Worker one = Worker.register(VILLAGER, COLONY);
        Worker other = Worker.restore(VILLAGER, UUID.randomUUID(), ProfessionType.FARMER);

        assertEquals(one, other);
        assertEquals(one.hashCode(), other.hashCode());
    }

    @Test
    void differentVillagersAreDifferentWorkers() {
        Worker one = Worker.register(UUID.randomUUID(), COLONY);
        Worker other = Worker.register(UUID.randomUUID(), COLONY);

        assertNotEquals(one, other);
    }

    @Test
    void everyProfessionOfTheProductionChainExists() {
        // Sete profissões produtoras e a função fundacional de construção.
        assertEquals(8, ProfessionType.values().length);

        // A ordem é a da cadeia produtiva, e ela importa: o distribuidor
        // preenche nessa sequência, então quem colhe vem antes de quem
        // transforma, e quem transforma antes de quem constrói.
        assertEquals(ProfessionType.MINER, ProfessionType.values()[0]);
        assertEquals(ProfessionType.LUMBERJACK, ProfessionType.values()[1]);
        assertEquals(ProfessionType.MASON, ProfessionType.values()[2]);
        assertEquals(ProfessionType.SMELTER, ProfessionType.values()[3]);
        assertEquals(ProfessionType.CARPENTER, ProfessionType.values()[4]);

        // O pedreiro entra ao lado do carpinteiro, e não no fim: os dois
        // transformam, e a regra da ordem é que quem transforma vem
        // depois de quem colhe e antes de quem constrói.
        assertEquals(ProfessionType.FARMER, ProfessionType.values()[5]);
        assertEquals(ProfessionType.SHEPHERD, ProfessionType.values()[6]);
        assertEquals(ProfessionType.BUILDER, ProfessionType.values()[7]);
    }

    /**
     * A capacidade que acabou de travar descansa — ADR-010, 2026-09-02.
     *
     * <p><b>Travado não é ocioso</b>, e essa é a descoberta que fez esta
     * peça existir. A sessão de 2026-09-02 deixou dois trabalhadores
     * parados por dezesseis e por dois minutos, e nenhum dos dois estava
     * ocioso pela definição do {@code WorkAssignment}: os dois tinham
     * tarefa aberta. Quem sabe a diferença é o guarda de travamento, e o
     * que ele aprende precisa de onde morar.
     *
     * <p>Mora aqui, no trabalhador, e não num mapa estático: é estado
     * dele, morre com ele, e não sobra atrás quando a colônia some. Não
     * vai para o disco — reabrir o mundo já recusando o próprio trabalho
     * seria pior que a tentativa a mais que isso custa.
     */
    @Test
    void aCapabilityThatJustStalledIsRested() {
        Worker worker = Worker.register(UUID.randomUUID(), UUID.randomUUID());

        worker.rest(Capability.COLLECT_STONE);

        assertTrue(worker.isResting(Capability.COLLECT_STONE));
    }

    /** E só ela: descansar pedra não pode calar a madeira. */
    @Test
    void restingOneCapabilityLeavesTheOthersAlone() {
        Worker worker = Worker.register(UUID.randomUUID(), UUID.randomUUID());

        worker.rest(Capability.COLLECT_STONE);

        assertFalse(worker.isResting(Capability.COLLECT_WOOD));
    }

    /** Quem nunca travou não descansa nada. */
    @Test
    void aWorkerWhoNeverStalledRestsNothing() {
        Worker worker = Worker.register(UUID.randomUUID(), UUID.randomUUID());

        assertFalse(worker.isResting(Capability.COLLECT_STONE));
    }

    /**
     * O descanso acaba, e conta em passagens de distribuição.
     *
     * <p>É o relógio que o {@code core} tem sem depender do mundo: a
     * distribuição roda uma vez por ciclo da colônia, então contar
     * passagens é contar ciclos — e a conta não precisa de
     * {@code world.getTime()}, que o Core não conhece (ADR-005).
     */
    @Test
    void theRestRunsOut() {
        Worker worker = Worker.register(UUID.randomUUID(), UUID.randomUUID());

        worker.rest(Capability.COLLECT_STONE);

        for (int i = 0; i < Worker.REST_CYCLES; i++) {
            assertTrue(worker.isResting(Capability.COLLECT_STONE));

            worker.aCycleWentBy();
        }

        assertFalse(worker.isResting(Capability.COLLECT_STONE));
    }

    /** Travar de novo renova o prazo, e não o encurta. */
    @Test
    void restingAgainStartsTheClockOver() {
        Worker worker = Worker.register(UUID.randomUUID(), UUID.randomUUID());

        worker.rest(Capability.COLLECT_STONE);
        worker.aCycleWentBy();
        worker.rest(Capability.COLLECT_STONE);

        for (int i = 0; i < Worker.REST_CYCLES - 1; i++) {
            worker.aCycleWentBy();
        }

        assertTrue(worker.isResting(Capability.COLLECT_STONE));
    }

    // --- as janelas de memória, 2026-09-25 (sobreviventes do PIT) ---

    private static void cycles(Worker worker, int count) {
        for (int i = 0; i < count; i++) {
            worker.aCycleWentBy();
        }
    }

    /** Nenhuma desistência, nenhum castigo — e castigo nunca é negativo. */
    @Test
    void noFailureMeansNoShun() {
        assertEquals(0, Worker.shunCyclesFor(0));
        assertEquals(0, Worker.shunCyclesFor(-1));
        assertEquals(Worker.SHUN_CYCLES, Worker.shunCyclesFor(1));
    }

    /**
     * A desistência some da conta depois de doze passagens, nem antes nem
     * depois: é o que separa "teima nesta parede" de "três azares numa hora".
     */
    @Test
    void aStrikeIsForgottenExactlyWhenItsWindowEnds() {
        Worker worker = Worker.register(VILLAGER, COLONY);

        worker.rest(Capability.COLLECT_STONE);

        cycles(worker, 11);

        assertEquals(1, worker.strikesOn(Capability.COLLECT_STONE), "ainda dentro da janela");

        cycles(worker, 1);

        assertEquals(0, worker.strikesOn(Capability.COLLECT_STONE), "a janela fechou");
    }

    /**
     * Largar o mesmo ofício de novo dentro de 128 passagens dobra o castigo;
     * depois disso, a contagem recomeça e o castigo volta ao de base.
     *
     * <p>O castigo é observado pela duração: o de base (8) já venceu na 8ª
     * passagem, o dobrado (16) ainda não.
     */
    @Test
    void theTallyOfAbandonedTradesRemembersFor128Cycles() {
        Worker within = Worker.register(VILLAGER, COLONY);
        within.assign(ProfessionType.MINER);
        within.giveUpProfession();
        cycles(within, 127);
        within.assign(ProfessionType.MINER);
        within.giveUpProfession();
        cycles(within, Worker.SHUN_CYCLES);

        assertTrue(within.isShunning(ProfessionType.MINER),
                "a segunda desistência na janela devia dobrar o castigo");

        Worker after = Worker.register(UUID.randomUUID(), COLONY);
        after.assign(ProfessionType.MINER);
        after.giveUpProfession();
        cycles(after, 128);
        after.assign(ProfessionType.MINER);
        after.giveUpProfession();
        cycles(after, Worker.SHUN_CYCLES);

        assertFalse(after.isShunning(ProfessionType.MINER),
                "passada a janela, a contagem recomeça e o castigo é o de base");
    }

    /**
     * O trabalhador é chave de mapa no registro; hash constante continuaria
     * "correto" e poria todos no mesmo balde.
     */
    @Test
    void theHashFollowsTheVillager() {
        UUID one = new UUID(0L, 1L);
        UUID two = new UUID(0L, 2L);

        assertEquals(one.hashCode(), Worker.register(one, COLONY).hashCode());
        assertNotEquals(Worker.register(one, COLONY).hashCode(),
                Worker.register(two, COLONY).hashCode());
    }
}
