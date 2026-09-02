package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

/**
 * Como o construtor chega ao bloco.
 *
 * <p>Saiu de {@code BuilderWork} em 2026-08-22, quando ele passou de
 * oitocentas linhas. É uma pergunta inteira e separada das outras
 * daquele arquivo: <b>onde o aldeão precisa pisar</b>, e <b>por que ele
 * não chegou</b> quando não chega. Quem constrói usa a resposta.
 *
 * <p>O corte é por responsabilidade, e não por contagem — ADR-009 §6:
 * quinhentas linhas são indicador, e o que decide onde cortar é o
 * assunto. Este assunto nasceu inteiro na sessão de jogo de 2026-08-22,
 * quando o construtor passou oito minutos andando sem chegar.
 */
public final class BuilderApproach {

    /**
     * Até onde o braço do construtor alcança, no plano.
     *
     * <p>Cinco blocos, e a vertical não entra: da fundação ao último
     * bloco da planta, ele põe de pé no chão do lote.
     */
    private static final int REACH = 5;

    /**
     * Quantos blocos acima e abaixo do chão do lote procurar um lugar de
     * pé — 2026-08-22.
     *
     * <p>Seis é a altura de uma duna de deserto sobre o lote, que é o caso
     * que pediu esta busca.
     */
    private static final int FOOT_SEARCH = 6;

    private BuilderApproach() {
    }

    /**
     * Se o construtor alcança este bloco — a Regra 14.
     *
     * <p>Só a distância no plano. A vertical não entra: da fundação ao
     * último bloco da planta, o construtor põe de pé no chão do lote. O
     * que ele não faz continua não fazendo — não voa, não sobe andaime e
     * não empilha bloco para subir, porque nada disso está na planta e a
     * Regra 3 manda escrever só o que ela diz.
     */
    static boolean isWithinReach(BlockPos worker, BlockPos target) {
        int dx = worker.getX() - target.getX();
        int dz = worker.getZ() - target.getZ();

        return dx * dx + dz * dz <= REACH * REACH;
    }

    /**
     * O pé da coluna do bloco: para onde o construtor caminha.
     *
     * <p>Andar até o bloco em si só servia enquanto a obra era rasa. Com
     * a Regra 14 o alvo pode estar no telhado, e mandar o aldeão a uma
     * posição no ar é pedir um caminho que não existe — ele fica parado
     * até o guarda de travamento devolver a tarefa, que é a mesma roda
     * por outra porta.
     *
     * <p>O chão do lote é a altura da origem do projeto: é onde a
     * fundação está e onde ele já esteve para pôr o primeiro bloco.
     */
    static BlockPos footOf(
            ServerWorld world, ConstructionProject project, BlockPos target) {

        BlockPos ground = new BlockPos(target.getX(), project.origin().y(), target.getZ());

        return standingSpotNear(world, ground).orElse(ground);
    }

    /**
     * Um lugar onde um aldeão cabe de pé, perto desta coluna.
     *
     * <p><b>Nasceu da sessão de 2026-08-22.</b> A vila de deserto
     * planejou a primeira casa da história do mod e o construtor passou
     * oito minutos com {@code walking for N ticks without reaching the
     * block}, três vezes até o guarda de dois minutos, sem colocar um
     * bloco. O alvo era o pé da coluna na altura da origem da obra — e
     * no deserto essa altura pode estar <b>enterrada na duna</b>. Andar
     * para dentro de areia sólida é pedir um caminho que não existe, e a
     * task Vanilla simplesmente não anda.
     *
     * <p>Procura, a partir do chão do lote, o primeiro lugar de pé —
     * dois blocos livres sobre bloco sólido — alternando para cima e
     * para baixo. Para cima resolve a duna; para baixo resolve o lote
     * numa depressão, e a Regra 14 já dizia que o alvo pode estar no ar.
     *
     * <p>Vazio quando o chunk não está carregado: pedir por ele aqui
     * forçaria carregamento dentro do tick, que é o defeito que travou o
     * servidor duas vezes neste projeto (§11).
     *
     * <p>Pública para o teste de jogo, e é uma leitura sem efeito: o
     * caminho inteiro —
     * construtor longe, lote enterrado — não cabe na arena da bateria,
     * e o que se pode afirmar é a decisão em si.
     */
    public static Optional<BlockPos> standingSpotNear(ServerWorld world, BlockPos ground) {
        if (world.getChunkManager().getWorldChunk(ground.getX() >> 4, ground.getZ() >> 4) == null) {
            return Optional.empty();
        }

        for (int step = 0; step <= FOOT_SEARCH; step++) {
            for (int sign = 1; sign >= -1; sign -= 2) {
                BlockPos at = ground.up(step * sign);

                if (at.getY() < world.getBottomY() || at.getY() > world.getTopY() - 2) {
                    continue;
                }

                if (standable(world, at)) {
                    return Optional.of(at);
                }

                if (step == 0) {
                    break;
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Dá para atravessar esta posição — nada de sólido no caminho.
     *
     * <p>Não é a mesma pergunta que {@link #standable}, e confundir as
     * duas custou o E34: a camada da cabeça de um degrau é
     * <b>atravessável</b> e não é <b>pisável</b>. Quem decide se um
     * corredor continua pergunta esta; quem escolhe onde parar pergunta a
     * outra.
     */
    public static boolean passable(ServerWorld world, BlockPos at) {
        return world.getBlockState(at).getCollisionShape(world, at).isEmpty();
    }

    /** Dois blocos livres sobre bloco sólido: onde um aldeão cabe. */
    public static boolean standable(ServerWorld world, BlockPos at) {
        return world.getBlockState(at.down()).isSolidBlock(world, at.down())
                && passable(world, at)
                && passable(world, at.up());
    }

    /**
     * Por que o construtor não chegou, dito em uma frase.
     *
     * <p>É o §11 outra vez: sem isto, "não chegou" tanto pode ser duna
     * por cima do lote, caminho bloqueado, aldeão longe demais para dois
     * minutos de caminhada, ou chunk que saiu de memória — e as quatro
     * têm correções diferentes. A sessão de 2026-08-22 gastou oito
     * minutos sem poder escolher entre elas.
     */
    static String whyNotReached(
            ServerWorld world, ConstructionProject project, VillagerEntity villager,
            BlockPos target) {

        BlockPos ground = new BlockPos(target.getX(), project.origin().y(), target.getZ());

        Optional<BlockPos> spot = standingSpotNear(world, ground);

        String where = spot.map(BlockPos::toShortString).orElse("nowhere to stand");

        return "the worker is at " + villager.getBlockPos().toShortString()
                + ", " + (int) Math.sqrt(villager.getBlockPos().getSquaredDistance(target))
                + " blocks away; it was walking to " + where
                + "; the lot floor at " + ground.toShortString() + " is "
                + world.getBlockState(ground).getBlock().getName().getString();
    }
}
