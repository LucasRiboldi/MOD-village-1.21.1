package com.villagecolony.fabric.integration;

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

    /** Quantas colunas uma passagem pode olhar. */
    public static final int MAX_COLUMNS = 1024;

    /** Onde cada dono parou. A chave é ele, nunca o lugar. */
    private static final Map<UUID, Integer> NEXT_RING = new HashMap<>();

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

        int columns = 0;

        int startRing = NEXT_RING.getOrDefault(owner, 0);

        if (startRing > radius) {
            startRing = 0;
        }

        for (int ring = startRing; ring <= radius; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {

                    if (Math.abs(dx) != ring && Math.abs(dz) != ring) {
                        dz = ring - 1;

                        continue;
                    }

                    if (++columns > MAX_COLUMNS) {
                        NEXT_RING.put(owner, ring);

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
        return Optional.ofNullable(NEXT_RING.get(owner));
    }

    /** Esquece o cursor de um dono só. */
    public static void forget(UUID owner) {
        NEXT_RING.remove(owner);
    }
}
