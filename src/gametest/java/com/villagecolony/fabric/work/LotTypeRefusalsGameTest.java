package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.gametest.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * O tipo de obra sem lote sai da vez, e com todos os que faltam recusados a casa entra no
 * lugar — playtest 0.3.41: cinco sessões sem obra, a oficina que faltava não cabia.
 */
public class LotTypeRefusalsGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "lot_type_refusals", tickLimit = 20)
    public void withEveryMissingWorkshopWithoutALotTheVillageBuildsAHouse(TestContext context) {
        ColonyFixture fixture = ColonyFixture.colonyAt(context, new BlockPos(1, 1, 1));
        Colony colony = fixture.colony();
        ColonyPos at = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(1, 1, 1)));
        List<String> refused = new ArrayList<>();

        VillageColonyMod.BUILDINGS.register(new Building(UUID.randomUUID(), colony.id(),
                ResourceId.vanilla("village/plains/houses/plains_small_house_1"), at, new ColonyPos(at.x() + 4, at.y() + 4, at.z() + 4)));

        try {
            List<Blueprint> next = HousePlans.plansForNext(context.getWorld(), colony);

            for (int round = 0; round < 40 && !next.isEmpty()
                    && !"house".equals(HousePlans.constructionType(next.getFirst().id())); round++) {
                String type = HousePlans.constructionType(next.getFirst().id());

                context.assertFalse(refused.contains(type), "o tipo recusado " + type + " voltou na vez");
                refused.add(type);
                LotTypeRefusals.refuse(colony.id(), type, context.getWorld().getTime());
                next = HousePlans.plansForNext(context.getWorld(), colony);
            }

            context.assertFalse(refused.isEmpty(), "a vila não ofereceu nenhuma oficina para recusar");
            context.assertTrue(!next.isEmpty() && "house".equals(HousePlans.constructionType(next.getFirst().id())),
                    "com " + refused + " sem lote, a vila não passou para a casa: "
                            + next.stream().map(Blueprint::id).toList());
        } finally {
            VillageColonyMod.BUILDINGS.removeOfColony(colony.id());
            LotTypeRefusals.clearAll();
            fixture.cleanUp();
        }

        context.complete();
    }
}
