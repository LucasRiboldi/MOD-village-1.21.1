package com.villagecolony.core.movement;

import com.villagecolony.core.type.ColonyPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Os três passos do aldeão e o que cada um exige do mundo — ADR-025, fase 2.
 *
 * <p>O aldeão ocupa dois blocos: os pés e a cabeça, logo acima. Um passo vai a
 * um vizinho horizontal no mesmo nível (andar), um acima (subir) ou um abaixo
 * (descer). Para cada um, as células por onde o corpo passa têm de estar
 * livres — ou ser rocha que se cava — e a célula sob os pés de destino tem de
 * ser chão — ou vão onde se põe bloco. É o repertório do Baritone
 * ({@code Traverse}, {@code Ascend}, {@code Descend}) sem pulo, sem queda
 * livre e sem cavar para baixo, que são os três que matam.
 *
 * <p><b>Uma regra só, dois usuários.</b> O planejador pergunta aqui ao
 * procurar o caminho, e quem executa pergunta de novo, contra o mundo de
 * agora, antes de cada passo. Se o mundo mudou, a resposta muda com ele.
 */
public final class DetourMoves {

    /** Custo de andar um bloco. Os outros são múltiplos dele. */
    static final int WALK = 2;

    static final int ASCEND = 4;

    static final int DESCEND = 3;

    /** Cavar custa três passos: o jogador contorna antes de abrir túnel. */
    static final int DIG = 6;

    /** Pôr bloco custa cinco passos — Baritone cobra caro pelo mesmo motivo: gasta material. */
    static final int PLACE = 10;

    private DetourMoves() {
    }

    /** O que o passo pede: quais células abrir, em ordem, e onde pôr bloco. */
    public record Actions(List<ColonyPos> dig, List<ColonyPos> place, int cost) {

        public Actions {
            dig = List.copyOf(dig);
            place = List.copyOf(place);
        }
    }

    /**
     * O que custa ir de {@code from} a {@code to}, ou vazio se não dá.
     *
     * @param keep células que não se cavam nem se ocupam — a pedra que o
     *     mineiro veio buscar, por exemplo, que é trabalho da picareta dele
     */
    public static Optional<Actions> actions(
            Terrain terrain, ColonyPos from, ColonyPos to, Set<ColonyPos> keep) {

        int dx = to.x() - from.x();
        int dy = to.y() - from.y();
        int dz = to.z() - from.z();

        if (Math.abs(dx) + Math.abs(dz) != 1 || Math.abs(dy) > 1) {
            return Optional.empty();
        }

        List<ColonyPos> body = new ArrayList<>();
        int cost;

        if (dy == 0) {
            body.add(to);
            body.add(up(to, 1));
            cost = WALK;
        } else if (dy > 0) {
            // A cabeça sobe antes do corpo: o bloco acima dela também sai.
            body.add(up(from, 2));
            body.add(to);
            body.add(up(to, 1));
            cost = ASCEND;
        } else {
            // O corpo passa pela borda no nível de cima e depois desce.
            body.add(up(to, 2));
            body.add(up(to, 1));
            body.add(to);
            cost = DESCEND;
        }

        List<ColonyPos> dig = new ArrayList<>();

        for (ColonyPos cell : body) {
            if (keep.contains(cell)) {
                return Optional.empty();
            }

            Cell kind = terrain.at(cell);

            if (kind.passable()) {
                continue;
            }

            if (!mayDig(terrain, cell)) {
                return Optional.empty();
            }

            dig.add(cell);
        }

        ColonyPos floor = up(to, -1);
        List<ColonyPos> place = new ArrayList<>();
        Cell under = terrain.at(floor);

        if (!under.standable()) {
            if (!under.placeable() || keep.contains(floor)) {
                return Optional.empty();
            }

            place.add(floor);
        }

        return Optional.of(new Actions(dig, place,
                cost + dig.size() * DIG + place.size() * PLACE));
    }

    /**
     * Rocha que se cava sem abrir líquido nem derrubar areia — o pedido do
     * autor em 2026-09-25: o mineiro não quebra o bloco que tem líquido atrás.
     */
    public static boolean mayDig(Terrain terrain, ColonyPos cell) {
        if (!terrain.at(cell).diggable() || terrain.at(up(cell, 1)) == Cell.LOOSE) {
            return false;
        }

        for (ColonyPos side : sides(cell)) {
            if (terrain.at(side) == Cell.FLUID) {
                return false;
            }
        }

        return true;
    }

    static List<ColonyPos> sides(ColonyPos cell) {
        return List.of(
                new ColonyPos(cell.x() + 1, cell.y(), cell.z()),
                new ColonyPos(cell.x() - 1, cell.y(), cell.z()),
                new ColonyPos(cell.x(), cell.y() + 1, cell.z()),
                new ColonyPos(cell.x(), cell.y() - 1, cell.z()),
                new ColonyPos(cell.x(), cell.y(), cell.z() + 1),
                new ColonyPos(cell.x(), cell.y(), cell.z() - 1));
    }

    static ColonyPos up(ColonyPos pos, int by) {
        return new ColonyPos(pos.x(), pos.y() + by, pos.z());
    }
}
