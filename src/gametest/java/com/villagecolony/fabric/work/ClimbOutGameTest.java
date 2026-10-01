package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Block;
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
 * O encalhado sobe até a superfície, andando e pulando de verdade — 2026-10-01.
 *
 * <p>O mineiro 199ad062 cavou os 32 degraus da fuga e parou a y=56, quinze
 * abaixo da superfície, "until someone frees it". Aqui o aldeão é marcado
 * preso no fundo e o tique da colônia tem de levá-lo para cima, sem prazo de
 * degraus e sem a navegação Vanilla:
 *
 * <ul>
 *   <li><b>escada</b> — fechado dentro de pedra maciça, dez blocos abaixo;
 *   <li><b>pilar</b> — num poço de tijolo (que ele não pode quebrar), com
 *       pedra acima da cabeça: sem degrau possível, ele quebra o que está
 *       acima, pula e põe o bloco debaixo dos pés.
 * </ul>
 */
public final class ClimbOutGameTest implements FabricGameTest {

    /**
     * O terreno: {@link #LENGTH} para o leste, onde fica a vila, e
     * {@link #SIDE} de largura. Comprido o bastante para a escada inteira e o
     * anel de 3 blocos do {@code isOut} ficarem dentro dele: numa torre
     * estreita, a beira da torre já é "fora".
     */
    private static final int SIDE = 9;

    private static final int LENGTH = 25;

    /** A superfície do bloco de terreno: o topo dele está em y=TOP. */
    private static final int TOP = 12;

    private static final BlockPos BOTTOM = new BlockPos(4, 2, 4);

    private static final int TICKS = 3_000;

    /** Os chunks do cenário ficam forçados: o aldeão que sobe não pode congelar num vizinho sem tique. */
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

    /** Um bloco de {@code wall} de y=1 a y=TOP, com {@code core} na coluna do meio, e ar em cima. */
    private static void terrain(TestContext context, Block wall, Block core) {
        forceChunks(context, true);

        for (int x = 0; x < LENGTH; x++) {
            for (int z = 0; z < SIDE; z++) {
                boolean middle = x == BOTTOM.getX() && z == BOTTOM.getZ();

                for (int y = 1; y <= TOP + 4; y++) {
                    Block block = y > TOP ? Blocks.AIR : middle ? core : wall;

                    context.setBlockState(new BlockPos(x, y, z), block.getDefaultState());
                }
            }
        }

        // O fundo onde ele está: dois de ar.
        context.setBlockState(BOTTOM, Blocks.AIR.getDefaultState());
        context.setBlockState(BOTTOM.up(), Blocks.AIR.getDefaultState());

        // E um bloco natural na parede, perto do topo: a pedra de cima dá
        // nove blocos para os dez que o pilar põe, e o último sai da parede
        // quando o bolso esvazia.
        context.setBlockState(new BlockPos(BOTTOM.getX() - 1, TOP - 1, BOTTOM.getZ()), core.getDefaultState());
    }

    /** Põe o trabalhador no fundo, numa colônia com o centro longe, e o marca preso. */
    private static VillagerEntity strandedAtTheBottom(TestContext context, Colony[] colony) {
        colony[0] = Colony.create(UUID.randomUUID(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(60, TOP + 1, BOTTOM.getZ()))));
        VillageColonyMod.COLONIES.register(colony[0]);

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, BOTTOM);
        VillageColonyMod.WORKERS.register(villager.getUuid(), colony[0].id()).assign(ProfessionType.MINER);
        StrandedWorkers.strandNow(villager.getUuid(), context.getAbsolutePos(BOTTOM), "test");

        return villager;
    }

    /**
     * Espera ele ser devolvido à escala com a cabeça na superfície — a fuga o
     * dá por fora com os pés um abaixo do topo do terreno — e com a maior
     * parte da subida feita pelo jeito {@code how}. Se não, conta onde parou.
     */
    private static void awaitOut(TestContext context, VillagerEntity villager, Colony colony, ClimbOut.Mode how) {
        UUID id = villager.getUuid();
        int bottom = context.getAbsolutePos(BOTTOM).getY();
        int out = context.getAbsolutePos(new BlockPos(0, TOP, 0)).getY();
        boolean[] done = {false};
        String[] last = {"none"};
        List<String> trace = new ArrayList<>();
        int[] tick = {0};

        context.runAtEveryTick(() -> {
            String now = ClimbOut.stateOf(id);

            if (!now.equals("none")) {
                last[0] = now;
            }

            if (++tick[0] % 100 == 0 && trace.size() < 40) {
                trace.add(tick[0] + ": " + now + " @ " + villager.getBlockPos().toShortString());
            }

            if (done[0] || StrandedWorkers.isStranded(id) || villager.getBlockY() < out) {
                return;
            }

            done[0] = true;

            try {
                int[] upBy = upBy(last[0]);
                int climbed = out - bottom;

                int byHow = how == ClimbOut.Mode.STAIRS ? upBy[0] : how == ClimbOut.Mode.PILLAR ? upBy[1] : upBy[2];

                context.assertTrue(byHow * 2 > climbed,
                        "saiu, mas não pelo " + how + ": " + last[0] + " para " + climbed + " níveis");
            } finally {
                forget(context, colony, villager);
            }

            context.complete();
        });

        context.runAtTick(TICKS - 5, () -> {
            if (done[0]) {
                return;
            }

            String where = villager.getBlockPos().toShortString() + " (fora a partir de y=" + out + ")";
            boolean stranded = StrandedWorkers.isStranded(id);

            forget(context, colony, villager);
            context.assertTrue(false, "o encalhado não saiu: está em " + where + ", preso=" + stranded
                    + ", último estado " + last[0] + " — " + String.join(" | ", trace));
        });
    }

    // "... up by stairs/pillar/tunnel A/B/C" -> {A, B, C}
    private static int[] upBy(String state) {
        String[] parts = state.substring(state.lastIndexOf(' ') + 1).split("/");
        int[] up = new int[parts.length];

        for (int i = 0; i < parts.length; i++) {
            up[i] = Integer.parseInt(parts[i]);
        }

        return up;
    }

    private static void forget(TestContext context, Colony colony, VillagerEntity villager) {
        forceChunks(context, false);

        UUID id = villager.getUuid();

        StrandedWorkers.forget(id);
        ClimbOut.forget(id);
        EscapeBackfill.forget(id);
        VillageColonyMod.WORKERS.remove(id);
        VillageColonyMod.COLONIES.remove(colony.id());
        villager.discard();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "climb_out_stairs", tickLimit = TICKS)
    public void aStrandedWorkerSealedInStoneClimbsOutByStairs(TestContext context) {
        terrain(context, Blocks.STONE, Blocks.STONE);

        Colony[] colony = new Colony[1];

        awaitOut(context, strandedAtTheBottom(context, colony), colony[0], ClimbOut.Mode.STAIRS);
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "climb_out_pillar", tickLimit = TICKS)
    public void aStrandedWorkerInABrickShaftPillarsUpThroughTheStone(TestContext context) {
        // Tijolo não é terreno natural: nenhum degrau, nenhuma parede para
        // cavar. Só a pedra acima da cabeça, que vira o bloco dos pés.
        terrain(context, Blocks.STONE_BRICKS, Blocks.STONE);

        Colony[] colony = new Colony[1];

        awaitOut(context, strandedAtTheBottom(context, colony), colony[0], ClimbOut.Mode.PILLAR);
    }
}
