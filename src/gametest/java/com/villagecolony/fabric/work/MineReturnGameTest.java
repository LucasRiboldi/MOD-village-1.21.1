package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * O encalhado volta pela escada da mina, e o chão da vila fica inteiro —
 * pedido do autor, 2026-10-02.
 *
 * <p>Um bloco de pedra com uma escada cavada do fundo até a superfície. A
 * caixa da vila cobre tudo. O mineiro desceu pela escada (o rastro dele) e está
 * no fundo, encalhado. Ele tem de sair pela própria escada, sem cavar nada nos
 * {@link MineReturn#SHELL_DEPTH} blocos de cima — subir reto, como antes, abria
 * um buraco no meio da vila.
 */
public final class MineReturnGameTest implements FabricGameTest {

    private static final int SIDE = 9;

    private static final int LENGTH = 25;

    /** O topo do terreno: a superfície fica em y=TOP+1. */
    private static final int TOP = 12;

    private static final BlockPos BOTTOM = new BlockPos(4, 2, 4);

    /** Degraus da escada: os pés em (4+i, 2+i, 4). */
    private static final int STAIRS = TOP - 1;

    private static final int TICKS = 1_200;

    private static void forceChunks(TestContext context, boolean force) {
        BlockPos low = context.getAbsolutePos(new BlockPos(-1, 0, -1));
        BlockPos high = context.getAbsolutePos(new BlockPos(LENGTH + 1, 0, SIDE + 1));

        for (int cx = Math.min(low.getX(), high.getX()) >> 4; cx <= Math.max(low.getX(), high.getX()) >> 4; cx++) {
            for (int cz = Math.min(low.getZ(), high.getZ()) >> 4; cz <= Math.max(low.getZ(), high.getZ()) >> 4;
                    cz++) {
                context.getWorld().setChunkForced(cx, cz, force);
            }
        }
    }

    /** Pedra de y=1 a y=TOP, ar em cima, e a escada da mina do fundo à superfície. */
    private static List<BlockPos> terrainWithMineStairs(TestContext context) {
        forceChunks(context, true);

        for (int x = 0; x < LENGTH; x++) {
            for (int z = 0; z < SIDE; z++) {
                for (int y = 1; y <= TOP + 4; y++) {
                    context.setBlockState(new BlockPos(x, y, z),
                            (y > TOP ? Blocks.AIR : Blocks.STONE).getDefaultState());
                }
            }
        }

        // Da boca (na superfície) até o fundo: a ordem em que ele desceu.
        List<BlockPos> descent = new ArrayList<>();

        for (int i = STAIRS + 1; i >= 0; i--) {
            BlockPos feet = BOTTOM.add(i, i, 0);

            for (int up = 0; up <= 2; up++) {
                context.setBlockState(feet.up(up), Blocks.AIR.getDefaultState());
            }

            descent.add(context.getAbsolutePos(feet));
        }

        return descent;
    }

    /** Uma colônia com a caixa medida cobrindo o terreno todo, e o centro longe. */
    private static Colony villageOverTheTerrain(TestContext context) {
        BlockPos a = context.getAbsolutePos(new BlockPos(0, 0, 0));
        BlockPos b = context.getAbsolutePos(new BlockPos(LENGTH - 1, TOP + 4, SIDE - 1));

        Colony colony = Colony.create(UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(
                context.getAbsolutePos(new BlockPos(LENGTH / 2, TOP + 1, SIDE / 2))));
        colony.measure(new VillageBounds(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()),
                Math.min(a.getZ(), b.getZ()), Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()),
                Math.max(a.getZ(), b.getZ())));
        VillageColonyMod.COLONIES.register(colony);

        return colony;
    }

    /** Quantos blocos sólidos há na casca: as camadas de cima do terreno. */
    private static int solidInTheShell(TestContext context) {
        int solid = 0;

        for (int x = 0; x < LENGTH; x++) {
            for (int z = 0; z < SIDE; z++) {
                for (int y = TOP - MineReturn.SHELL_DEPTH + 1; y <= TOP; y++) {
                    if (!context.getBlockState(new BlockPos(x, y, z)).isAir()) {
                        solid++;
                    }
                }
            }
        }

        return solid;
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_return", tickLimit = TICKS)
    public void aStrandedMinerGoesBackUpTheMineStairsAndLeavesTheVillageGroundWhole(TestContext context) {
        List<BlockPos> descent = terrainWithMineStairs(context);
        Colony colony = villageOverTheTerrain(context);
        int shellBefore = solidInTheShell(context);

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, BOTTOM);
        UUID id = villager.getUuid();

        VillageColonyMod.WORKERS.register(id, colony.id()).assign(ProfessionType.MINER);

        for (BlockPos step : descent) {
            MineReturn.walked(id, step, step.equals(descent.get(0)));
        }

        StrandedWorkers.strandNow(id, context.getAbsolutePos(BOTTOM), "test");

        boolean[] done = {false};
        boolean[] retraced = {false};
        List<String> trace = new ArrayList<>();
        String[] last = {""};
        int[] tick = {0};

        Runnable cleanup = () -> {
            forceChunks(context, false);
            StrandedWorkers.forget(id);
            ClimbOut.forget(id);
            EscapeBackfill.forget(id);
            MineReturn.forget(id);
            VillageColonyMod.WORKERS.remove(id);
            VillageColonyMod.COLONIES.remove(colony.id());
            villager.discard();
        };

        context.runAtEveryTick(() -> {
            tick[0]++;

            String now = MineReturn.stateOf(id) + " / " + ClimbOut.stateOf(id);

            retraced[0] |= MineReturn.stateOf(id).startsWith("at step");

            if (!now.equals(last[0]) && trace.size() < 30) {
                trace.add(tick[0] + ": " + now + " @ " + villager.getBlockPos().toShortString());
                last[0] = now;
            }

            if (done[0] || StrandedWorkers.isStranded(id)) {
                return;
            }

            done[0] = true;

            int shellAfter = solidInTheShell(context);
            BlockPos out = villager.getBlockPos();

            cleanup.run();

            context.assertTrue(retraced[0], "ele saiu sem voltar pelo rastro — " + String.join(" | ", trace));
            context.assertTrue(shellAfter == shellBefore, "a casca da vila perdeu " + (shellBefore - shellAfter)
                    + " blocos; saiu em " + out.toShortString() + " — " + String.join(" | ", trace));
            context.complete();
        });

        context.runAtTick(TICKS - 5, () -> {
            if (done[0]) {
                return;
            }

            String where = villager.getBlockPos().toShortString();

            cleanup.run();
            context.assertTrue(false, "o mineiro não saiu pela escada da mina: está em " + where
                    + " — " + String.join(" | ", trace));
        });
    }

    /** A casca: dentro da caixa, os blocos de cima não se cavam; mais fundo, sim. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "mine_return_shell", tickLimit = 20)
    public void theTopOfTheVillageGroundIsNeverDugByAStrandedWorker(TestContext context) {
        terrainWithMineStairs(context);
        Colony colony = villageOverTheTerrain(context);

        try {
            BlockPos shallow = context.getAbsolutePos(new BlockPos(20, TOP - 1, 7));
            BlockPos deep = context.getAbsolutePos(new BlockPos(20, TOP - MineReturn.SHELL_DEPTH - 1, 7));

            context.assertFalse(StrandedEscape.mayDig(context.getWorld(), shallow,
                            context.getWorld().getBlockState(shallow)),
                    "a pedra a dois blocos da superfície da vila foi dada como cavável");
            context.assertTrue(StrandedEscape.mayDig(context.getWorld(), deep,
                            context.getWorld().getBlockState(deep)),
                    "abaixo da casca a fuga devia poder cavar");
        } finally {
            // "Fora de qualquer vila" não se testa aqui: a bateria divide um
            // mundo só, e a caixa de outra colônia de teste pode cobrir este
            // canto — visto na rodada de 02-10.
            VillageColonyMod.COLONIES.remove(colony.id());
            forceChunks(context, false);
        }

        context.complete();
    }
}
