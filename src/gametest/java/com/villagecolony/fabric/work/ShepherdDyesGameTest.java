package com.villagecolony.fabric.work;

import com.villagecolony.core.coordination.SupplyException;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ColonyChests;
import com.villagecolony.fabric.integration.Supplies;
import com.villagecolony.gametest.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/** O corante das flores no ócio do pastor — Regra 272 (autor, 2026-11-13, ADR-055 item 11). */
public class ShepherdDyesGameTest implements FabricGameTest {

    private static List<ColonyPos> chest(TestContext context) {
        BlockPos at = new BlockPos(1, Y + 1, 1);

        context.setBlockState(at, Blocks.CHEST.getDefaultState());

        return List.of(MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(at)));
    }

    /** Sessenta acima do piso, longe do teto da arena e do cenário dos outros testes. */
    private static final int Y = 60;

    private static void ground(TestContext context) {
        for (int x = 0; x <= 7; x++) {
            for (int z = 0; z <= 7; z++) {
                context.setBlockState(new BlockPos(x, Y, z), Blocks.GRASS_BLOCK.getDefaultState());
            }
        }
    }

    private static void clean(TestContext context) {
        for (int x = 0; x <= 7; x++) {
            for (int z = 0; z <= 7; z++) {
                for (int up = 0; up <= 3; up++) {
                    context.setBlockState(new BlockPos(x, Y + up, z), Blocks.AIR.getDefaultState());
                }
            }
        }
    }

    /** Com papoula perto, o pastor colhe a flor e o corante vermelho vai para o baú; nada surge. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "shepherd_dyes_flower", tickLimit = 40)
    public void aPoppyNearTheVillageBecomesRedDye(TestContext context) {
        ServerWorld world = context.getWorld();

        ground(context);

        ColonyFixture fixture = ColonyFixture.colonyAt(context, new BlockPos(4, Y + 1, 4));
        List<ColonyPos> chests = chest(context);
        BlockPos poppy = new BlockPos(5, Y + 1, 5);

        context.setBlockState(poppy, Blocks.POPPY.getDefaultState());

        VillagerEntity shepherd = context.spawnEntity(EntityType.VILLAGER, new BlockPos(5, Y + 1, 4));

        try {
            ShepherdDyes.forget(fixture.colony().id());
            boolean busy = ShepherdDyes.step(world, fixture.colony(), shepherd, chests);

            context.assertTrue(busy, "Regra 272: o pastor ao lado da papoula não colheu");
            context.assertTrue(ColonyChests.countIn(world, chests, Items.RED_DYE) >= 1,
                    "Regra 272: a papoula não virou corante vermelho no baú");
            context.assertTrue(world.getBlockState(context.getAbsolutePos(poppy)).isAir(), "a papoula continua no chão");
            context.assertTrue(Supplies.sessionCount(fixture.colony().id(), SupplyException.valueOf("FLOWER_DYE")) == 0,
                    "Regra 272: com a flor ao alcance, o corante surgiu");
        } finally {
            fixture.cleanUp();
            clean(context);
        }

        context.complete();
    }

    /** Sem flor nenhuma em volta, o corante da cor que falta surge e é declarado. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "shepherd_dyes_none", tickLimit = 40)
    public void withoutFlowersTheMissingDyeAppears(TestContext context) {
        ServerWorld world = context.getWorld();

        ground(context);

        ColonyFixture fixture = ColonyFixture.colonyAt(context, new BlockPos(4, Y + 1, 4));
        List<ColonyPos> chests = chest(context);
        VillagerEntity shepherd = context.spawnEntity(EntityType.VILLAGER, new BlockPos(5, Y + 1, 4));

        try {
            ShepherdDyes.forget(fixture.colony().id());
            ShepherdDyes.step(world, fixture.colony(), shepherd, chests);

            context.assertTrue(Supplies.sessionCount(fixture.colony().id(), SupplyException.valueOf("FLOWER_DYE")) == 1,
                    "Regra 272: sem flor, o corante que falta não surgiu");
        } finally {
            fixture.cleanUp();
            clean(context);
        }

        context.complete();
    }
}
