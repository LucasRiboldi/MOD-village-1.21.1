package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * O golem da vila sai do curral — pedido do autor, 2026-10-02: <i>"os golens
 * da vila também devem poder sair pela porteira do curral e saltar as
 * cercas"</i>.
 *
 * <p>Três cenários: sem porteira ele pula; com porteira dupla ele passa pela
 * junta das duas folhas; com porteira simples — onde o corpo de 1,4 não cabe —
 * ele pula também, e a porteira não é aberta à toa.
 */
public final class PenGolemGameTest implements FabricGameTest {

    /** Curral de 7 × 7 de 2 a 8; o lado de dentro é de 3 a 7. */
    private static final int LOW = 2;

    private static final int HIGH = 8;

    private static final BlockPos CENTER = new BlockPos(5, 2, 5);

    private static final BlockPos GATE = new BlockPos(HIGH, 2, 5);

    private static final BlockPos TWIN = new BlockPos(HIGH, 2, 4);

    private static final int TICKS = 900;

    private static void forceChunks(TestContext context, boolean force) {
        BlockPos low = context.getAbsolutePos(new BlockPos(-1, 0, -1));
        BlockPos high = context.getAbsolutePos(new BlockPos(11, 0, 11));

        for (int cx = Math.min(low.getX(), high.getX()) >> 4; cx <= Math.max(low.getX(), high.getX()) >> 4; cx++) {
            for (int cz = Math.min(low.getZ(), high.getZ()) >> 4; cz <= Math.max(low.getZ(), high.getZ()) >> 4;
                    cz++) {
                context.getWorld().setChunkForced(cx, cz, force);
            }
        }
    }

    /** Chão de pedra, ar até y=6, a cerca; e as porteiras pedidas, a leste. */
    private static void pen(TestContext context, BlockPos... gates) {
        forceChunks(context, true);

        for (int x = 0; x <= 10; x++) {
            for (int z = 0; z <= 10; z++) {
                context.setBlockState(new BlockPos(x, 1, z), Blocks.STONE.getDefaultState());

                for (int y = 2; y <= 6; y++) {
                    context.setBlockState(new BlockPos(x, y, z), Blocks.AIR.getDefaultState());
                }

                boolean wall = (x == LOW || x == HIGH) && z >= LOW && z <= HIGH
                        || (z == LOW || z == HIGH) && x >= LOW && x <= HIGH;

                if (wall) {
                    context.setBlockState(new BlockPos(x, 2, z), Blocks.OAK_FENCE.getDefaultState());
                }
            }
        }

        for (BlockPos gate : gates) {
            context.setBlockState(gate, Blocks.OAK_FENCE_GATE.getDefaultState()
                    .with(FenceGateBlock.FACING, Direction.EAST)
                    .with(FenceGateBlock.OPEN, false));
        }
    }

    private static boolean isInside(TestContext context, IronGolemEntity golem) {
        BlockPos at = golem.getBlockPos();
        BlockPos low = context.getAbsolutePos(new BlockPos(LOW, 2, LOW));
        BlockPos high = context.getAbsolutePos(new BlockPos(HIGH, 2, HIGH));

        return at.getX() > Math.min(low.getX(), high.getX()) && at.getX() < Math.max(low.getX(), high.getX())
                && at.getZ() > Math.min(low.getZ(), high.getZ()) && at.getZ() < Math.max(low.getZ(), high.getZ());
    }

    /**
     * Põe um golem da vila no meio do curral, numa colônia, e espera ele sair
     * do jeito {@code how} ("THROUGH" pela porteira, "AIRBORNE" no pulo).
     * Com {@code gateMustStayShut}, a porteira simples não pode ser aberta.
     */
    private static void golemEscapes(TestContext context, String how, boolean gateMustStayShut) {
        Colony colony = Colony.create(UUID.randomUUID(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CENTER).add(-40, 0, 0)));
        VillageColonyMod.COLONIES.register(colony);

        IronGolemEntity golem = context.spawnEntity(EntityType.IRON_GOLEM, CENTER);
        UUID id = golem.getUuid();

        boolean[] done = {false};
        boolean[] started = {false};
        boolean[] sawHow = {false};
        boolean[] gateOpened = {false};
        List<String> trace = new ArrayList<>();
        String[] last = {""};
        int[] tick = {0};

        Runnable cleanup = () -> {
            forceChunks(context, false);
            PenEscape.forget(id);
            VillageColonyMod.COLONIES.remove(colony.id());
            golem.discard();
        };

        context.assertTrue(isInside(context, golem), "o cenário não pôs o golem dentro do curral");

        context.runAtEveryTick(() -> {
            tick[0]++;
            started[0] |= PenEscape.isEscaping(id);

            String now = PenEscape.stateOf(id);

            sawHow[0] |= now.startsWith(how);
            gateOpened[0] |= gateMustStayShut && context.getBlockState(GATE).get(FenceGateBlock.OPEN);

            if (!now.equals(last[0]) && trace.size() < 30) {
                trace.add(tick[0] + ": " + now + " @ " + golem.getBlockPos().toShortString()
                        + (golem.isOnGround() ? "" : " (air)"));
                last[0] = now;
            }

            if (done[0] || !started[0] || !sawHow[0] || isInside(context, golem) || !golem.isOnGround()
                    || PenEscape.isEscaping(id)) {
                return;
            }

            done[0] = true;
            cleanup.run();

            context.assertFalse(gateOpened[0], "a porteira simples foi aberta, e o golem não cabe nela");
            context.complete();
        });

        context.runAtTick(TICKS - 5, () -> {
            if (done[0]) {
                return;
            }

            String where = golem.getBlockPos().toShortString();

            cleanup.run();
            context.assertTrue(false, "o golem não saiu do curral: está em " + where
                    + ", saída começou=" + started[0] + ", " + how + " visto=" + sawHow[0]
                    + " — " + String.join(" | ", trace));
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "pen_golem_jump", tickLimit = TICKS)
    public void aGolemInAPenWithoutAGateJumpsTheFence(TestContext context) {
        pen(context);
        golemEscapes(context, "AIRBORNE", false);
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "pen_golem_double_gate",
            tickLimit = TICKS)
    public void aGolemWalksOutThroughADoubleGate(TestContext context) {
        pen(context, GATE, TWIN);
        golemEscapes(context, "THROUGH", false);
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "pen_golem_single_gate",
            tickLimit = TICKS)
    public void aGolemDoesNotSqueezeThroughASingleGateItJumps(TestContext context) {
        pen(context, GATE);
        golemEscapes(context, "AIRBORNE", true);
    }
}
