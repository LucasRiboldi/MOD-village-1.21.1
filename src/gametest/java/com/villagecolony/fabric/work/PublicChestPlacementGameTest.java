package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.gametest.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Optional;
import java.util.UUID;

/** A vila sem baú público ganha um dentro de uma casa, encostado na parede e longe da porta. */
public class PublicChestPlacementGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "public_chest_placement", tickLimit = 20)
    public void aVillageWithoutAPublicChestGetsOneInAHouse(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos min = context.getAbsolutePos(new BlockPos(1, 1, 1));
        BlockPos max = min.add(6, 3, 6);
        BlockPos door = min.add(3, 1, 0);
        BlockPos ownAt = min.add(9, 1, 3);

        for (BlockPos at : BlockPos.iterate(min, max)) {
            boolean shell = at.getY() == min.getY() || at.getY() == max.getY()
                    || at.getX() == min.getX() || at.getX() == max.getX()
                    || at.getZ() == min.getZ() || at.getZ() == max.getZ();

            world.setBlockState(at, (shell ? Blocks.STONE : Blocks.AIR).getDefaultState(), 2);
        }

        world.setBlockState(door, Blocks.OAK_DOOR.getDefaultState()
                .with(DoorBlock.FACING, Direction.SOUTH).with(DoorBlock.HALF, DoubleBlockHalf.LOWER), 2);
        world.setBlockState(door.up(), Blocks.OAK_DOOR.getDefaultState()
                .with(DoorBlock.FACING, Direction.SOUTH).with(DoorBlock.HALF, DoubleBlockHalf.UPPER), 2);
        world.setBlockState(ownAt.down(), Blocks.STONE.getDefaultState());
        world.setBlockState(ownAt, Blocks.CHEST.getDefaultState());

        ColonyPos own = MinecraftTypeAdapter.toColonyPos(ownAt);
        Colony colony = Colony.create(UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(min.add(3, 1, 3)));
        ColonyFixture owned = ColonyFixture.create().owning(colony);
        UUID carpenter = UUID.randomUUID();

        // A casa entra antes da colônia: registrada depois, ela empurraria a caixa 16 blocos
        // (Regra 56) para cima das arenas vizinhas da bateria.
        VillageColonyMod.BUILDINGS.register(new Building(UUID.randomUUID(), colony.id(),
                ResourceId.vanilla("village/plains/houses/plains_small_house_1"),
                MinecraftTypeAdapter.toColonyPos(min), MinecraftTypeAdapter.toColonyPos(max)));
        colony.measure(new VillageBounds(min.getX() - 2, min.getY() - 2, min.getZ() - 2,
                ownAt.getX() + 2, max.getY() + 2, max.getZ() + 2));
        VillageColonyMod.COLONIES.register(colony);
        VillageColonyMod.WORKERS.register(carpenter, colony.id());
        owned.owning(carpenter);
        VillageColonyMod.STORAGES.register(WorkerStorage.of(carpenter, own));
        ChestDepositor.deposit(world, own, Items.OAK_PLANKS, 8);

        try {
            Optional<BlockPos> placed = PublicChestPlacement.ensure(world, colony);

            context.assertTrue(placed.isPresent(), "a vila sem baú público não ganhou um");

            BlockPos chest = placed.get();

            context.assertTrue(world.getBlockState(chest).isOf(Blocks.CHEST), "não há baú no lugar escolhido");
            context.assertTrue(chest.getX() > min.getX() && chest.getX() < max.getX()
                    && chest.getZ() > min.getZ() && chest.getZ() < max.getZ(), "o baú ficou fora da casa: " + chest);
            context.assertTrue(Math.max(Math.abs(chest.getX() - door.getX()), Math.abs(chest.getZ() - door.getZ()))
                    > PublicChestPlacement.DOOR_CLEARANCE, "o baú ficou na frente da porta: " + chest);
            context.assertTrue(Direction.Type.HORIZONTAL.stream()
                            .anyMatch(side -> world.getBlockState(chest.offset(side)).isOf(Blocks.STONE)),
                    "o baú não está encostado numa parede: " + chest);
            context.assertTrue(PublicChests.of(world, colony.id(), chest).contains(MinecraftTypeAdapter.toColonyPos(chest)),
                    "o baú novo não conta como baú público");
            context.assertTrue(PublicChestPlacement.ensure(world, colony).isEmpty(), "pôs um segundo baú público");
        } finally {
            VillageColonyMod.BUILDINGS.removeOfColony(colony.id());
            ((Inventory) world.getBlockEntity(ownAt)).clear();

            for (BlockPos at : BlockPos.iterate(min, max.add(4, 0, 0))) {
                if (world.getBlockEntity(at) instanceof Inventory inventory) {
                    inventory.clear();
                }

                world.setBlockState(at, Blocks.AIR.getDefaultState(), 2);
            }

            world.setBlockState(ownAt.down(), Blocks.AIR.getDefaultState());
            owned.cleanUp();
            PublicChestPlacement.clearAll();
        }

        context.complete();
    }
}
