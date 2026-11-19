package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.WorldPathTerrain;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Set;
import java.util.UUID;

/** Regra 321: a porta do jogador, fora de construção registrada e de peça da vila, é entrada do grafo. */
public class PlayerDoorsGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "player_doors", tickLimit = 5)
    public void aPlayerDoorInTheVillageIsAnEntrance(TestContext context) {
        for (int x = 0; x <= 6; x++) {
            for (int z = 0; z <= 6; z++) {
                context.setBlockState(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK.getDefaultState());
            }
        }

        // Uma casinha de tábua: a porta olha para o norte (para dentro), e a frente fica ao sul, em céu aberto.
        BlockPos door = new BlockPos(3, 2, 3);
        for (int x = 2; x <= 4; x++) {
            for (int y = 2; y <= 4; y++) {
                context.setBlockState(new BlockPos(x, y, 2), Blocks.OAK_PLANKS.getDefaultState());
            }
        }
        BlockState lower = Blocks.OAK_DOOR.getDefaultState().with(DoorBlock.FACING, Direction.NORTH);
        context.setBlockState(door, lower);
        context.setBlockState(door.up(), lower.with(DoorBlock.HALF, DoubleBlockHalf.UPPER));

        BlockPos origin = context.getAbsolutePos(BlockPos.ORIGIN);
        Colony colony = Colony.create(UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(origin));
        colony.restoreBounds(new VillageBounds(origin.getX() - 1, origin.getY(), origin.getZ() - 1,
                origin.getX() + 8, origin.getY() + 6, origin.getZ() + 8));

        try {
            VillageColonyMod.COLONIES.register(colony);

            StructureFronts.Reading reading = StructureFronts.read(context.getWorld(), colony,
                    new WorldPathTerrain(context.getWorld(), colony.id()), Set.of());
            BlockPos front = context.getAbsolutePos(new BlockPos(3, 1, 4));
            context.assertTrue(reading.fronts().contains(MinecraftTypeAdapter.toColonyPos(front)),
                    "Regra 321: a porta do jogador não virou entrada em " + front.toShortString() + ": " + reading.fronts());
        } finally {
            VillageColonyMod.COLONIES.remove(colony.id());
        }

        context.complete();
    }
}
