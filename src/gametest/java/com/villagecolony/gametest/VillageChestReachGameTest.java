package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ColonyChests;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

/**
 * Os baús da colônia são os da vila, e só eles — 2026-10-01, pedido do autor:
 * <i>"aumentar a zona de varredura dos baús para analisar todos baús dentro da
 * vila, evitar baús fora de alcance com essa regra"</i>.
 *
 * <ul>
 *   <li>vila medida: todo baú livre dentro da caixa entra, de dentro ou de
 *       fora de casa;</li>
 *   <li>fora da caixa está fora de alcance — o baú dentro de casa, e o do
 *       trabalhador também;</li>
 *   <li>baú de trabalhador de outra colônia nunca é estoque livre — o canal
 *       pelo qual os testes de carpinteiro e pedreiro, a 13 blocos um do
 *       outro, gastavam o baú um do outro.</li>
 * </ul>
 */
public class VillageChestReachGameTest implements FabricGameTest {

    private static final ResourceId HOUSE =
            new ResourceId("minecraft", "village/plains/houses/plains_small_house_1");

    private static ColonyPos at(TestContext context, int x, int y, int z) {
        return MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(x, y, z)));
    }

    private static void chest(TestContext context, int x, int y, int z) {
        context.setBlockState(new BlockPos(x, y, z), Blocks.CHEST.getDefaultState());
    }

    /** A vila medida, de (0, 0) a (12, 12) da arena, com a altura do chão. */
    private static Colony measuredVillage(TestContext context) {
        ColonyPos low = at(context, 0, 1, 0);
        ColonyPos high = at(context, 12, 3, 12);
        Colony colony = Colony.create(UUID.randomUUID(), at(context, 6, 2, 6));

        colony.measure(new VillageBounds(
                Math.min(low.x(), high.x()), low.y(), Math.min(low.z(), high.z()),
                Math.max(low.x(), high.x()), high.y(), Math.max(low.z(), high.z())));
        VillageColonyMod.COLONIES.register(colony);

        return colony;
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "chest_reach_inside")
    public void everyFreeChestInsideTheMeasuredVillageIsFound(TestContext context) {
        ServerWorld world = context.getWorld();
        Colony colony = measuredVillage(context);
        ColonyFixture owned = ColonyFixture.create().owning(colony);

        try {
            // No campo, longe de qualquer casa: antes só contava o de dentro de casa.
            chest(context, 11, 2, 11);

            List<ColonyPos> chests = ColonyChests.nearestFirst(world, colony.id(), colony.center());

            context.assertTrue(chests.contains(at(context, 11, 2, 11)),
                    "o baú no campo, dentro da vila medida, não foi achado: " + chests);
        } finally {
            owned.cleanUp();
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "chest_reach_outside")
    public void aChestOutsideTheVillageIsOutOfReachEvenIndoorsOrClaimed(TestContext context) {
        ServerWorld world = context.getWorld();
        Colony colony = measuredVillage(context);
        UUID worker = UUID.randomUUID();
        ColonyFixture owned = ColonyFixture.create().owning(colony).owning(worker);

        // A casa é de outra vila. Se fosse desta, a caixa cresceria para
        // contê-la (Emenda 6) e o baú estaria, com razão, dentro da vila.
        UUID neighbour = UUID.randomUUID();

        try {
            // Dentro de uma casa, mas fora da caixa: a regra antiga (raio de
            // 64 em volta do centro, dentro de casa) o contava.
            VillageColonyMod.BUILDINGS.register(new Building(UUID.randomUUID(), neighbour, HOUSE,
                    at(context, 15, 1, 0), at(context, 19, 4, 4)));
            chest(context, 17, 2, 2);

            // E o baú de um trabalhador da colônia, também fora da caixa.
            chest(context, 17, 2, 8);
            VillageColonyMod.WORKERS.register(worker, colony.id());
            VillageColonyMod.STORAGES.register(WorkerStorage.of(worker, at(context, 17, 2, 8)));

            List<ColonyPos> chests = ColonyChests.nearestFirst(world, colony.id(), colony.center());

            context.assertFalse(chests.contains(at(context, 17, 2, 2)),
                    "o baú de casa fora da vila entrou na conta: " + chests);
            context.assertFalse(chests.contains(at(context, 17, 2, 8)),
                    "o baú do trabalhador fora da vila entrou na conta: " + chests);
        } finally {
            VillageColonyMod.BUILDINGS.removeOfColony(neighbour);
            owned.cleanUp();
        }

        context.complete();
    }

    /**
     * Duas colônias sem caixa, como nos testes de fabricação: cada baú dentro
     * da obra aberta da sua colônia, e cada um de um trabalhador. A colônia A
     * não pode ver o baú do trabalhador de B como estoque livre.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "chest_reach_claimed")
    public void aChestClaimedByAnotherColonysWorkerIsNeverFree(TestContext context) {
        ServerWorld world = context.getWorld();
        Colony a = Colony.create(UUID.randomUUID(), at(context, 2, 2, 2));
        Colony b = Colony.create(UUID.randomUUID(), at(context, 9, 2, 2));
        UUID workerB = UUID.randomUUID();
        ColonyFixture ownedA = ColonyFixture.create().owning(a);
        ColonyFixture ownedB = ColonyFixture.create().owning(b).owning(workerB);

        try {
            VillageColonyMod.COLONIES.register(a);
            VillageColonyMod.COLONIES.register(b);

            // A casa de B cobre o baú de B: "dentro de casa" para a regra do raio.
            VillageColonyMod.BUILDINGS.register(new Building(UUID.randomUUID(), b.id(), HOUSE,
                    at(context, 8, 1, 1), at(context, 10, 3, 3)));
            chest(context, 9, 2, 2);
            VillageColonyMod.WORKERS.register(workerB, b.id());
            VillageColonyMod.STORAGES.register(WorkerStorage.of(workerB, at(context, 9, 2, 2)));

            List<ColonyPos> seenByA = ColonyChests.nearestFirst(world, a.id(), a.center());

            context.assertFalse(seenByA.contains(at(context, 9, 2, 2)),
                    "a colônia A tomou o baú do trabalhador de B como estoque livre: " + seenByA);
        } finally {
            ownedB.cleanUp();
            ownedA.cleanUp();
        }

        context.complete();
    }
}
