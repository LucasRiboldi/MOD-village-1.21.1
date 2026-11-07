package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.VillageStructures;
import com.villagecolony.gametest.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Com um de cada tipo de pé, a vila volta a construir quando a população pede — Regra 218. */
public class VillageKeepsBuildingGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_keeps_building", tickLimit = 20)
    public void moreAdultsReopenTheConstructionList(TestContext context) {
        ColonyFixture fixture = ColonyFixture.colonyAt(context, new BlockPos(1, 1, 1));
        Colony colony = fixture.colony();
        ColonyPos at = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(1, 1, 1)));
        ColonyPos far = new ColonyPos(at.x() + 4, at.y() + 4, at.z() + 4);

        Set<String> types = new HashSet<>();

        colony.observe(at, 500);

        for (ResourceId id : VillageStructures.buildableFor(HousePlans.paletteOf(context.getWorld(), colony.center()).style())) {
            String type = HousePlans.constructionType(id);

            if (!"house".equals(type) && types.add(type)) {
                VillageColonyMod.BUILDINGS.register(new Building(UUID.randomUUID(), colony.id(), id, at, far, true));
            }
        }

        try {
            for (int adult = 0; adult < 4; adult++) {
                fixture.owning(VillageColonyMod.WORKERS.register(UUID.randomUUID(), colony.id()).villagerId());
            }

            List<Blueprint> few = HousePlans.plansForNext(context.getWorld(), colony);

            context.assertTrue(few.isEmpty(), "com 4 adultos e tudo de pé a vila abriu " + few.stream().map(Blueprint::id).toList());

            fixture.owning(VillageColonyMod.WORKERS.register(UUID.randomUUID(), colony.id()).villagerId());

            List<Blueprint> more = HousePlans.plansForNext(context.getWorld(), colony);

            context.assertFalse(more.isEmpty(), "com 5 adultos a vila devia repetir uma obra");
            context.assertFalse("house".equals(HousePlans.constructionType(more.getFirst().id())),
                    "com 500 camas a repetição não é casa: " + more.getFirst().id());
        } finally {
            VillageColonyMod.BUILDINGS.removeOfColony(colony.id());
            fixture.cleanUp();
        }

        context.complete();
    }
}
