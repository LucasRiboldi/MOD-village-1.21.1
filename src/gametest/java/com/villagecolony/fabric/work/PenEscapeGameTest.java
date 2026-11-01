package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.brain.WalkOverride;
import com.villagecolony.gametest.Arena;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;
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
        Arena.forceChunks(context, new BlockPos(-1, 0, -1), new BlockPos(11, 0, 11), force);
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

    /** Regra 159: observa a saída conduzida sem dar à navegação livre uma janela de até 39 tiques. */
    private static VillagerEntity workerBeforeNextScan(TestContext context, Colony colony) {
        long now = context.getWorld().getTime();
        long delay = Math.floorMod(PenEscape.SCAN_EVERY - 2 - now, PenEscape.SCAN_EVERY);
        int group = (int) (((now + delay + 2) / PenEscape.SCAN_EVERY) % 2);
        UUID candidate;
        do {
            candidate = UUID.randomUUID();
        } while (Math.floorMod(candidate.hashCode(), 2) != group);
        UUID id = candidate;
        VillagerEntity villager = EntityType.VILLAGER.create(context.getWorld());
        if (villager == null) {
            throw new AssertionError("não foi possível criar aldeão");
        }
        villager.setUuid(id);
        BlockPos start = context.getAbsolutePos(CENTER);
        villager.refreshPositionAndAngles(start.getX() + 0.5, start.getY(), start.getZ() + 0.5, 0, 0);
        villager.setBreedingAge(0);
        // TestContext itera o mesmo mapa de runAtTick: não registrar observadores neste callback.
        context.waitAndRun(delay, () -> {
            long time = context.getWorld().getTime();
            context.assertTrue(time % PenEscape.SCAN_EVERY == PenEscape.SCAN_EVERY - 2,
                    "spawn fora da fase anterior à inspeção: " + time);
            context.getWorld().spawnEntity(villager);
            VillageColonyMod.WORKERS.register(id, colony.id());
            VillageColonyMod.LOGGER.info("PEN_SPAWN uuid={} worldTime={} uuidGroup={} firstScan={}",
                    id, time, group, time + 2);
        });
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

    /** Diagnóstico da Regra 159: registra sem alterar a IA nem a cadência de inspeção. */
    private static void diagnose(TestContext context, VillagerEntity villager, int tick, boolean withGate) {
        if (tick > 60) {
            return;
        }
        long time = context.getWorld().getTime();
        int group = Math.floorMod(villager.getUuid().hashCode(), 2);
        var path = villager.getNavigation().getCurrentPath();
        var pos = villager.getPos();
        var velocity = villager.getVelocity();
        VillageColonyMod.LOGGER.info(
                "PEN_DIAG id={} t={} worldTime={} uuidGroup={} eligible={} pos={} vel={} ground={}"
                        + " navIdle={} node={} walk={} state={} fence={} gate={} support={}",
                villager.getUuid(), tick, time, group,
                time % PenEscape.SCAN_EVERY == 0 && (time / PenEscape.SCAN_EVERY) % 2 == group,
                String.format(java.util.Locale.ROOT, "%.4f,%.4f,%.4f", pos.x, pos.y, pos.z),
                String.format(java.util.Locale.ROOT, "%.4f,%.4f,%.4f", velocity.x, velocity.y, velocity.z),
                villager.isOnGround(), villager.getNavigation().isIdle(),
                path == null ? "-" : path.getCurrentNodeIndex() + "/" + path.getLength(),
                villager.getBrain().getOptionalRegisteredMemory(MemoryModuleType.WALK_TARGET)
                        .map(w -> w.getLookTarget().getBlockPos().toShortString()).orElse("-"),
                PenEscape.stateOf(villager.getUuid()), fenceIntegrity(context, withGate),
                context.getBlockState(GATE),
                context.getWorld().getBlockState(villager.getBlockPos().down()));
    }

    private static String fenceIntegrity(TestContext context, boolean withGate) {
        for (int x = LOW; x <= HIGH; x++) {
            for (int z = LOW; z <= HIGH; z++) {
                if (x != LOW && x != HIGH && z != LOW && z != HIGH) {
                    continue;
                }
                BlockPos at = new BlockPos(x, 2, z);
                var expected = withGate && at.equals(GATE) ? Blocks.OAK_FENCE_GATE : Blocks.OAK_FENCE;
                if (!context.getBlockState(at).isOf(expected)) {
                    return at.toShortString() + "=" + context.getBlockState(at);
                }
            }
        }
        return "ok";
    }

    /** O livro de falha tem limite de 1.024 caracteres; deixa margem para o título do teste. */
    private static String boundedFailure(String message) {
        return message.length() < 900 ? message : message.substring(0, 896) + "...";
    }

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
            if (context.getWorld().getEntity(villager.getUuid()) != villager) {
                return;
            }
            diagnose(context, villager, tick[0] + 1, withGate);
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

            // Sem portão, sair andando por cima da cerca também vale: o aldeão da colônia pula cerca (Regra 159).
            boolean conducted = escaped[0] && sawHow[0] || !withGate;

            if (done[0] || !conducted || !sawOpen[0] || isInside(context, villager) || !villager.isOnGround()
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
            String failure = "o aldeão não saiu do curral: está em " + where
                    + ", dentro=" + isInside(context, villager) + ", saindo=" + escaping
                    + ", saída começou=" + escaped[0] + ", " + how + " visto=" + sawHow[0]
                    + ", portão visto aberto=" + sawOpen[0]
                    + " — " + String.join(" | ", trace)
                    + ", portão aberto=" + gateOpen;
            VillageColonyMod.LOGGER.error("PEN_FAILURE id={} {}", villager.getUuid(), failure);
            context.assertTrue(false, boundedFailure(failure));
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "pen_escape_gate",
            tickLimit = TICKS)
    public void aWorkerInAPenWalksOutThroughTheGateAndTheGateCloses(TestContext context) {
        pen(context, true);

        Colony colony = colony(context);

        awaitOut(context, colony, workerBeforeNextScan(context, colony), true);
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "pen_escape_jump",
            tickLimit = TICKS)
    public void aWorkerInAPenWithoutAGateJumpsTheFence(TestContext context) {
        pen(context, false);

        Colony colony = colony(context);

        awaitOut(context, colony, workerBeforeNextScan(context, colony), false);
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
    /** Regra 159: isola a janela de inspeção com alvo fixo, navegação e salto reais. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "pen_escape_scan_race", tickLimit = 100)
    public void normalFenceNavigationCanLeaveBeforeTheScheduledScan(TestContext context) {
        pen(context, true);
        BlockPos floorMin = new BlockPos(-8, 0, -8);
        BlockPos floorMax = new BlockPos(18, 0, 18);
        Arena.forceChunks(context, floorMin, floorMax, true);
        for (int x = -8; x <= 18; x++) {
            for (int z = -8; z <= 18; z++) {
                if (x >= 0 && x <= 10 && z >= 0 && z <= 10) {
                    continue;
                }
                context.setBlockState(new BlockPos(x, 1, z), Blocks.STONE.getDefaultState());
                for (int y = 2; y <= 6; y++) {
                    context.setBlockState(new BlockPos(x, y, z), Blocks.AIR.getDefaultState());
                }
            }
        }
        Colony colony = colony(context);
        VillagerEntity[] worker = {null};
        long[] began = {-1};
        boolean[] airborne = {false};
        boolean[] escaping = {false};
        boolean[] finished = {false};
        BlockPos target = context.getAbsolutePos(new BlockPos(-3, 2, 5));
        context.runAtEveryTick(() -> {
            if (finished[0]) {
                return;
            }
            long time = context.getWorld().getTime();
            if (worker[0] == null) {
                // SCAN_GROUPS=2 em PenEscape: hash 2 pertence ao grupo zero; próxima inspeção em +39.
                if (time % (2 * PenEscape.SCAN_EVERY) != 1) {
                    return;
                }
                VillagerEntity villager = EntityType.VILLAGER.create(context.getWorld());
                if (villager == null) {
                    throw new AssertionError("não foi possível criar aldeão");
                }
                villager.setUuid(new UUID(0, 2));
                BlockPos start = context.getAbsolutePos(CENTER);
                villager.refreshPositionAndAngles(start.getX() + 0.5, start.getY(), start.getZ() + 0.5, 0, 0);
                villager.setBreedingAge(0);
                context.getWorld().spawnEntity(villager);
                VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
                worker[0] = villager;
                began[0] = time;
            }
            VillagerEntity villager = worker[0];
            long elapsed = time - began[0];
            diagnose(context, villager, (int) elapsed + 1, true);
            escaping[0] |= PenEscape.isEscaping(villager.getUuid());
            airborne[0] |= villager.getY() > context.getAbsolutePos(CENTER).getY() + 1;
            double fenceWest = context.getAbsolutePos(new BlockPos(LOW, 2, 5)).getX();
            boolean outside = villager.getBoundingBox().maxX < fenceWest && villager.isOnGround()
                    && Math.abs(villager.getY() - target.getY()) < 0.001
                    && context.getWorld().getBlockState(villager.getBlockPos().down()).isOf(Blocks.STONE);
            if (!outside && elapsed < 39) {
                // O alvo é a variável controlada; o motor continua calculando caminho e movendo o corpo.
                villager.getBrain().remember(MemoryModuleType.WALK_TARGET, new WalkTarget(target, 1.0f, 0));
                villager.getNavigation().startMovingTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 1.0);
                return;
            }
            finished[0] = true;
            try {
                context.assertTrue(outside && airborne[0] && elapsed < 39,
                        "saída completa antes da inspeção não demonstrada; elapsed=" + elapsed + "; veja PEN_DIAG");
                context.assertFalse(escaping[0], "PenEscape conduziu o experimento");
                context.assertTrue("ok".equals(fenceIntegrity(context, true)), "cerca ou portão alterados");
                context.assertFalse(context.getBlockState(GATE).get(FenceGateBlock.OPEN), "portão abriu");
                VillageColonyMod.LOGGER.info("PEN_RACE_PROVED elapsed={} firstScan={} worldTime={} uuid={}",
                        elapsed, began[0] + 39, time, villager.getUuid());
            } finally {
                PenEscape.forget(villager.getUuid());
                forget(context, colony, villager);
                Arena.forceChunks(context, floorMin, floorMax, false);
            }
            context.complete();
        });
    }
}
