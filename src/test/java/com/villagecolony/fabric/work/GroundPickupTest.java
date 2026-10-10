package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionOutcome;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.SkipReason;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** A ajuda de chão também enxerga peças que a obra deixou de lado. */
class GroundPickupTest {

    private static final ResourceId HOUSE = ResourceId.vanilla("ground_pickup");

    private static final ResourceId COBBLE = ResourceId.vanilla("cobblestone");

    private static final ColonyPos ORIGIN = new ColonyPos(0, 64, 0);

    @BeforeAll
    static void bootMinecraft() {
        SharedConstants.createGameVersion();
        Bootstrap.initialize();
    }

    @Test
    void deferredPiecesAreStillWantedFromTheGround() {
        BlueprintBlock first = new BlueprintBlock(new ColonyPos(0, 0, 0), COBBLE);
        Blueprint blueprint = Blueprint.of(HOUSE, List.of(first));
        ConstructionProject project = ConstructionProject.plan(UUID.randomUUID(), blueprint, ORIGIN);

        project.defer(
                first,
                ConstructionOutcome.skipped(project.worldPositionOf(first), SkipReason.UNSUPPORTED),
                "support-before");

        assertTrue(project.remainingMaterials().isEmpty(),
                "o contrato do projeto continua: peça adiada não gera demanda normal");
        assertTrue(GroundPickup.itemsOf(project).contains(Items.COBBLESTONE),
                "ajudante no chão precisa recolher peça adiada para destravar a obra");
    }
}
