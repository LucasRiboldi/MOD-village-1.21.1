package com.villagecolony.gametest;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.BuildSiteScanner;
import com.villagecolony.fabric.integration.SweepDeadline;
import com.villagecolony.fabric.integration.SweepState;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.OptionalInt;
import java.util.UUID;

/**
 * O prazo de relógio da varredura de lote — 2026-09-24.
 *
 * <p>O planejador foi 91% do tempo dos ciclos que estouraram o tique no
 * playtest de 24-09. O conserto dá à varredura um prazo, além do teto de
 * colunas, e ela guarda o cursor quando o tempo acaba. Este caso mede as
 * duas pontas: com prazo vencido a passagem para logo depois do piso de
 * colunas, e sem prazo ela vai até o teto, como antes.
 */
public class SweepDeadlineGameTest implements FabricGameTest {

    /** Um raio que passa de 1.024 colunas, para a passagem sem prazo pausar no teto. */
    private static final int RADIUS = 30;

    private static final ColonyPos SMALL_HOUSE = new ColonyPos(2, 3, 2);

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "sweep_deadline")
    public void anExpiredDeadlinePausesTheSweepEarlyAndNoDeadlineGoesToTheCap(TestContext context) {
        // Longe da arena: nenhuma obra ou rua de outro teste no raio.
        BlockPos center = context.getAbsolutePos(new BlockPos(1, 1, 1)).add(-5000, 0, 5000);
        ColonyPos at = MinecraftTypeAdapter.toColonyPos(center);
        UUID hurried = UUID.randomUUID();
        UUID patient = UUID.randomUUID();

        try {
            SweepDeadline.within(0, () -> BuildSiteScanner.find(
                    context.getWorld(), hurried, at, RADIUS, SMALL_HOUSE));
            BuildSiteScanner.find(context.getWorld(), patient, at, RADIUS, SMALL_HOUSE);

            OptionalInt early = SweepState.sweepPausedAt(hurried);
            OptionalInt capped = SweepState.sweepPausedAt(patient);

            context.assertTrue(early.isPresent(),
                    "com o prazo vencido a varredura não guardou o cursor");
            context.assertTrue(capped.isPresent(),
                    "sem prazo, um raio de " + RADIUS + " deveria pausar no teto de colunas");
            context.assertTrue(early.getAsInt() < capped.getAsInt(),
                    "o prazo não encurtou a passagem: anel " + early.getAsInt()
                            + " com prazo, " + capped.getAsInt() + " sem");
            // 64 colunas de piso cabem nos quatro primeiros anéis (1+8+16+24+32).
            context.assertTrue(early.getAsInt() <= 4,
                    "com prazo vencido a passagem foi além do piso: anel " + early.getAsInt());
        } finally {
            BuildSiteScanner.clear(hurried);
            BuildSiteScanner.clear(patient);
        }

        context.complete();
    }
}
