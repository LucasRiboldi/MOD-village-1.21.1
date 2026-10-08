package com.villagecolony.fabric.work;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Se uma pedra pode virar alvo deste mineiro — E1, decisão do autor de 2026-10-08:
 * <b>um bloco só vira alvo se existir uma posição de trabalho válida e alcançável</b>.
 *
 * <p>Existir não basta: a pedra pode estar exposta e não ter onde ficar de pé ao
 * alcance do braço (caverna embaixo da galeria), ou ter só um lugar alto demais
 * para o degrau de um bloco ({@link MinerWork#CLIMB}). O playtest de 2026-10-08
 * mostrou o preço: a pedra -751, 31, -942 tinha um lugar de pé só em y34, o
 * mineiro estava em y30, e ele andou 400 tiques por vez sem chegar.
 *
 * <p><b>Geometria, e não a navegação.</b> Perguntar ao {@code findPathTo} de um
 * aldeão já recusou árvores boas (ver {@link TreeMarks}); a busca aqui é local,
 * determinística e com teto. Quando o teto estoura, a resposta é "não sei" — e "não
 * sei" nunca recusa pedra: quem decide nesse caso continua sendo o guarda de
 * travamento.
 */
public final class MiningTarget {

    /** Por que a pedra não serve agora. */
    public enum Rejection {
        NONE,
        /** Nenhum lugar de pé ao alcance do braço. */
        NO_STANDING_POSITION,
        /** Só há lugar de pé mais alto do que o degrau que ele sobe, e ele está ao lado. */
        TOO_HIGH,
        /** Há lugar de pé, mas a busca a pé, fechada, prova que ele não chega lá. */
        NO_PATH
    }

    /** O veredito: a posição de trabalho, ou o motivo. */
    public record Verdict(@Nullable BlockPos workPosition, Rejection rejection) {

        public boolean eligible() {
            return rejection == Rejection.NONE;
        }

        static Verdict at(BlockPos where) {
            return new Verdict(where.toImmutable(), Rejection.NONE);
        }

        static Verdict no(Rejection why) {
            return new Verdict(null, why);
        }
    }

    /** Até onde o mineiro conta como "ao lado da pedra", na horizontal. */
    static final int NEAR = 12;

    /** E na vertical: mais que isto, ele ainda vai descer ou subir até a frente. */
    static final int NEAR_VERTICAL = 4;

    /** Teto de posições visitadas na busca a pé. */
    static final int SEARCH_BUDGET = 512;

    /** Quanto ele cai sem se machucar — a navegação do Vanilla desce até três. */
    private static final int DROP = 3;

    private MiningTarget() {
    }

    /**
     * Só geometria, sem busca: serve ao laço do cursor, que olha até sessenta e
     * quatro posições por passagem.
     *
     * @param miner onde ele está, ou nulo quando não se sabe (então só o lugar de pé conta)
     */
    public static Verdict quick(ServerWorld world, BlockPos target, @Nullable BlockPos miner) {
        BlockPos first = null;

        for (Vec3i offset : MinerReach.APPROACH_OFFSETS) {
            BlockPos at = target.add(offset);

            if (!BuilderApproach.standable(world, at)) {
                continue;
            }

            if (miner == null || at.getY() - miner.getY() <= MinerWork.CLIMB) {
                return Verdict.at(at);
            }

            if (first == null) {
                first = at;
            }
        }

        if (first == null) {
            return Verdict.no(Rejection.NO_STANDING_POSITION);
        }

        // Todo lugar de pé está acima do degrau dele. Longe, ele ainda vai mudar
        // de altura no caminho; ao lado, não há para onde subir.
        return isNear(miner, target) ? Verdict.no(Rejection.TOO_HIGH) : Verdict.at(first);
    }

    /**
     * Completo: a geometria e, ao lado da pedra, a busca a pé até um dos lugares
     * de pé dela. Uma vez por pedra — quando ela é escolhida, ou quando ele chega perto.
     */
    public static Verdict judge(ServerWorld world, BlockPos target, BlockPos miner) {
        Verdict geometry = quick(world, target, miner);

        if (!geometry.eligible() || !isNear(miner, target)) {
            return geometry;
        }

        Set<BlockPos> spots = new LinkedHashSet<>();

        for (Vec3i offset : MinerReach.APPROACH_OFFSETS) {
            BlockPos at = target.add(offset);

            if (BuilderApproach.standable(world, at)) {
                spots.add(at.toImmutable());
            }
        }

        Deque<BlockPos> frontier = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        BlockPos start = miner.toImmutable();

        frontier.add(start);
        seen.add(start);

        boolean open = false;
        Set<BlockPos> reached = new HashSet<>();

        while (!frontier.isEmpty()) {
            BlockPos at = frontier.poll();

            if (spots.contains(at)) {
                reached.add(at);
            }

            if (seen.size() > SEARCH_BUDGET) {
                open = true;
                break;
            }

            for (Direction way : Direction.Type.HORIZONTAL) {
                for (int dy = MinerWork.CLIMB; dy >= -DROP; dy--) {
                    BlockPos next = at.offset(way).up(dy);

                    if (!stepAllowed(world, at, way, dy) || !BuilderApproach.standable(world, next)) {
                        continue;
                    }

                    if (Math.abs(next.getX() - target.getX()) > NEAR + DROP
                            || Math.abs(next.getZ() - target.getZ()) > NEAR + DROP
                            || Math.abs(next.getY() - target.getY()) > NEAR) {
                        // Saiu da caixa: o caminho pode dar a volta por fora, e
                        // "não sei" não recusa pedra.
                        open = true;
                        continue;
                    }

                    if (seen.add(next.toImmutable())) {
                        frontier.add(next.toImmutable());
                    }

                    break;
                }
            }
        }

        // O lugar que ele alcança e que fica mais perto da pedra — a ordem dos
        // deslocamentos. O primeiro que a busca visitava era o mais perto DELE: na
        // borda do braço, e ele "chegava" sem alcançar (playtest de 08-10, 4,1 blocos).
        for (BlockPos spot : spots) {
            if (reached.contains(spot)) {
                return Verdict.at(spot);
            }
        }

        if (open) {
            return geometry;
        }

        // A busca fechou sem achar: a região onde ele anda não encosta em nenhum
        // lugar de pé da pedra. Se todos são altos, é o degrau; senão, é caminho.
        boolean anyClimbable = spots.stream().anyMatch(at -> at.getY() - miner.getY() <= MinerWork.CLIMB);

        return Verdict.no(anyClimbable ? Rejection.NO_PATH : Rejection.TOO_HIGH);
    }

    /** O espaço por onde o corpo passa no passo: em cima para subir, à frente para descer. */
    private static boolean stepAllowed(ServerWorld world, BlockPos from, Direction way, int dy) {
        BlockPos ahead = from.offset(way);

        if (dy > 0) {
            return BuilderApproach.passable(world, from.up(2));
        }

        return dy == 0 || (BuilderApproach.passable(world, ahead) && BuilderApproach.passable(world, ahead.up()));
    }

    static boolean isNear(@Nullable BlockPos miner, BlockPos target) {
        return miner != null
                && Math.abs(miner.getX() - target.getX()) <= NEAR
                && Math.abs(miner.getZ() - target.getZ()) <= NEAR
                && Math.abs(miner.getY() - target.getY()) <= NEAR_VERTICAL;
    }
}
