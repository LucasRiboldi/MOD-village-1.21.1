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
     */
    private static final Map<BlockPos, Long> REJECTED = new HashMap<>();

    /**
     * Quanto tempo um grupo de troncos fica marcado como "não é árvore".
     *
     * <p>Dez ciclos da colônia, o mesmo prazo de {@link #UNREACHABLE_MEMORY}
     * e pelo mesmo motivo: é tempo bastante para a busca não reencontrar
     * a mesma parede a cada passagem, e curto bastante para o jogador
     * ver o mod mudar de ideia dentro da mesma sessão.
     */
    private static final int REJECTED_MEMORY = 10 * VillageDetector.CYCLE_TICKS;

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
        // O prazo do castigo é um; o da contagem é outro, e mais longo.
        // Ver TALLY_MEMORY: é ele que faz a segunda recusa custar mais
        // que a primeira em vez de recomeçar do zero.
        UNREACHABLE.values().removeIf(
                refusal -> world.getTime() - refusal.since() >= TALLY_MEMORY);

        // E as recusas de "não é árvore", pela Regra 23: o jogador
        // planta uma muda ao lado do pilar, e o que era construção passa
        // a ser floresta.
        REJECTED.values().removeIf(
                since -> world.getTime() - since >= REJECTED_MEMORY);
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
        if (REJECTED.size() + trunk.size() > MAX_REJECTED) {
            REJECTED.clear();
        }

        for (BlockPos log : trunk) {
            REJECTED.put(log.toImmutable(), world.getTime());
        }

        VillageColonyMod.LOGGER.info(
                "Not a tree at {} — {} logs without a living canopy,"
                        + " skipping it for {} ticks",
                trunk.isEmpty() ? "?" : trunk.get(0).toShortString(),
                trunk.size(),
                REJECTED_MEMORY);
    }

    /**
     * Se este grupo de troncos ainda está marcado como "não é árvore".
     *
     * <p>Tira a marca vencida ao perguntar, do mesmo jeito que
     * {@link #isOutOfReach} faz — quem reencontra o lugar é quem paga
     * por limpá-lo.
     */
    static boolean isRejected(ServerWorld world, BlockPos log) {
        Long since = REJECTED.get(log);

        if (since == null) {
            return false;
        }

        if (world.getTime() - since >= REJECTED_MEMORY) {
            REJECTED.remove(log);

            return false;
        }

        return true;
    }
}
