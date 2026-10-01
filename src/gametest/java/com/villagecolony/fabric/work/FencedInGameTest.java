package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.UUID;

/**
 * Preso num curral na superfície — E52, playtest de 2026-10-01.
 *
 * <p>Oito trabalhadores, entre eles os três lenhadores, ficaram quatro horas
 * num curral de vila: a fuga do E47 os via no nível do chão e os soltava
 * "after 0 steps", de volta à cerca. Sem madeira, a casa parou.
 *
 * <p>Cada cenário é montado longe da arena, numa plataforma própria: a busca
 * anda até 12 blocos, e a construção de outro teste mudaria a resposta.
 */
public final class FencedInGameTest implements FabricGameTest {

    private static final int PLATFORM = 15;

    /** Plataforma de pedra com o ar limpo em cima; devolve o centro, onde se pisa. */
    private static BlockPos platform(TestContext context, int offset) {
        ServerWorld world = context.getWorld();
        BlockPos center = context.getAbsolutePos(new BlockPos(1, 2, 1))
                .add(1_400_000 + offset * 64, 0, 1_500_000);

        for (int dx = -PLATFORM; dx <= PLATFORM; dx++) {
            for (int dz = -PLATFORM; dz <= PLATFORM; dz++) {
                world.setBlockState(center.add(dx, -1, dz), Blocks.STONE.getDefaultState());
                world.setBlockState(center.add(dx, 0, dz), Blocks.AIR.getDefaultState());
                world.setBlockState(center.add(dx, 1, dz), Blocks.AIR.getDefaultState());
            }
        }

        return center;
    }

    /** Um curral de 7 × 7 em volta do centro; com portão na parede leste, se pedido. */
    private static BlockPos pen(ServerWorld world, BlockPos center, boolean withGate) {
        BlockPos gate = center.add(3, 0, 0);

        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (Math.abs(dx) == 3 || Math.abs(dz) == 3) {
                    world.setBlockState(center.add(dx, 0, dz), Blocks.OAK_FENCE.getDefaultState());
                }
            }
        }

        if (withGate) {
            world.setBlockState(gate, Blocks.OAK_FENCE_GATE.getDefaultState()
                    .with(FenceGateBlock.FACING, Direction.EAST)
                    .with(FenceGateBlock.OPEN, false));
        }

        return gate;
    }

    private static boolean isOpen(ServerWorld world, BlockPos gate) {
        return world.getBlockState(gate).get(FenceGateBlock.OPEN);
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "fenced_in")
    public void openGroundIsNotAPen(TestContext context) {
        BlockPos center = platform(context, 0);

        context.assertFalse(FencedIn.check(context.getWorld(), center).enclosed(),
                "chão aberto foi lido como cercado");
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "fenced_in")
    public void aPenWithAGateIsLeftThroughTheGate(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos center = platform(context, 1);
        BlockPos gate = pen(world, center, true);

        FencedIn.Result inside = FencedIn.check(world, center);

        context.assertTrue(inside.enclosed() && inside.gates().contains(gate),
                "dentro do curral: cercado=" + inside.enclosed() + ", portões=" + inside.gates());

        context.assertTrue(FencedIn.openTheGate(world, gate, inside.reach()).isPresent()
                        && isOpen(world, gate),
                "o portão não abriu");
        context.assertFalse(FencedIn.check(world, center).enclosed(),
                "com o portão aberto ele continua cercado");
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "fenced_in")
    public void aPenWithoutAGateHasNoWayOut(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos center = platform(context, 2);
        pen(world, center, false);

        FencedIn.Result inside = FencedIn.check(world, center);

        context.assertTrue(inside.enclosed() && inside.gates().isEmpty(),
                "curral sem portão: cercado=" + inside.enclosed() + ", portões=" + inside.gates());
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "fenced_in",
            tickLimit = (int) FencedIn.OPEN_TICKS + 100)
    public void theGateClosesAgainAfterward(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos center = platform(context, 3);
        BlockPos gate = pen(world, center, true);

        FencedIn.openTheGate(world, gate, FencedIn.check(world, center).reach());

        // Longe da arena ninguém segura o chunk: sem isto ele descarrega antes
        // dos 600 tiques, e o teste mediria o descarregamento.
        world.setChunkForced(gate.getX() >> 4, gate.getZ() >> 4, true);

        context.runAtTick(FencedIn.OPEN_TICKS + 20, () -> {
            FencedIn.tick(world);
            world.setChunkForced(gate.getX() >> 4, gate.getZ() >> 4, false);

            context.assertFalse(isOpen(world, gate),
                    "o portão ficou aberto: os animais do curral fugiriam");
            context.complete();
        });
    }

    /**
     * O caminho inteiro: o encalhado dentro do curral não é solto "after 0
     * steps" — o portão abre e ele recebe o lado de fora como destino.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "fenced_in")
    public void aStrandedWorkerInAPenOpensTheGateInsteadOfBeingReleased(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos center = platform(context, 4);
        BlockPos gate = pen(world, center, true);

        VillagerEntity villager = EntityType.VILLAGER.create(world);
        if (villager == null) {
            throw new AssertionError("nao foi possivel criar aldeao");
        }
        villager.refreshPositionAndAngles(
                center.getX() + 0.5, center.getY(), center.getZ() + 0.5, 0.0F, 0.0F);
        world.spawnEntity(villager);

        UUID id = villager.getUuid();
        Colony colony = Colony.create(UUID.randomUUID(),
                MinecraftTypeAdapter.toColonyPos(center.add(-40, 0, 0)));
        VillageColonyMod.COLONIES.register(colony);
        VillageColonyMod.WORKERS.register(id, colony.id()).assign(ProfessionType.LUMBERJACK);

        try {
            StrandedWorkers.frozeAt(id, center);
            StrandedWorkers.frozeAt(id, center);

            context.assertTrue(StrandedWorkers.isStranded(id), "o cenário não marcou o encalhado");

            StrandedEscape.pass(world, id);

            context.assertTrue(isOpen(world, gate),
                    "o encalhado no curral não abriu o portão");
            context.assertTrue(StrandedWorkers.isStranded(id),
                    "o encalhado no curral foi solto sem sair — o defeito do E52");
        } finally {
            StrandedWorkers.forget(id);
            StrandedEscape.clearAll();
            VillageColonyMod.WORKERS.remove(id);
            VillageColonyMod.COLONIES.remove(colony.id());
            villager.discard();
        }

        context.complete();
    }
}
