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
 * recurso de castigo na colônia. Enquanto dura, a tarefa fica aberta e
 * ninguém a reserva — o trabalhador vai fazer outra coisa. Achar o recurso
 * zera a contagem.
 *
 * <p><b>Na terceira, o material aparece</b> — pedido do autor, 2026-10-03:
 * {@link LocateFallback}. O castigo cresce em partes, um minuto por varredura
 * vazia, até {@link #MAX_STEPS} minutos (ADR-039 E1): a P7 pediu 5 minutos, e a
 * entrega na quarta busca (ADR-036 item 6) não pode esperar 5 minutos já na primeira.
 */
public final class EmptySweeps {

    static {
        ServerMemory.register(EmptySweeps.class, EmptySweeps::clearAll);
    }

    /** O primeiro castigo: um minuto. */
    static final int BASE = 1_200;

    /** Até quantos minutos o castigo cresce: 1, 2, 3, 4, 5. */
    static final int MAX_STEPS = 5;

    private record Key(UUID colonyId, ResourceType resource) {
    }

    private record Mark(long since, int count) {
    }

    private static final Map<Key, Mark> MARKS = new HashMap<>();

    private EmptySweeps() {
    }

    static long memoryFor(int count) {
        return count <= 0 ? 0 : (long) BASE * Math.min(count, MAX_STEPS);
    }

    /**
     * Uma varredura completa não achou {@code resource} no raio da colônia.
     *
     * @return quantas varreduras seguidas não acharam, contando esta
     */
    static int foundNothing(UUID colonyId, ResourceType resource, long now) {
        Key key = new Key(colonyId, resource);
        Mark before = MARKS.get(key);
        Mark mark = new Mark(now, before == null ? 1 : before.count() + 1);

        MARKS.put(key, mark);

        VillageColonyMod.LOGGER.info(
                "Colony {} has no {} anywhere in the radius — {} empty sweeps in a row; nobody looks again for {} ticks",
                colonyId, resource.name().toLowerCase(java.util.Locale.ROOT), mark.count(), memoryFor(mark.count()));

        return mark.count();
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
