package com.villagecolony.core.worker.service;

import com.villagecolony.core.type.Capability;
import com.villagecolony.core.worker.model.Profession;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.ToolType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfessionRegistryTest {

    /**
     * O que trava a regressão real: uma profissão acrescentada ao enum
     * sem entrada no catálogo só apareceria quando alguém a atribuísse.
     */
    @Test
    void everyProfessionTypeIsDefined() {
        for (ProfessionType type : ProfessionType.values()) {
            assertEquals(type, ProfessionRegistry.of(type).type());
        }

        assertEquals(ProfessionType.values().length, ProfessionRegistry.all().size());
    }

    @Test
    void capabilitiesMatchTheDesign() {
        assertTrue(ProfessionRegistry.of(ProfessionType.LUMBERJACK)
                .canPerform(Capability.COLLECT_WOOD));

        assertTrue(ProfessionRegistry.of(ProfessionType.MANUFACTURER)
                .canPerform(Capability.CRAFT_ITEMS));

        assertTrue(ProfessionRegistry.of(ProfessionType.FARMER)
                .canPerform(Capability.MAINTAIN_FOOD));

        assertTrue(ProfessionRegistry.of(ProfessionType.BUILDER)
                .canPerform(Capability.BUILD_STRUCTURE));
    }

    /** Profession-System.md: o Lumberjack não sabe que o Builder existe. */
    @Test
    void professionsDoNotOverlap() {
        assertFalse(ProfessionRegistry.of(ProfessionType.LUMBERJACK)
                .canPerform(Capability.BUILD_STRUCTURE));

        assertFalse(ProfessionRegistry.of(ProfessionType.BUILDER)
                .canPerform(Capability.COLLECT_WOOD));
    }

    @Test
    void toolsMatchTheDesign() {
        assertEquals(ToolType.WOODEN_AXE,
                ProfessionRegistry.of(ProfessionType.LUMBERJACK).requiredTool());

        assertEquals(ToolType.WOODEN_HOE,
                ProfessionRegistry.of(ProfessionType.FARMER).requiredTool());

        assertEquals(ToolType.NONE,
                ProfessionRegistry.of(ProfessionType.MANUFACTURER).requiredTool());

        assertEquals(ToolType.NONE,
                ProfessionRegistry.of(ProfessionType.BUILDER).requiredTool());
    }

    /**
     * O mineiro começa de madeira, como todo mundo — decisão do autor,
     * 2026-09-04.
     *
     * <p><b>A frase dele:</b> <i>"todos trabalhadores começam com a
     * ferramenta nível 1 de madeira"</i>. Desfaz a de 08-27, que este
     * teste afirmava, e que dava diamante ao mineiro porque <i>"são
     * vinte blocos de descida antes de a mina render alguma coisa, e com
     * picareta de madeira isso é uma sessão inteira"</i>.
     *
     * <p>Continua sendo uma sessão inteira — o que mudou é que agora há
     * saída. O {@code ToolUpgrade} troca pela melhor ferramenta do baú do
     * trabalhador, e a colônia mesma põe picaretas lá: a descida lenta
     * deixou de ser um teto e virou o primeiro degrau de uma progressão.
     *
     * <p><b>E a discordância que este teste guardava fechou pelo outro
     * lado.</b> Ele nasceu porque o catálogo entregava madeira e o
     * {@code MinerWork} media a velocidade com uma constante de
     * diamante: o aldeão minerava como diamante segurando madeira.
     * Aquela constante não existe mais — o {@code BlockBreakTime}
     * pergunta à mão. Os dois já não <b>podem</b> discordar, e o que
     * este teste afirma hoje é o degrau inicial, não a concordância.
     */
    @Test
    void theMinerStartsWithWoodLikeEveryoneElse() {
        assertEquals(ToolType.WOODEN_PICKAXE,
                ProfessionRegistry.of(ProfessionType.MINER).requiredTool());
    }

    /**
     * E a regra é de <b>todos</b>, não só do mineiro.
     *
     * <p>Escrito como varredura do catálogo inteiro de propósito: a
     * profissão nova que nascer amanhã cai aqui sozinha, e é isso que
     * torna esta uma regra em vez de quatro afirmações soltas.
     *
     * <p>A tesoura do pastor é a exceção, e é a exceção que o próprio
     * {@code ToolType} já documenta: <i>"tesoura não tem grau, então é
     * ela mesma"</i>. Não há tesoura de madeira a exigir dele.
     */
    @Test
    void everyProfessionStartsAtTheFirstRung() {
        for (ProfessionType profession : ProfessionType.values()) {
            ToolType tool = ProfessionRegistry.of(profession).requiredTool();

            assertTrue(
                    tool == ToolType.NONE
                            || tool == ToolType.SHEARS
                            || tool.name().startsWith("WOODEN_"),
                    profession + " começa com " + tool + ", que não é o primeiro degrau");
        }
    }

    @Test
    void needsToolAnswersForBothCases() {
        assertTrue(ProfessionRegistry.of(ProfessionType.LUMBERJACK).needsTool());
        assertFalse(ProfessionRegistry.of(ProfessionType.BUILDER).needsTool());
    }

    /** É como a colônia acha quem faz uma tarefa. */
    @Test
    void findsProfessionByCapability() {
        List<Profession> found = ProfessionRegistry.withCapability(Capability.BUILD_STRUCTURE);

        assertEquals(1, found.size());
        assertEquals(ProfessionType.BUILDER, found.get(0).type());
    }

    @Test
    void unknownCapabilityFindsNobody() {
        assertTrue(ProfessionRegistry.withCapability(null).isEmpty());
    }

    @Test
    void ofRejectsNull() {
        assertThrows(NullPointerException.class, () -> ProfessionRegistry.of(null));
    }

    @Test
    void capabilitySetIsUnmodifiable() {
        var capabilities = ProfessionRegistry.of(ProfessionType.FARMER).capabilities();

        assertThrows(UnsupportedOperationException.class, () -> capabilities.clear());
    }
}
