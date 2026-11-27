package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.GradePlan;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.UUID;

/**
 * ADR-063, fase 2: a leitura do mundo para o plano de alturas. A frente da porta e a faixa da construção viram âncoras,
 * a pegada fica fora, o caminho é caminho, o paredão conta como coluna a mexer, e nenhum bloco muda.
 */
public class GradeSurveyGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "grade_survey", tickLimit = 40)
    public void theSurveyReadsAnchorsPathsAndWhatIsOut(TestContext context) {
        ServerWorld world = context.getWorld();
        UUID colonyId = UUID.randomUUID();
        UUID houseId = UUID.randomUUID();
        // Longe da arena: a barreira que a cerca seria o topo das colunas.
        BlockPos base = context.getAbsolutePos(BlockPos.ORIGIN).add(2_600_000, 20, 2_600_000);

        for (int cx = (base.getX() >> 4) - 1; cx <= ((base.getX() + 23) >> 4) + 1; cx++) {
            for (int cz = (base.getZ() >> 4) - 1; cz <= ((base.getZ() + 23) >> 4) + 1; cz++) {
                world.setChunkForced(cx, cz, true);
            }
        }

        for (int x = 0; x <= 23; x++) {
            for (int z = 0; z <= 23; z++) {
                world.setBlockState(base.add(x, 0, z), Blocks.STONE.getDefaultState());
                world.setBlockState(base.add(x, 1, z), Blocks.GRASS_BLOCK.getDefaultState());
                // O paredão de 3 a partir de x 16.
                for (int y = 2; y <= 4 && x >= 16; y++) {
                    world.setBlockState(base.add(x, y, z), y == 4 ? Blocks.GRASS_BLOCK.getDefaultState()
                            : Blocks.DIRT.getDefaultState());
                }
            }
        }

        // A casa: paredes em z 4 e 6 e nos lados x 4 e 8, porta em (6, 2, 6) olhando para dentro (norte).
        for (int y = 2; y <= 4; y++) {
            for (int x = 4; x <= 8; x++) {
                world.setBlockState(base.add(x, y, 4), Blocks.SANDSTONE.getDefaultState());
                world.setBlockState(base.add(x, y, 6), Blocks.SANDSTONE.getDefaultState());
            }
            world.setBlockState(base.add(4, y, 5), Blocks.SANDSTONE.getDefaultState());
            world.setBlockState(base.add(8, y, 5), Blocks.SANDSTONE.getDefaultState());
        }
        world.setBlockState(base.add(6, 2, 6), Blocks.OAK_DOOR.getDefaultState().with(DoorBlock.FACING, Direction.NORTH)
                .with(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        world.setBlockState(base.add(6, 3, 6), Blocks.OAK_DOOR.getDefaultState().with(DoorBlock.FACING, Direction.NORTH)
                .with(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        // O caminho que sai da porta.
        for (int z = 8; z <= 12; z++) {
            world.setBlockState(base.add(6, 1, z), Blocks.DIRT_PATH.getDefaultState());
        }

        BlockPos min = base.add(2, 0, 2);
        BlockPos max = base.add(21, 4, 21);
        Colony colony = Colony.create(colonyId, MinecraftTypeAdapter.toColonyPos(base.add(12, 1, 12)));
        colony.restoreBounds(new VillageBounds(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ()));
        VillageColonyMod.BUILDINGS.register(new Building(houseId, colonyId, ResourceId.vanilla("house"),
                MinecraftTypeAdapter.toColonyPos(base.add(4, 1, 4)),
                MinecraftTypeAdapter.toColonyPos(base.add(8, 4, 6))));

        context.waitAndRun(2, () -> {
            try {
                var survey = GradeSurvey.read(world, colony);

                context.assertTrue(survey.isPresent(), "a leitura não saiu");

                BlockPos front = base.add(6, 1, 7);
                BlockPos footprint = base.add(6, 1, 5);
                BlockPos ring = base.add(3, 1, 5);
                BlockPos path = base.add(6, 1, 10);
                GradeSurvey.Survey read = survey.get();

                context.assertTrue(read.kind(front.getX(), front.getZ()) == GradePlan.Kind.ANCHOR
                        && read.anchor(front.getX(), front.getZ()) == front.getY(),
                        "a frente da porta não virou âncora na altura dela: " + read.kind(front.getX(), front.getZ()));
                context.assertTrue(read.kind(footprint.getX(), footprint.getZ()) == GradePlan.Kind.OUT,
                        "a pegada da casa não ficou fora do plano");
                context.assertTrue(read.kind(ring.getX(), ring.getZ()) == GradePlan.Kind.ANCHOR
                        && read.anchor(ring.getX(), ring.getZ()) == ring.getY(),
                        "a faixa da casa não virou âncora na altura do chão dela: " + read.kind(ring.getX(), ring.getZ())
                                + " em " + read.anchor(ring.getX(), ring.getZ()) + ", chão em " + ring.getY());
                context.assertTrue(read.kind(path.getX(), path.getZ()) == GradePlan.Kind.PATH, "o caminho não foi lido");
                context.assertTrue(read.doors() == 1, "portas lidas: " + read.doors());
                context.assertTrue(read.pending() > 0, "o paredão de 3 não deu coluna a mexer");
                context.assertTrue(read.line(colonyId, 0).startsWith("VC_GRADE version=1")
                        && read.line(colonyId, 0).contains(" doors=1 "), "a linha diária: " + read.line(colonyId, 0));
                context.assertTrue(world.getBlockState(base.add(16, 4, 10)).isOf(Blocks.GRASS_BLOCK)
                        && world.getBlockState(base.add(15, 2, 10)).isAir(), "a leitura mexeu no mundo");
            } catch (RuntimeException failed) {
                // A falha dentro do waitAndRun não sai com a mensagem no log da bateria.
                VillageColonyMod.LOGGER.error("VC_TEST_FAIL GradeSurveyGameTest: {}", failed.getMessage());
                throw failed;
            } finally {
                VillageColonyMod.BUILDINGS.remove(houseId);
                for (int cx = (base.getX() >> 4) - 1; cx <= ((base.getX() + 23) >> 4) + 1; cx++) {
                    for (int cz = (base.getZ() >> 4) - 1; cz <= ((base.getZ() + 23) >> 4) + 1; cz++) {
                        world.setChunkForced(cx, cz, false);
                    }
                }
            }

            context.complete();
        });
    }
}
