package com.villagecolony.core.coordination;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * A vila onde o jogador passa mais tempo — 2026-09-24, decisão do autor.
 *
 * <p><b>A frase dele:</b> <i>"a maioria pode ficar salva apenas da vila onde o
 * player passa mais tempo, pausando a evolução ou análise de outras vilas para
 * não sobrecarregar"</i>. A avaliação técnica de 24-09 mediu o motivo: o
 * planejador foi 91% do tempo dos ciclos que estouraram o tique, com oito
 * colônias planejando ao mesmo tempo.
 *
 * <p><b>Presença com memória que esquece.</b> A cada ciclo, a colônia com
 * jogador dentro do raio da vila ganha um ponto, e toda presença decai pela
 * meia-vida de {@value #HALF_LIFE_CYCLES} ciclos (duas horas de jogo com o
 * ciclo de 30 s). Assim uma visita de passagem não rouba o foco, mas quem se
 * muda de vila leva o foco junto em algumas horas — o foco segue o jogador,
 * e não o primeiro lugar em que ele esteve.
 *
 * <p>Sem estado de Minecraft: quem diz quem está presente é a camada fabric.
 */
public final class ColonyFocus {

    /** A meia-vida da presença, em ciclos de colônia (240 × 30 s = 2 h). */
    public static final int HALF_LIFE_CYCLES = 240;

    private static final double DECAY = Math.pow(0.5, 1.0 / HALF_LIFE_CYCLES);

    /** Presença abaixo disto é esquecida: a colônia foi largada há muito. */
    private static final double FORGOTTEN = 0.001;

    private final Map<UUID, Double> presence = new HashMap<>();

    /**
     * Um ciclo: quem está com jogador ganha um ponto, todo mundo decai.
     *
     * @param known as colônias que ainda existem; as outras são esquecidas
     * @param present as colônias com jogador dentro do raio da vila agora
     */
    public void record(Collection<UUID> known, Set<UUID> present) {
        Set<UUID> ids = new HashSet<>(presence.keySet());
        ids.addAll(present);

        for (UUID id : ids) {
            double next = presence.getOrDefault(id, 0.0) * DECAY + (present.contains(id) ? 1.0 : 0.0);

            if (!known.contains(id) || next < FORGOTTEN) {
                presence.remove(id);
            } else {
                presence.put(id, next);
            }
        }
    }

    /** A colônia de maior presença, se alguma já teve jogador por perto. */
    public Optional<UUID> focus() {
        return presence.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey);
    }

    /** A presença acumulada desta colônia, em "ciclos equivalentes". */
    public double presenceOf(UUID colonyId) {
        return presence.getOrDefault(colonyId, 0.0);
    }

    /** Esquece tudo — mundo novo, ou teste. */
    public void clear() {
        presence.clear();
    }
}
