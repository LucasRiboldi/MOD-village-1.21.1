package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.ClimbLimit;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.BlockState;
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
     * O mesmo, sabendo de onde o construtor vem — 2026-09-16.
     *
     * <p><b>O travamento que isto corrige.</b> O log de 02:58 mostrou a
     * obra progredindo — de 61 para 37 blocos — e nenhuma casa terminando.
     * O construtor parava com <i>"has not moved a block in 300 ticks"</i>,
     * e as três linhas dizem tudo:
     *
     * <pre>
     * on the way to 2515, 67, -3054
     * the worker is at 2506, 68, -3051
     * the lot floor at 2515, 63, -3054 is Pedregulho
     * </pre>
     *
     * <p>Ele estava em <b>y=68</b>, em cima da própria obra, e o destino
     * era o <b>piso do lote, y=63</b>. Cinco blocos abaixo: um aldeão desce
     * degrau de um, e a navegação Vanilla não anda para uma queda dessas.
     * Ele ficava parado até o guarda devolver a tarefa, e a obra recomeçava
     * com outro construtor no mesmo lugar.
     *
     * <p><b>A saída é o patamar</b>, e não o piso: quando o pé da coluna
     * está longe demais de onde ele já está, o destino vira o degrau
     * intermediário que ele alcança — ver {@link ClimbLimit#landingBetween}.
     * A passagem seguinte o leva mais um degrau, e assim por diante.
     *
     * <p>O chão continua sendo o destino quando ele está no chão, que é o
     * caso comum e o que o {@code standingSpotNear} já resolvia para a
     * duna e a depressão.
     */
    static BlockPos footOf(
            ServerWorld world, ConstructionProject project, BlockPos target, BlockPos worker) {

        BlockPos ground = footOf(world, project, target);

        if (ClimbLimit.reachableFrom(worker.getY(), ground.getY())) {
            return ground;
        }

        // Longe demais na vertical: o destino é o patamar que ele alcança,
        // na mesma coluna do pé. Subir um degrau de cada vez é o que a
        // navegação sabe fazer.
        BlockPos landing = new BlockPos(
                ground.getX(),
                ClimbLimit.landingBetween(worker.getY(), ground.getY()),
                ground.getZ());

        return standingSpotNear(world, landing).orElse(landing);
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
     *
    /** Onde um aldeão atravessa: caixa de colisão vazia. */
    public static boolean passable(ServerWorld world, BlockPos at) {
        return world.getBlockState(at).getCollisionShape(world, at).isEmpty();
    }

    /**
     * Dois blocos livres sobre chão que segura um aldeão.
     *
     * <p><b>O chão era {@code isSolidBlock}</b>, que quer dizer cubo
     * cheio e opaco — e por isso ninguém ficava de pé num degrau. O que
     * um aldeão precisa embaixo do pé é alguma coisa com colisão, e é o
     * que se pergunta desde 2026-09-05.
     */
    public static boolean standable(ServerWorld world, BlockPos at) {
        BlockPos floor = at.down();

        return !world.getBlockState(floor).getCollisionShape(world, floor).isEmpty()
                && passable(world, at)
                && passable(world, at.up())
                && isDry(world, at);
    }

    /**
     * Um lugar de ficar de pé não pode estar alagado — P1.2, 2026-09-17.
     *
     * <p><b>Água tem caixa de colisão vazia</b>, e é por isso que ela
     * passava por {@link #passable}: para o Vanilla ela não obstrui, mas
     * para um aldeão que precisa <b>parar ali e trabalhar</b> ela é
     * exatamente o que o impede.
     *
     * <p><b>O defeito, medido.</b> Playtest de 2026-09-17: <b>trinta</b>
     * desistências com a frase {@code the place to stand is …, which is
     * flooded}, e nelas o aldeão estava a <b>onze blocos</b> do alvo,
     * andando, até o guarda de imobilidade expirar em trezentos tiques e
     * devolver a tarefa. Perdia-se o expediente inteiro para descobrir no
     * fim o que dava para saber na escolha.
     *
     * <p><b>Por que aqui, e não num guarda novo.</b> O mod já sabia:
     * {@code MinerReport} imprimia <i>"which is flooded"</i> na linha da
     * desistência — a informação existia e era jogada fora. O conserto é
     * fazer a escolha usar o que o diagnóstico já dizia.
     *
     * <p><b>E o conserto de 2026-09-03 continua, porque é outro caso.</b>
     * {@code MineFlooding.seal} tapa a fonte que a <b>picareta</b> abriu,
     * e no mesmo log ele agiu cinco vezes, com acerto. O que ele não
     * alcança é a água que já estava lá antes de o aldeão chegar: ela
     * nunca vira {@code seal} porque a picareta não chega a bater. Um
     * cobre um caso em seis; juntos cobrem os dois.
     *
     * <p>Lava entra pela mesma porta — {@code getFluidState} não
     * distingue —, e é o que o autor pediu ao citar <i>"água ou lava"</i>.
     *
     * <p><b>Só o bloco dos pés, e a camada da cabeça fica de fora</b> —
     * e isto foi medido, não escolhido. A primeira versão perguntava
     * pelos dois, e o
     * {@code theMinerGoesDownToTheStoneInsteadOfDiggingItFromAbove}
     * reprovou numa rodada em três; sem a mudança, o mesmo teste deu
     * quatro verdes em quatro. Num túnel de dois blocos a camada de cima
     * é a da cabeça, e água ali não impede o aldeão de ficar de pé e
     * bater a picareta — exigi-la seca recusava lugar bom e mandava o
     * mineiro procurar outro que não existia.
     */
    public static boolean isDry(ServerWorld world, BlockPos at) {
        return world.getBlockState(at).getFluidState().isEmpty();
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
