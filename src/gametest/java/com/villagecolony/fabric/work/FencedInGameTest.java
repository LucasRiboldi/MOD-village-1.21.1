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
        // 4.096 entre um cenário e outro, nos dois eixos: com 64, dois testes
        // cujas arenas distam um múltiplo de 64 montavam um em cima do outro,
        // e o ar que um limpa apagava o portão do outro — "Block{air}" no
        // theGateClosesAgainAfterward, 2 vezes em 6 rodadas de 01-10.
        BlockPos center = context.getAbsolutePos(new BlockPos(1, 2, 1))
                .add(1_400_000 + offset * 4_096, 0, 1_500_000 + offset * 4_096);

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
            // Só o que é deste aldeão: os testes de um lote correm juntos, e
            // limpar tudo (StrandedEscape/PenEscape/FencedIn.clearAll) apagava
            // o portão a fechar do theGateClosesAgainAfterward no meio dele.
            StrandedWorkers.forget(id);
            PenEscape.forget(id);
            ClimbOut.forget(id);
            EscapeBackfill.forget(id);
            FencedIn.closeWhenClear(gate);
            VillageColonyMod.WORKERS.remove(id);
            VillageColonyMod.COLONIES.remove(colony.id());
            villager.discard();
        }

        context.complete();
    }

    /**
     * Regra 275: a soleira de uma casa não é curral. Degrau entre dois muretes, porta atrás e a copa
     * de um pinheiro na altura da cabeça à frente: quem está ali alcança a casa pela porta, e isso é
     * casa, não cercado — sem a regra, o pastor pulava o murete sem fim.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "fenced_in")
    public void aDoorstepBehindLeavesIsNotAPen(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos center = platform(context, 6);

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -1; dz <= 4; dz++) {
                for (int dy = 2; dy <= 3; dy++) {
                    world.setBlockState(center.add(dx, dy, dz), Blocks.AIR.getDefaultState());
                }
            }
        }

        // A casa: chão de pedregulho um bloco acima do chão de fora, paredes de dois e teto.
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = 1; dz <= 4; dz++) {
                world.setBlockState(center.add(dx, 0, dz), Blocks.COBBLESTONE.getDefaultState());
                world.setBlockState(center.add(dx, 3, dz), Blocks.COBBLESTONE.getDefaultState());
                boolean wall = Math.abs(dx) == 2 || dz == 1 || dz == 4;

                if (wall && !(dx == 0 && dz == 1)) {
                    world.setBlockState(center.add(dx, 1, dz), Blocks.COBBLESTONE.getDefaultState());
                    world.setBlockState(center.add(dx, 2, dz), Blocks.COBBLESTONE.getDefaultState());
                }
            }
        }

        world.setBlockState(center.add(0, 1, 1), Blocks.SPRUCE_DOOR.getDefaultState()
                .with(net.minecraft.block.DoorBlock.FACING, Direction.NORTH));
        world.setBlockState(center.add(0, 2, 1), Blocks.SPRUCE_DOOR.getDefaultState()
                .with(net.minecraft.block.DoorBlock.FACING, Direction.NORTH)
                .with(net.minecraft.block.DoorBlock.HALF, net.minecraft.block.enums.DoubleBlockHalf.UPPER));

        // A soleira: degrau entre dois muretes de dois de altura.
        world.setBlockState(center, Blocks.COBBLESTONE_STAIRS.getDefaultState()
                .with(net.minecraft.block.StairsBlock.FACING, Direction.SOUTH));

        for (int dy = 0; dy <= 1; dy++) {
            world.setBlockState(center.add(-1, dy, 0), Blocks.COBBLESTONE_WALL.getDefaultState());
            world.setBlockState(center.add(1, dy, 0), Blocks.COBBLESTONE_WALL.getDefaultState());
        }

        // A copa do pinheiro na altura da cabeça, à frente da soleira.
        for (int dx = -1; dx <= 1; dx++) {
            world.setBlockState(center.add(dx, 2, -1), Blocks.SPRUCE_LEAVES.getDefaultState()
                    .with(net.minecraft.block.LeavesBlock.PERSISTENT, true));
        }

        FencedIn.Result doorstep = FencedIn.check(world, center.up());

        context.assertFalse(doorstep.enclosed(),
                "a soleira da casa foi lida como curral: alcance de " + doorstep.reach().size() + " blocos");
        context.complete();
    }

    /**
     * Regra 276: alçapões abertos em volta prendem como cerca baixa — o canteiro da vila de taiga. O aldeão
     * preso ali tem saída por cima deles, e o caminho da colônia não entra no nó do alçapão em pé.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "fenced_in")
    public void uprightTrapdoorsAreJumpedAndAvoided(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos center = platform(context, 7);

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx != 0 || dz != 0) {
                    world.setBlockState(center.add(dx, 0, dz), Blocks.SPRUCE_TRAPDOOR.getDefaultState()
                            .with(net.minecraft.block.TrapdoorBlock.OPEN, true)
                            .with(net.minecraft.block.TrapdoorBlock.FACING, Direction.NORTH));
                }
            }
        }

        FencedIn.Result bed = FencedIn.check(world, center);
        List<FencedIn.Exit> exits = FencedIn.exits(world, bed, center);

        context.assertTrue(bed.enclosed(), "o canteiro de alçapões não foi lido como cercado");
        context.assertTrue(!exits.isEmpty() && !exits.get(0).gate(),
                "sem saída por cima dos alçapões: " + exits);

        BlockPos upright = center.add(1, 0, 0);
        BlockPos lying = center.add(4, 0, 0);
        world.setBlockState(lying, Blocks.SPRUCE_TRAPDOOR.getDefaultState());

        context.assertTrue(com.villagecolony.fabric.integration.ColonyPathing.uprightTrapdoor(
                        world, upright.getX(), upright.getY(), upright.getZ())
                        == net.minecraft.entity.ai.pathing.PathNodeType.BLOCKED,
                "o caminho entra no nó do alçapão em pé");
        context.assertTrue(com.villagecolony.fabric.integration.ColonyPathing.uprightTrapdoor(
                        world, lying.getX(), lying.getY(), lying.getZ())
                        == net.minecraft.entity.ai.pathing.PathNodeType.TRAPDOOR,
                "o alçapão fechado, deitado, deixou de ser andável");
        context.complete();
    }
}
