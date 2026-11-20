package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * O curral é um lugar (Regra 327), o pastor desiste do desgarrado que não alcança (Regra 328) e faz a ronda do dia
 * (Regra 329).
 */
public class ShepherdRoundGameTest implements FabricGameTest {

    /** O chão da arena: o lado de fora fica maior que o curral, senão a borda da arena o fecha e ele é lido como curral. */
    private static final int FLOOR = 28;

    /**
     * Chão de grama e um curral quadrado de cerca, com portão no lado norte.
     *
     * @param inner o lado do chão de dentro
     * @return o portão
     */
    private static BlockPos pen(TestContext context, int y, int inner, boolean gateOpen) {
        ServerWorld world = context.getWorld();
        int last = inner + 2;

        for (int x = 0; x <= FLOOR; x++) {
            for (int z = 0; z <= FLOOR; z++) {
                world.setBlockState(context.getAbsolutePos(new BlockPos(x, y, z)), Blocks.GRASS_BLOCK.getDefaultState());
                world.setBlockState(context.getAbsolutePos(new BlockPos(x, y + 1, z)), Blocks.AIR.getDefaultState());

                boolean ring = (x == 1 || x == last || z == 1 || z == last) && x >= 1 && x <= last && z >= 1 && z <= last;

                if (ring) {
                    world.setBlockState(context.getAbsolutePos(new BlockPos(x, y + 1, z)), Blocks.OAK_FENCE.getDefaultState());
                }
            }
        }

        BlockPos gate = context.getAbsolutePos(new BlockPos(1 + (inner + 1) / 2, y + 1, 1));
        world.setBlockState(gate, Blocks.OAK_FENCE_GATE.getDefaultState().with(FenceGateBlock.OPEN, gateOpen));

        return gate;
    }

    private static Colony colony(TestContext context, int y, BlockPos gate) {
        BlockPos low = context.getAbsolutePos(new BlockPos(0, y - 2, 0));
        BlockPos high = context.getAbsolutePos(new BlockPos(FLOOR, y + 4, FLOOR));
        Colony colony = Colony.create(UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(gate));

        colony.measure(new VillageBounds(Math.min(low.getX(), high.getX()), low.getY(), Math.min(low.getZ(), high.getZ()),
                Math.max(low.getX(), high.getX()), high.getY(), Math.max(low.getZ(), high.getZ())));

        return colony;
    }

    private static <T extends Entity> T spawn(ServerWorld world, EntityType<T> type, BlockPos at, List<Entity> spawned) {
        T entity = type.create(world);

        entity.refreshPositionAndAngles(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);

        if (entity instanceof net.minecraft.entity.mob.MobEntity mob) {
            mob.setAiDisabled(true);
        }

        world.spawnEntity(entity);
        spawned.add(entity);

        return entity;
    }

    private static void clean(TestContext context, int y, List<Entity> spawned) {
        spawned.forEach(Entity::discard);

        for (int x = 0; x <= FLOOR; x++) {
            for (int z = 0; z <= FLOOR; z++) {
                context.getWorld().setBlockState(context.getAbsolutePos(new BlockPos(x, y + 1, z)), Blocks.AIR.getDefaultState());
                context.getWorld().setBlockState(context.getAbsolutePos(new BlockPos(x, y, z)), Blocks.AIR.getDefaultState());
            }
        }
    }

    /** Regra 327: a porteira aberta não desfaz o curral na leitura. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "shepherd_round", tickLimit = 20)
    public void anOpenGateDoesNotUnmakeThePen(TestContext context) {
        int y = 60;
        BlockPos gate = pen(context, y, 5, true);
        Colony colony = colony(context, y, gate);

        try {
            List<ShepherdPens.Pen> pens = ShepherdPens.pensOf(context.getWorld(), colony);

            context.assertTrue(pens.size() == 1 && pens.get(0).reach().size() == 25,
                    "Regra 327: com a porteira aberta o curral sumiu da leitura: " + pens.size() + " currais");
        } finally {
            clean(context, y, new ArrayList<>());
        }

        context.complete();
    }

    /**
     * Regra 327: o animal no meio do curral grande, longe da cerca, e o de dentro com a porteira aberta estão guardados;
     * só o de fora é desgarrado.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "shepherd_round", tickLimit = 20)
    public void anAnimalInsideAKnownPenIsNotAStray(TestContext context) {
        ServerWorld world = context.getWorld();
        int y = 80;
        BlockPos gate = pen(context, y, 13, true);
        Colony colony = colony(context, y, gate);
        List<Entity> spawned = new ArrayList<>();

        try {
            // O meio do chão de 13 × 13, o tamanho do curral grande, fica a 7 blocos da cerca.
            SheepEntity middle = spawn(world, EntityType.SHEEP, context.getAbsolutePos(new BlockPos(8, y + 1, 8)), spawned);
            SheepEntity byTheGate = spawn(world, EntityType.SHEEP, context.getAbsolutePos(new BlockPos(8, y + 1, 2)), spawned);
            SheepEntity outside = spawn(world, EntityType.SHEEP, context.getAbsolutePos(new BlockPos(26, y + 1, 26)), spawned);
            VillagerEntity shepherd = spawn(world, EntityType.VILLAGER, context.getAbsolutePos(new BlockPos(20, y + 1, 20)), spawned);

            List<AnimalEntity> strays = HerdCandidates.of(world, colony, shepherd);

            List<ShepherdPens.Pen> pens = ShepherdPens.pensOf(world, colony);

            context.assertFalse(strays.contains(middle), "Regra 327: a ovelha no meio do curral grande é desgarrada; currais="
                    + pens.size() + " chão=" + pens.stream().map(pen -> pen.reach().size()).toList()
                    + " contém=" + pens.stream().anyMatch(pen -> pen.reach().contains(middle.getBlockPos())));
            context.assertFalse(strays.contains(byTheGate), "Regra 327: com a porteira aberta, a ovelha de dentro é desgarrada");
            context.assertTrue(strays.contains(outside), "a ovelha de fora não é desgarrada");
        } finally {
            clean(context, y, spawned);
        }

        context.complete();
    }

    /** Regra 328: trinta segundos sem chegar mais perto, ele larga o animal, que fica fora da coleta por um tempo. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "shepherd_round", tickLimit = 20)
    public void theShepherdSetsAsideAStrayHeCannotReach(TestContext context) {
        ServerWorld world = context.getWorld();
        int y = 100;
        BlockPos gate = pen(context, y, 5, false);
        Colony colony = colony(context, y, gate);
        List<Entity> spawned = new ArrayList<>();
        UUID shepherdId = UUID.randomUUID();

        try {
            SheepEntity stray = spawn(world, EntityType.SHEEP, context.getAbsolutePos(new BlockPos(26, y + 1, 26)), spawned);
            VillagerEntity shepherd = spawn(world, EntityType.VILLAGER, context.getAbsolutePos(new BlockPos(4, y + 1, 0)), spawned);
            long now = world.getTime();

            context.assertFalse(HerdPatience.losesPatience(shepherdId, stray.getUuid(), 20, now), "desistiu no primeiro passo");
            context.assertFalse(HerdPatience.losesPatience(shepherdId, stray.getUuid(), 15, now + 500),
                    "chegou mais perto e mesmo assim desistiu");
            context.assertFalse(HerdPatience.losesPatience(shepherdId, stray.getUuid(), 15, now + 500 + HerdPatience.PATIENCE),
                    "desistiu antes do prazo");
            context.assertTrue(HerdPatience.losesPatience(shepherdId, stray.getUuid(), 15, now + 501 + HerdPatience.PATIENCE),
                    "Regra 328: parado longe do animal, não desistiu");

            context.assertFalse(HerdPatience.failedEntry(shepherdId, stray.getUuid(), now), "desistiu na primeira entrada");
            context.assertFalse(HerdPatience.failedEntry(shepherdId, stray.getUuid(), now), "desistiu na segunda entrada");
            context.assertTrue(HerdPatience.failedEntry(shepherdId, stray.getUuid(), now),
                    "Regra 328: o animal não entrou três vezes e ele não desistiu");

            context.assertTrue(HerdCandidates.of(world, colony, shepherd).contains(stray), "o desgarrado não é candidato");
            HerdPatience.setAside(world, shepherdId, stray.getUuid(), "test");
            context.assertFalse(HerdCandidates.of(world, colony, shepherd).contains(stray),
                    "Regra 328: o animal largado voltou logo à coleta");
        } finally {
            HerdPatience.forget(shepherdId);
            clean(context, y, spawned);
        }

        context.complete();
    }

    /** Regra 329: na ronda ele conta o rebanho de dentro e fecha a porteira que ficou aberta. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "shepherd_round_clock", tickLimit = 20)
    public void theRoundCountsTheFlockAndClosesTheGate(TestContext context) {
        ServerWorld world = context.getWorld();
        int y = 120;
        BlockPos gate = pen(context, y, 5, true);
        Colony colony = colony(context, y, gate);
        List<Entity> spawned = new ArrayList<>();
        long before = world.getTimeOfDay();

        try {
            world.setTimeOfDay(2_000);

            List<ShepherdPens.Pen> pens = ShepherdPens.pensOf(world, colony);
            spawn(world, EntityType.SHEEP, context.getAbsolutePos(new BlockPos(4, y + 1, 3)), spawned);
            spawn(world, EntityType.SHEEP, context.getAbsolutePos(new BlockPos(5, y + 1, 4)), spawned);
            VillagerEntity shepherd = spawn(world, EntityType.VILLAGER, context.getAbsolutePos(new BlockPos(4, y + 1, 0)), spawned);

            ShepherdRound.Round round = new ShepherdRound.Round(colony, pens, world.getTime());

            context.assertTrue(ShepherdRound.step(world, shepherd.getUuid(), round), "a ronda acabou antes do curral");
            context.assertTrue(round.visited == 1 && round.inside == 2,
                    "Regra 329: a ronda contou " + round.inside + " em " + round.visited + " currais");
            context.assertFalse(world.getBlockState(gate).get(FenceGateBlock.OPEN), "Regra 329: a porteira ficou aberta");
            context.assertFalse(ShepherdRound.step(world, shepherd.getUuid(), round), "a ronda não acabou");

            context.assertTrue(ShepherdRound.due(world, shepherd.getUuid()), "a ronda de hoje já constava feita");
            ShepherdRound.start(world, colony, shepherd);
            context.assertFalse(ShepherdRound.due(world, shepherd.getUuid()), "Regra 329: a ronda pode se repetir no mesmo dia");
        } finally {
            world.setTimeOfDay(before);
            clean(context, y, spawned);
        }

        context.complete();
    }

    /** Regra 329: a separação do curral misto (Regra 72) acontece no máximo uma vez por dia em cada vila. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "shepherd_round", tickLimit = 20)
    public void aMixedPenIsSplitOnceADay(TestContext context) {
        ServerWorld world = context.getWorld();
        UUID colonyId = UUID.randomUUID();

        context.assertTrue(PenRegroup.mayRegroup(world, colonyId), "a vila nova não pode separar");
        PenRegroup.regrouped(world, colonyId);
        context.assertFalse(PenRegroup.mayRegroup(world, colonyId), "Regra 329: a vila separou duas vezes no mesmo dia");

        context.complete();
    }
}
