package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.service.VillageDetector;
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
    private static final Map<BlockPos, Long> UNREACHABLE = new HashMap<>();

    /**
     * Por quantos ticks uma árvore fica marcada como inalcançável.
     *
     * <p>Dez ciclos da colônia, cinco minutos. Longo o bastante para a
     * busca passar adiante em vez de reencontrar a mesma árvore a cada
     * ciclo, e curto o bastante para a ponte que o jogador acabou de
     * construir valer na mesma sessão.
     */
    private static final int UNREACHABLE_MEMORY = 10 * VillageDetector.CYCLE_TICKS;

    /**
     * Anota que este grupo de tronco não é árvore.
     *
     * <p>A busca deixa de devolvê-lo, e o lenhador passa ao próximo. Ver
     * {@link #REJECTED} para o que acontece sem isto.
     */
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

        UNREACHABLE.put(base, world.getTime());

        VillageColonyMod.LOGGER.info(
                "Tree at {} is out of reach — skipping it for {} ticks",
                base.toShortString(),
                UNREACHABLE_MEMORY);
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
        UNREACHABLE.values().removeIf(
                since -> world.getTime() - since >= UNREACHABLE_MEMORY);

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

    /** Se esta árvore ainda está no prazo de esquecimento. */
    static boolean isOutOfReach(ServerWorld world, BlockPos base) {
        Long since = UNREACHABLE.get(base);

        if (since == null) {
            return false;
        }

        if (world.getTime() - since < UNREACHABLE_MEMORY) {
            return true;
        }

        UNREACHABLE.remove(base);

        return false;
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
