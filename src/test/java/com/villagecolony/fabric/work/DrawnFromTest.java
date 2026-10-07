package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.Side;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * De quantas plantas saiu a casa — a conta que o log diz, 2026-09-18.
 *
 * <p><b>Por que este teste existe.</b> O playtest de 02:25 levantou duas
 * plantas diferentes e isso <b>não</b> provou que o sorteio entre irmãs
 * funcionou: as duas tinham pegadas diferentes — 517 e 382 blocos —, então
 * a variedade podia ter vindo da lista de tamanhos que já existia antes,
 * pela Regra 25 descendo um degrau. Do lado de fora, <i>"sorteou entre
 * oito"</i> e <i>"só havia uma"</i> eram a mesma linha.
 *
 * <p><b>E por que ele é unitário e não de jogo.</b> A linha do
 * {@code planned} <b>não sai na bateria</b> — medido em 09-18, zero
 * ocorrências com e sem a mudança, porque os gametests montam o projeto
 * sem passar pelo {@code open}. Uma formatação embutida no
 * {@code LOGGER.info} ficaria sem nenhuma verificação possível, e esta base
 * já pagou o preço de um trecho que nada exercitava: quando a divisão do
 * fabricante entrou, removido o {@code continue} dela, 701 unitários e 275
 * testes de jogo continuavam verdes.
 */
class DrawnFromTest {

    /** Um lote nunca pode receber a planta oferecida se ela nao couber nele. */
    @Test
    void noMismatchedPlanCanEscapeTheValidatedLot() {
        Blueprint smaller = blueprint("small", 2);
        Blueprint offered = blueprint("offered", 3);

        assertTrue(
                SiteOpening.fittingPlans(
                        List.of(smaller, offered), Side.NORTH, new ColonyPos(4, 1, 4)).isEmpty(),
                "a planta oferecida nao cabe neste lote e nao pode ser usada como reserva: " + offered.id());
    }

    private static Blueprint blueprint(String name, int width) {
        return Blueprint.of(
                ResourceId.vanilla(name),
                List.of(
                        new BlueprintBlock(new ColonyPos(0, 0, 0), ResourceId.vanilla("stone")),
                        new BlueprintBlock(new ColonyPos(width - 1, 0, width - 1), ResourceId.vanilla("stone"))));
    }
}
