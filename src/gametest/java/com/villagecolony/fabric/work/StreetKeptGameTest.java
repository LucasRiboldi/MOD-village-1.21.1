package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.UUID;

/**
 * Regra 364 (nota do jogo da 0.4.141, N2 🅲: "blocos surgindo do nada"): o Zelador só sobe ou desce chão natural; a rua
 * e o caminho ficam na altura e no bloco em que estão.
 */
public class StreetKeptGameTest implements FabricGameTest {

    private static final int SIDE = 20;

    /** A coluna de rua de arenito liso não sobe: antes, o bloco de cima era copiado para o alto. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "street_kept_column", tickLimit = 20)
    public void aStreetColumnIsNeverRaised(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos street = context.getAbsolutePos(new BlockPos(3, 2, 3));

        world.setBlockState(street.down(2), Blocks.STONE.getDefaultState());
        world.setBlockState(street.down(), Blocks.SANDSTONE.getDefaultState());
        world.setBlockState(street, Blocks.SMOOTH_SANDSTONE.getDefaultState());
        world.setBlockState(street.up(), Blocks.AIR.getDefaultState());
        world.setBlockState(street.up(2), Blocks.AIR.getDefaultState());

        try {
            boolean changed = GroundSmoothing.column(world, street.getX(), street.getZ(), street.getY(), street.getY() + 2);

            context.assertFalse(changed || world.getBlockState(street.up(2)).isOf(Blocks.SMOOTH_SANDSTONE),
                    "Regra 364: a coluna de rua subiu, com o arenito liso copiado para o alto");
            context.assertTrue(world.getBlockState(street).isOf(Blocks.SMOOTH_SANDSTONE), "a rua mudou de bloco");
        } finally {
            for (int y = -2; y <= 2; y++) {
                world.setBlockState(street.up(y), Blocks.AIR.getDefaultState());
            }
        }

        context.complete();
    }

    /** O plano de alturas não põe o caminho na fila do Zelador, nem quando ele fica um acima do chão em volta. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "street_kept_plan", tickLimit = 20)
    public void theHeightPlanLeavesThePathWhereItIs(TestContext context) {
        ServerWorld world = context.getWorld();
        // Longe da arena: a barreira que a cerca seria o topo das colunas.
        BlockPos base = context.getAbsolutePos(BlockPos.ORIGIN).add(2_500_000, 20, 2_500_000);
        BlockPos path = base.add(10, 2, 10);

        force(world, base, true);

        for (int x = 0; x < SIDE; x++) {
            for (int z = 0; z < SIDE; z++) {
                world.setBlockState(base.add(x, 0, z), Blocks.STONE.getDefaultState());
                world.setBlockState(base.add(x, 1, z), Blocks.GRASS_BLOCK.getDefaultState());
                for (int y = 2; y <= 4; y++) {
                    world.setBlockState(base.add(x, y, z), Blocks.AIR.getDefaultState());
                }
            }
        }
        world.setBlockState(path.down(), Blocks.DIRT.getDefaultState());
        world.setBlockState(path, Blocks.DIRT_PATH.getDefaultState());

        Colony colony = Colony.create(UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(base.add(5, 2, 5)));

        colony.restoreBounds(new VillageBounds(base.getX() + 2, base.getY(), base.getZ() + 2, base.getX() + SIDE - 3,
                base.getY() + 4, base.getZ() + SIDE - 3));

        try {
            Optional<GradeSurvey.Survey> survey = GradeSurvey.read(world, colony);

            context.assertTrue(survey.isPresent(), "o plano não foi lido");
            context.assertTrue(survey.get().work().stream().noneMatch(item -> item[0] == path.getX() && item[1] == path.getZ()),
                    "Regra 364: o caminho entrou na fila do Zelador para mudar de altura");
        } finally {
            force(world, base, false);
        }

        context.complete();
    }

    private static void force(ServerWorld world, BlockPos base, boolean forced) {
        for (int cx = (base.getX() >> 4) - 1; cx <= ((base.getX() + SIDE) >> 4) + 1; cx++) {
            for (int cz = (base.getZ() >> 4) - 1; cz <= ((base.getZ() + SIDE) >> 4) + 1; cz++) {
                world.setChunkForced(cx, cz, forced);
            }
        }
    }
}
