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

import java.util.Optional;
import java.util.UUID;

/**
 * Regra 312: o mineiro preso 5 vezes no mesmo lugar não vai para a cama; cava a subida rumo à borda da vila, furando a
 * casca da vila e pondo bloco onde falta piso.
 */
public class ClimbOutwardGameTest implements FabricGameTest {

    private static final BlockPos FEET = new BlockPos(3, 3, 3);

    /** Dentro da casca da vila, a escada de sempre não sai; a rumo à borda cava o degrau do lado de fora. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "climb_outward_shell", tickLimit = 20)
    public void theStepTowardTheEdgeDigsThroughTheVillageShell(TestContext context) {
        pit(context, false);
        BlockPos feet = context.getAbsolutePos(FEET);
        Colony colony = shellOver(context);
        BlockPos away = feet.east(20);

        try {
            Optional<StrandedEscape.Step> usual = StrandedEscape.planStep(context.getWorld(), feet, away);
            Optional<ClimbOutward.Move> outward = ClimbOutward.plan(context.getWorld(), feet, away, false);

            context.assertTrue(usual.isEmpty(), "a casca da vila devia barrar a escada de sempre: " + usual);
            context.assertTrue(outward.map(ClimbOutward.Move::standAt).equals(Optional.of(feet.east().up())),
                    "Regra 312: o degrau devia sair para a borda (leste), um acima: " + outward);
        } finally {
            VillageColonyMod.COLONIES.remove(colony.id());
        }

        context.complete();
    }

    /** Rumo à borda, o piso que falta ganha bloco. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "climb_outward_place", tickLimit = 20)
    public void theMissingFloorTowardTheEdgeGetsABlock(TestContext context) {
        pit(context, true);
        BlockPos feet = context.getAbsolutePos(FEET);
        Optional<ClimbOutward.Move> move = ClimbOutward.plan(context.getWorld(), feet, feet.east(20), true);

        context.assertTrue(move.map(ClimbOutward.Move::standAt).equals(Optional.of(feet.east().up()))
                        && move.get().place().equals(Optional.of(feet.east())),
                "Regra 312: devia pôr bloco no piso à frente, rumo à borda: " + move);
        context.complete();
    }

    /** Cinco tentativas no mesmo lugar: o mineiro sobe rumo à borda, e não é levado para a cama nem para o centro. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "climb_outward_trigger", tickLimit = 100)
    public void fiveAttemptsSendTheMinerTowardTheEdge(TestContext context) {
        pit(context, false);
        Colony colony = Colony.create(UUID.randomUUID(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(1, 6, 1))));
        VillageColonyMod.COLONIES.register(colony);
        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, FEET);
        UUID id = villager.getUuid();
        VillageColonyMod.WORKERS.register(id, colony.id()).assign(ProfessionType.MINER);

        context.runAtTick(40, () -> {
            BlockPos feet = villager.getBlockPos();

            try {
                for (int i = 0; i < BedTeleport.STUCK_ATTEMPTS; i++) {
                    BedTeleport.attempt(id, feet);
                }

                ClimbOut.pass(context.getWorld(), villager, id, feet.west(20), null);

                context.assertTrue(ClimbOut.stateOf(id).startsWith("OUTWARD"),
                        "Regra 312: depois de 5 tentativas o mineiro devia subir rumo à borda: " + ClimbOut.stateOf(id)
                                + ", em " + villager.getBlockPos().toShortString() + " (estava em " + feet.toShortString() + ")");
            } finally {
                ClimbOut.forget(id);
                VillageColonyMod.WORKERS.remove(id);
                VillageColonyMod.COLONIES.remove(colony.id());
                villager.discard();
            }

            context.complete();
        });
    }

    /**
     * Um poço de pedra de 1 x 1, 3 de fundo, com os pés em {@link #FEET}. Com {@code gapEast}, o bloco a leste na altura
     * dos pés é ar, e o chão a leste também: falta o piso do degrau.
     */
    private static void pit(TestContext context, boolean gapEast) {
        for (int x = 1; x <= 5; x++) {
            for (int z = 1; z <= 5; z++) {
                for (int y = 1; y <= 5; y++) {
                    boolean hole = x == FEET.getX() && z == FEET.getZ() && y >= FEET.getY();
                    context.setBlockState(new BlockPos(x, y, z), hole ? Blocks.AIR.getDefaultState()
                            : Blocks.STONE.getDefaultState());
                }
            }
        }

        if (gapEast) {
            for (int y = FEET.getY() - 1; y <= 5; y++) {
                context.setBlockState(FEET.east().withY(y), Blocks.AIR.getDefaultState());
            }

            context.setBlockState(FEET.east().withY(FEET.getY() - 2), Blocks.STONE.getDefaultState());
        }
    }

    /** Uma colônia cuja caixa cobre o poço, com o chão no alto dele: o poço inteiro é casca da vila. */
    private static Colony shellOver(TestContext context) {
        BlockPos low = context.getAbsolutePos(new BlockPos(0, 0, 0));
        BlockPos high = context.getAbsolutePos(new BlockPos(8, 5, 8));
        Colony colony = Colony.create(UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(FEET)));
        colony.restoreBounds(new VillageBounds(Math.min(low.getX(), high.getX()), high.getY(), Math.min(low.getZ(), high.getZ()),
                Math.max(low.getX(), high.getX()), high.getY() + 4, Math.max(low.getZ(), high.getZ())));
        VillageColonyMod.COLONIES.register(colony);

        return colony;
    }
}
