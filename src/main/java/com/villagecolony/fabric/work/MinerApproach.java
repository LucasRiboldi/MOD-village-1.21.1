package com.villagecolony.fabric.work;

import com.villagecolony.fabric.work.MinerWork.Job;
import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.ColonyEdits;
import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.MineArm;
import com.villagecolony.core.coordination.IdleReason;
import com.villagecolony.core.coordination.WorkAssignment;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskState;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceGroup;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.brain.WorkHours;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.fabric.integration.BlockBreakTime;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.MineFlooding;
import com.villagecolony.fabric.integration.OreVein;
import com.villagecolony.fabric.integration.MineMouth;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Onde o mineiro fica para alcançar a pedra, e o chão por onde ele anda até lá — separado de
 * {@link MinerWork} em 2026-09-24, quando ele passou de 500 linhas. Os
 * comentários vieram junto sem mudança.
 */
public final class MinerApproach {

    private MinerApproach() {
    }

    /**
     * Onde ficar de pé para bater nesta pedra — 2026-08-27.
     *
     * <p><b>Mandar o aldeão até a pedra era mandá-lo para dentro da
     * rocha.</b> Bloco sólido nunca é alcançável: a navegação devolve
     * caminho parcial, e ele estaciona onde parou.
     *
     * <p><b>Olhar só os vizinhos era pouco, e a Regra 29 é a prova.</b>
     * Um degrau da escada anda um para a frente e um para baixo:
     *
     * <pre>
     * degrau 1   (1, 64, 0)   onde ele está de pé
     * degrau 2   (2, 63, 0)   o alvo — DIAGONAL, não encosta em face nenhuma
     * </pre>
     *
     * <p>As seis faces não alcançam a diagonal, e o método caía no "fica
     * a própria pedra" já no segundo degrau. <b>E o aldeão alcançava o
     * tempo todo</b>: de pé no degrau 1 ele está a 1,1 bloco do centro do
     * degrau 2, e o braço dele é quatro. O lugar existia; a busca é que
     * não sabia procurá-lo.
     *
     * <p>Explica por que algumas sessões cavaram e outras não: a galeria
     * é reta, e blocos consecutivos dela <b>encostam</b>. Os onze blocos
     * da sessão das 22:23 foram todos de galeria; a escada e a frente do
     * túnel nunca saíram.
     *
     * <p><b>A busca é por distância, e não por ordem de face.</b> O
     * lugar mais perto do alvo é o que dá menos chance de o caminho ser
     * interrompido no meio. O cubo de raio quatro são umas seiscentas
     * leituras — caro para um tique, barato uma vez por pedra, e é uma
     * vez por pedra que ela roda: quem chama guarda o resultado.
     *
     * <p>Sem lugar nenhum ao alcance fica a própria pedra, que é o que
     * se fazia antes — pior destino, mas nunca pior que nenhum. Quem
     * trata esse caso é o guarda de travamento e o recuo da galeria.
     *
     * <p><b>"Cabe um aldeão" é uma pergunta só</b>, e quem responde é o
     * {@link BuilderApproach#standable}. Esta classe tinha a sua, mais
     * frouxa — pedia <i>qualquer coisa que não fosse ar</i> embaixo, e
     * água serve —, e a sessão da meia-noite as pegou discordando na
     * mesma linha de log: <i>"it was walking to 732,46,878, which is not
     * standable"</i>. Escolhedor e relator não podem responder diferente
     * à mesma pergunta; é a falha que a distância já tinha tido.
     *
     * <p><b>E a varredura inteira deixou de ser paga toda vez</b> —
     * 2026-09-03. O parágrafo acima se defendia dizendo que as
     * seiscentas leituras rodavam <i>uma vez por pedra</i>. Isso deixou
     * de ser verdade em 2026-09-02, quando a guarda de emparedada passou
     * a chamar este método de dentro do laço do {@code nextCut} — até
     * sessenta e quatro posições por passagem, e uma passagem por tique.
     * Seiscentas leituras viraram até trinta e oito mil por tique, e
     * este ciclo ainda estende a guarda ao minério.
     *
     * <p>As posições agora vêm prontas e <b>ordenadas por distância</b>
     * do {@link MinerReach#APPROACH_OFFSETS}, e a resposta é a primeira
     * que servir. Num corredor o vizinho colado responde na primeira ou
     * segunda leitura; a varredura completa só é paga quando a resposta
     * é <i>não há lugar nenhum</i>, que é o caso em que ela vale.
     *
     * <p>O bloco devolvido é <b>o mesmo de antes</b>: mesma conta de
     * distância, mesmo filtro do braço, e a ordenação é estável — entre
     * empatadas continua vencendo a primeira na ordem do laço antigo.
     */
    public static BlockPos approachTo(ServerWorld world, BlockPos target) {
        for (Vec3i offset : MinerReach.APPROACH_OFFSETS) {
            BlockPos at = target.add(offset);

            if (BuilderApproach.standable(world, at)) {
                return at;
            }
        }

        return target;
    }

    /**
     * O mesmo, sabendo de onde ele vem — E40, 2026-09-09.
     *
     * <p><b>O primeiro deslocamento da lista é em cima da própria
     * pedra</b>, a meio bloco, e ele ganha de todos os outros por
     * distância. Com o teto acima dela aberto — que numa mina acontece o
     * tempo todo — a resposta sai dali. Só que em cima da pedra é
     * <b>dois</b> acima de quem está de pé no chão ao lado dela, e aldeão
     * sobe um:
     *
     * <pre>
     * gave up the stone at 2427,48,-1437 — 2 blocks below it and unable to climb
     * </pre>
     *
     * <p>Três vezes em dois minutos na sessão de 09-09, sempre a mesma
     * pedra: a navegação não cumpre o destino, o guarda de imobilidade
     * devolve a tarefa, e o cursor da galeria segura a posição — como
     * deve, porque pular a pedra por uma desistência já custou três
     * sessões com a galeria intacta. O laço fecha aí, e quem o abre é
     * esta escolha.
     *
     * <p><b>O filtro é de um lado só.</b> Descer é de graça — aldeão cai
     * sem se machucar a esta altura, e a navegação desce —; subir é que
     * tem degrau de um. Por isso a conta é {@code at.getY() - villager}
     * contra {@link #CLIMB}, e não uma distância.
     *
     * <p><b>E não há resposta pior que a de antes.</b> Sem nenhum lugar
     * ao alcance dele, vale o primeiro pisável que a busca achou, que é
     * exatamente o que a sobrecarga de duas mãos devolve. O mineiro que
     * pergunta de longe — do alto da boca, com a galeria vinte blocos
     * abaixo — não muda de resposta: lá embaixo nada está acima dele.
     */
    public static BlockPos approachTo(
            ServerWorld world, BlockPos target, BlockPos villager) {

        BlockPos tooHigh = null;

        for (Vec3i offset : MinerReach.APPROACH_OFFSETS) {
            BlockPos at = target.add(offset);

            if (!BuilderApproach.standable(world, at)) {
                continue;
            }

            if (at.getY() - villager.getY() <= MinerWork.CLIMB) {
                return at;
            }

            // Guardado, e a busca segue: é a resposta de antes, para o
            // caso de não existir nenhuma que ele alcance.
            if (tooHigh == null) {
                tooHigh = at;
            }
        }

        return tooHigh != null ? tooHigh : target;
    }

    /**
     * Evita entregar à navegação uma perna acima do degrau que o aldeão
     * consegue subir.
     *
     * <p>A perna da mina normalmente é a boca ou uma posição da escada.
     * Quando o aldeão cai fora dela, porém, {@link MinerReach#legTowards}
     * pode devolver a boca três blocos acima. A navegação fica girando no
     * destino alto e o relatório registra exatamente o sintoma de E44:
     * {@code blocks below it and unable to climb}.
     *
     * <p>O destino intermediário é procurado só nesse caso excepcional. A
     * busca usa a mesma regra de lugar pisável do {@link #approachTo}, e a
     * próxima passagem pode avançar mais um degrau quando o aldeão chegar.
     */
    public static BlockPos climbableWalkTarget(
            ServerWorld world, BlockPos villager, BlockPos leg) {

        if (leg.getY() - villager.getY() <= MinerWork.CLIMB) {
            return leg;
        }

        BlockPos landing = approachTo(world, leg, villager);

        return landing.getY() - villager.getY() <= MinerWork.CLIMB
                && BuilderApproach.standable(world, landing)
                ? landing
                : leg;
    }

    /**
     * As duas perguntas da perna, respondidas pelo mundo de verdade.
     *
     * <p>Aqui, e não dentro do {@code MinerReach}, porque aquela classe é
     * geometria e não carrega fora do jogo — é o que permite afirmá-la sem
     * subir servidor. Quem tem o mundo é esta.
     *
     * <p>As duas saem do {@code BuilderApproach}, que é onde mora a conta
     * de "cabe um aldeão aqui" desde 2026-08-28. Uma conta só, e é a do
     * construtor.
     *
     * O que o mundo responde ao passo do mineiro.
     *
     * <p><b>Pública porque a bateria precisa da mesma.</b> O
     * {@code MinerGameTest} tinha uma cópia destas duas linhas, e em
     * 2026-09-05 a cópia ficou para trás: a correção da escada do jogador
     * entrou aqui e o teste do E32 continuou medindo o predicado antigo.
     * Teste que valida uma cópia da regra não valida a regra.
     */
    public static MinerLeg.Footing footingIn(ServerWorld world) {
        return new MinerLeg.Footing() {

            /**
             * <b>Ou o lugar é vazio, ou dá para ficar de pé em cima
             * dele</b> — 2026-09-05, e é a escada que o jogador constrói.
             *
             * <p>Era só "a caixa de colisão é vazia", e degrau tem
             * colisão. O autor trocou a descida da mina por uma escada de
             * tijolos de pedra e a colônia inteira parou na porta: o
             * corredor quebrava no primeiro degrau, o passo não achava
             * saída, e o desvio devolvia a boca — o bloco debaixo do pé
             * dele.
             *
             * <pre>
             * he is at 1436, 64, 81, walking to the mine mouth at 1436, 63, 81
             * </pre>
             *
             * <p>Treze desistências sem um passo dado.
             *
             * <p><b>A segunda metade é o que não deixa isto virar buraco
             * na rocha.</b> Pedra maciça no meio de uma coluna também tem
             * colisão, e ela continua sendo parede: em cima dela há mais
             * pedra, então não se fica de pé ali. O que passa são as
             * coisas que se sobe — degrau, laje —, porque acima delas
             * cabe um aldeão.
             */
            @Override
            public boolean passable(BlockPos at) {
                return BuilderApproach.passable(world, at)
                        || BuilderApproach.standable(world, at.up());
            }

            @Override
            public boolean standable(BlockPos at) {
                return BuilderApproach.standable(world, at);
            }
        };
    }
}
