package com.villagecolony.core.worker.service;

import com.villagecolony.core.worker.model.Capability;
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
