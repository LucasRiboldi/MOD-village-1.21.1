package com.villagecolony.core.construction.service;

import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Nada se constrói dentro do que já está de pé — 2026-09-19.
 *
 * <p><b>O defeito que isto tranca, visto em jogo duas vezes.</b> Uma obra
 * nasceu em cima de uma casa pronta, acavalando as duas. O portão de
 * caixa consertou quem <b>escolhe</b> lote — mas a obra <b>gravada no
 * save</b> não passava por ele, então um lote ruim escolhido por uma
 * versão anterior sobrevivia a todo conserto e voltava a cada
 * carregamento.
 *
 * <p>A pergunta é da caixa, e não de um ponto: um prédio que cobre a
 * coluna em outra altura escapava de {@code isColonyBuilt(ground)}, que
 * olha uma posição só.
 */
class OverlapGuardTest {

    private static final UUID COLONY = UUID.randomUUID();

    private static Building at(int x, int y, int z, int size) {
        return new Building(
                UUID.randomUUID(),
                COLONY,
                ResourceId.vanilla("village/desert/houses/desert_small_house_1"),
                new ColonyPos(x, y, z),
                new ColonyPos(x + size, y + size, z + size));
    }

    /** Caixa dentro de caixa é acavalamento. */
    @Test
    void theBoxInsideAnotherIsCaught() {
        BuildingRegistry registry = new BuildingRegistry();

        registry.register(at(0, 64, 0, 6));

        assertTrue(
                registry.anythingBuiltInside(
                        new ColonyPos(2, 65, 2), new ColonyPos(4, 68, 4)),
                "uma obra inteiramente dentro de outra passou pelo portao");
    }

    /**
     * E o encosto de canto também, porque as caixas se cruzam.
     *
     * <p>É o caso que um teste por ponto deixaria passar: o canto de uma
     * está fora do centro da outra, e mesmo assim os volumes se cruzam.
     */
    @Test
    void theCornerThatCrossesIsCaught() {
        BuildingRegistry registry = new BuildingRegistry();

        registry.register(at(0, 64, 0, 6));

        assertTrue(
                registry.anythingBuiltInside(
                        new ColonyPos(5, 64, 5), new ColonyPos(11, 70, 11)),
                "duas caixas que se cruzam pelo canto passaram pelo portao");
    }

    /**
     * Lado a lado sem se tocar NÃO é acavalamento.
     *
     * <p>A metade que impede o portão de virar "nunca construir": vilas
     * são feitas de casas vizinhas, e recusar todas elas travaria a
     * colônia.
     */
    @Test
    void theNeighbourThatOnlyTouchesIsAllowed() {
        BuildingRegistry registry = new BuildingRegistry();

        registry.register(at(0, 64, 0, 6));

        assertFalse(
                registry.anythingBuiltInside(
                        new ColonyPos(7, 64, 0), new ColonyPos(13, 70, 6)),
                "a casa vizinha foi recusada — a vila nunca cresceria");
    }

    /**
     * E a altura conta: o que passa por cima não acavala.
     *
     * <p>Sem a comparação em Y, um porão e um telhado no mesmo x/z
     * seriam o mesmo lugar.
     */
    @Test
    void theHeightIsPartOfTheQuestion() {
        BuildingRegistry registry = new BuildingRegistry();

        registry.register(at(0, 64, 0, 6));

        assertFalse(
                registry.anythingBuiltInside(
                        new ColonyPos(0, 80, 0), new ColonyPos(6, 86, 6)),
                "uma obra bem acima da outra foi contada como acavalamento");
    }
}
