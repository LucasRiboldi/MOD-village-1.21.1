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
        assertEquals(ToolType.IRON_AXE,
                ProfessionRegistry.of(ProfessionType.LUMBERJACK).requiredTool());

        assertEquals(ToolType.IRON_HOE,
                ProfessionRegistry.of(ProfessionType.FARMER).requiredTool());

        assertEquals(ToolType.NONE,
                ProfessionRegistry.of(ProfessionType.MANUFACTURER).requiredTool());

        assertEquals(ToolType.NONE,
                ProfessionRegistry.of(ProfessionType.BUILDER).requiredTool());
    }

    /**
     * O mineiro começa de ferro, como todo mundo — decisão do autor,
     * 2026-09-05.
     *
     * <p><b>A frase dele:</b> <i>"trocar todas ferramentas dos
     * trabalhadores iniciais para ferramentas de ferro"</i>. Desfaz a de
     * 09-04, que punha todo mundo em madeira, e que este teste afirmava.
     *
     * <p><b>O que derrubou a de madeira</b> foi o degrau que nunca
     * chegou. O argumento dela era que a descida lenta <i>"deixou de ser
     * um teto e virou o primeiro degrau de uma progressão"</i>, porque o
     * {@code ToolUpgrade} troca pela melhor ferramenta do baú — e a
     * versão anterior deste javadoc chegava a afirmar que <i>"a colônia
     * mesma põe picaretas lá"</i>. <b>Não põe</b>: nada no mod fabrica ou
     * deposita ferramenta, e isso está no TODO como pendência vermelha
     * desde 09-04. Sem o segundo degrau, o primeiro é o teto.
     *
     * <p>Ferro é o grau que a Regra 2 já usava para medir o tempo de
     * quebra desde o começo — <i>"no tempo de um jogador com ferramenta
     * de ferro"</i>. A mão passou a combinar com a conta.
     *
     * <p>A troca pela melhor do baú continua valendo, e passa a ser o que
     * sempre deveria ter sido: um <b>bônus</b> do que o jogador puser
     * ali, e não a única saída de um piso baixo demais.
     */
    @Test
    void theMinerStartsWithIronLikeEveryoneElse() {
        assertEquals(ToolType.IRON_PICKAXE,
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
     * ela mesma"</i>. Não há tesoura de ferro a exigir dele.
     *
     * <p><b>Era madeira até 2026-09-05</b>, e a troca é decisão do autor:
     * <i>"trocar todas ferramentas dos trabalhadores iniciais para
     * ferramentas de ferro"</i>. Ver
     * {@link #theMinerStartsWithIronLikeEveryoneElse}.
     */
    @Test
    void everyProfessionStartsAtIron() {
        for (ProfessionType profession : ProfessionType.values()) {
            ToolType tool = ProfessionRegistry.of(profession).requiredTool();

            assertTrue(
                    tool == ToolType.NONE
                            || tool == ToolType.SHEARS
                            || tool.name().startsWith("IRON_"),
                    profession + " comeca com " + tool + ", que nao e ferro");
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
