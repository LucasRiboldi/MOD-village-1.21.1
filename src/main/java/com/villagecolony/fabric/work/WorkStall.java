package com.villagecolony.fabric.work;

import com.villagecolony.fabric.brain.WorkHours;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * O trabalhador não sai do lugar — 2026-09-03.
 *
 * <p><b>Toda profissão que anda tem o mesmo guarda, e ele é cego do mesmo
 * jeito.</b> O contador de travamento conta tique de expediente
 * <i>indo até o alvo</i>, e nunca pergunta se o aldeão andou: um
 * trabalhador congelado paga os dois minutos inteiros antes de a tarefa
 * voltar para a fila.
 *
 * <p>Isto nasceu no mineiro, onde o preço estava medido: <i>"seis vezes a
 * mesma frase, dois minutos de expediente cada, e zero pedra em dezessete
 * minutos"</i>. Mas o defeito nunca foi dele — é do <b>desenho</b>, que é
 * o do lenhador e que as sete profissões compartilham. Construtor,
 * lenhador, fazendeiro, pastor e fabricante andam até um alvo e têm
 * exatamente o mesmo buraco.
 *
 * <p><b>E congelado é a assinatura.</b> Não "andando devagar", não "quase
 * lá" — parado no mesmo bloco, com destino posto:
 *
 * <pre>
 * he is at 718, 44, 878, walking to 718, 44, 878
 * </pre>
 *
 * <p>É a lição do {@code mineBlock} do AnimaFabric — <i>reports failure if
 * the block is not broken</i> — aplicada ao passo anterior: <b>conferir
 * que a ação surtiu efeito</b>, em vez de esperar o orçamento acabar.
 *
 * <p><b>Por que uma classe e não um campo em cada Job.</b> São duas
 * perguntas que precisam ser feitas juntas — <i>é expediente?</i> e
 * <i>ele saiu do lugar?</i> —, e soltas elas se separam: o pastor conta
 * travamento fora do expediente desde sempre justamente porque a primeira
 * ficou por conta de cada profissão lembrar. Aqui não há como esquecer:
 * quem não é expediente não conta, por construção.
 *
 * <p>Sem mapa estático e sem limpeza: cada {@code Job} tem o seu, e ele
 * morre junto com o trabalho. Morte, zumbificação e dispensa já derrubam
 * o {@code Job}, e não precisam saber que isto existe.
 */
public final class WorkStall {

    /**
     * Quantos tiques de expediente parado no mesmo bloco antes de desistir.
     *
     * <p>Trezentos — quinze segundos —, e não menos: aldeão para de
     * verdade. Porta, outro aldeão na passagem, o recálculo da rota.
     * Folgado para todos esses e ainda assim <b>oito vezes</b> mais rápido
     * que os dois minutos do guarda de travamento, que continua existindo
     * como teto para quem <b>anda</b> sem chegar — oscilar entre dois
     * blocos mexe este contador e não escapa daquele.
     */
    public static final int LIMIT = 300;

    private BlockPos wasAt;

    private int still;

    /**
     * Mais um tique, e se já está parado tempo demais.
     *
     * <p>Chamada no ramo em que o trabalhador <b>deveria estar andando</b>
     * — fora de alcance, com destino posto. Chamá-la enquanto ele trabalha
     * puniria quem está parado de propósito, que é o aldeão batendo na
     * pedra.
     *
     * @return true quando ele não muda de bloco há {@link #LIMIT} tiques
     *     de expediente
     */
    public boolean stuck(ServerWorld world, VillagerEntity villager) {
        if (!WorkHours.isWorkTime(world, villager)) {
            // Fora da hora a GoToWorkTargetTask nem começa, então ele está
            // PROIBIDO de andar. Punir quem não pode andar é queimar o
            // orçamento com ele dormindo — a sessão de 2026-08-26 viu o
            // contador do mineiro ir de 886 a 2086 com o relatório dizendo
            // "off hours".
            return false;
        }

        BlockPos now = villager.getBlockPos();

        if (now.equals(wasAt)) {
            still++;
        } else {
            wasAt = now;
            still = 0;
        }

        return still >= LIMIT;
    }

    /**
     * Ele trabalhou: a contagem recomeça.
     *
     * <p><b>Alvo novo NÃO é motivo</b> — E36, 2026-09-04. A pergunta que
     * esta classe faz é <i>o aldeão saiu do bloco?</i>, e a resposta não
     * muda quando o alvo muda: quem estava congelado continua congelado
     * depois de a pedra à frente dele sumir. Enquanto o mineiro, o
     * fazendeiro e o pastor zeravam ao pegar e ao largar alvo, quem
     * trocava de alvo com frequência ficava <b>imune</b> aos dois
     * guardas — {@code stall 0/2400, still 0/300} por vinte e cinco
     * minutos, com nenhum passo dado, na sessão de 09-04.
     *
     * <p>É a forma de erro que a pergunta 20 desta casa já nomeia:
     * pendurar a limpeza num <b>momento</b> em vez de conferir uma
     * <b>invariante</b>. Sobram dois motivos, e os dois são prova de que
     * ele não está congelado:
     *
     * <ul>
     *   <li><b>ele andou</b> — o {@link #stuck} vê sozinho, e não precisa
     *       que ninguém o avise;
     *   <li><b>ele trabalhou</b> — esta chamada, feita no ramo em que a
     *       profissão age sobre o alvo. É onde o {@code BuilderWork} e o
     *       {@code ManufacturerWork} sempre zeraram, e foi por isso que
     *       os dois nunca tiveram o E36.
     * </ul>
     */
    public void reset() {
        wasAt = null;
        still = 0;
    }

    /**
     * Há quantos tiques de expediente ele não sai do bloco.
     *
     * <p>Para o relatório do ciclo. O guarda só fala quando estoura, e a
     * pergunta que ele responde — <i>ele está andando?</i> — não tem outro
     * observável de fora.
     */
    public int ticks() {
        return still;
    }
}
