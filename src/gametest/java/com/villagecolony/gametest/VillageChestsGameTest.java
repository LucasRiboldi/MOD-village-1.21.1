package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.VillageChests;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

/**
 * O cache da varredura de baús livres não esconde baú — 2026-09-25.
 *
 * <p>O {@code VillageChests} guarda a varredura cara por um segundo. O
 * risco de todo cache é responder o mundo de antes: o jogador põe o baú
 * com o material que a obra espera, e a colônia não o vê. Este teste pede
 * a lista, muda o mundo <b>no mesmo tique</b> — dentro do prazo — e exige
 * a resposta nova.
 *
 * <p>O baú conta como "dentro de casa" por estar na caixa de um
 * {@link Building} da colônia: o mundo do gametest não tem vila gerada.
 */
public class VillageChestsGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_chests_cache")
    public void theCacheSeesAChestPlacedAndOneBrokenInTheSameTick(TestContext context) {
        ServerWorld world = context.getWorld();

        BlockPos first = new BlockPos(1, 2, 1);
        // Longe o bastante para não formar baú duplo com o primeiro.
        BlockPos second = new BlockPos(1, 2, 3);

        ColonyPos corner = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(first));
        Colony colony = Colony.create(UUID.randomUUID(), corner);

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);

        try {
            VillageColonyMod.BUILDINGS.register(new Building(
                    UUID.randomUUID(),
                    colony.id(),
                    new ResourceId("minecraft", "village/plains/houses/plains_small_house_1"),
                    corner,
                    new ColonyPos(corner.x() + 3, corner.y() + 2, corner.z() + 3)));

            context.setBlockState(first, Blocks.CHEST.getDefaultState());

            ColonyPos firstAt = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(first));
            ColonyPos secondAt = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(second));

            List<ColonyPos> before = VillageChests.around(world, colony.center(), List.of());

            context.assertTrue(
                    before.contains(firstAt),
                    "o baú dentro da casa não foi achado nem sem cache: " + before);

            context.setBlockState(second, Blocks.CHEST.getDefaultState());

            List<ColonyPos> placed = VillageChests.around(world, colony.center(), List.of());

            context.assertTrue(
                    placed.contains(secondAt),
                    "baú posto no mesmo tique ficou invisível — o cache não foi"
                            + " invalidado pelo evento de block entity: " + placed);

            context.setBlockState(first, Blocks.AIR.getDefaultState());

            List<ColonyPos> broken = VillageChests.around(world, colony.center(), List.of());

            context.assertTrue(
                    !broken.contains(firstAt),
                    "baú quebrado continuou na lista — a colônia iria buscar"
                            + " material onde não há baú: " + broken);

            context.assertTrue(
                    broken.contains(secondAt),
                    "o baú que ficou sumiu da lista: " + broken);
        } finally {
            owned.cleanUp();
        }

        context.complete();
    }
}
