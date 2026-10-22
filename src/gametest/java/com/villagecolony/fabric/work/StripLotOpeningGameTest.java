package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.coordination.VillageExpansion;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.Side;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.StripLot;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * O planejador abre a obra no lote da faixa e calça o caminho — A5, decisão do autor de 2026-10-22. É a
 * ligação de ponta a ponta: faixa pendente, lote no fundo dela, obra registrada, caminho calçado e a
 * faixa deixando de estar pendente.
 */
public class StripLotOpeningGameTest implements FabricGameTest {

    private static final int Y = 40;

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "strip_lot_opening")
    public void thePlannerOpensTheStripLotAndPavesItsPath(TestContext context) {
        ServerWorld world = context.getWorld();

        for (int x = 0; x <= 10; x++) {
            for (int z = 0; z <= 21; z++) {
                context.setBlockState(new BlockPos(x, Y, z), Blocks.DIRT.getDefaultState());
            }

            context.setBlockState(new BlockPos(x, Y, 5), Blocks.DIRT_PATH.getDefaultState());
        }

        for (int x = 0; x <= 8; x++) {
            context.setBlockState(new BlockPos(x, Y + 1, 7), Blocks.STONE.getDefaultState());
            context.setBlockState(new BlockPos(x, Y + 2, 7), Blocks.STONE.getDefaultState());
        }

        BlockPos low = context.getAbsolutePos(new BlockPos(0, Y - 2, 0));
        BlockPos high = context.getAbsolutePos(new BlockPos(10, Y + 8, 5));
        Colony colony = Colony.create(UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(
                context.getAbsolutePos(new BlockPos(5, Y + 1, 5))));

        colony.measure(new VillageBounds(low.getX(), low.getY(), low.getZ(), high.getX(), high.getY(), high.getZ()));
        VillageColonyMod.COLONIES.register(colony);
        VillageExpansion.annexed(colony.id(), Side.SOUTH);

        List<BlueprintBlock> floor = new ArrayList<>();

        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                floor.add(new BlueprintBlock(new ColonyPos(x, 0, z), ResourceId.vanilla("cobblestone")));
            }
        }

        Blueprint plan = Blueprint.of(ResourceId.vanilla("test_strip_hut"), floor);

        try {
            // Uma casa achada perto de outra rua não encerra a faixa (0.3.87: encerrava, e o lote
            // dentro dela nunca mais foi tentado na sessão).
            com.villagecolony.fabric.integration.RoadExtension.lotFound(colony.id());
            context.assertTrue(VillageExpansion.hasPendingStrip(colony.id()),
                    "um lote achado em outro lugar encerrou a faixa anexada");

            Optional<ConstructionProject> opened = Optional.empty();

            for (int pass = 0; pass < 20 && opened.isEmpty(); pass++) {
                opened = RoadGrowthPlanning.lotInTheStrip(world, colony, plan, List.of(plan), 1);
            }

            context.assertTrue(opened.isPresent(), "a faixa pendente não abriu obra");
            context.assertTrue(VillageColonyMod.CONSTRUCTIONS.openOf(colony.id()).isPresent(), "a obra não ficou registrada");
            context.assertFalse(VillageExpansion.hasPendingStrip(colony.id()), "a faixa continuou pendente com a obra aberta");

            int stripStart = context.getAbsolutePos(new BlockPos(0, Y, 6)).getZ();

            context.assertTrue(opened.get().origin().z() > stripStart,
                    "a obra não nasceu dentro da faixa: " + opened.get().origin());

            BlockPos throughTheGap = context.getAbsolutePos(new BlockPos(9, Y, 7));
            BlockPos gapOther = context.getAbsolutePos(new BlockPos(10, Y, 7));

            context.assertTrue(world.getBlockState(throughTheGap).isOf(Blocks.DIRT_PATH)
                            || world.getBlockState(gapOther).isOf(Blocks.DIRT_PATH),
                    "o caminho não foi calçado pela volta da duna");
        } finally {
            VillageColonyMod.CONSTRUCTIONS.removeOfColony(colony.id());
            VillageExpansion.lotFound(colony.id());
            VillageColonyMod.COLONIES.remove(colony.id());
            StripLot.clearAll();
        }

        context.complete();
    }
}
