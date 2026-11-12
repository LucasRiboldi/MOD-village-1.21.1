package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.Schedule;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/**
 * Aldeão fora da simulação não congela: o guarda de imobilidade não o conta, e ele não vira encalhado (Regra 244).
 *
 * <p>No jogo da 0.4.53 o fazendeiro e6951512 foi marcado duas vezes "frozen twice on the same spot" num chão plano a
 * 154 blocos do centro e ficou 60 minutos na fuga sem nenhuma linha de log: o trabalho o mandava andar pelo servidor,
 * mas o chunk dele estava carregado sem simular entidades, e ele não podia sair do lugar.
 */
public final class PinnedOnFlatGameTest implements FabricGameTest {

    private static final int PLATFORM = 3;

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "pinned_on_flat")
    public void aWorkerOutsideTheSimulationIsNotFrozen(TestContext context) {
        ServerWorld world = context.getWorld();
        // Longe de qualquer jogador e de qualquer arena: o chunk carrega pelo setBlockState, mas não simula entidades.
        BlockPos center = context.getAbsolutePos(new BlockPos(1, 2, 1)).add(1_700_000, 0, 1_700_000);

        for (int dx = -PLATFORM; dx <= PLATFORM; dx++) {
            for (int dz = -PLATFORM; dz <= PLATFORM; dz++) {
                world.setBlockState(center.add(dx, -1, dz), Blocks.STONE.getDefaultState());
                world.setBlockState(center.add(dx, 0, dz), Blocks.AIR.getDefaultState());
                world.setBlockState(center.add(dx, 1, dz), Blocks.AIR.getDefaultState());
            }
        }

        VillagerEntity villager = EntityType.VILLAGER.create(world);
        if (villager == null) {
            throw new AssertionError("nao foi possivel criar aldeao");
        }
        villager.refreshPositionAndAngles(center.getX() + 0.5, center.getY(), center.getZ() + 0.5, 0.0F, 0.0F);
        world.spawnEntity(villager);

        UUID id = villager.getUuid();
        Colony colony = Colony.create(UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(center.add(0, 0, 180)));
        VillageColonyMod.COLONIES.register(colony);
        VillageColonyMod.WORKERS.register(id, colony.id()).assign(ProfessionType.FARMER);
        world.setTimeOfDay(Schedule.WORK_TIME);

        try {
            context.assertFalse(world.shouldTickEntity(center), "o cenário ficou dentro da simulação");

            WorkStall stall = new WorkStall();
            boolean stuck = false;

            for (int pass = 0; pass < 2; pass++) {
                for (int tick = 0; tick <= WorkStall.LIMIT; tick++) {
                    stuck |= stall.stuck(world, villager);
                }
                stall = new WorkStall();
            }

            context.assertFalse(stuck, "o aldeão fora da simulação foi dado como parado");
            context.assertFalse(StrandedWorkers.isStranded(id),
                    "o aldeão fora da simulação foi marcado encalhado em " + center.toShortString());
        } finally {
            StrandedWorkers.forget(id);
            VillageColonyMod.WORKERS.remove(id);
            VillageColonyMod.COLONIES.remove(colony.id());
            villager.discard();
        }

        context.complete();
    }
}
