package com.villagecolony.fabric.work;

import com.villagecolony.core.type.ServerMemory;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * O percurso por etapas até a boca da mina — decisão do autor, 2026-10-14.
 *
 * <p><b>O problema</b> (playtest 14-10, Spark {@code MiIM0AisSh}): numa vila grande o mineiro
 * recebia a boca da mina como alvo único de caminhada a ~95 blocos, e a navegação do jogo não
 * traça um caminho tão longo de uma vez — o aldeão ficava {@code walking to nowhere}, minerando 0%.
 *
 * <p><b>A solução</b>: quando a navegação não alcança a boca inteira, o mineiro anda até o
 * <b>fim do caminho parcial</b> que o pathfinder de fato encontrou ({@link Path#getEnd()}) — uma
 * posição caminhável real no rumo da boca, nunca uma divisão geométrica de uma reta. Ao chegar,
 * recalcula e avança o próximo trecho. A boca e o caracol de descida ({@link MineEntrance},
 * {@link com.villagecolony.core.construction.model.MineShaft}) continuam donos do resto do percurso.
 *
 * <p><b>O que NÃO faz</b>: não decide a falha nem libera a tarefa. Quem mede "não chegou mais perto
 * da pedra nem desceu" e cede a tarefa é o guarda de descida que já existe
 * ({@link MineLease#outOfTimeOnDescent} → {@code MinerSteps.stuck} → {@code MinerHands.giveUp}).
 * Assim não há dois sistemas de travamento (ressalva do autor): este só escolhe o alvo de
 * <b>caminhada</b>; o destino da tarefa ({@code job.target}) não muda.
 *
 * <p>Confirmado no código (Yarn 1.21.1): {@code findPathTo(BlockPos, int)} devolve {@code Path} ou
 * {@code null}; {@code reachesTarget()} é true só quando o caminho chega ao alvo; um caminho
 * parcial tem {@code path != null}, nós presentes e {@code reachesTarget()==false}. Reusa o padrão
 * de cache do {@link MinerDirectPath} (um A* por janela, não por tique).
 */
final class MineApproachPath {

    static {
        ServerMemory.register(MineApproachPath.class, MineApproachPath::clearAll);
    }

    /** De quanto em quanto tempo o caminho até a boca é refeito (rede de segurança). */
    static final int RECHECK_TICKS = 200;

    /** Folga: o caminho pode parar a esta distância da boca e ainda contar como "chega". */
    static final int NEAR = 2;

    /** Quanto o fim de um caminho parcial precisa encurtar até a boca para valer como avanço. */
    static final double PROGRESS_MIN = 2.0;

    private static final class Approach {
        BlockPos mouth;
        BlockPos waypoint;
        double closest;
        long at;

        Approach(BlockPos mouth, BlockPos waypoint, double closest, long at) {
            this.mouth = mouth;
            this.waypoint = waypoint;
            this.closest = closest;
            this.at = at;
        }
    }

    private static final Map<UUID, Approach> APPROACHES = new HashMap<>();

    private MineApproachPath() {
    }

    /**
     * O alvo de caminhada do mineiro rumo à {@code mouth}: a própria boca quando a navegação a
     * alcança, ou o fim do caminho parcial (um waypoint caminhável) quando não.
     *
     * <p>Nunca devolve posição que não venha de um caminho do jogo. Quando não há caminho nenhum
     * ({@code path == null}), devolve a boca inalterada e deixa o guarda de descida medir a falta
     * de progresso — não reinstala um waypoint inventado.
     */
    static BlockPos toward(ServerWorld world, VillagerEntity villager, BlockPos mouth) {
        UUID id = villager.getUuid();
        long now = world.getTime();
        Approach known = APPROACHES.get(id);

        // Boca mudou: recomeça o episódio (limpa estado antigo).
        if (known != null && !known.mouth.equals(mouth)) {
            known = null;
            APPROACHES.remove(id);
        }

        boolean atWaypoint = known != null && known.waypoint != null
                && villager.getBlockPos().isWithinDistance(known.waypoint.toCenterPos(), NEAR + 1.0);

        // Recalcula ao chegar no waypoint, ao vencer a janela, ou sem estado ainda.
        boolean recompute = known == null || atWaypoint || now - known.at >= RECHECK_TICKS;

        if (!recompute && known.waypoint != null) {
            return known.waypoint;
        }

        Path path = villager.getNavigation().findPathTo(mouth, NEAR);
        double toMouth = dist(villager.getBlockPos(), mouth);

        // A navegação alcança a boca (inteira ou a NEAR dela): o alvo é a boca.
        if (path != null && (path.reachesTarget()
                || (path.getEnd() != null && path.getEnd().getBlockPos().getManhattanDistance(mouth) <= NEAR))) {
            APPROACHES.put(id, new Approach(mouth.toImmutable(), mouth.toImmutable(),
                    Math.min(toMouth, known == null ? toMouth : known.closest), now));

            return mouth;
        }

        // Caminho parcial: o fim é um waypoint caminhável, se encurta a distância à boca com
        // progresso útil. Sem caminho (path==null) ou sem progresso: mantém o alvo e deixa o
        // guarda de descida contar a falta de avanço.
        if (path != null && path.getEnd() != null) {
            BlockPos end = path.getEnd().getBlockPos();
            double endToMouth = dist(end, mouth);
            double best = known == null ? Double.MAX_VALUE : known.closest;

            if (usefulProgress(endToMouth, best) && !end.equals(villager.getBlockPos())) {
                APPROACHES.put(id, new Approach(mouth.toImmutable(), end.toImmutable(), endToMouth, now));

                return end;
            }
        }

        if (known == null) {
            APPROACHES.put(id, new Approach(mouth.toImmutable(), null, toMouth, now));

            return mouth;
        }

        known.at = now;

        return known.waypoint != null ? known.waypoint : mouth;
    }

    /**
     * Se o fim de um caminho parcial é progresso útil: encurta a menor distância já atingida à
     * boca por ao menos {@link #PROGRESS_MIN} (tolera pequenas oscilações e waypoint repetido).
     * Lógica pura — exercitada em {@code MineApproachProgressTest}.
     */
    static boolean usefulProgress(double endToMouth, double closest) {
        return endToMouth <= closest - PROGRESS_MIN;
    }

    private static double dist(BlockPos a, BlockPos b) {
        return Math.sqrt(a.getSquaredDistance(b));
    }

    /** Esquece o percurso deste mineiro — ao liberar a tarefa ou entrar no nível. */
    static void forget(UUID workerId) {
        APPROACHES.remove(workerId);
    }

    static void clearAll() {
        APPROACHES.clear();
    }
}
