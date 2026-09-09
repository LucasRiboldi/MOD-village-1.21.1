package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.service.VillageDetector;
import com.villagecolony.fabric.integration.TreeHarvester;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * O que a colônia decidiu não olhar de novo por enquanto.
 *
 * <p>Saiu de {@code LumberjackWork} em 2026-08-20, quando ele passou de
 * mil e duzentas linhas. É uma pergunta inteira e sem nada do trabalho
 * em si: <b>que árvore sai da escolha</b>, por quanto tempo, e por quê.
 * Nenhum método daqui conhece trabalhador, tarefa ou machado.
 *
 * <p>São duas marcas, e a diferença entre elas é o motivo:
 *
 * <pre>
 * REJECTED      "isto não é árvore" — pilar de tronco sem copa viva,
 *               que a Regra 3 manda não derrubar
 * UNREACHABLE   "não consigo chegar" — a Regra 9, e o guarda de
 *               travamento que a fecha
 * </pre>
 *
 * <p><b>As duas vencem</b>, e é a Regra 23 de 2026-08-19: o jogador
 * derruba a parede, planta a muda, deixa a copa crescer sobre o tronco
 * descascado. Marca que não vence é uma afirmação sobre o futuro do
 * mundo do jogador, e o mod não tem como fazer nenhuma.
 */
public final class TreeMarks {

    private TreeMarks() {
    }

    /** Esvazia as duas marcas. Chamado ao parar o servidor. */
    public static void clearAll() {
        REJECTED.clear();

        UNREACHABLE.clear();
    }

    /**
     * Os troncos que a regra da copa já recusou.
     *
     * <p>Sem isto a colônia trava. A busca é determinística a partir do
     * centro: se o tronco mais próximo é construção — casa de vila,
     * cabana, pilar —, a regra da copa devolve plano vazio, a busca
     * recomeça do centro no ciclo seguinte e acha o mesmo tronco. Para
     * sempre, e sem uma linha de log dizendo o quê.
     *
     * <p>Aconteceu com a vila de {@code 1109,730} em 2026-08-13:
     * dezesseis minutos em horário de trabalho, dois lenhadores, nenhuma
     * árvore, e uma floresta inteira ao alcance. O defeito nasceu junto
     * com a regra da copa, um dia antes.
     *
     * <p>Guarda o grupo inteiro, e não o tronco que a busca devolveu:
     * recusar de um em um faria uma parede de vinte e cinco troncos
     * custar vinte e cinco buscas.
     *
     * <p><b>A recusa envelhece</b>, desde 2026-08-19. Ela não envelhecia,
     * e o argumento escrito aqui era que "construção não vira árvore".
     * O argumento estava errado pelo lado do jogador: ele derruba a
     * parede, planta uma muda ao lado do pilar, deixa a copa crescer
     * sobre o tronco que ele havia descascado. O mundo muda, e o mod
     * ficava com uma opinião de trinta minutos atrás.
     *
     * <p>É a Regra 23 — <i>o que já foi analisado pode ser analisado de
     * novo</i> —, e agora esta marca é igual à de {@link #UNREACHABLE}:
     * guarda quando nasceu e esquece sozinha.
     *
     * <p><b>E o prazo dela cresce, desde 2026-09-09.</b> Ele era fixo em
     * dez ciclos, e a sessão de 09-06 mostrou o que isso custa: <b>925
     * linhas de {@code Not a tree} sobre 140 coordenadas</b> — cada
     * parede reavaliada sete vezes, exatamente de cinco em cinco minutos
     * —, 273 desistências por travamento e <b>48 árvores derrubadas</b>
     * na sessão inteira. Das amostras de estado, 749 tinham o lenhador
     * procurando e 93 cortando: sete por cento do expediente com machado
     * na mão.
     *
     * <p>O argumento da Regra 23 continua de pé, e é por isso que a
     * primeira recusa não mudou. O que ele não previa é que <b>pilar sem
     * copa não vira árvore sozinho</b>: o mundo muda quando o jogador
     * planta a muda, e não a cada cinco minutos. Reperguntar à mesma
     * parede na mesma cadência é pagar para reaprender o que não mudou.
     *
     * <p>Então a segunda recusa vale o dobro, e assim por diante até o
     * teto de {@link #memoryFor} — a mesma escada que {@link #UNREACHABLE}
     * subiu em 2026-09-02, pelo mesmo motivo e com o mesmo teto. O
     * jogador que planta a muda continua vendo o mod mudar de ideia; a
     * parede de sempre sai da frente por mais de uma hora.
     */
    private static final Map<BlockPos, Refusal> REJECTED = new HashMap<>();

    /**
     * Quantos troncos recusados se guarda antes de esquecer tudo.
     *
     * <p>Um teto, não uma regra: uma vila cercada de construção de
     * madeira encheria o conjunto sem limite. Esquecer tudo custa uma
     * busca perdida por grupo, e é melhor que crescer para sempre.
     */
    private static final int MAX_REJECTED = 4096;

    /**
     * As árvores que a navegação não entrega, e desde quando.
     *
     * <p>A Regra 9, de 2026-08-15: o aldeão sobe e desce o que for
     * preciso para alcançar o recurso, <b>de maneira que ao ir ele possa
     * voltar</b>. O autor decidiu a leitura estreita — só navegação, o
     * lenhador não põe nem tira bloco para chegar. Então árvore que o
     * caminho não alcança deixa de ser alvo.
     *
     * <p>Separado de {@link #REJECTED} de propósito, e é a diferença
     * entre "não é árvore" e "não dá para chegar agora". A primeira é
     * para sempre; a segunda não pode ser: o jogador constrói ponte,
     * abre porta, aplaina barranco, e a árvore volta a valer. Por isso
     * isto esquece sozinho — ver {@link #UNREACHABLE_MEMORY}.
     *
     * <p><b>Aprende tentando, e não prevendo.</b> A primeira versão disto
     * perguntava à navegação, antes de escolher, se havia caminho até a
     * árvore. Rodada contra a bateria, ela recusou seis árvores comuns:
     * {@code findPathTo} não responde de forma confiável para um aldeão
     * recém-posto no mundo, e a resposta errada é cara — árvore boa
     * descartada por cinco minutos. Quem sabe de verdade se dá para
     * chegar é o guarda de travamento, depois de dois minutos de horário
     * de trabalho tentando. Ver {@link #giveUp}.
     */
    private static final Map<BlockPos, Refusal> UNREACHABLE = new HashMap<>();

    /**
     * Quando a árvore recusou pela última vez, e quantas vezes já recusou.
     *
     * <p>A contagem é o que faltava em 2026-09-02: sem ela toda recusa
     * é a primeira, e o prazo nunca cresce.
     */
    private record Refusal(long since, int count) {
    }

    /**
     * Por quantos ticks uma árvore fica marcada como inalcançável.
     *
     * <p>Dez ciclos da colônia, cinco minutos. Longo o bastante para a
     * busca passar adiante em vez de reencontrar a mesma árvore a cada
     * ciclo, e curto o bastante para a ponte que o jogador acabou de
     * construir valer na mesma sessão.
     */
    private static final int UNREACHABLE_MEMORY = 10 * VillageDetector.CYCLE_TICKS;

    /** Quantas vezes o prazo pode dobrar. Além disso ele para de crescer. */
    private static final int MAX_DOUBLINGS = 3;

    /** E o teto em múltiplos do prazo base: oitenta ciclos, mais de uma hora. */
    private static final int MAX_MEMORY_FACTOR = 8;

    /**
     * Por quanto tempo se lembra <b>quantas vezes</b> a árvore recusou.
     *
     * <p>Mais longo que o maior castigo, e é o que faz a escada de
     * prazos existir: se a contagem morresse junto com o castigo, a
     * árvore voltaria a ser ré primária a cada volta, e o prazo nunca
     * passaria de dez ciclos — que é exatamente o laço que a sessão de
     * 2026-09-02 mediu.
     */
    private static final int TALLY_MEMORY = 2 * UNREACHABLE_MEMORY * MAX_MEMORY_FACTOR;

    /**
     * Quanto tempo fica de fora a árvore que já recusou tantas vezes —
     * 2026-09-02.
     *
     * <p><b>Um prazo só era curto demais para o que ele custa.</b> A
     * sessão de 2026-09-02 mediu a volta inteira: marcada às 19:18:36
     * por 6.000 ticks, a árvore de {@code 749, 63, 905} era de novo a
     * mais próxima nove minutos e trinta e quatro segundos depois, e
     * custou outros <b>dois minutos de expediente</b> — o
     * {@link #stallLimit} inteiro — para o lenhador reaprender o que já
     * sabia. Duas árvores, quatro tentativas, perto de metade do tempo
     * dos dois lenhadores da vila.
     *
     * <p>A primeira recusa continua valendo dez ciclos, e é decisão do
     * autor: o jogador constrói a ponte e vê o mod mudar de ideia na
     * mesma sessão. <b>A segunda é outra coisa</b> — é prova de que a
     * primeira não foi azar —, e por isso o prazo dobra a cada recusa.
     *
     * <p>Com teto, pela Regra 23: barranco aplainado vira floresta de
     * novo, e castigo maior que a vida do servidor não é castigo, é
     * esquecimento. Oito voltas do prazo base é mais de uma hora — tempo
     * de sobra para o jogador mudar o terreno.
     *
     * <p><b>Serve as duas marcas desde 2026-09-09</b>, e o prazo base é
     * o mesmo para ambas — {@link #UNREACHABLE_MEMORY} responde pelo
     * nome mais velho, e não só pelo que ele diz. {@link #REJECTED}
     * subiu a mesma escada pelo mesmo argumento: a segunda recusa é
     * prova de que a primeira não foi azar, e vale mais que ela.
     */
    static long memoryFor(int refusals) {
        if (refusals <= 0) {
            return 0;
        }

        long doubled = (long) UNREACHABLE_MEMORY << Math.min(refusals - 1, MAX_DOUBLINGS);

        return Math.min(doubled, (long) UNREACHABLE_MEMORY * MAX_MEMORY_FACTOR);
    }

    /**
     * Marca uma árvore como fora de alcance por ora.
     *
     * <p>Chamado de dois lugares, e os dois importam: da escolha, quando
     * a navegação já diz que não dá; e de {@link #giveUp}, quando o
     * lenhador andou dois minutos de horário de trabalho e não chegou.
     *
     * <p>O segundo é o que fecha o G2. Sem ele o guarda de travamento
     * soltava a tarefa, a busca reencontrava a mesma árvore — ela é a
     * mais próxima, e a busca é determinística — e o ciclo recomeçava
     * inteiro. Soltar a tarefa sem esquecer a árvore é trocar de
     * trabalhador, não de problema.
     *
     * <p>Público porque a bateria precisa chegar aqui. Chamar
     * {@link #giveUp} num teste custaria os 2.400 ticks de
     * {@link #STALL_LIMIT} — dois minutos de relógio contra uma bateria
     * que roda em vinte e cinco segundos, que é o E1 do grupo E.
     */
    public static void markUnreachable(ServerWorld world, BlockPos base) {
        forgetStaleMarks(world);

        Refusal before = UNREACHABLE.get(base);
        int count = before == null ? 1 : before.count() + 1;

        Refusal refusal = new Refusal(world.getTime(), count);

        // <b>O tronco inteiro, e não só a base</b> — 2026-09-05, e é o
        // que fazia o castigo não valer nada.
        //
        // O filtro da busca pergunta por <b>cada bloco de tronco</b> —
        // {@code TreeScanner.findNearestLog} varre logs, não árvores —, e
        // esta marca ficava num bloco só. O scanner achava o tronco um
        // bloco acima da base, que não estava marcado, e devolvia a mesma
        // árvore: a contagem subia porque a base é a mesma, e o prazo
        // nunca mordia.
        //
        // A sessão de 2026-09-05 mediu o preço: a árvore de
        // {@code 1460, 63, 79} foi recusada <b>sete vezes em três
        // minutos</b>, uma por ciclo da colônia, e cada volta custou os
        // 300 tiques do guarda de imobilidade. Os dois lenhadores da vila
        // terminaram cinco árvores em meia hora.
        //
        // O {@link #reject} nunca teve esse defeito: ele sempre marcou o
        // grupo inteiro. Esta passa a fazer o mesmo, e é de lá que a
        // forma vem.
        List<BlockPos> trunk = TreeHarvester.trunkOf(world, base);

        for (BlockPos log : trunk) {
            UNREACHABLE.put(log.toImmutable(), refusal);
        }

        // Árvore que já saiu do mundo entre a desistência e esta linha
        // não tem tronco a marcar, e a base ainda precisa carregar a
        // contagem — é ela que faz a recusa seguinte custar mais.
        UNREACHABLE.put(base.toImmutable(), refusal);

        VillageColonyMod.LOGGER.info(
                "Tree at {} is out of reach — refused {} times now,"
                        + " skipping its {} logs for {} ticks",
                base.toShortString(),
                count,
                Math.max(trunk.size(), 1),
                memoryFor(count));
    }

    /**
     * Tira do registro as marcas cujo prazo já passou.
     *
     * <p>{@link #isOutOfReach} também as tira, mas só quando alguém
     * pergunta por aquela árvore — e a busca só pergunta pelo que ela
     * reencontra. Árvore marcada num canto que a colônia nunca mais
     * visita ficaria no mapa enquanto o servidor vivesse.
     *
     * <p>É o teto que {@link #REJECTED} tem em {@link #MAX_REJECTED} e
     * que este mapa não tinha. Aqui sai mais barato: a marca já carrega
     * o instante em que nasceu, então dá para varrer por prazo em vez de
     * esquecer tudo ao encher.
     */
    static void forgetStaleMarks(ServerWorld world) {
        forgetStaleMarksAt(world.getTime());
    }

    /**
     * O mesmo, pelo relógio que quem chama entrega.
     *
     * <p><b>Separado do mundo de propósito</b>, e as três irmãs abaixo
     * também — {@link #rejectAt} e {@link #isRejectedAt}. Nenhuma
     * decisão desta classe precisa de um {@link ServerWorld}: o que ela
     * pergunta ao mundo é <b>que horas são</b>, e mais nada.
     *
     * <p>Sem isso a escada de prazos não teria como ser provada. A
     * bateria não avança o relógio do mundo — o castigo mais curto é de
     * 6.000 tiques, e nenhum {@code tickLimit} de gametest chega perto
     * —, então a única forma de medir "a segunda recusa dura mais que a
     * primeira" é entregar o relógio. Ver {@code TreeMarksTest}, que
     * vive neste pacote justamente para alcançar estes métodos.
     */
    static void forgetStaleMarksAt(long now) {
        // O prazo do castigo é um; o da contagem é outro, e mais longo.
        // Ver TALLY_MEMORY: é ele que faz a segunda recusa custar mais
        // que a primeira em vez de recomeçar do zero.
        UNREACHABLE.values().removeIf(
                refusal -> now - refusal.since() >= TALLY_MEMORY);

        // E as recusas de "não é árvore", pela Regra 23: o jogador
        // planta uma muda ao lado do pilar, e o que era construção passa
        // a ser floresta.
        //
        // Pelo prazo da contagem, e não pelo do castigo — desde
        // 2026-09-09 esta marca também sobe a escada, e vale para ela a
        // mesma razão escrita em TALLY_MEMORY.
        REJECTED.values().removeIf(
                refusal -> now - refusal.since() >= TALLY_MEMORY);
    }

    /**
     * Esquece as árvores fora de alcance. Só os testes precisam disso.
     *
     * <p>{@link #UNREACHABLE} é estático e vive enquanto o servidor
     * viver, o que em jogo é o certo — a colônia não deve reaprender a
     * cada ciclo que não chega naquele barranco. Numa bateria de testes
     * é o contrário: as áreas de teste são reaproveitadas, e uma posição
     * marcada por um teste reaparece como árvore boa no seguinte.
     */
    public static void forgetUnreachable() {
        UNREACHABLE.clear();
    }

    /**
     * Se esta árvore ainda está no prazo de esquecimento.
     *
     * <p><b>Público por causa do teste, como {@link #forgetUnreachable}.</b>
     * Quem usa em produção é o {@link TreeChoice}, do mesmo pacote. O
     * teste do guarda de travamento vive em {@code gametest} e precisa
     * perguntar isto para provar a segunda metade da Regra 9 — que o
     * guarda não só devolve a tarefa, mas <b>esquece a árvore</b>. O
     * javadoc daquele teste prometia as duas provas desde sempre e só
     * fazia a primeira; a segunda entrou em 2026-08-26.
     */
    public static boolean isOutOfReach(ServerWorld world, BlockPos base) {
        Refusal refusal = UNREACHABLE.get(base);

        if (refusal == null) {
            return false;
        }

        // A entrada fica mesmo depois de o castigo vencer, e é de
        // propósito: ela carrega a contagem, que é o que faz o castigo
        // seguinte ser maior. Quem a apaga é forgetStaleMarks.
        return world.getTime() - refusal.since() < memoryFor(refusal.count());
    }

    static void reject(ServerWorld world, List<BlockPos> trunk) {
        rejectAt(world.getTime(), trunk);
    }

    /** A recusa medida pelo relógio de quem chama — ver {@link #forgetStaleMarksAt}. */
    static void rejectAt(long now, List<BlockPos> trunk) {
        forgetStaleMarksAt(now);

        if (REJECTED.size() + trunk.size() > MAX_REJECTED) {
            REJECTED.clear();
        }

        // A contagem é do grupo, e vem do maior que algum tronco dele
        // carregue — não da primeira posição da lista.
        //
        // {@link TreeHarvester#trunkOf} devolve o grupo conectado a
        // partir do tronco que a busca achou, e a busca não acha o mesmo
        // bloco toda vez: {@code logInColumn} varre a coluna de baixo
        // para cima e devolve o primeiro tronco de <b>cada</b> coluna, de
        // modo que uma parede de vinte e cinco troncos entra por
        // qualquer um deles. Ler a contagem só de {@code trunk.get(0)}
        // faria a parede voltar a ser ré primária sempre que a busca
        // entrasse por outro canto, e a escada nunca subiria — que é o
        // mesmo defeito que markUnreachable teve até 2026-09-05.
        int before = 0;

        for (BlockPos log : trunk) {
            Refusal refusal = REJECTED.get(log);

            if (refusal != null) {
                before = Math.max(before, refusal.count());
            }
        }

        Refusal refusal = new Refusal(now, before + 1);

        for (BlockPos log : trunk) {
            REJECTED.put(log.toImmutable(), refusal);
        }

        VillageColonyMod.LOGGER.info(
                "Not a tree at {} — {} logs without a living canopy,"
                        + " refused {} times now, skipping it for {} ticks",
                trunk.isEmpty() ? "?" : trunk.get(0).toShortString(),
                trunk.size(),
                refusal.count(),
                memoryFor(refusal.count()));
    }

    /**
     * Se este grupo de troncos ainda está marcado como "não é árvore".
     *
     * <p><b>A marca vencida fica</b>, e é igual ao que
     * {@link #isOutOfReach} faz. Isto apagava a entrada ao ver o prazo
     * vencido, e apagar era o que impedia a escada de subir: a contagem
     * morre junto com o castigo, a parede volta a ser ré primária na
     * volta seguinte e o prazo nunca passa de dez ciclos. É o laço que
     * {@link #TALLY_MEMORY} descreve, e a sessão de 09-06 o mediu do
     * lado desta marca — sete recusas por parede, todas de dez ciclos.
     *
     * <p>Quem apaga é {@link #forgetStaleMarks}, no prazo mais longo da
     * contagem.
     */
    static boolean isRejected(ServerWorld world, BlockPos log) {
        return isRejectedAt(world.getTime(), log);
    }

    /** A pergunta medida pelo relógio de quem chama — ver {@link #forgetStaleMarksAt}. */
    static boolean isRejectedAt(long now, BlockPos log) {
        Refusal refusal = REJECTED.get(log);

        if (refusal == null) {
            return false;
        }

        return now - refusal.since() < memoryFor(refusal.count());
    }

    /**
     * Esquece as recusas de "não é árvore". Só os testes precisam disso,
     * como {@link #forgetUnreachable} — e pelo mesmo motivo: a marca é
     * estática, e um teste que recusa uma parede a deixaria recusada
     * para o teste seguinte.
     */
    static void forgetRejected() {
        REJECTED.clear();
    }
}
