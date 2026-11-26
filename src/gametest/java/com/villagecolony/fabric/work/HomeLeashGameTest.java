package com.villagecolony.fabric.work;

import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.gametest.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;

/**
 * Regra 372 (notas do jogo da 0.4.145, J1 🅱️: "fundidor completamente perdido, muito longe do centro da vila,
 * inaceitável" e "nenhuma outra profissão pode se afastar mais de XX blocos", com o raio contado da cama): fora o mineiro
 * e o lenhador, ninguém busca alvo a mais de {@link HomeLeash#RANGE} blocos da própria cama, e quem fica longe dela volta.
 */
public class HomeLeashGameTest implements FabricGameTest {

    /** O fundidor parado a 76 blocos da cama é trazido de volta; o mineiro, tão longe quanto, fica. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "home_leash", tickLimit = 40)
    public void aWorkerFarFromItsBedIsBroughtBack(TestContext context) {
        ServerWorld world = context.getWorld();
        ColonyFixture fixture = ColonyFixture.colonyAt(context, new BlockPos(1, 2, 1));
        BlockPos home = context.getAbsolutePos(new BlockPos(4, 2, 4)).add(76, 0, 0);

        world.setChunkForced(home.getX() >> 4, home.getZ() >> 4, true);

        try {
            for (BlockPos at : BlockPos.iterate(home.add(-2, -1, -2), home.add(2, -1, 2))) {
                world.setBlockState(at, Blocks.STONE.getDefaultState());
            }

            Worker smelter = fixture.emptyHandedWorker(context, ProfessionType.SMELTER, new BlockPos(4, 2, 4));
            Worker miner = fixture.emptyHandedWorker(context, ProfessionType.MINER, new BlockPos(5, 2, 4));
            VillagerEntity lost = homed(world, smelter, home);
            VillagerEntity digger = homed(world, miner, home);
            BlockPos minerAt = digger.getBlockPos();

            context.assertFalse(HomeLeash.within(world, smelter.villagerId(), home.add(60, 0, 0)),
                    "Regra 372: o fundidor aceitou alvo a 60 blocos da cama");
            context.assertTrue(HomeLeash.within(world, smelter.villagerId(), home.add(30, 0, 0)),
                    "o fundidor recusou alvo a 30 blocos da cama");
            context.assertTrue(HomeLeash.within(world, miner.villagerId(), home.add(60, 0, 0)),
                    "o mineiro ficou preso ao raio da cama");

            for (int pass = 0; pass <= HomeLeash.TRIES; pass++) {
                HomeLeash.pass(world, fixture.colony());
            }

            context.assertTrue(lost.getBlockPos().getSquaredDistance(home) <= 9,
                    "Regra 372: o fundidor ficou em " + lost.getBlockPos().toShortString() + ", longe da cama em "
                            + home.toShortString());
            context.assertTrue(digger.getBlockPos().equals(minerAt), "o mineiro foi tirado de onde estava");
        } finally {
            fixture.cleanUp();
            world.setChunkForced(home.getX() >> 4, home.getZ() >> 4, false);
        }

        context.complete();
    }

    /** O aldeão parado (sem IA: não anda), com a cama em {@code home}. */
    private static VillagerEntity homed(ServerWorld world, Worker worker, BlockPos home) {
        VillagerEntity villager = (VillagerEntity) world.getEntity(worker.villagerId());

        if (villager == null) {
            throw new AssertionError("o aldeão do teste não existe");
        }

        villager.setAiDisabled(true);
        villager.getBrain().remember(MemoryModuleType.HOME, GlobalPos.create(world.getRegistryKey(), home));

        return villager;
    }
}
