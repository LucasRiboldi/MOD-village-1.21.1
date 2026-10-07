package com.villagecolony.fabric.integration;

import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.core.construction.model.ColonySweepCursor;
import com.villagecolony.fabric.integration.SweepState.Sweep;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A varredura de lote e o índice de ruas indo para o disco e voltando —
 * 2026-08-27. Separado de {@link BuildSiteScanner} em 2026-10-02 pelo teto de
 * 500 linhas; o texto veio sem mudança.
 */
public final class SweepPersistence {

    private SweepPersistence() {
    }

    /**
     * O índice de cada colônia, para o disco — 2026-08-27.
     *
     * <p>Só os prontos: o {@link SweepState#BUILDING} fica de fora de propósito,
     * pela mesma razão que o separa aqui dentro. Índice pela metade
     * mente sobre ter visto o raio inteiro, e gravado ele mentiria
     * também na sessão seguinte, quando ninguém mais lembra que a
     * varredura tinha parado no meio.
     */
    public static List<ColonyRoads> saved() {
        return List.copyOf(SweepState.ROADS.values());
    }

    /**
     * Recoloca um índice que veio do disco — 2026-08-27.
     *
     * <p>É o que apaga os dezessete ciclos da primeira busca de lote de
     * cada sessão. Não é acreditado de olhos fechados: cada coluna é
     * reconferida no mundo quando visitada, e o centro que veio junto faz
     * o {@link BuildSiteScanner#find} descartar o índice inteiro se a colônia tiver
     * andado demais desde que ele foi medido.
     *
     * <p>Índice que não passa no {@link RoadIndex#fits} é ignorado em silêncio, e
     * a colônia varre o quadrado como antes desta versão.
     */
    public static void restore(ColonyRoads roads) {
        if (!RoadIndex.fits(roads.columns())) {
            return;
        }

        // Lista nova, cursor novo: o mundo abriu agora, e ninguém parou
        // no meio desta volta.
        SweepState.ROAD_CURSOR.remove(roads.colonyId());

        SweepState.ROADS.put(roads.colonyId(), roads);
    }

    /**
     * A varredura pela metade de cada colônia, para o disco —
     * 2026-08-27.
     *
     * <p>Vai junto o que ela já achou: sem isso a volta terminaria com
     * meia lista de ruas e viraria um índice que mente sobre ter visto o
     * raio inteiro. Ver {@link ColonySweepCursor}.
     *
     * <p><b>Cursor cuja memória não caberia num índice fica de fora.</b>
     * Ele nunca chegaria a virar índice — o {@link RoadIndex#fits} recusaria no
     * fim da volta — e gravá-lo custaria disco para adiar a mesma
     * recusa. Vila assim varre do centro, que é o que ela já fazia.
     */
    public static List<ColonySweepCursor> pausedSweeps() {
        List<ColonySweepCursor> saving = new ArrayList<>();

        SweepState.SWEEPS.forEach((colonyId, paused) -> {
            Set<Long> seen = SweepState.BUILDING.get(colonyId);

            if (seen == null || seen.size() > BuildSiteScanner.MAX_COLUMNS) {
                return;
            }

            saving.add(new ColonySweepCursor(
                    colonyId, paused.from(), paused.ring(), paused.column(),
                    List.copyOf(seen)));
        });

        return List.copyOf(saving);
    }

    /**
     * Recoloca uma varredura que veio do disco — 2026-08-27.
     *
     * <p>É o que faz catorze passagens de dezessete deixarem de ser
     * jogadas fora quando o mundo fecha. As duas metades voltam juntas: o
     * lugar onde ela parou e o que ela achou até ali.
     *
     * <p>O que ela achou <b>não</b> vira índice — meia volta não viu o
     * raio inteiro, e o {@link SweepState#ROADS} continua vazio até a volta
     * terminar de verdade.
     */
    public static void restore(ColonySweepCursor cursor) {
        SweepState.SWEEPS.put(cursor.colonyId(), new Sweep(cursor.ring(), cursor.column(), cursor.from()));

        SweepState.BUILDING.put(cursor.colonyId(), new LinkedHashSet<>(cursor.found()));
    }
}
