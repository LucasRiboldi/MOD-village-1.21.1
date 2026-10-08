package com.villagecolony.fabric.event;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.coordination.ColonyMerge;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/** O gatilho da fusão de colônias — ADR-007, 2026-09-30. */
public final class ColonyMergeTriggerGameTest implements FabricGameTest {

    /**
     * Duas colônias longe pelo centro, mas com casas encostadas, viram uma.
     *
     * <p>É o gatilho original da ADR-007 §4: não é distância, é encosto de
     * bloco. Os centros ficam a 60 blocos, além da sobreposição de 32.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "colony_merge")
    public void coloniesWhoseBuildingsTouchBecomeOne(TestContext context) {
        BlockPos base = context.getAbsolutePos(new BlockPos(1, 1, 1));
        Colony west = Colony.create(UUID.randomUUID(), at(base, -30));
        Colony east = Colony.create(UUID.randomUUID(), at(base, 30));
        VillageColonyMod.COLONIES.register(west);
        VillageColonyMod.COLONIES.register(east);

        try {
            VillageColonyMod.BUILDINGS.register(new Building(UUID.randomUUID(), west.id(),
                    ResourceId.vanilla("hut"), at(base, 0), at(base, 3)));
            VillageColonyMod.BUILDINGS.register(new Building(UUID.randomUUID(), east.id(),
                    ResourceId.vanilla("hut"), at(base, 4), at(base, 7)));

            context.assertTrue(ColonyMergeTrigger.shouldMerge(context.getWorld(), west, east),
                    "as casas encostadas não disparam a fusão");

            ColonyMerge.Result result = ColonyMerge.merge(west, east,
                    VillageColonyMod.COLONIES, VillageColonyMod.WORKERS, VillageColonyMod.TASKS,
                    VillageColonyMod.CONSTRUCTIONS, VillageColonyMod.BUILDINGS, VillageColonyMod.MINES);

            context.assertTrue(VillageColonyMod.BUILDINGS.ofColony(result.survivor()).size() == 2,
                    "a colônia resultante não ficou com as duas casas");
            context.assertTrue(VillageColonyMod.COLONIES.find(result.absorbed()).isEmpty(),
                    "a colônia absorvida continua no registro");
        } finally {
            cleanUp(west, east);
        }

        context.complete();
    }

    /** Sem jogador por perto, nem colônias encostadas se juntam (ADR-039 item 3, opção B). */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "colony_merge")
    public void onlyTheAttendedColonyMerges(TestContext context) {
        BlockPos base = context.getAbsolutePos(new BlockPos(1, 1, 1));
        Colony west = Colony.create(UUID.randomUUID(), at(base, -30));
        Colony east = Colony.create(UUID.randomUUID(), at(base, 30));
        VillageColonyMod.COLONIES.register(west);
        VillageColonyMod.COLONIES.register(east);

        try {
            VillageColonyMod.BUILDINGS.register(new Building(UUID.randomUUID(), west.id(),
                    ResourceId.vanilla("hut"), at(base, 0), at(base, 3)));
            VillageColonyMod.BUILDINGS.register(new Building(UUID.randomUUID(), east.id(),
                    ResourceId.vanilla("hut"), at(base, 4), at(base, 7)));

            int unseen = ColonyMergeTrigger.mergeTouchingColonies(context.getWorld(), id -> false);

            context.assertTrue(unseen == 0 && VillageColonyMod.COLONIES.find(west.id()).isPresent()
                            && VillageColonyMod.COLONIES.find(east.id()).isPresent(),
                    "sem jogador, as colônias encostadas se juntaram");

            int seen = ColonyMergeTrigger.mergeTouchingColonies(context.getWorld(), west.id()::equals);

            context.assertTrue(seen >= 1 && (VillageColonyMod.COLONIES.find(west.id()).isEmpty()
                            || VillageColonyMod.COLONIES.find(east.id()).isEmpty()),
                    "com o jogador numa delas, as colônias encostadas não se juntaram");
        } finally {
            cleanUp(west, east);
        }

        context.complete();
    }

    /** Longe, sem casa encostada e sem vila gerada em comum: continuam duas. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "colony_merge")
    public void coloniesApartStayApart(TestContext context) {
        BlockPos base = context.getAbsolutePos(new BlockPos(1, 1, 1));
        Colony west = Colony.create(UUID.randomUUID(), at(base, -30));
        Colony east = Colony.create(UUID.randomUUID(), at(base, 30));
        VillageColonyMod.COLONIES.register(west);
        VillageColonyMod.COLONIES.register(east);

        try {
            VillageColonyMod.BUILDINGS.register(new Building(UUID.randomUUID(), west.id(),
                    ResourceId.vanilla("hut"), at(base, -30), at(base, -27)));
            VillageColonyMod.BUILDINGS.register(new Building(UUID.randomUUID(), east.id(),
                    ResourceId.vanilla("hut"), at(base, 27), at(base, 30)));

            context.assertTrue(!ColonyMergeTrigger.shouldMerge(context.getWorld(), west, east),
                    "colônias a 60 blocos, sem nada encostado, foram fundidas");
        } finally {
            cleanUp(west, east);
        }

        context.complete();
    }

    private static ColonyPos at(BlockPos base, int dx) {
        return MinecraftTypeAdapter.toColonyPos(base.add(dx, 0, 0));
    }

    private static void cleanUp(Colony... colonies) {
        for (Colony colony : colonies) {
            VillageColonyMod.BUILDINGS.removeOfColony(colony.id());
            VillageColonyMod.COLONIES.remove(colony.id());
        }
    }
}
