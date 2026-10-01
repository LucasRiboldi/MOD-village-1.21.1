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

import java.util.List;
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
    public void aPenWithAGateIsLeftThroughTheGateFirst(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos center = platform(context, 1);
        BlockPos gate = pen(world, center, true);

        FencedIn.Result inside = FencedIn.check(world, center);

        context.assertTrue(inside.enclosed() && inside.gates().contains(gate),
                "dentro do curral: cercado=" + inside.enclosed() + ", portões=" + inside.gates());

        List<FencedIn.Exit> exits = FencedIn.exits(world, inside, center);

        context.assertTrue(!exits.isEmpty() && exits.get(0).gate() && exits.get(0).barrier().equals(gate),
                "a primeira saída não é o portão: " + exits);
        context.assertFalse(inside.reach().contains(exits.get(0).outside()),
                "o lado de fora do portão está dentro do curral: " + exits.get(0));
        context.assertTrue(FencedIn.open(world, gate) && isOpen(world, gate), "o portão não abriu");
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "fenced_in")
    public void aPenWithoutAGateIsLeftOverTheFence(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos center = platform(context, 2);
        pen(world, center, false);

        FencedIn.Result inside = FencedIn.check(world, center);
        List<FencedIn.Exit> exits = FencedIn.exits(world, inside, center);

        context.assertTrue(inside.enclosed() && inside.gates().isEmpty(),
                "curral sem portão: cercado=" + inside.enclosed() + ", portões=" + inside.gates());
        context.assertTrue(!exits.isEmpty() && exits.stream().noneMatch(FencedIn.Exit::gate),
                "curral sem portão sem cerca para pular: " + exits);
        context.complete();
    }

    /**
     * Do lado de fora, numa faixa entre a cerca e um fosso, ele também não
     * chega a 12 blocos — e a única "saída" seria pular para dentro do curral.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "fenced_in")
    public void aStripOutsideThePenNeverJumpsIntoIt(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos center = platform(context, 5);
        pen(world, center, true);

        // Um fosso fundo em volta de uma faixa de dois blocos fora da cerca.
        for (int dx = -PLATFORM; dx <= PLATFORM; dx++) {
            for (int dz = -PLATFORM; dz <= PLATFORM; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) > 5) {
                    for (int dy = -1; dy >= -6; dy--) {
                        world.setBlockState(center.add(dx, dy, dz), Blocks.AIR.getDefaultState());
                    }
                }
            }
        }

        BlockPos strip = center.add(4, 0, 0);
        FencedIn.Result there = FencedIn.check(world, strip);

        context.assertTrue(there.enclosed(), "o cenário não prende a faixa: o teste não mediria nada");
        context.assertTrue(FencedIn.exits(world, there, strip).isEmpty(),
                "da faixa de fora ele pularia para dentro do curral: "
                        + FencedIn.exits(world, there, strip));
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "fenced_in",
            tickLimit = (int) FencedIn.OPEN_TICKS + 100)
    public void theGateClosesAgainAfterward(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos center = platform(context, 3);
        BlockPos gate = pen(world, center, true);

        FencedIn.open(world, gate);

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
     * O caminho inteiro do encalhado: ele não é solto "after 0 steps" — nem na
     * primeira passagem, nem na seguinte, já com o portão aberto. A segunda é a
     * da sessão das 09:16: a busca de dentro passava pelo portão aberto e o
     * soltava no curral.
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

            context.assertTrue(PenEscape.isEscaping(id),
                    "o encalhado no curral não começou a sair dele");
            context.assertTrue(StrandedWorkers.isStranded(id),
                    "o encalhado no curral foi solto sem sair — o defeito do E52");

            // O portão aberto, como fica quando ele chega nele.
            FencedIn.open(world, gate);
            StrandedEscape.pass(world, id);

            context.assertTrue(StrandedWorkers.isStranded(id) && PenEscape.isEscaping(id),
                    "com o portão aberto, ainda dentro, ele foi solto — o defeito das 09:16");
        } finally {
            StrandedWorkers.forget(id);
            StrandedEscape.clearAll();
            PenEscape.clearAll();
            FencedIn.clearAll();
            VillageColonyMod.WORKERS.remove(id);
            VillageColonyMod.COLONIES.remove(colony.id());
            villager.discard();
        }

        context.complete();
    }
}
