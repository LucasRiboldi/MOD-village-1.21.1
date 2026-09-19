package com.villagecolony.core.type;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * As variantes de arenito são material do pedreiro — 2026-09-19.
 *
 * <p><b>O que isto tranca, medido na sessão de 14:01.</b> A obra parou em
 * {@code waiting for minecraft:cut_sandstone} com <b>178 blocos</b> por
 * pôr, e o log dizia {@code no mason work: no task open for it — 1 able
 * to}. O pedreiro existia, tinha baú, estava apto — e não tinha tarefa,
 * porque das dez variantes de arenito que a casa do deserto pede só
 * <b>duas</b> eram {@code ResourceType}.
 *
 * <p>É a frase que o {@code STONE_BRICKS} já escrevia: <i>profissão sem
 * material declarado nunca recebe pedido</i>.
 *
 * <p><b>Mora aqui, e não na bateria de jogo</b>, porque a afirmação é
 * sobre a declaração e não sobre o mundo. A primeira versão era gametest
 * e <b>quebrou o {@code ColonyDetectionGameTest}</b> — duas anotações a
 * mais mudam o escalonamento dos batches, e aquele cenário conta aldeões
 * registrados dentro de um orçamento de três passagens. O teste não
 * precisava de mundo nenhum.
 */
class DesertMasonryTest {

    /** O arenito cortado é do pedreiro — era ele que estava sem tarefa. */
    @Test
    void theCutSandstoneIsMasonWork() {
        assertEquals(
                Production.CRAFTED_STONE,
                ResourceType.CUT_SANDSTONE.production(),
                "o arenito cortado nao e do pedreiro — a tarefa vai para a profissao errada");
    }

    /** E as outras seis variantes do deserto, pela mesma razão. */
    @Test
    void everySandstoneVariantBelongsToTheMason() {
        ResourceType[] masonry = {
            ResourceType.CUT_SANDSTONE,
            ResourceType.SMOOTH_SANDSTONE_STAIRS,
            ResourceType.SMOOTH_SANDSTONE_SLAB,
            ResourceType.SANDSTONE_STAIRS,
            ResourceType.SANDSTONE_SLAB,
            ResourceType.SANDSTONE_WALL,
            ResourceType.CHISELED_SANDSTONE,
        };

        for (ResourceType type : masonry) {
            assertEquals(
                    Production.CRAFTED_STONE,
                    type.production(),
                    type + " nao e do pedreiro");
        }
    }

    /**
     * E nenhuma delas entra em grupo de recurso.
     *
     * <p>Grupo {@code NONE} de propósito, como todo processado: pô-las no
     * grupo da pedra faria arenito cortado contar como pedregulho na meta
     * do mineiro, que é a discordância de 2026-08-22.
     */
    @Test
    void theProcessedSandstoneJoinsNoGroup() {
        assertEquals(ResourceGroup.NONE, ResourceType.CUT_SANDSTONE.group());

        assertEquals(ResourceGroup.NONE, ResourceType.SANDSTONE_WALL.group());
    }
}
