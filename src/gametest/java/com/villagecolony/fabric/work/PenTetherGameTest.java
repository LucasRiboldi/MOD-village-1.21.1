package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.coordination.WorkClock;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.gametest.ColonyFixture;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.Set;

/**
 * Regra 284: enquanto atravessa o portão, o animal fica preso a 2 blocos do lugar de dentro (o que a corda do jogo usa,
 * {@code setPositionTarget}), e o passeio e o pânico dele não o levam para longe; a amarra solta quando ele entra.
 */
public class PenTetherGameTest implements FabricGameTest {

    /** O portão fica em (3, 1, 2); fora, (3, 1, 1); dentro, (3, 1, 3) e (3, 1, 4). */
    private static ShepherdPens.Pen pen(TestContext context) {
        BlockPos gate = context.getAbsolutePos(new BlockPos(3, 1, 2));
        BlockPos inside = context.getAbsolutePos(new BlockPos(3, 1, 3));
        BlockPos outside = context.getAbsolutePos(new BlockPos(3, 1, 1));
        context.setBlockState(new BlockPos(3, 1, 2), Blocks.OAK_FENCE_GATE.getDefaultState().with(FenceGateBlock.OPEN, true));
        return new ShepherdPens.Pen(gate, inside, outside,
                Set.of(inside, context.getAbsolutePos(new BlockPos(3, 1, 4))), false);
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "pen_lead_alignment", tickLimit = 20)
    public void theLeashStaysAttachedUntilTheAnimalReachesTheGateFront(TestContext context) {
        ShepherdPens.Pen pen = pen(context);
        VillagerEntity shepherd = context.spawnEntity(EntityType.VILLAGER, new BlockPos(3, 1, 1));
        CowEntity cow = context.spawnEntity(EntityType.COW, new BlockPos(5, 1, 1));
        cow.attachLeash(shepherd, true);
        ShepherdHerding.Herd herd = new ShepherdHerding.Herd(cow.getUuid(), pen.gate(), context.getWorld().getTime());
        herd.pen = pen;
        herd.phase = ShepherdHerding.Phase.LEAD;
        try {
            ShepherdHerding.step(context.getWorld(), shepherd.getUuid(), herd);
            context.assertTrue(herd.phase == ShepherdHerding.Phase.LEAD && cow.getLeashHolder() == shepherd,
                    "N26: a corda soltou com o animal dois blocos ao lado da entrada");
        } finally {
            WorkTargets.clear(shepherd.getUuid());
            shepherd.discard();
            cow.discard();
        }
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "pen_lead_wait", tickLimit = 20)
    public void waitingAtTheGateReplacesTheOldFetchDestination(TestContext context) {
        ShepherdPens.Pen pen = pen(context);
        context.getWorld().setBlockState(pen.gate(), Blocks.OAK_FENCE_GATE.getDefaultState());
        VillagerEntity shepherd = context.spawnEntity(EntityType.VILLAGER, new BlockPos(3, 1, 1));
        CowEntity cow = context.spawnEntity(EntityType.COW, new BlockPos(7, 1, 1));
        cow.attachLeash(shepherd, true);
        WorkTargets.set(shepherd.getUuid(), cow.getBlockPos(), 2);
        ShepherdHerding.Herd herd = new ShepherdHerding.Herd(cow.getUuid(), pen.gate(), context.getWorld().getTime());
        herd.pen = pen;
        herd.phase = ShepherdHerding.Phase.LEAD;
        try {
            ShepherdHerding.step(context.getWorld(), shepherd.getUuid(), herd);
            context.assertTrue(context.getWorld().getBlockState(pen.gate()).get(FenceGateBlock.OPEN),
                    "N27: o pastor chama o animal com a porteira fechada");
            context.assertTrue(WorkTargets.of(shepherd.getUuid()).filter(pen.outside()::equals).isPresent()
                            && WorkTargets.arrivalOf(shepherd.getUuid()) == 0,
                    "N26: o pastor espera na entrada mas ainda caminha para o destino antigo");
            BlockPos near = pen.outside().east(2);
            cow.refreshPositionAndAngles(near.getX() + 0.5, near.getY(), near.getZ() + 0.5, 0, 0);
            ShepherdHerding.step(context.getWorld(), shepherd.getUuid(), herd);
            context.assertTrue(WorkTargets.of(shepherd.getUuid()).filter(ShepherdPens.deeper(pen)::equals).isPresent()
                            && cow.getLeashHolder() == shepherd,
                    "N27: com o animal perto, o pastor não entra primeiro mantendo a corda");
        } finally {
            WorkTargets.clear(shepherd.getUuid());
            shepherd.discard();
            cow.discard();
        }
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "pen_enter_stops_fetch", tickLimit = 20)
    public void startingEntryClearsTheShepherdsPreviousDestination(TestContext context) {
        ShepherdPens.Pen pen = pen(context);
        VillagerEntity shepherd = context.spawnEntity(EntityType.VILLAGER, new BlockPos(2, 1, 1));
        CowEntity cow = context.spawnEntity(EntityType.COW, new BlockPos(3, 1, 1));
        cow.attachLeash(shepherd, true);
        WorkTargets.set(shepherd.getUuid(), cow.getBlockPos(), 2);
        ShepherdHerding.Herd herd = new ShepherdHerding.Herd(cow.getUuid(), pen.gate(), context.getWorld().getTime());
        herd.pen = pen;
        herd.phase = ShepherdHerding.Phase.LEAD;
        try {
            ShepherdHerding.step(context.getWorld(), shepherd.getUuid(), herd);
            context.assertTrue(herd.phase == ShepherdHerding.Phase.ENTER && cow.hasPositionTarget(),
                    "o animal alinhado não iniciou a entrada com a amarra do curral");
            context.assertTrue(WorkTargets.of(shepherd.getUuid()).isEmpty(),
                    "N26: o pastor continua perseguindo o animal durante a entrada");
        } finally {
            WorkTargets.clear(shepherd.getUuid());
            PenTether.release(cow);
            shepherd.discard();
            cow.discard();
        }
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "pen_lead_slope", tickLimit = 280)
    public void theAnimalWalksFromASideLedgeThroughTheGate(TestContext context) {
        int height = 160;
        com.villagecolony.gametest.Arena.forceChunks(context, BlockPos.ORIGIN, new BlockPos(10, 0, 9), true);
        for (int x = 0; x <= 10; x++) {
            for (int z = 0; z <= 9; z++) {
                context.setBlockState(new BlockPos(x, height - 1, z), Blocks.GRASS_BLOCK.getDefaultState());
                if (x >= 8 && z <= 1) {
                    context.setBlockState(new BlockPos(x, height, z), Blocks.DIRT.getDefaultState());
                }
                if (((x == 1 || x == 7) && z >= 2 && z <= 8)
                        || ((z == 2 || z == 8) && x >= 1 && x <= 7)) {
                    context.setBlockState(new BlockPos(x, height, z), Blocks.OAK_FENCE.getDefaultState()
                            .with(FenceBlock.NORTH, (x == 1 || x == 7) && z > 2)
                            .with(FenceBlock.SOUTH, (x == 1 || x == 7) && z < 8)
                            .with(FenceBlock.WEST, (z == 2 || z == 8) && x > 1)
                            .with(FenceBlock.EAST, (z == 2 || z == 8) && x < 7));
                }
            }
        }
        BlockPos gate = context.getAbsolutePos(new BlockPos(4, height, 2));
        context.getWorld().setBlockState(gate, Blocks.OAK_FENCE_GATE.getDefaultState());
        Set<BlockPos> reach = new java.util.HashSet<>();
        for (int x = 2; x <= 6; x++) {
            for (int z = 3; z <= 7; z++) {
                reach.add(context.getAbsolutePos(new BlockPos(x, height, z)));
            }
        }
        ShepherdPens.Pen pen = new ShepherdPens.Pen(gate, gate.south(), gate.north(), reach, false);
        VillagerEntity shepherd = context.spawnEntity(EntityType.VILLAGER, new BlockPos(4, height, 0));
        CowEntity cow = context.spawnEntity(EntityType.COW, new BlockPos(8, height + 1, 1));
        cow.attachLeash(shepherd, true);
        ShepherdHerding.Herd herd = new ShepherdHerding.Herd(cow.getUuid(), gate, context.getWorld().getTime());
        herd.pen = pen;
        herd.phase = ShepherdHerding.Phase.LEAD;
        boolean[] finished = {false};
        context.getWorld().setTimeOfDay(1_000);
        StringBuilder history = new StringBuilder();
        int[] ticks = {0};
        context.runAtEveryTick(() -> {
            if (!finished[0] && context.getWorld().getTime() % 5 == 0) {
                finished[0] = !ShepherdHerding.step(context.getWorld(), shepherd.getUuid(), herd);
            }
            // Mesma chegada do destino público e mesma cadência de fechamento da produção.
            WorkTargets.of(shepherd.getUuid()).ifPresentOrElse(target -> {
                var memory = net.minecraft.entity.ai.brain.MemoryModuleType.WALK_TARGET;
                if (shepherd.getBrain().getOptionalRegisteredMemory(memory)
                        .filter(walk -> walk.getLookTarget().getBlockPos().equals(target)).isEmpty()) {
                    shepherd.getBrain().remember(memory, new net.minecraft.entity.ai.brain.WalkTarget(
                            target, 0.5f, WorkTargets.arrivalOf(shepherd.getUuid())));
                }
            }, () -> shepherd.getBrain().forget(net.minecraft.entity.ai.brain.MemoryModuleType.WALK_TARGET));
            if (context.getWorld().getTime() % 20 == 0) FencedIn.tick(context.getWorld());
            if (++ticks[0] % 80 == 0) {
                history.append(" | t=").append(ticks[0]).append(" idade=").append(cow.age).append(" fase=").append(herd.phase)
                        .append(" posição=").append(cow.getPos().subtract(
                                net.minecraft.util.math.Vec3d.of(context.getAbsolutePos(BlockPos.ORIGIN))))
                        .append(" chão=").append(cow.isOnGround())
                        .append(" destino=").append(cow.getNavigation().getTargetPos())
                        .append(" parado=").append(cow.getNavigation().isIdle())
                        .append(" corda=").append(cow.getLeashHolder() == shepherd);
            }
        });
        context.runAtTick(240, () -> {
            try {
                context.assertTrue(cow.age > 100, "a arena não manteve a vaca ativa" + history);
                context.assertTrue(finished[0] && ShepherdPens.isInside(pen, cow),
                        "N26: a vaca não entrou andando a partir do desnível lateral; fase=" + herd.phase
                                + history);
                context.assertTrue(cow.getLeashHolder() == null && !cow.hasPositionTarget(),
                        "a entrada terminou mas deixou corda ou amarra no animal");
                context.assertFalse(context.getWorld().getBlockState(gate).get(FenceGateBlock.OPEN),
                        "a porteira ficou aberta depois da entrada");
            } finally {
                finished[0] = true;
                WorkTargets.clear(shepherd.getUuid());
                PenTether.release(cow);
                com.villagecolony.gametest.Arena.forceChunks(context, BlockPos.ORIGIN, new BlockPos(10, 0, 9), false);
                shepherd.discard();
                cow.discard();
            }
            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "pen_tether_walk", tickLimit = 20)
    public void theAnimalIsTetheredInsideWhileWalkingThrough(TestContext context) {
        ShepherdPens.Pen pen = pen(context);
        CowEntity cow = context.spawnEntity(EntityType.COW, new BlockPos(3, 1, 1));

        context.waitAndRun(2, () -> {
            ShepherdPens.startIn(context.getWorld(), pen, cow);
            boolean tethered = cow.hasPositionTarget();
            BlockPos at = cow.getPositionTarget();
            float range = cow.getPositionTargetRange();
            cow.discard();

            context.assertTrue(tethered && at.equals(ShepherdPens.deeper(pen)) && range == PenTether.RANGE,
                    "Regra 284: o animal atravessa o portão sem amarra: preso=" + tethered + " em=" + at + " raio=" + range);
            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "pen_tether_release", tickLimit = 20)
    public void theTetherLetsGoOnceTheAnimalIsInside(TestContext context) {
        ShepherdPens.Pen pen = pen(context);
        ColonyFixture fixture = ColonyFixture.colonyAt(context, new BlockPos(1, 1, 1));
        VillagerEntity shepherd = context.spawnEntity(EntityType.VILLAGER, new BlockPos(2, 1, 0));
        CowEntity cow = context.spawnEntity(EntityType.COW, new BlockPos(3, 1, 1));

        context.waitAndRun(2, () -> {
            boolean tethered;
            boolean stillTethered;
            try {
                if (!WorkClock.isWorkTime(context.getWorld().getTimeOfDay())) {
                    context.getWorld().setTimeOfDay(1_000);
                }
                fixture.owning(shepherd.getUuid());
                VillageColonyMod.WORKERS.register(shepherd.getUuid(), fixture.colony().id()).assign(ProfessionType.SHEPHERD);

                ShepherdPens.startIn(context.getWorld(), pen, cow);
                tethered = cow.hasPositionTarget();

                // Entrou: está no lugar de dentro e não encosta mais no portão.
                BlockPos deeper = ShepherdPens.deeper(pen);
                cow.refreshPositionAndAngles(deeper.getX() + 0.5, deeper.getY(), deeper.getZ() + 0.5, 0, 0);
                ShepherdHerding.Herd herd = new ShepherdHerding.Herd(cow.getUuid(), pen.gate(), context.getWorld().getTime());
                herd.pen = pen;
                herd.phase = ShepherdHerding.Phase.ENTER;
                herd.walking = context.getWorld().getTime();
                ShepherdHerding.step(context.getWorld(), shepherd.getUuid(), herd);
                stillTethered = cow.hasPositionTarget();
            } finally {
                fixture.cleanUp();
                cow.discard();
            }

            context.assertTrue(tethered, "Regra 284: o animal atravessa o portão sem amarra");
            context.assertFalse(stillTethered, "Regra 284: a amarra do portão ficou depois que o animal entrou");
            context.complete();
        });
    }
}
