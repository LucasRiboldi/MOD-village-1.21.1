package com.villagecolony.core.construction.model;

/**
 * Até onde o construtor sobe e desce sozinho — 2026-09-16.
 *
 * <p><b>Pedido do autor:</b> <i>"pode melhorar para facilitar que o
 * trabalhador consiga subir nos blocos conforme a construção suba, não
 * fazer uma diferença maior de 2 blocos, o que impede o aldeão de subir e
 * continuar construindo"</i>.
 *
 * <p><b>O que o log de 02:58 mostrou.</b> A obra progredia — de 61 para 37
 * blocos restantes — e nenhuma casa terminava. O construtor travava com
 * <i>"has not moved a block in 300 ticks"</i>, e as três linhas do relato
 * contam a história inteira: ele estava em <b>y=68</b>, em cima da própria
 * obra, e o destino calculado era o <b>piso do lote, y=63</b>. Cinco
 * blocos abaixo. Um aldeão desce degrau de um; cinco é queda, e a
 * navegação Vanilla não anda para lá.
 *
 * <p><b>Dois é a régua, e é do autor.</b> Coincide com o que o jogo
 * permite: o aldeão sobe um degrau por vez e desce dois sem dano.
 *
 * <p>Mora no Core e se afirma sem mundo: é comparação de duas alturas, e
 * é o tipo de conta que erra calada — um sinal trocado manda o construtor
 * para o lugar errado e o log só diz que ele não andou.
 */
public final class ClimbLimit {

    /**
     * Quantos blocos de desnível o construtor vence sozinho.
     *
     * <p>Decisão do autor, 2026-09-16. Acima disto ele precisa de ajuda —
     * e é onde o andaime entra, quando a obra não oferece patamar.
     */
    public static final int STEP = 2;

    private ClimbLimit() {
    }

    /**
     * Se quem está em {@code from} alcança a altura {@code to}.
     *
     * <p>A mesma régua para cima e para baixo. Subir é mais caro que
     * descer no jogo, mas a diferença não muda a resposta dentro de dois
     * blocos — e uma régua só é uma régua que não se contradiz.
     */
    public static boolean reachableFrom(int from, int to) {
        return Math.abs(to - from) <= STEP;
    }

    /**
     * O patamar seguinte entre quem está em {@code from} e o alvo em
     * {@code to}.
     *
     * <p>É o que a construção em camadas usa: em vez de mandar o
     * construtor ao piso do lote — que é o que travava —, manda-o ao
     * degrau mais alto que ele alcança na direção do alvo.
     *
     * @return o próprio alvo quando ele já está ao alcance
     */
    public static int landingBetween(int from, int to) {
        if (reachableFrom(from, to)) {
            return to;
        }

        return to > from ? from + STEP : from - STEP;
    }
}
