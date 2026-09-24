package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

/**
 * Até onde uma obra pode ficar do centro da vila — separado de
 * {@link ConstructionProject} em 2026-09-24, quando ele passou de 500 linhas.
 *
 * <p>É uma regra sobre a vila, e não sobre uma obra: o E46 e o alcance pela
 * rua (ver a memória "alcance da obra é a rua, não o centro") moram aqui. Os
 * comentários de cada método vieram junto sem mudança.
 */
public final class ConstructionReach {

    private ConstructionReach() {
    }

    /**
     * Se esta obra ficou fora do alcance do centro da vila.
     *
     * <p><b>Decisão de 2026-09-15</b>, e o log do autor traz a aritmética
     * inteira. Às 21:02 ele relatou não ver construção nascendo; a sessão
     * tinha uma obra aberta às 20:54:35 — {@code plains_butcher_shop_2} em
     * {@code 638,65,-2793} — parada em <i>"382 blocks left"</i> por sete
     * minutos e meio, sem um bloco assentado, segurando a vaga única:
     * <i>"no building work: one is already open"</i>.
     *
     * <p><b>O centro da vila não é estável.</b> A mesma sessão registrou
     * oito centros diferentes, de {@code 616,-2863} a {@code 640,-2891} —
     * ele é recalculado das camas vistas, e camas entram e saem de chunk
     * carregado. A obra nasceu quando o centro era {@code 625,-2854}, a
     * 62,4 blocos: dentro do raio. O centro que prevaleceu,
     * {@code 637,-2871}, a deixa a <b>78</b> — fora dele, e portanto fora
     * do alcance de qualquer trabalhador.
     *
     * <p><b>Por que ninguém reclamava.</b> O relógio de paciência só conta
     * para obra em {@code WAITING_RESOURCES}, e esta estava em
     * {@code BUILDING}; o passo do construtor sai em silêncio quando o
     * aldeão não está em chunk carregado. Obra viva, inalcançável, calada
     * e ocupando a única vaga, as quatro coisas ao mesmo tempo.
     *
     * <p><b>Horizontal, como o raio da vila.</b> A altura fica de fora de
     * propósito: a mina desce dezenas de blocos abaixo do centro e não
     * está fora da vila por isso.
     *
     * <p>Aqui, e não no {@code fabric}, porque é aritmética de dois pontos
     * e não precisa de mundo — o que a torna afirmável sem servidor. Quem
     * decide o que fazer com a resposta é {@code ConstructionPlanner}.
     *
     * @param origin onde a obra está
     * @param centre o centro da vila agora
     * @param radius o raio da vila, inclusivo na borda
     */
    public static boolean isOutOfReach(ColonyPos origin, ColonyPos centre, int radius) {
        long dx = (long) origin.x() - centre.x();
        long dz = (long) origin.z() - centre.z();

        return dx * dx + dz * dz > (long) radius * radius;
    }

    /**
     * Quantos blocos da rua uma obra pode estar e ainda ser da vila.
     *
     * <p>A casa nasce <b>encostada</b> na rua — é a decisão 1 do
     * {@code BuildSiteScanner}, e o lote começa no bloco seguinte ao
     * calçamento. A folga aqui é para a obra grande, cujo canto de origem
     * fica a uma casa de distância da rua que a serve: a maior planta em
     * uso é 13×11, e o canto mais longe dela está a treze blocos.
     *
     * <p>Dezesseis é esse número com margem, e ele é deliberadamente
     * apertado: o que se quer excluir é a obra <b>solta no campo</b>, e
     * não a obra na ponta da estrada.
     */
    public static final int BESIDE_THE_ROAD = 16;

    /**
     * O mesmo, sabendo a que distância da rua a obra está — E46,
     * 2026-09-16.
     *
     * <p><b>O defeito que isto conserta.</b> Medir do centro larga obra
     * legítima, e não por acidente: a rua cresce <b>pela ponta mais
     * distante</b> do centro — {@code RoadExtension.consider} ordena as
     * candidatas da mais longe para a mais perto, e a frase no código é
     * <i>"a rua cresce pela ponta, e não pelo meio"</i>. Uma vila que se
     * estende ao longo da estrada passa a receber lote de fora do raio, e
     * o guarda antigo matava essa obra no ciclo seguinte. No playtest de
     * 2026-09-16 21:41 foram duas bibliotecas, planejadas e abandonadas em
     * trinta segundos, com 628 blocos restantes de 628.
     *
     * <p><b>O raio de 64 é da detecção de vila, não um limite de
     * crescimento.</b> Quem responde "esta obra é alcançável" é a rede de
     * ruas: obra encostada na rua está ligada à vila por onde o
     * trabalhador anda, esteja o centro onde estiver.
     *
     * <p><b>E o defeito de 2026-09-15 continua pego</b>, que é o ponto de
     * não afrouxar isto à toa. A obra daquele log — {@code 638,65,-2793},
     * 382 blocos parados por sete minutos e meio segurando a vaga única —
     * ficou longe do centro <b>e</b> longe de qualquer rua; ela continua
     * sendo largada. O que deixa de ser largada é a obra que a estrada
     * alcança.
     *
     * <p><b>Sem índice, o centro volta a valer.</b> Colônia que ainda não
     * varreu o raio, ou que perdeu o índice por deriva, não sabe onde
     * estão as ruas — e não saber não é o mesmo que não haver. Nesse caso
     * a pergunta cai na sobrecarga de dois pontos, que é o comportamento
     * de antes deste conserto.
     *
     * @param origin onde a obra está
     * @param centre o centro da vila agora
     * @param radius o raio da vila, inclusivo na borda
     * @param blocksToTheNearestRoad a distância em quadrado até a rua mais
     *     próxima, ou vazio quando a colônia não tem índice de ruas. Ver
     *     {@code ColonyRoads.blocksToTheNearestRoad}
     */
    public static boolean isOutOfReach(
            ColonyPos origin, ColonyPos centre, int radius,
            OptionalInt blocksToTheNearestRoad) {

        Objects.requireNonNull(blocksToTheNearestRoad, "blocksToTheNearestRoad");

        if (blocksToTheNearestRoad.isEmpty()) {
            return isOutOfReach(origin, centre, radius);
        }

        return blocksToTheNearestRoad.getAsInt() > BESIDE_THE_ROAD;
    }
}
