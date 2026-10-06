package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.BuildSiteScanner;
import com.villagecolony.fabric.integration.SweepDeadline;
import com.villagecolony.fabric.integration.SweepState;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * A passagem extra da vila em foco continua a varredura em curso — estudo de
 * 01-10, §7-A — e só ela: obra aberta não ganha passagem.
 */
public final class SweepCadenceGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "sweep_cadence")
    public void anExtraPassContinuesThePausedSweep(TestContext context) {
        BlockPos center = context.getAbsolutePos(new BlockPos(1, 1, 1)).add(4000, 0, -4000);
        ColonyPos at = MinecraftTypeAdapter.toColonyPos(center);
        Colony colony = Colony.create(UUID.randomUUID(), at);
        UUID builder = UUID.randomUUID();

        colony.observe(at, 8);
        VillageColonyMod.COLONIES.register(colony);
        VillageColonyMod.WORKERS.register(builder, colony.id()).assign(ProfessionType.BUILDER);

        try {
            // O ciclo com o prazo vencido: a varredura para no piso e guarda o cursor.
            SweepDeadline.within(0, () -> ConstructionPlanner.plan(context.getWorld(), colony));

            context.assertTrue(VillageColonyMod.CONSTRUCTIONS.openOf(colony.id()).isEmpty()
                            && SweepState.stillLookingForALot(colony.id()),
                    "o cenário precisava de uma varredura pausada, sem obra aberta");

            OptionalInt before = SweepState.sweepPausedAt(colony.id());

            context.assertTrue(SweepCadence.passFor(context.getWorld(), colony),
                    "a varredura estava em curso e a passagem extra não rodou");

            boolean opened = VillageColonyMod.CONSTRUCTIONS.openOf(colony.id()).isPresent();
            boolean finished = !SweepState.stillLookingForALot(colony.id());
            OptionalInt after = SweepState.sweepPausedAt(colony.id());
            boolean advanced = before.isPresent() && after.isPresent() && after.getAsInt() > before.getAsInt();

            context.assertTrue(opened || finished || advanced,
                    "a passagem extra não andou: anel " + before + " antes, " + after + " depois");
        } finally {
            VillageColonyMod.CONSTRUCTIONS.removeOfColony(colony.id());
            VillageColonyMod.WORKERS.remove(builder);
            VillageColonyMod.COLONIES.remove(colony.id());
            BuildSiteScanner.clear(colony.id());
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "sweep_cadence")
    public void anOpenBuildGetsNoExtraPass(TestContext context) {
        BlockPos center = context.getAbsolutePos(new BlockPos(1, 1, 1)).add(-4000, 0, 4000);
        ColonyPos at = MinecraftTypeAdapter.toColonyPos(center);
        Colony colony = Colony.create(UUID.randomUUID(), at);
        UUID builder = UUID.randomUUID();

        colony.observe(at, 8);
        VillageColonyMod.COLONIES.register(colony);
        VillageColonyMod.WORKERS.register(builder, colony.id()).assign(ProfessionType.BUILDER);

        try {
            SweepDeadline.within(0, () -> ConstructionPlanner.plan(context.getWorld(), colony));

            Blueprint plan = Blueprint.of(ResourceId.vanilla("test_sweep_cadence"), List.of(
                    new BlueprintBlock(new ColonyPos(0, 0, 0), ResourceId.vanilla("cobblestone"))));
            VillageColonyMod.CONSTRUCTIONS.register(ConstructionProject.plan(colony.id(), plan, at));

            context.assertFalse(SweepCadence.passFor(context.getWorld(), colony),
                    "com obra aberta a passagem extra não pode procurar outro lote");
        } finally {
            VillageColonyMod.CONSTRUCTIONS.removeOfColony(colony.id());
            VillageColonyMod.WORKERS.remove(builder);
            VillageColonyMod.COLONIES.remove(colony.id());
            BuildSiteScanner.clear(colony.id());
        }

        context.complete();
    }

    /**
     * A entrada de produção, com jogador de verdade no mundo: sem jogador ela
     * não roda; com ele dentro da vila, ela escolhe a vila sozinha.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "sweep_cadence_player")
    public void theSecondTickPassRunsOnlyWithAPlayerInside(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos center = context.getAbsolutePos(new BlockPos(1, 1, 1)).add(6000, 0, 6000);
        ColonyPos at = MinecraftTypeAdapter.toColonyPos(center);
        Colony colony = Colony.create(UUID.randomUUID(), at);
        UUID builder = UUID.randomUUID();
        FakePlayer player = FakePlayer.get(world);

        colony.observe(at, 8);
        VillageColonyMod.COLONIES.register(colony);
        VillageColonyMod.WORKERS.register(builder, colony.id()).assign(ProfessionType.BUILDER);

        try {
            SweepDeadline.within(0, () -> ConstructionPlanner.plan(world, colony));
            // Um minuto de presença contínua (ADR-036 item 11).
            for (long tick = world.getTime() - Colony.SETTLE_TICKS; tick <= world.getTime(); tick += 20) {
                colony.attend(tick);
            }

            OptionalInt paused = SweepState.sweepPausedAt(colony.id());

            SweepCadence.pass(world);

            context.assertTrue(world.getPlayers().isEmpty() && SweepState.sweepPausedAt(colony.id()).equals(paused)
                            && VillageColonyMod.CONSTRUCTIONS.openOf(colony.id()).isEmpty(),
                    "sem jogador no mundo a passagem extra não podia rodar");

            player.refreshPositionAndAngles(center.getX() + 0.5, center.getY(), center.getZ() + 0.5, 0, 0);
            world.onPlayerConnected(player);

            SweepCadence.pass(world);

            boolean opened = VillageColonyMod.CONSTRUCTIONS.openOf(colony.id()).isPresent();
            boolean finished = !SweepState.stillLookingForALot(colony.id());
            OptionalInt after = SweepState.sweepPausedAt(colony.id());
            boolean advanced = paused.isPresent() && after.isPresent() && after.getAsInt() > paused.getAsInt();

            context.assertTrue(opened || finished || advanced,
                    "com o jogador dentro a passagem extra não andou: anel " + paused + " antes, " + after + " depois");
        } finally {
            world.removePlayer(player, Entity.RemovalReason.DISCARDED);
            VillageColonyMod.CONSTRUCTIONS.removeOfColony(colony.id());
            VillageColonyMod.WORKERS.remove(builder);
            VillageColonyMod.COLONIES.remove(colony.id());
            BuildSiteScanner.clear(colony.id());
        }

        context.complete();
    }
}
