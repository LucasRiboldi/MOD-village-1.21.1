package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.integration.VillageFoundation;
import com.villagecolony.gametest.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.BedPart;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.GlobalPos;

/**
 * Regra 376 (notas do jogo da 0.4.145, J7: "o construtor precisa se mudar para a casa livre mais perto da construção se
 * ele estiver ter que caminhar mais de 30 blocos para chegar na construção atual"): sem cama livre perto da obra, o
 * construtor troca de cama com o adulto sem profissão que dorme perto dela.
 */
public class BuilderHomeGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "builder_home", tickLimit = 40)
    public void theBuilderSwapsBedsWithAnIdleAdultNearTheBuild(TestContext context) {
        ServerWorld world = context.getWorld();
        ColonyFixture fixture = ColonyFixture.colonyAt(context, new BlockPos(1, 2, 1));
        BlockPos site = context.getAbsolutePos(new BlockPos(2, 2, 2));
        BlockPos nearFoot = context.getAbsolutePos(new BlockPos(6, 2, 2));
        BlockPos farFoot = site.add(60, 0, 0);

        world.setChunkForced(farFoot.getX() >> 4, farFoot.getZ() >> 4, true);

        try {
            bed(world, nearFoot);
            bed(world, farFoot);

            Worker builder = fixture.emptyHandedWorker(context, ProfessionType.BUILDER, new BlockPos(3, 2, 6));
            VillagerEntity idle = context.spawnEntity(EntityType.VILLAGER, new BlockPos(4, 2, 6));

            idle.setBreedingAge(0);
            VillageColonyMod.WORKERS.register(idle.getUuid(), fixture.colony().id());
            fixture.owning(idle.getUuid());

            VillagerEntity worker = (VillagerEntity) world.getEntity(builder.villagerId());

            VillageFoundation.giveHome(world, worker, farFoot);
            VillageFoundation.giveHome(world, idle, nearFoot);

            context.assertTrue(BuilderHome.settle(world, fixture.colony(), site) == 1, "o construtor não se mudou");
            context.assertTrue(home(worker).equals(nearFoot.south()),
                    "Regra 376: o construtor ficou com a cama em " + home(worker).toShortString() + ", longe da obra");
            context.assertTrue(home(idle).equals(farFoot.south()),
                    "o adulto sem profissão ficou sem a cama do construtor: " + home(idle).toShortString());
        } finally {
            fixture.cleanUp();
            world.setChunkForced(farFoot.getX() >> 4, farFoot.getZ() >> 4, false);
        }

        context.complete();
    }

    /** Uma cama com o pé em {@code foot} e a cabeceira ao sul, sobre pedra. */
    private static void bed(ServerWorld world, BlockPos foot) {
        var state = Blocks.WHITE_BED.getDefaultState().with(BedBlock.FACING, Direction.SOUTH);

        world.setBlockState(foot.down(), Blocks.STONE.getDefaultState());
        world.setBlockState(foot.south().down(), Blocks.STONE.getDefaultState());
        world.setBlockState(foot, state.with(BedBlock.PART, BedPart.FOOT));
        world.setBlockState(foot.south(), state.with(BedBlock.PART, BedPart.HEAD));
    }

    private static BlockPos home(VillagerEntity villager) {
        return villager.getBrain().getOptionalRegisteredMemory(MemoryModuleType.HOME).map(GlobalPos::pos)
                .orElse(BlockPos.ORIGIN);
    }
}
