package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O contorno do lote escolhido — 2026-09-15.
 *
 * <p><b>Pedido do autor:</b> <i>"adicionar um marcador visual, um efeito
 * que demonstre onde no terreno está o espaço alocado para a construção
 * escolhida"</i>. Ele entrou no jogo três sessões seguidas sem ver casa
 * nascendo, e o log dizia que a colônia planejava — sem o marcador, "não
 * achou lote" e "achou lote longe daqui" são a mesma coisa vista de dentro
 * do jogo.
 *
 * <p><b>A geometria mora no Core e é afirmada sem mundo.</b> Desenhar
 * partícula precisa de servidor; decidir <b>onde</b> desenhar é aritmética
 * de dois cantos, e é o que costuma ter defeito — um contorno que erra o
 * canto marca o lote errado e faz o autor procurar no lugar errado.
 */
class SiteOutlineTest {

    /** Um lote 5x3 no plano, com altura qualquer. */
    private static List<ColonyPos> outline() {
        return SiteOutline.of(new ColonyPos(10, 64, 20), new ColonyPos(14, 70, 22));
    }

    /**
     * Só a borda, e não o miolo.
     *
     * <p>É o que separa um marcador de uma nuvem: um lote 5x3 tem 15
     * colunas, e só 12 delas são borda. Preencher o miolo esconderia o
     * terreno que o autor quer justamente ver.
     */
    @Test
    void theOutlineIsTheBorderAndNotTheWholeArea() {
        List<ColonyPos> marks = outline();

        assertEquals(
                12,
                marks.size(),
                "o contorno de um lote 5x3 tem 12 colunas de borda; veio " + marks.size());

        assertFalse(
                marks.contains(new ColonyPos(12, 64, 21)),
                "o miolo do lote entrou no contorno, e ele esconderia o terreno");
    }

    /** Os quatro cantos estão lá — são eles que dizem onde o lote começa. */
    @Test
    void theFourCornersAreMarked() {
        List<ColonyPos> marks = outline();

        assertTrue(marks.contains(new ColonyPos(10, 64, 20)), "faltou o canto mínimo");
        assertTrue(marks.contains(new ColonyPos(14, 64, 20)), "faltou o canto x máximo");
        assertTrue(marks.contains(new ColonyPos(10, 64, 22)), "faltou o canto z máximo");
        assertTrue(marks.contains(new ColonyPos(14, 64, 22)), "faltou o canto oposto");
    }

    /**
     * O contorno fica na altura do piso, e não espalhado pela altura da
     * casa.
     *
     * <p>Decisão: o que o autor procura é <b>onde no terreno</b>, e uma
     * coluna de partículas até o telhado vira parede opaca que esconde o
     * lote em vez de mostrá-lo.
     */
    @Test
    void theOutlineSitsAtTheFloorHeight() {
        assertTrue(
                outline().stream().allMatch(pos -> pos.y() == 64),
                "o contorno subiu do piso e viraria parede");
    }

    /** Lote de uma coluna só ainda tem contorno — e é ela mesma. */
    @Test
    void aSingleColumnLotIsItsOwnOutline() {
        List<ColonyPos> marks =
                SiteOutline.of(new ColonyPos(5, 64, 5), new ColonyPos(5, 70, 5));

        assertEquals(List.of(new ColonyPos(5, 64, 5)), marks, "lote de uma coluna sumiu");
    }

    /** Cantos invertidos não quebram: o contorno é o mesmo retângulo. */
    @Test
    void invertedCornersDescribeTheSameRectangle() {
        List<ColonyPos> straight =
                SiteOutline.of(new ColonyPos(10, 64, 20), new ColonyPos(14, 64, 22));

        List<ColonyPos> inverted =
                SiteOutline.of(new ColonyPos(14, 64, 22), new ColonyPos(10, 64, 20));

        assertEquals(
                Set.copyOf(straight),
                Set.copyOf(inverted),
                "trocar os cantos mudou o contorno");
    }
}
