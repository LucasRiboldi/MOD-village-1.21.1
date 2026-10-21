package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.UUID;

/**
 * As duas alturas da vila do deserto — pedido do autor, 2026-10-21: quem desiste abaixo do destino
 * sobe, fica uma escada fixa, a roça cresce na areia e o viveiro planta na areia da vila a céu aberto.
 */
public class VillageLevelsGameTest implements FabricGameTest {

    /** A escada sobe a duna de areia, degrau por degrau; a segunda, perto da primeira, não se faz. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_levels")
    public void aStairIsLaidUpTheDuneAndOnlyOnce(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos bottom = context.getAbsolutePos(new BlockPos(1, 4, 1)).add(1_000_000, 0, 2_200_000);
        UUID colony = UUID.randomUUID();
        world.getChunk(bottom);
        world.getChunk(bottom.add(16, 0, 0));

        for (int dx = -2; dx <= 12; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                world.setBlockState(bottom.add(dx, -1, dz), Blocks.STONE.getDefaultState());
                for (int dy = 0; dy <= 9; dy++) {
                    world.setBlockState(bottom.add(dx, dy, dz),
                            dx >= 1 && dy < 6 ? Blocks.SAND.getDefaultState() : Blocks.AIR.getDefaultState());
                }
            }
        }

        try {
            int steps = VillageStairs.connect(world, colony, bottom, bottom.up(6), bottom.add(40, 6, 0));

            context.assertTrue(steps == 6, "a escada devia ter 6 degraus e tem " + steps);
            for (int step = 1; step <= steps; step++) {
                BlockPos feet = bottom.add(step, step, 0);
                context.assertTrue(world.getBlockState(feet).isAir() && world.getBlockState(feet.up()).isAir()
                                && world.getBlockState(feet.down()).isSolidBlock(world, feet.down()),
                        "o degrau " + step + " não ficou de pé");
            }
            context.assertTrue(VillageStairs.connect(world, colony, bottom.add(2, 0, 2), bottom.up(6),
                    bottom.add(40, 6, 0)) == 0, "controle: fez uma segunda escada ao lado da primeira");
        } finally {
            for (BlockPos pos : BlockPos.iterate(bottom.add(-2, -1, -2), bottom.add(12, 9, 2))) {
                world.setBlockState(pos, Blocks.AIR.getDefaultState());
            }
            VillageStairs.clearAll();
        }

        context.complete();
    }

    /** Quem desiste da caminhada dez abaixo do destino vira encalhado; na mesma altura, não. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_levels", tickLimit = 40)
    public void givingUpBelowTheWayStrandsTheWorker(TestContext context) {
        VillagerEntity low = context.spawnEntity(EntityType.VILLAGER, new BlockPos(1, 2, 1));
        VillagerEntity level = context.spawnEntity(EntityType.VILLAGER, new BlockPos(3, 2, 3));
        UUID colony = UUID.randomUUID();
        VillageColonyMod.WORKERS.register(low.getUuid(), colony);
        VillageColonyMod.WORKERS.register(level.getUuid(), colony);

        context.waitAndRun(5, () -> {
            try {
                StrandedWorkers.gaveUpOnTheWayTo(low.getUuid(), low.getBlockPos().up(10));
                StrandedWorkers.gaveUpOnTheWayTo(level.getUuid(), level.getBlockPos().east(20));
                StrandedWorkers.strandThoseBelowTheirWay(context.getWorld());

                context.assertTrue(StrandedWorkers.isStranded(low.getUuid()),
                        "dez abaixo do destino e não entrou na subida");
                context.assertFalse(StrandedWorkers.isStranded(level.getUuid()),
                        "controle: na mesma altura do destino virou encalhado");
            } finally {
                StrandedWorkers.forget(low.getUuid());
                StrandedWorkers.forget(level.getUuid());
                VillageColonyMod.WORKERS.remove(low.getUuid());
                VillageColonyMod.WORKERS.remove(level.getUuid());
                // Aldeão que sobra vaga para a arena vizinha e vira morador da vila de outro teste.
                low.discard();
                level.discard();
            }

            context.complete();
        });
    }

    /** A roça do deserto: borda de arenito e areia um abaixo; com água perto, ali cabe canteiro. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_levels")
    public void aNewPlotFitsOnTheSandBeyondTheBorder(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos farm = context.getAbsolutePos(new BlockPos(1, 4, 1)).add(1_000_000, 0, 2_300_000);
        BlockPos dry = farm.add(0, 0, 40);
        world.getChunk(farm);
        world.getChunk(dry);

        for (BlockPos at : new BlockPos[] {farm, dry}) {
            for (BlockPos pos : BlockPos.iterate(at.add(-3, -2, -3), at.add(3, -1, 3))) {
                world.setBlockState(pos, Blocks.SAND.getDefaultState());
            }
            world.setBlockState(at, Blocks.FARMLAND.getDefaultState());
            world.setBlockState(at.down(), Blocks.SAND.getDefaultState());
            world.setBlockState(at.east(), Blocks.CUT_SANDSTONE.getDefaultState());
            world.setBlockState(at.east().down(), Blocks.SAND.getDefaultState());
        }
        world.setBlockState(farm.west(), Blocks.WATER.getDefaultState());
        world.setBlockState(farm.west().down(), Blocks.SAND.getDefaultState());

        try {
            Optional<BlockPos> plot = FarmerTill.nextPlot(world, farm);

            context.assertTrue(plot.isPresent() && world.getBlockState(plot.get()).isOf(Blocks.SAND),
                    "a areia em volta da roça do deserto não serviu de canteiro: " + plot);
            context.assertTrue(FarmerTill.nextPlot(world, dry).isEmpty(),
                    "controle: sem água perto, a areia virou canteiro");
        } finally {
            for (BlockPos at : new BlockPos[] {farm, dry}) {
                for (BlockPos pos : BlockPos.iterate(at.add(-3, -2, -3), at.add(3, 1, 3))) {
                    world.setBlockState(pos, Blocks.AIR.getDefaultState());
                }
            }
        }

        context.complete();
    }

    /** A areia da vila a céu aberto serve de viveiro perto do baú; debaixo de teto, não. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_levels")
    public void theNurseryTakesOpenSandButNotSandUnderARoof(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos open = context.getAbsolutePos(new BlockPos(1, 4, 1)).add(1_000_000, 0, 2_400_000);
        BlockPos roofed = open.add(6, 0, 0);
        world.getChunk(open);

        world.setBlockState(open, Blocks.SAND.getDefaultState());
        world.setBlockState(roofed, Blocks.SAND.getDefaultState());
        world.setBlockState(roofed.up(4), Blocks.SMOOTH_SANDSTONE.getDefaultState());

        try {
            context.assertTrue(LumberjackNursery.isOpenNaturalGround(world, open),
                    "a areia a céu aberto não serviu de viveiro");
            context.assertFalse(LumberjackNursery.isOpenNaturalGround(world, roofed),
                    "controle: a areia debaixo do teto serviu de viveiro");
        } finally {
            for (BlockPos pos : BlockPos.iterate(open, roofed.up(4))) {
                world.setBlockState(pos, Blocks.AIR.getDefaultState());
            }
        }

        context.complete();
    }
}
