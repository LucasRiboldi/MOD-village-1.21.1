package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.coordination.ResourceReach;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.gametest.ColonyFixture;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

/** A busca de recurso alterna centro e borda, até 10 além da borda (ADR-036 18). */
public class ResourceSearchesGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "resource_searches", tickLimit = 20)
    public void aMeasuredVillageAlternatesCentreAndEdge(TestContext context) {
        ColonyFixture fixture = ColonyFixture.colonyAt(context, new BlockPos(1, 1, 1));
        Colony colony = fixture.colony();

        try {
            ColonyPos c = colony.center();
            colony.measure(new VillageBounds(c.x() - 20, c.y() - 5, c.z() - 20, c.x() + 20, c.y() + 5, c.z() + 20));

            BlockPos centre = MinecraftTypeAdapter.toBlockPos(colony.center());
            VillageBounds box = colony.bounds().orElseThrow();

            ResourceSearches.Plan first = ResourceSearches.current(
                    context.getWorld(), colony.id(), ProfessionType.LUMBERJACK, centre, 64, false);
            ResourceSearches.advance(colony.id(), ProfessionType.LUMBERJACK);
            ResourceSearches.Plan second = ResourceSearches.current(
                    context.getWorld(), colony.id(), ProfessionType.LUMBERJACK, centre, 64, false);

            context.assertTrue(first.origin().equals(centre), "a primeira busca não partiu do centro");
            context.assertTrue(second.origin().getZ() == box.minZ() - ResourceReach.EDGE_MARGIN,
                    "a segunda busca devia partir da borda norte, partiu de " + second.origin().toShortString());
            context.assertTrue(first.inside().test(centre.add(box.maxX() - centre.getX() + 10, 0, 0))
                            && !first.inside().test(centre.add(box.maxX() - centre.getX() + 11, 0, 0)),
                    "o limite não é dez além da borda");

            ResourceSearches.Plan shortened = ResourceSearches.current(
                    context.getWorld(), colony.id(), ProfessionType.LUMBERJACK, centre, 8, true);

            context.assertTrue(shortened.origin().equals(centre) && shortened.radius() == 8,
                    "o raio encurtado do teste deixou de valer");
        } finally {
            fixture.cleanUp();
        }

        context.complete();
    }
}
