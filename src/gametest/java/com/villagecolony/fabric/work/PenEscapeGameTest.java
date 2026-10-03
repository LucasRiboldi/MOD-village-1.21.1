package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.brain.WalkOverride;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.UUID;

/**
 * O aldeão sai do curral andando de verdade — E52, segunda volta, 2026-10-01.
 *
 * <p>Na sessão das 09:16 o primeiro conserto abriu o portão e soltou o aldeão
 * dentro do curral um segundo depois; um mineiro e dois construtores nem foram
 * vistos, porque não congelavam. Aqui ninguém é marcado encalhado: o
 * trabalhador só é posto no curral, e o tique da colônia tem de achá-lo e
 * tirá-lo — pelo portão quando há portão, pulando a cerca quando não há.
 *
 * <p>O cenário é montado na arena do próprio teste, onde as entidades ticam: o
 * aldeão precisa andar e pular, e a 1,4 milhão de blocos ele não anda.
 */
public final class PenEscapeGameTest implements FabricGameTest {

    /** Curral de 7 × 7 de 2 a 8; o lado de dentro é de 3 a 7. */
    private static final int LOW = 2;

    private static final int HIGH = 8;

    private static final BlockPos CENTER = new BlockPos(5, 2, 5);

    private static final BlockPos GATE = new BlockPos(HIGH, 2, 5);

    /**
     * Os chunks do cenário ficam forçados enquanto o teste corre. Sem isso, o
     * aldeão que pula a cerca pode cair num chunk vizinho que não tica
     * entidade, e fica parado no ar — visto em 01-10, a 1 bloco da borda.
     */
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

    /** Chão de pedra de 0 a 10, ar por cima, e a cerca; com portão a leste, se pedido. */
    private static void pen(TestContext context, boolean withGate) {
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

        if (withGate) {
            context.setBlockState(GATE, Blocks.OAK_FENCE_GATE.getDefaultState()
                    .with(FenceGateBlock.FACING, Direction.EAST)
                    .with(FenceGateBlock.OPEN, false));
        }
    }

    /** Um trabalhador da colônia, sem ofício, de pé no meio do curral. */
    private static VillagerEntity workerInThePen(TestContext context, Colony colony) {
        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, CENTER);

        VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());

        return villager;
    }

    private static Colony colony(TestContext context) {
        Colony colony = Colony.create(UUID.randomUUID(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CENTER).add(-40, 0, 0)));
        VillageColonyMod.COLONIES.register(colony);

        return colony;
    }

    // Pelas coordenadas absolutas: a primeira versão comparava a posição
    // relativa e passou sem o aldeão nunca ter sido visto dentro.
    private static boolean isInside(TestContext context, VillagerEntity villager) {
        BlockPos at = villager.getBlockPos();
        BlockPos low = context.getAbsolutePos(new BlockPos(LOW, 2, LOW));
        BlockPos high = context.getAbsolutePos(new BlockPos(HIGH, 2, HIGH));

        return at.getX() > Math.min(low.getX(), high.getX()) && at.getX() < Math.max(low.getX(), high.getX())
                && at.getZ() > Math.min(low.getZ(), high.getZ()) && at.getZ() < Math.max(low.getZ(), high.getZ());
    }

    private static void forget(TestContext context, Colony colony, VillagerEntity villager) {
        forceChunks(context, false);

        UUID id = villager.getUuid();

        VillageColonyMod.WORKERS.remove(id);
        VillageColonyMod.COLONIES.remove(colony.id());
        WalkOverride.release(id);
        villager.discard();
    }

    private static void assertFenceIntact(TestContext context, boolean withGate) {
        for (int i = LOW; i <= HIGH; i++) {
            for (BlockPos at : new BlockPos[] {
                    new BlockPos(LOW, 2, i), new BlockPos(HIGH, 2, i),
                    new BlockPos(i, 2, LOW), new BlockPos(i, 2, HIGH)}) {

                if (withGate && at.equals(GATE)) {
                    continue;
                }

                context.expectBlock(Blocks.OAK_FENCE, at);
            }
        }
    }

    /** Quanto tempo a saída tem no teste: a medição, a caminhada e o portão fechando. */
    private static final int TICKS = 900;

    /**
     * Espera o aldeão estar fora, de pé e solto; então confere a cerca e
     * encerra. Se não sair a tempo, falha dizendo onde ele ficou.
     */
    private static void awaitOut(TestContext context, Colony colony, VillagerEntity villager,
            boolean withGate) {

        boolean[] done = {false};
        // O teste só vale se ele foi visto dentro e a saída foi conduzida.
        boolean[] startedInside = {isInside(context, villager)};
        boolean[] escaped = {false};
        // E pelo caminho certo: atravessou o portão aberto, ou esteve no ar
        // sobre a cerca — um não pode passar pelo outro.
        String how = withGate ? "THROUGH" : "AIRBORNE";
        boolean[] sawHow = {false};
        // Pelo portão é com o portão aberto: um portão que não abre e um
        // aldeão encostado nele já passaram por saída uma vez.
        boolean[] sawOpen = {!withGate};
        // As mudanças de estado da saída, com o tique e a posição: é o que a
        // falha conta, já que o log do mod some no meio da bateria.
        java.util.List<String> trace = new java.util.ArrayList<>();
        String[] last = {""};
        int[] tick = {0};

        context.assertTrue(startedInside[0], "o cenário não pôs o aldeão dentro do curral: "
                + villager.getBlockPos().toShortString());

        context.runAtEveryTick(() -> {
            boolean gateShut = !withGate || !context.getBlockState(GATE).get(FenceGateBlock.OPEN);

            escaped[0] |= PenEscape.isEscaping(villager.getUuid());
            tick[0]++;

            String now = PenEscape.stateOf(villager.getUuid());

            sawHow[0] |= now.startsWith(how);
            sawOpen[0] |= withGate && context.getBlockState(GATE).get(FenceGateBlock.OPEN);

            if (tick[0] % 100 == 50 && trace.size() < 30) {
                var brain = villager.getBrain();
                trace.add(tick[0] + ": walk=" + brain.getOptionalRegisteredMemory(
                                net.minecraft.entity.ai.brain.MemoryModuleType.WALK_TARGET)
                        .map(w -> w.getLookTarget().getBlockPos().toShortString()).orElse("-")
                        + " path=" + brain.hasMemoryModule(net.minecraft.entity.ai.brain.MemoryModuleType.PATH)
                        + " cantReach=" + brain.getOptionalRegisteredMemory(
                                net.minecraft.entity.ai.brain.MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE)
                        .map(String::valueOf).orElse("-")
                        + " navIdle=" + villager.getNavigation().isIdle()
                        + " activity=" + brain.getFirstPossibleNonCoreActivity().map(Object::toString).orElse("-")
                        + " ai=" + !villager.isAiDisabled());
            }

            if (!now.equals(last[0]) && trace.size() < 30) {
                trace.add(tick[0] + ": " + now + " @ " + villager.getBlockPos().toShortString()
                        + (villager.isOnGround() ? "" : " (air)"));
                last[0] = now;
            }

            if (done[0] || !escaped[0] || !sawHow[0] || !sawOpen[0] || isInside(context, villager) || !villager.isOnGround()
                    || PenEscape.isEscaping(villager.getUuid()) || !gateShut) {
                return;
            }

            done[0] = true;

            try {
                assertFenceIntact(context, withGate);
            } finally {
                forget(context, colony, villager);
            }

            context.complete();
        });

        context.runAtTick(TICKS - 5, () -> {
            if (done[0]) {
                return;
            }

            String where = villager.getBlockPos().toShortString()
                    + " (curral de " + context.getAbsolutePos(new BlockPos(LOW, 2, LOW)).toShortString()
                    + " a " + context.getAbsolutePos(new BlockPos(HIGH, 2, HIGH)).toShortString() + ")";
            boolean escaping = PenEscape.isEscaping(villager.getUuid());
            boolean gateOpen = withGate && context.getBlockState(GATE).get(FenceGateBlock.OPEN);

            forget(context, colony, villager);
            context.assertTrue(false, "o aldeão não saiu do curral: está em " + where
                    + ", dentro=" + isInside(context, villager) + ", saindo=" + escaping
                    + ", saída começou=" + escaped[0] + ", " + how + " visto=" + sawHow[0]
                    + ", portão visto aberto=" + sawOpen[0]
                    + " — " + String.join(" | ", trace)
                    + ", portão aberto=" + gateOpen);
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "pen_escape_gate",
            tickLimit = TICKS)
    public void aWorkerInAPenWalksOutThroughTheGateAndTheGateCloses(TestContext context) {
        pen(context, true);

        Colony colony = colony(context);

        awaitOut(context, colony, workerInThePen(context, colony), true);
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "pen_escape_jump",
            tickLimit = TICKS)
    public void aWorkerInAPenWithoutAGateJumpsTheFence(TestContext context) {
        pen(context, false);

        Colony colony = colony(context);

        awaitOut(context, colony, workerInThePen(context, colony), false);
    }

    /**
     * Controle: o mesmo trabalhador, do lado de fora, encostado na cerca, não
     * é tomado por preso — senão o teste acima passaria mandando todo mundo
     * pular cerca.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "pen_escape_control")
    public void aWorkerBesideAPenOnTheOutsideIsNotFencedIn(TestContext context) {
        pen(context, false);

        Colony colony = colony(context);
        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, new BlockPos(9, 2, 5));
        VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());

        context.waitAndRun(5, () -> {
            try {
                context.assertFalse(PenEscape.check(context.getWorld(), villager),
                        "do lado de fora da cerca ele foi lido como preso");
            } finally {
                forget(context, colony, villager);
            }

            context.complete();
        });
    }
}
