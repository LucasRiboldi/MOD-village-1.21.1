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

    // ----------------------------------------------------------------
    // A Regra 5, de 2026-08-13: quanto fabricar.
    //
    // A conta da Regra 1 não serve. Um tronco vira quatro tábuas, então
    // fabricar aumenta o volume guardado: "fabricar até encher"
    // transformaria toda a madeira da colônia em tábua e pararia a
    // coleta junto, porque é o baú cheio que faz o lenhador parar.
    // ----------------------------------------------------------------

    /**
     * A meta de tábua é metade do que os baús comportam.
     *
     * <p>O tronco no estoque é o que abre a meta: sem material não se
     * pede fabricação. Ver {@link #withoutLogsThereIsNoPlankGoal}.
     */
    @Test
    void thePlankGoalIsHalfOfWhatTheChestsHold() {
        Map<ResourceType, Integer> goal = ColonyGoals.of(
                colony(), owned(ResourceType.OAK_LOG, 8), 0, 200);

        assertEquals(100, goal.get(ResourceType.OAK_PLANKS));
    }

    /**
     * A capacidade conta o que já está guardado.
     *
     * <p>Sem isso a meta subiria a cada peça feita: o espaço livre cai
     * quando a tábua entra, e uma meta que só olhasse o espaço livre
     * mandaria fabricar cada vez menos sem nunca se dar por satisfeita.
     */
    @Test
    void whatIsAlreadyStoredCountsTowardTheCapacity() {
        Map<ResourceType, Integer> counts = new EnumMap<>(ResourceType.class);
        counts.put(ResourceType.BIRCH_PLANKS, 60);
        counts.put(ResourceType.OAK_LOG, 8);

        Map<ResourceType, Integer> goal =
                ColonyGoals.of(colony(), ResourceTally.of(counts), 0, 140);

        assertEquals(100, goal.get(ResourceType.OAK_PLANKS));
    }

    /**
     * Na metade, a colônia para de pedir.
     *
     * <p>É o ponto fixo da regra, e o que impede o E1 de voltar por esta
     * porta: cada peça feita sobe o guardado e desce o que cabe, até os
     * dois se encontrarem.
     */
    @Test
    void atHalfTheColonyStopsAsking() {
        Map<ResourceType, Integer> counts = new EnumMap<>(ResourceType.class);
        counts.put(ResourceType.OAK_PLANKS, 100);
        counts.put(ResourceType.OAK_LOG, 8);

        ResourceTally stock = ResourceTally.of(counts);

        Map<ResourceType, Integer> missing = ResourceDemand.deficit(
                ColonyGoals.of(colony(), stock, 0, 100), stock);

        assertFalse(missing.containsKey(ResourceType.OAK_PLANKS));
    }

    /** Tábua de qualquer espécie satisfaz a meta. */
    @Test
    void anyPlankSatisfiesTheGoal() {
        Map<ResourceType, Integer> counts = new EnumMap<>(ResourceType.class);
        counts.put(ResourceType.JUNGLE_PLANKS, 100);
        counts.put(ResourceType.OAK_LOG, 8);

        ResourceTally jungle = ResourceTally.of(counts);

        Map<ResourceType, Integer> missing = ResourceDemand.deficit(
                ColonyGoals.of(colony(), jungle, 0, 100), jungle);

        assertFalse(missing.containsKey(ResourceType.OAK_PLANKS));
    }

    /** Sem baú, nenhuma meta de tábua. */
    @Test
    void withoutChestsThereIsNoPlankGoal() {
        Map<ResourceType, Integer> missing = ResourceDemand.deficit(
                ColonyGoals.of(colony(), ResourceTally.empty(), 0, 0),
                ResourceTally.empty());

        assertFalse(missing.containsKey(ResourceType.OAK_PLANKS));
    }

    /** Espaço de tábua negativo é erro de quem mediu. */
    @Test
    void negativePlankRoomIsRefused() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ColonyGoals.of(colony(), ResourceTally.empty(), 0, -1));
    }

    /**
     * Sem tronco, não se pede tábua.
     *
     * <p>Não se pede o que não há com que fazer. Sem esta regra, uma
     * colônia sem madeira abriria tarefa de fabricação a cada ciclo para
     * o fabricante encerrá-la no tick seguinte — o E1 voltando por outra
     * porta.
     */
    @Test
    void withoutLogsThereIsNoPlankGoal() {
        Map<ResourceType, Integer> missing = ResourceDemand.deficit(
                ColonyGoals.of(colony(), ResourceTally.empty(), 100, 200),
                ResourceTally.empty());

        assertFalse(missing.containsKey(ResourceType.OAK_PLANKS));
    }

    /** Com tronco no baú, a meta de tábua abre. */
    @Test
    void withLogsThePlankGoalOpens() {
        ResourceTally stock = owned(ResourceType.OAK_LOG, 8);

        Map<ResourceType, Integer> missing = ResourceDemand.deficit(
                ColonyGoals.of(colony(), stock, 0, 200), stock);

        assertEquals(100, missing.get(ResourceType.OAK_PLANKS));
    }

    // --- a segunda metade da Regra 5: a obra manda ---

    /**
     * Com obra, a meta de tábua é a da obra.
     *
     * <p>A metade do armazém deixa de ser teto e vira lote de partida.
     */
    @Test
    void theWorkDemandReplacesTheHalf() {
        Map<ResourceType, Integer> counts = new EnumMap<>(ResourceType.class);
        counts.put(ResourceType.OAK_LOG, 10);
        counts.put(ResourceType.OAK_PLANKS, 4);

        Map<ResourceType, Integer> goal =
                ColonyGoals.of(colony(), ResourceTally.of(counts), 0, 100, 33);

        assertEquals(33, goal.get(ResourceType.OAK_PLANKS));
    }

    /** Sem obra, continua valendo a metade. */
    @Test
    void withoutWorkTheHalfStillRules() {
        Map<ResourceType, Integer> counts = new EnumMap<>(ResourceType.class);
        counts.put(ResourceType.OAK_LOG, 10);
        counts.put(ResourceType.OAK_PLANKS, 4);

        Map<ResourceType, Integer> goal =
                ColonyGoals.of(colony(), ResourceTally.of(counts), 0, 100, 0);

        assertEquals(52, goal.get(ResourceType.OAK_PLANKS));
    }

    /**
     * Sem tronco, nem a obra levanta a meta.
     *
     * <p>Pedir tábua sem madeira com que fazê-la abriria uma tarefa por
     * ciclo para o fabricante encerrá-la no tick seguinte — o E1 por
     * outra porta. A obra espera em WAITING_RESOURCES, e quem destrava é
     * a meta de madeira.
     */
    @Test
    void withoutLogsEvenTheWorkWaits() {
        Map<ResourceType, Integer> counts = new EnumMap<>(ResourceType.class);
        counts.put(ResourceType.OAK_PLANKS, 4);

        Map<ResourceType, Integer> goal =
                ColonyGoals.of(colony(), ResourceTally.of(counts), 0, 100, 33);

        assertEquals(4, goal.get(ResourceType.OAK_PLANKS));
    }

    /**
     * O vidro da obra vira meta, e a areia dele junto — 2026-08-20.
     *
     * <p>O elo que faltava na cadeia. Antes desta linha o vidro nunca era
     * meta: a colônia tinha um fundidor que sabia fundir e nunca recebia
     * tarefa, e a areia não tinha para quem ser colhida.
     */
    @Test
    void theGlassTheWorkWantsAsksForSandToo() {
        Map<ResourceType, Integer> goal = withGlassDemand(
                ResourceTally.of(new EnumMap<>(ResourceType.class)), 6);

        assertEquals(6, goal.get(ResourceType.GLASS));
        assertEquals(6, goal.get(ResourceType.SAND));
    }

    /**
     * A areia que se pede é a que falta, e não a que a janela custa.
     *
     * <p>Pedir areia pelo tamanho da obra ignoraria o vidro já fundido, e
     * a colônia continuaria raspando a praia com o baú cheio de vidro.
     */
    @Test
    void theSandGoalDiscountsTheGlassAlreadyMade() {
        Map<ResourceType, Integer> goal = withGlassDemand(owned(ResourceType.GLASS, 4), 6);

        assertEquals(6, goal.get(ResourceType.GLASS));
        assertEquals(2, goal.get(ResourceType.SAND));
    }

    /** Fundido o bastante, a areia sai da lista sozinha. */
    @Test
    void theSandGoalDriesUpWhenTheGlassIsThere() {
        Map<ResourceType, Integer> goal = withGlassDemand(owned(ResourceType.GLASS, 6), 6);

        assertFalse(goal.containsKey(ResourceType.SAND));
        assertEquals(6, goal.get(ResourceType.GLASS));
    }

    /**
     * Sem janela na obra, nem vidro nem areia entram.
     *
     * <p>A cabana de deserto não tem janela, e uma meta de areia ali
     * mandaria o mineiro raspar a duna por nada.
     */
    @Test
    void aWorkWithoutWindowsAsksForNeither() {
        Map<ResourceType, Integer> goal = withGlassDemand(
                ResourceTally.of(new EnumMap<>(ResourceType.class)), 0);

        assertFalse(goal.containsKey(ResourceType.GLASS));
        assertFalse(goal.containsKey(ResourceType.SAND));
    }

    /** A areia é meta de coleta, e o vidro é de fundição. */
    @Test
    void theSandIsStillMissingWhileNoOneBroughtIt() {
        ResourceTally stock = owned(ResourceType.SAND, 2);

        Map<ResourceType, Integer> missing = ResourceDemand.deficit(
                withGlassDemand(stock, 6), stock);

        assertEquals(4, missing.get(ResourceType.SAND));
    }

    private static Map<ResourceType, Integer> withGlassDemand(
            ResourceTally stock, int glassForWork) {

        return ColonyGoals.of(
                colony(), stock, 0, 0, 0,
                ResourceType.COBBLESTONE, 0, 0, glassForWork);
    }

    @Test
    void aNegativeWorkDemandIsRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> ColonyGoals.of(colony(), ResourceTally.of(new EnumMap<>(ResourceType.class)), 0, 0, -1));
    }
}
