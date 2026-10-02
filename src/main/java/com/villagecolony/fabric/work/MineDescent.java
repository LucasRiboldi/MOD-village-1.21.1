package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.type.ServerMemory;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Onde a descida para — F-1, 2026-10-02.
 *
 * <p><b>O que se via.</b> Na sessão das 01:36 três mineiros congelaram sete
 * vezes no mesmo bloco, {@code -241, 10, 379}: a passagem da mina descia até
 * ali e parava numa caverna com lava. A pedra pedida mudava a cada vez —
 * {@code y=-15}, depois {@code y=-49}, um ramal, outro —, e a marca do
 * {@link MineMarks} é da pedra e do cubo de oito em volta dela. Nenhuma marca
 * pegava o que de fato se repetia: <b>o ponto onde a descida trava</b>.
 *
 * <p><b>O que muda.</b> O mineiro que desiste de uma pedra abaixo dele, parado
 * debaixo da terra, deixa marcado onde parou. Da {@link #TIMES_TO_BLOCK}ª vez no
 * mesmo ponto, toda pedra abaixo dele, a {@link #RADIUS} blocos para o lado,
 * fica de fora pelo prazo do {@link MineMarks#memoryFor} — 5, 10, 20 minutos,
 * dobrando enquanto voltar a acontecer. Acima do ponto (por onde ele passou)
 * nada muda.
 */
final class MineDescent {

    static {
        ServerMemory.register(MineDescent.class, MineDescent::clearAll);
    }

    /** Quanto para o lado do ponto travado a pedra de baixo fica de fora. */
    static final int RADIUS = 16;

    /** A que distância duas paradas contam como o mesmo ponto. */
    private static final int SAME_SPOT = 2;

    /** Quantas paradas no mesmo ponto bastam: uma pode ser azar de navegação. */
    static final int TIMES_TO_BLOCK = 2;

    /** Pedra mais de tantos blocos abaixo dos pés conta como descida. */
    private static final int BELOW = 2;

    private static final int MAX_STOPS = 64;

    private record Stop(BlockPos at, long since, int count) {
    }

    private static final List<Stop> STOPS = new ArrayList<>();

    private MineDescent() {
    }

    /**
     * O mineiro parado em {@code miner}, debaixo da terra, desistiu de
     * {@code stone}.
     */
    static void stoppedAt(long now, BlockPos miner, BlockPos stone) {
        if (stone.getY() >= miner.getY() - BELOW) {
            return;
        }

        int count = 1;

        for (int i = 0; i < STOPS.size(); i++) {
            Stop stop = STOPS.get(i);

            if (isSameSpot(stop.at(), miner)) {
                count = stop.count() + 1;
                STOPS.remove(i);
                break;
            }
        }

        if (STOPS.size() >= MAX_STOPS) {
            STOPS.remove(0);
        }

        STOPS.add(new Stop(miner.toImmutable(), now, count));

        if (count >= TIMES_TO_BLOCK) {
            VillageColonyMod.LOGGER.info(
                    "The way down stops at {} — miners froze there {} times; stone below it within {} blocks"
                            + " is skipped for {} ticks",
                    miner.toShortString(), count, RADIUS, memoryFor(count));
        }
    }

    /** Se esta pedra fica abaixo de uma descida travada e ainda de castigo. */
    static boolean blocksAt(long now, BlockPos stone) {
        for (Stop stop : STOPS) {
            if (stop.count() >= TIMES_TO_BLOCK
                    && now - stop.since() < memoryFor(stop.count())
                    && stone.getY() < stop.at().getY() - 1
                    && Math.abs(stone.getX() - stop.at().getX()) <= RADIUS
                    && Math.abs(stone.getZ() - stop.at().getZ()) <= RADIUS) {
                return true;
            }
        }

        return false;
    }

    /** O prazo: a primeira vez que trava conta como a primeira recusa da pedra. */
    static long memoryFor(int count) {
        return MineMarks.memoryFor(count - TIMES_TO_BLOCK + 1);
    }

    private static boolean isSameSpot(BlockPos a, BlockPos b) {
        return Math.abs(a.getX() - b.getX()) <= SAME_SPOT
                && Math.abs(a.getY() - b.getY()) <= SAME_SPOT
                && Math.abs(a.getZ() - b.getZ()) <= SAME_SPOT;
    }

    static void clearAll() {
        STOPS.clear();
    }
}
