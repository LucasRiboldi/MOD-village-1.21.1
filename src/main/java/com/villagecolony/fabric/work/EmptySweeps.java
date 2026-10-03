package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.type.ServerMemory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * O raio inteiro já foi varrido e não tem — F-1, 2026-10-02.
 *
 * <p><b>O que se via.</b> Na sessão das 00:43 o fundidor varreu o raio
 * inteiro atrás de areia 38 vezes em 20 minutos, uma a cada 30 s, sem nunca
 * achar: 0,55% do servidor, e o fundidor preso à tarefa de areia enquanto
 * podia fundir. Varrer de novo logo depois de uma varredura completa e vazia
 * só repete a resposta.
 *
 * <p><b>O que muda.</b> A varredura completa e vazia solta a tarefa e põe o
 * recurso de castigo na colônia: 5 minutos, depois 10, depois 20 enquanto
 * continuar vazio. Enquanto dura, a tarefa fica aberta e ninguém a reserva —
 * o trabalhador vai fazer outra coisa. Achar o recurso zera a contagem.
 */
public final class EmptySweeps {

    static {
        ServerMemory.register(EmptySweeps.class, EmptySweeps::clearAll);
    }

    /** O primeiro castigo: cinco minutos. */
    static final int BASE = 6_000;

    /** Quantas vezes o castigo dobra: 5, 10, 20 minutos. */
    private static final int MAX_DOUBLINGS = 2;

    private record Key(UUID colonyId, ResourceType resource) {
    }

    private record Mark(long since, int count) {
    }

    private static final Map<Key, Mark> MARKS = new HashMap<>();

    private EmptySweeps() {
    }

    static long memoryFor(int count) {
        return count <= 0 ? 0 : (long) BASE << Math.min(count - 1, MAX_DOUBLINGS);
    }

    /** Uma varredura completa não achou {@code resource} no raio da colônia. */
    static void foundNothing(UUID colonyId, ResourceType resource, long now) {
        Key key = new Key(colonyId, resource);
        Mark before = MARKS.get(key);
        Mark mark = new Mark(now, before == null ? 1 : before.count() + 1);

        MARKS.put(key, mark);

        VillageColonyMod.LOGGER.info(
                "Colony {} has no {} anywhere in the radius — {} empty sweeps in a row; nobody looks again for {} ticks",
                colonyId, resource.name().toLowerCase(java.util.Locale.ROOT), mark.count(), memoryFor(mark.count()));
    }

    /** Achou: a próxima varredura vazia volta ao primeiro castigo. */
    static void found(UUID colonyId, ResourceType resource) {
        MARKS.remove(new Key(colonyId, resource));
    }

    /** Se ninguém deve procurar {@code resource} nesta colônia agora. */
    public static boolean isWaiting(UUID colonyId, ResourceType resource, long now) {
        Mark mark = MARKS.get(new Key(colonyId, resource));

        return mark != null && now - mark.since() < memoryFor(mark.count());
    }

    static void clearAll() {
        MARKS.clear();
    }
}
