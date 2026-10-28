package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.worker.model.ProfessionType;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/** O chão do curral é caro no caminho de quem não é pastor — Regra 122. */
public class PenFloorsGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "pen_floors", tickLimit = 20)
    public void thePenFloorCostsTheMinerButNotTheShepherd(TestContext context) {
        UUID colony = UUID.randomUUID();
        BlockPos floor = context.getAbsolutePos(new BlockPos(3, 2, 3));
        BlockPos outside = context.getAbsolutePos(new BlockPos(6, 2, 6));
        VillagerEntity miner = context.spawnEntity(EntityType.VILLAGER, new BlockPos(1, 2, 1));
        VillagerEntity shepherd = context.spawnEntity(EntityType.VILLAGER, new BlockPos(1, 2, 2));
        VillagerEntity stranger = context.spawnEntity(EntityType.VILLAGER, new BlockPos(2, 2, 1));

        VillageColonyMod.WORKERS.register(miner.getUuid(), colony).assign(ProfessionType.MINER);
        VillageColonyMod.WORKERS.register(shepherd.getUuid(), colony).assign(ProfessionType.SHEPHERD);

        try {
            PenFloors.update(colony, List.of(Set.of(floor)));

            context.assertTrue(cost(miner, floor) == PenFloors.COSTLY, "o mineiro devia ver o curral como caro");
            context.assertTrue(cost(shepherd, floor) == PathNodeType.WALKABLE, "o pastor anda no curral como sempre");
            context.assertTrue(cost(stranger, floor) == PathNodeType.WALKABLE,
                    "aldeão de fora da colônia não é afetado");
            context.assertTrue(cost(miner, outside) == PathNodeType.WALKABLE, "fora do curral nada muda");
            context.assertTrue(PenFloors.forWalker(miner, floor.getX(), floor.getY(), floor.getZ(), PathNodeType.BLOCKED)
                    == PathNodeType.BLOCKED, "o que já é bloqueado continua bloqueado");
        } finally {
            PenFloors.update(colony, List.of());
            VillageColonyMod.WORKERS.remove(miner.getUuid());
            VillageColonyMod.WORKERS.remove(shepherd.getUuid());
        }

        context.complete();
    }

    private static PathNodeType cost(VillagerEntity villager, BlockPos at) {
        return PenFloors.forWalker(villager, at.getX(), at.getY(), at.getZ(), PathNodeType.WALKABLE);
    }
}
