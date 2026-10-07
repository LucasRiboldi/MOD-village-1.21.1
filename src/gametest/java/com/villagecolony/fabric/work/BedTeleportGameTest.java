package com.villagecolony.fabric.work;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.BedPart;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.GlobalPos;

/** O aldeão preso vai para a cama dele (ADR-036 15). */
public class BedTeleportGameTest implements FabricGameTest {

    private static final BlockPos BED = new BlockPos(6, 2, 6);

    private static final BlockPos STAND = new BlockPos(1, 2, 1);

    /** Cama de duas metades, com o pé em {@code foot} e a cabeceira ao norte. */
    private static BlockPos placeBed(TestContext context, BlockPos foot) {
        BlockState state = Blocks.RED_BED.getDefaultState().with(BedBlock.FACING, Direction.NORTH);

        context.setBlockState(foot, state.with(BedBlock.PART, BedPart.FOOT));
        context.setBlockState(foot.north(), state.with(BedBlock.PART, BedPart.HEAD));

        return context.getAbsolutePos(foot.north());
    }

    /** Caixa de vidro em volta da cama: paredes e teto. */
    private static void enclose(TestContext context, BlockPos foot) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -2; dz <= 1; dz++) {
                for (int dy = 0; dy <= 2; dy++) {
                    BlockPos at = foot.add(dx, dy, dz);
                    boolean inside = dx == 0 && (dz == 0 || dz == -1) && dy < 2;

                    if (!inside) {
                        context.setBlockState(at, Blocks.GLASS.getDefaultState());
                    }
                }
            }
        }
    }

    /** Chão de pedra sob o cenário: o vazio da arena não dá caminho. */
    private static void floor(TestContext context) {
        for (int x = 0; x <= 8; x++) {
            for (int z = 0; z <= 8; z++) {
                context.setBlockState(new BlockPos(x, 1, z), Blocks.STONE.getDefaultState());
            }
        }
    }

    private static VillagerEntity villagerHomedAt(TestContext context, BlockPos bed) {
        floor(context);

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, STAND);
        ServerWorld world = context.getWorld();

        villager.getBrain().remember(MemoryModuleType.HOME, GlobalPos.create(world.getRegistryKey(), bed));

        return villager;
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "bed_teleport", tickLimit = 100)
    public void withNoPathHomeTheVillagerGoesToBed(TestContext context) {
        BlockPos bed = placeBed(context, BED);
        VillagerEntity villager = villagerHomedAt(context, bed);
        enclose(context, BED);

        context.waitAndRun(20, () -> {
            boolean went = NightHome.check(context.getWorld(), villager, villager.getUuid());

            context.assertTrue(went, "a cama está fechada em vidro e o aldeão não foi para ela");
            context.assertTrue(villager.getBlockPos().isWithinDistance(bed, 1.5),
                    "o aldeão ficou em " + villager.getBlockPos().toShortString() + ", longe da cama");
            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "bed_teleport", tickLimit = 100)
    public void withAPathHomeTheVillagerWalks(TestContext context) {
        BlockPos bed = placeBed(context, BED);
        VillagerEntity villager = villagerHomedAt(context, bed);

        context.waitAndRun(20, () -> {
            context.assertTrue(villager.isOnGround(), "o aldeão ainda não pousou; o controle não mediria nada");

            // O cérebro anda sozinho à noite; o caminho em curso dele não é o que se mede.
            villager.getNavigation().stop();

            BlockPos before = villager.getBlockPos();
            boolean went = NightHome.check(context.getWorld(), villager, villager.getUuid());

            context.assertTrue(!went, "havia caminho até a cama e o aldeão foi teletransportado, de "
                    + before.toShortString() + " para a cama em " + bed.toShortString());
            context.assertTrue(villager.getBlockPos().equals(before), "o aldeão mudou de lugar sem andar");
            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "bed_teleport", tickLimit = 100)
    public void fiveAttemptsInThePlaceSendTheVillagerToBed(TestContext context) {
        BlockPos bed = placeBed(context, BED);
        VillagerEntity villager = villagerHomedAt(context, bed);

        context.waitAndRun(20, () -> {
            ServerWorld world = context.getWorld();
            BlockPos feet = villager.getBlockPos();

            for (int attempt = 1; attempt < BedTeleport.STUCK_ATTEMPTS; attempt++) {
                BedTeleport.attempt(villager.getUuid(), feet);
            }

            context.assertTrue(!BedTeleport.tooManyAttempts(world, villager, villager.getUuid()),
                    "foi para a cama antes da quinta tentativa");

            // Uma tentativa longe dali recomeça a conta.
            BedTeleport.attempt(villager.getUuid(), feet.add(5, 0, 0));
            context.assertTrue(BedTeleport.attempts(villager.getUuid()) == 1,
                    "tentativa em outro lugar não recomeçou a conta");

            for (int attempt = 1; attempt < BedTeleport.STUCK_ATTEMPTS; attempt++) {
                BedTeleport.attempt(villager.getUuid(), feet.add(5, 0, 0));
            }

            context.assertTrue(BedTeleport.tooManyAttempts(world, villager, villager.getUuid()),
                    "cinco tentativas no mesmo lugar e o aldeão não foi para a cama");
            context.assertTrue(villager.getBlockPos().isWithinDistance(bed, 1.5),
                    "o aldeão ficou em " + villager.getBlockPos().toShortString() + ", longe da cama");
            context.complete();
        });
    }

    /** O levado para a cama fecha a porta da casa (ADR-037 V2, C3). */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "bed_teleport", tickLimit = 100)
    public void goingToBedClosesTheHouseDoor(TestContext context) {
        BlockPos bed = placeBed(context, BED);
        VillagerEntity villager = villagerHomedAt(context, bed);
        BlockPos near = new BlockPos(BED.getX() + 2, BED.getY(), BED.getZ());

        for (BlockPos door : new BlockPos[] {near}) {
            context.setBlockState(door, Blocks.OAK_DOOR.getDefaultState()
                    .with(net.minecraft.block.DoorBlock.OPEN, true)
                    .with(net.minecraft.block.DoorBlock.HALF, net.minecraft.block.enums.DoubleBlockHalf.LOWER));
            context.setBlockState(door.up(), Blocks.OAK_DOOR.getDefaultState()
                    .with(net.minecraft.block.DoorBlock.OPEN, true)
                    .with(net.minecraft.block.DoorBlock.HALF, net.minecraft.block.enums.DoubleBlockHalf.UPPER));
        }

        context.waitAndRun(20, () -> {
            for (int attempt = 0; attempt < BedTeleport.STUCK_ATTEMPTS; attempt++) {
                BedTeleport.attempt(villager.getUuid(), villager.getBlockPos());
            }

            context.assertTrue(BedTeleport.tooManyAttempts(context.getWorld(), villager, villager.getUuid()),
                    "o aldeão não foi levado para a cama");
            context.assertTrue(!context.getBlockState(near).get(net.minecraft.block.DoorBlock.OPEN),
                    "a porta da casa ficou aberta");
            context.complete();
        });
    }
}
