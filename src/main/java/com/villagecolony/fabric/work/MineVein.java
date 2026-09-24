package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.MineArm;
import com.villagecolony.core.construction.model.MineShaft;
import com.villagecolony.core.construction.service.MineRecovery;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.Side;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.core.coordination.IdleReason;
import com.villagecolony.fabric.integration.BlockProtection;
import com.villagecolony.fabric.integration.MineFlooding;
import com.villagecolony.fabric.integration.MineLighting;
import com.villagecolony.fabric.integration.MineMouth;
import com.villagecolony.fabric.integration.OreVein;
import com.villagecolony.fabric.integration.RingSweep;
import com.villagecolony.fabric.integration.StonePatch;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * A pedra exposta na superfície e o veio seguido pelo braço da mina, com a subida de volta e o patamar para ficar de pé — separado de
 * {@link MineDigging} em 2026-09-24, quando ele passou de 500 linhas. Os
 * comentários vieram junto sem mudança.
 */
final class MineVein {

    private MineVein() {
    }

    /**
     * A pedra de superfície, quando a mina não tem onde nascer.
     *
     * <p><b>É alternativa, e não substituto.</b> A escada continua sendo
     * o caminho: é ela que traz carvão e ferro, e ela rende mais. Isto só
     * roda quando as vinte e quatro colunas da boca falharam — e o que
     * ele evita é o que a sessão de 2026-08-25 mostrou: uma vila cercada
     * de água ficou sem pedra nenhuma, a obra morreu de fome esperando
     * pedregulho, e a colônia parou de crescer por causa do terreno em
     * volta.
     *
     * <p>Mesma espiral da areia, mesmo teto por passagem, e a distinção
     * que o log precisa: "não terminei de olhar" não é "não há".
     */
    static Optional<BlockPos> exposedStone(
            ServerWorld world, UUID workerId, UUID colonyId, BlockPos center) {

        Optional<BlockPos> found = RingSweep.around(
                workerId,
                center,
                MineDigging.surfaceRadius,
                // <b>E a marca vale aqui também</b> — E44, 2026-09-10, e
                // este era o buraco que o verificador achou: o giveUp
                // marca TODA pedra largada, inclusive a de superfície,
                // mas só o lado da escada perguntava pela marca. Uma
                // pedra exposta do outro lado da água reproduzia o E44
                // inteiro numa colônia sem boca de mina — mesmo alvo,
                // mesma desistência, todo ciclo.
                column -> StonePatch.in(world, column, center.getY())
                        .filter(stone -> !MineMarks.isUnreachableAround(world, stone)));

        if (found.isEmpty()) {
            // Pelo recordAt, como a areia — 2026-09-11. Este é o irmão
            // da busca de areia e tem o mesmo desenho: roda por tique, e
            // a varredura em anéis alterna pausada e completa a cada
            // volta. Ele não apareceu na enxurrada das 02:03 porque
            // aquela colônia tinha boca de mina — o que é sorte, e não
            // defesa.
            IdleLog.recordAt(
                    colonyId,
                    MineDigging.SURFACE_SUBJECT,
                    RingSweep.pausedAt(workerId).isPresent()
                            ? IdleReason.SWEEP_INCOMPLETE
                            : IdleReason.NO_TARGET,
                    "no mine mouth, and no exposed stone within "
                            + MineDigging.surfaceRadius + " blocks either",
                    world.getTime());

            return Optional.empty();
        }

        IdleLog.clear(colonyId, MineDigging.SURFACE_SUBJECT);

        return found;
    }

    /**
     * O minério colado no que acabou de sair, se a veia continuar.
     *
     * <p><b>A veia manda no túnel.</b> Minério não vem sozinho, e voltar
     * para a escada com metade da veia aberta faria o aldeão andar até lá
     * outra vez na passagem seguinte. Enquanto houver minério ao lado do
     * último, é ele o alvo.
     *
     * <p>Quando acabar, a memória da veia sai e o túnel volta a mandar —
     * senão o mineiro reperguntaria por ela a cada passagem, para sempre.
     */
    static Optional<BlockPos> followingTheVein(ServerWorld world, MineArm arm) {
        Optional<BlockPos> from = arm.vein().map(MinecraftTypeAdapter::toBlockPos);

        if (from.isEmpty()) {
            return Optional.empty();
        }

        Optional<BlockPos> more = OreVein.beside(world, from.get());

        if (more.isEmpty()) {
            arm.veinExhausted();

            return Optional.empty();
        }

        // A mesma guarda do MineCuts.nextCut, e aqui ela é a que fecha o laço —
        // 2026-09-03. Este método roda ANTES do túnel a cada passagem, e
        // a veia mora no Mine, que é da colônia: um minério sem lugar de
        // onde bater era servido de novo, e de novo, e ao mineiro
        // seguinte também. O MineTrouble.couldNotReach não alcançava o caso — ele só
        // recua o cursor do túnel, e diz por escrito que é "silencioso
        // quando a pedra não era do túnel — veio, areia".
        //
        // O resultado em jogo era a colônia inteira parada num bolsão de
        // carvão dentro da rocha: dezessete minutos, zero pedra.
        //
        // Desistir da veia é a saída barata, e é a que o stepBackUp já
        // escolhe logo abaixo — <i>a colônia prefere perder o minério a
        // perder o mineiro</i>. O túnel volta a mandar, e ele reabre o
        // caminho até este mesmo minério pelo lado de onde se alcança.
        //
        // <b>E a pedra de castigo entra por esta mesma porta</b> — E44,
        // 2026-09-10. O MineTrouble.couldNotReach larga a veia quando a pedra
        // recusada É a veia; o que ele não alcança é o minério VIZINHO
        // que já recusou noutra passagem, e é ele que este método serve.
        // Sem esta linha o laço voltaria pelo lado do minério, que é
        // justamente por onde ele voltou em 2026-09-03.
        if (nowhereToStand(world, more.get())
                || MineMarks.isUnreachableAround(world, more.get())) {
            arm.veinExhausted();

            return Optional.empty();
        }

        if (more.get().getY() < from.get().getY()) {
            Optional<BlockPos> step = stepBackUp(world, from.get());

            if (step.isEmpty()) {
                // Sem degrau possível não se desce. A colônia prefere
                // perder o minério a perder o mineiro — a escada volta a
                // mandar, e ela é subível por construção.
                arm.veinExhausted();

                return Optional.empty();
            }

            if (!step.get().equals(from.get())) {
                // E o degrau é alvo como qualquer outro: se não há de
                // onde bater nele, ele trava a veia do mesmo jeito que o
                // minério travaria — 2026-09-03.
                if (nowhereToStand(world, step.get())) {
                    arm.veinExhausted();

                    return Optional.empty();
                }

                // O degrau primeiro, e o veio NÃO avança: a passagem
                // seguinte acha o mesmo minério com a saída pronta.
                return step;
            }
        }

        arm.followVein(MinecraftTypeAdapter.toColonyPos(more.get()));

        return more;
    }

    /**
     * O bloco que falta abrir para se voltar de um degrau abaixo —
     * decisão do autor, 2026-08-27.
     *
     * <p><b>Por que o veio precisa disto e a escada não.</b> A escada da
     * Regra 29 abre três blocos por degrau desde 08-27, e sobe-se por
     * ela na mesma geometria em que se desce. O veio não tem geometria:
     * {@link OreVein#beside} olha as seis faces, e a de baixo é a
     * primeira da lista. Minério empilhado abre um poço de um bloco de
     * largura, e de poço não se sobe — o aldeão não pula dois.
     *
     * <p><b>Qual bloco falta é sempre o mesmo:</b> o teto do nível de
     * onde ele veio. Subir um degrau pede dois blocos de ar no nível de
     * destino, e o de baixo já é o minério recém-tirado; o de cima é
     * este. Com ele aberto, a subida se faz um degrau de cada vez até a
     * boca do poço.
     *
     * @param from o minério de onde o veio parte — o nível ao qual o
     *     mineiro precisa conseguir voltar
     * @return o bloco a abrir; o próprio {@code from} quando já dá para
     *     subir; vazio quando não há degrau possível e portanto não se
     *     deve descer
     */
    static Optional<BlockPos> stepBackUp(ServerWorld world, BlockPos from) {
        BlockPos ceiling = from.up();

        if (world.getBlockState(ceiling).isAir()) {
            return Optional.of(from);
        }

        // Rocha, e não só "cavável": uma laje que o jogador pôs de teto
        // passa no canDig e não é degrau nenhum — 2026-09-05.
        return MineRock.isDiggableRock(world, ceiling)
                ? Optional.of(ceiling)
                : Optional.empty();
    }

    /**
     * Se não há de onde bater nesta pedra — 2026-09-03.
     *
     * <p><b>Uma pergunta só, num lugar só.</b> O
     * {@link MinerWork#approachTo} devolve <i>a própria pedra</i> quando
     * não acha vizinho onde um aldeão caiba de pé, e essa igualdade é a
     * resposta — escrita à mão em três lugares, ela seria a próxima a
     * discordar de si mesma, que é a falha que o {@code standable} já
     * teve em 2026-08-28.
     *
     * <p>Toda posição que vira alvo do mineiro passa por aqui: a do
     * túnel, a do minério colado nela, o minério da veia e o degrau de
     * volta. Alvo que não passa é alvo que custa dois minutos de
     * expediente e devolve a tarefa.
     *
     * <p>Barato desde que as posições de aproximação vêm ordenadas por
     * distância — ver {@link MinerReach#APPROACH_OFFSETS}. A varredura
     * completa só é paga quando a resposta é <b>sim</b>.
     */
    static boolean nowhereToStand(ServerWorld world, BlockPos at) {
        return MinerApproach.approachTo(world, at).equals(at);
    }
}
