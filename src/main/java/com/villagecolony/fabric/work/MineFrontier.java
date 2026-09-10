package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.MineArm;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.OptionalInt;

/**
 * Onde a galeria de fato acaba — a fronteira lida do mundo.
 *
 * <p><b>Saiu do {@code MineDigging} em 2026-09-10</b>, na terceira
 * extração da dívida daquele arquivo. Refatoração pura: nenhum corpo
 * mudou.
 *
 * <p><b>Os dois andam juntos porque um é a pergunta do outro.</b> O
 * {@link #findTheFrontier} varre a ordem de cavar procurando onde a rocha
 * começa, e quem responde bloco a bloco é o {@code isStillClosed} — que
 * por sua vez tem de concordar com o {@code MineRock.isOpenSpace} usado
 * pela escolha do alvo. Foi a discordância entre esses dois lados que
 * emudeceu a mina em 2026-09-05.
 *
 * <p><b>Um vizinho morto saiu no mesmo commit.</b> O
 * {@code backUpToTheRealFrontier} vivia aqui desde {@code dbb2c14},
 * privado e <b>sem um único chamador</b> — superado por este
 * {@code findTheFrontier} e esquecido. Cinquenta e uma linhas que nada
 * executava, e que ninguém veria: o compilador não reclama de método
 * privado sem uso.
 */
public final class MineFrontier {

    private MineFrontier() {
    }

    /**
     * Onde a galeria de fato acaba, lido do mundo — 2026-08-28.
     *
     * <p><b>A primeira posição ainda fechada, na ordem de cavar.</b> Ela
     * é conectada por construção: tudo o que vem antes já está aberto, e
     * a ordem é um caminho contínuo a partir da boca. É o que faz o
     * mineiro cavar sempre a partir de onde ele consegue estar.
     *
     * <p><b>Por que o recuo passo a passo não bastou</b> — sessão de
     * 2026-08-28, 00:14. Ele voltava até achar uma posição de onde dava
     * para bater, e o <b>túnel que o jogador cavou à mão</b> oferece
     * exatamente isso. Os dois mineiros ficaram parados no degrau 7 da
     * escada mirando uma lanterna a vinte e quatro blocos, dentro de um
     * bolsão que não se liga à escada por lugar nenhum:
     *
     * <pre>
     * the miner is at 725, 57, 898 ... the stone at 732, 45, 878 is Lanterna
     * </pre>
     *
     * <p>Lido do mundo, e não lembrado: é a mesma escolha que o baú da
     * boca e a marca do baú já faziam. O cursor gravado no save deixa de
     * poder mentir, e nenhum buraco solto engana a conta.
     *
     * <p>Posição que não se cava — bedrock, casa da vila — é pulada: ela
     * ficaria sendo a frente para sempre. Quem a trata é o
     * {@code blockedAgain}, que vira a galeria.
     *
     * <p><b>E ela procura de trás para frente</b> — 2026-09-02. A
     * premissa acima vale para um lado só: tudo o que vem <b>antes</b> da
     * frente está aberto, e não o contrário. Numa galeria já cavada, o
     * primeiro bloco fechado da ordem inteira é um resto solto dentro do
     * túnel — e o cursor recuava 83 passos até ele, passagem após
     * passagem, com o corredor à frente aberto. Ver
     * {@link Mine#frontierWhereRockBegins}.
     */
    static void findTheFrontier(ServerWorld world, MineArm arm) {
        OptionalInt frontier =
                arm.frontierWhereRockBegins(i -> isStillClosed(world, arm.shaft().positionAt(i)));

        if (frontier.isEmpty()) {
            return;
        }

        int step = frontier.getAsInt();
        BlockPos at = MinecraftTypeAdapter.toBlockPos(arm.shaft().positionAt(step));

        VillageColonyMod.LOGGER.info(
                "The gallery really ends at {} — the cursor was {} steps ahead of it",
                at.toShortString(),
                arm.cut() - step);

        arm.rewindTo(step);
    }

    /**
     * Se esta posição da ordem de cavar ainda é rocha que vale a picareta.
     *
     * <p>Ar, água, lava e a tocha da própria mina são espaço aberto; o
     * que não se cava — bedrock, casa da vila — não é frente, porque
     * ficaria sendo frente para sempre.
     *
     * <p><b>Uma tentativa de exigir cubo cheio saiu daqui em
     * 2026-09-05</b>, e vale ficar dita. A ideia era não picaretar a
     * escada que o jogador construiu — {@code digging Escadas de Tijolos
     * de Pedra} —, e ela apagou a mina inteira: a sessão seguinte não
     * teve uma linha de fronteira, de cursor recuado nem de picareta.
     *
     * <p>O motivo é que esta pergunta não é local. Ela alimenta o recuo
     * do cursor, e recuo e escolha do alvo <b>têm de concordar</b>: uma
     * posição que o {@link #nextCut} vai pular não pode ser a fronteira,
     * senão o cursor recua até ela toda passagem. Mexer num lado só troca
     * um defeito por outro maior.
     *
     * <p><b>Refeito no mesmo dia, e com as duas pontas juntas:</b> a
     * lista passou a ser uma — {@link #isOpenSpace} —, e é dela que este
     * método e o {@link #nextCut} tiram a resposta. A concordância deixou
     * de ser coincidência, e tem o par de testes que ela pedia:
     * {@code theMinerDoesNotDigThePlayersStaircase} para a escolha do
     * alvo e {@code thePlayersStepInTheDigOrderIsNotTheFrontier} para o
     * recuo. Um sem o outro passa com a mina quebrada.
     */
    private static boolean isStillClosed(ServerWorld world, ColonyPos position) {
        BlockPos at = MinecraftTypeAdapter.toBlockPos(position);

        if (!world.isInBuildLimit(at)) {
            return false;
        }

        // <b>E a pedra que está de castigo não é a frente</b> — E44,
        // 2026-09-10, e esta é a metade que o javadoc acima já exigia por
        // escrito: <i>"uma posição que o nextCut vai pular não pode ser a
        // fronteira, senão o cursor recua até ela toda passagem"</i>.
        //
        // Sem esta linha a marca não valeria nada: o nextCut pularia a
        // pedra, esta busca recuaria o cursor de volta para ela na
        // passagem seguinte, e o laço voltaria pela porta do recuo. É o
        // par de sempre, e ele tem o par de testes que pede.
        if (MineMarks.isOutOfReach(world, at)) {
            return false;
        }

        return MineRock.isDiggableRock(world, at);
    }
}
