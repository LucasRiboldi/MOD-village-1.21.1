package com.villagecolony.fabric.integration;

import com.villagecolony.core.type.ServerMemory;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * Varrer em anéis a partir de um centro, com teto e cursor.
 *
 * <p>Nasceu em 2026-08-20, e não é invenção: é o desenho que
 * {@code TreeScanner} e {@code BuildSiteScanner} já usavam cada um do seu
 * jeito, escrito uma vez. O mineiro seria o terceiro, e três cópias de
 * uma espiral com orçamento é onde um defeito passa a morar em duas
 * delas e não na terceira.
 *
 * <p><b>Três decisões que a espiral carrega</b>, e cada uma custou uma
 * sessão para ser aprendida:
 *
 * <ul>
 *   <li><b>Só a casca do anel.</b> Percorrer o miolo descartando
 *       iterava mais de um milhão de posições para olhar quatro mil
 *       colunas;
 *   <li><b>Teto por passagem.</b> Uma varredura de raio 64 são dezesseis
 *       mil colunas, e fazê-las num tique é travar o servidor;
 *   <li><b>Cursor por dono, e não por posição.</b> Foi o defeito de
 *       2026-08-20: o centro da colônia troca de âncora a cada trinta
 *       segundos, e um cursor guardado pela posição recomeçava do zero
 *       toda vez. A busca nunca passava das mil primeiras colunas.
 * </ul>
 */
public final class RingSweep {

    static {
        ServerMemory.register(RingSweep.class, RingSweep::clearAll);
    }

    /** Quantas colunas uma passagem pode olhar. */
    public static final int MAX_COLUMNS = 1024;

    /**
     * Onde cada dono parou. A chave é ele, nunca o lugar.
     *
     * <p><b>E é a coluna, não só o anel — 2026-08-25.</b> Guardava só o
     * anel, e a passagem seguinte recomeçava do primeiro bloco dele:
     * numa casca de quinhentas colunas, metade do orçamento gasto para
     * chegar de volta ao que a passagem anterior já tinha respondido. É
     * o mesmo defeito que o {@code BuildSiteScanner} tinha, corrigido no
     * mesmo dia e pelo mesmo motivo.
     */
    private static final Map<UUID, Sweep> NEXT_RING = new HashMap<>();

    /**
     * Onde uma varredura parou: o anel, e a coluna na casca dele.
     *
     * @param ring o anel em que o orçamento acabou
     * @param column a posição, nessa casca, da primeira coluna que
     *     <b>não</b> chegou a ser olhada
     */
    private record Sweep(int ring, int column) {
    }

    private RingSweep() {
    }

    /** Esquece os cursores. Chamado ao parar o servidor. */
    public static void clearAll() {
        NEXT_RING.clear();
    }

    /**
     * A primeira coluna que {@code test} aceitar, a partir do centro.
     *
     * <p>Vazio não quer dizer "não existe": pode ser "o orçamento desta
     * passagem acabou". Quem precisa distinguir as duas coisas pergunta a
     * {@link #pausedAt}, e a diferença importa — dizer "não há" quando se
     * quer dizer "não terminei de olhar" é o log mentindo justamente no
     * caso que ele existe para explicar.
     *
     * @param owner de quem é esta busca. Colônia ou trabalhador, e nunca
     *     a posição do centro
     */
    public static <T> Optional<T> around(
            UUID owner, BlockPos center, int radius, Function<BlockPos, Optional<T>> test) {

        return around(owner, center, radius, column -> true, test);
    }

    /**
     * O mesmo, pulando de graça a coluna que nem vale a pergunta —
     * 2026-09-16.
     *
     * <p><b>O defeito que isto corrige.</b> A coleta de terra e grama
     * procura <b>fora</b> da vila, num cone de 90° — é a Regra 3
     * protegendo a vila de ser escavada —, mas a varredura é um
     * <b>círculo</b>: das 9.409 colunas de um raio 48, só cerca de um
     * quarto cai no cone, e as outras gastavam orçamento para serem
     * descartadas lá dentro do {@code test}.
     *
     * <p>O log de 01:19 mostrou o preço: <b>24 de 26</b> ciclos sem coleta
     * diziam <i>"still sweeping — the budget ran out before an answer —
     * dirt"</i>, e as obras esperaram {@code dirt} 93 vezes e
     * {@code grass_block} 42 na semana — 78% de todas as esperas.
     *
     * <p><b>O filtro tem de ser barato</b>, e é o contrato desta porta: o
     * que entra aqui é aritmética de coordenada, nunca leitura de mundo.
     * Um filtro que lesse bloco só teria movido o custo de lugar. Ver
     * {@code FarthestVillageSector.isInSector}, que é a conta de dois
     * produtos escalares para a qual isto foi escrito.
     *
     * @param worth a pergunta barata: falso pula a coluna sem gastar
     *     orçamento nem ler o mundo
     */
    public static <T> Optional<T> around(
            UUID owner,
            BlockPos center,
            int radius,
            java.util.function.Predicate<BlockPos> worth,
            Function<BlockPos, Optional<T>> test) {

        int columns = 0;

        Sweep paused = NEXT_RING.get(owner);

        int startRing = paused == null || paused.ring() > radius ? 0 : paused.ring();

        int startColumn = startRing == 0 ? 0 : paused.column();

        for (int ring = startRing; ring <= radius; ring++) {

            // A posição desta coluna na casca, contando as que a passagem
            // anterior já respondeu: é a absoluta que se retoma.
            int inRing = 0;

            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {

                    if (Math.abs(dx) != ring && Math.abs(dz) != ring) {
                        dz = ring - 1;

                        continue;
                    }

                    int column = inRing++;

                    if (ring == startRing && column < startColumn) {
                        // Já respondida, e sair daqui não custa leitura
                        // nem gasta orçamento.
                        continue;
                    }

                    // A coluna que o chamador já sabe não servir sai
                    // aqui, antes do orçamento: é a diferença entre pagar
                    // por um círculo e pagar por um cone.
                    if (!worth.test(center.add(dx, 0, dz))) {
                        continue;
                    }

                    if (++columns > MAX_COLUMNS) {
                        NEXT_RING.put(owner, new Sweep(ring, column));

                        return Optional.empty();
                    }

                    Optional<T> found = test.apply(center.add(dx, 0, dz));

                    if (found.isPresent()) {
                        NEXT_RING.remove(owner);

                        return found;
                    }
                }
            }
        }

        // Varreu o raio inteiro sem achar. O cursor sai, e a próxima
        // passagem recomeça do centro: o mundo muda, e o que não havia
        // ontem pode haver amanhã. É a Regra 23.
        NEXT_RING.remove(owner);

        return Optional.empty();
    }

    /** Em que anel a busca deste dono parou por falta de orçamento. */
    public static Optional<Integer> pausedAt(UUID owner) {
        return Optional.ofNullable(NEXT_RING.get(owner)).map(Sweep::ring);
    }

    /** Esquece o cursor de um dono só. */
    public static void forget(UUID owner) {
        NEXT_RING.remove(owner);
    }
}
