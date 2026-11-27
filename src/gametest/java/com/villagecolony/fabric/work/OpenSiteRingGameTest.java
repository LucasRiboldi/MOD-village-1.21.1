package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Regra 368 (notas do jogo da 0.4.143, H4 🅱️: "buraco de dois andares errado, não pode ter"): a faixa em volta da obra
 * aberta fica na altura do chão em volta, e não na base da planta, que fica abaixo do chão com a fundação. O Zelador não
 * cava vala em volta da obra.
 */
public class OpenSiteRingGameTest implements FabricGameTest {

    private static final int SIDE = 22;

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "open_site_ring", tickLimit = 20)
    public void theRingOfAnOpenSiteStaysAtTheGround(TestContext context) {
        ServerWorld world = context.getWorld();
        // Longe da arena: a barreira que a cerca seria o topo das colunas.
        BlockPos base = context.getAbsolutePos(BlockPos.ORIGIN).add(2_700_000, 20, 2_700_000);

        force(world, base, true);

        for (int x = 0; x < SIDE; x++) {
            for (int z = 0; z < SIDE; z++) {
                world.setBlockState(base.add(x, 0, z), Blocks.STONE.getDefaultState());
                world.setBlockState(base.add(x, 1, z), Blocks.GRASS_BLOCK.getDefaultState());
                for (int y = 2; y <= 6; y++) {
                    world.setBlockState(base.add(x, y, z), Blocks.AIR.getDefaultState());
                }
            }
        }

        UUID colonyId = UUID.randomUUID();
        Colony colony = Colony.create(colonyId, MinecraftTypeAdapter.toColonyPos(base.add(3, 2, 3)));

        colony.restoreBounds(new VillageBounds(base.getX() + 2, base.getY(), base.getZ() + 2, base.getX() + SIDE - 3,
                base.getY() + 6, base.getZ() + SIDE - 3));

        // A planta com dois blocos de fundação: a base dela fica dois abaixo do chão (y 1).
        Blueprint blueprint = Blueprint.of(ResourceId.parse("minecraft:village/desert/houses/desert_tool_smith_1"),
                List.of(new BlueprintBlock(new ColonyPos(0, 0, 0), ResourceId.vanilla("sandstone")),
                        new BlueprintBlock(new ColonyPos(4, 3, 4), ResourceId.vanilla("sandstone"))));
        ConstructionProject project = ConstructionProject.plan(colonyId, blueprint,
                MinecraftTypeAdapter.toColonyPos(base.add(8, -1, 8)));

        project.moveTo(ConstructionState.PREPARING);
        project.moveTo(ConstructionState.BUILDING);

        try {
            VillageColonyMod.CONSTRUCTIONS.register(project);

            Optional<GradeSurvey.Survey> survey = GradeSurvey.read(world, colony);

            context.assertTrue(survey.isPresent(), "o plano não foi lido");

            List<int[]> ring = survey.get().work().stream().filter(item -> {
                int x = item[0] - base.getX();
                int z = item[1] - base.getZ();
                return x >= 7 && x <= 13 && z >= 7 && z <= 13;
            }).toList();

            context.assertTrue(ring.isEmpty(), "Regra 368: o Zelador ia cavar em volta da obra aberta: "
                    + ring.stream().map(item -> item[0] + "," + item[1] + " para " + item[2]).toList());
        } finally {
            VillageColonyMod.CONSTRUCTIONS.removeOfColony(colonyId);
            force(world, base, false);
        }

        context.complete();
    }

    /**
     * Regra 368: a cova natural de 2 ou mais perto da obra, entre o chão e um barranco, entra na fila do Zelador para
     * subir até um abaixo do vizinho mais baixo dos que a cercam.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "open_site_hole", tickLimit = 20)
    public void aDeepHoleNearTheSiteIsFilled(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos base = context.getAbsolutePos(BlockPos.ORIGIN).add(2_800_000, 20, 2_800_000);

        force(world, base, true);

        // Chão em y 3; do x 16 para leste, um barranco de 5 e 7. A cova fica em (15, 13), com o chão em y 1.
        for (int x = 0; x < SIDE; x++) {
            for (int z = 0; z < SIDE; z++) {
                int top = x == 15 && z == 13 ? 1 : x == 16 ? 5 : x > 16 ? 7 : 3;
                for (int y = 0; y <= 9; y++) {
                    world.setBlockState(base.add(x, y, z), y < top ? Blocks.STONE.getDefaultState()
                            : y == top ? Blocks.GRASS_BLOCK.getDefaultState() : Blocks.AIR.getDefaultState());
                }
            }
        }

        UUID colonyId = UUID.randomUUID();
        Colony colony = Colony.create(colonyId, MinecraftTypeAdapter.toColonyPos(base.add(3, 4, 3)));

        colony.restoreBounds(new VillageBounds(base.getX() + 2, base.getY(), base.getZ() + 2, base.getX() + SIDE - 3,
                base.getY() + 9, base.getZ() + SIDE - 3));

        Blueprint blueprint = Blueprint.of(ResourceId.parse("minecraft:village/desert/houses/desert_tool_smith_1"),
                List.of(new BlueprintBlock(new ColonyPos(0, 0, 0), ResourceId.vanilla("sandstone")),
                        new BlueprintBlock(new ColonyPos(3, 3, 3), ResourceId.vanilla("sandstone"))));
        ConstructionProject project = ConstructionProject.plan(colonyId, blueprint,
                MinecraftTypeAdapter.toColonyPos(base.add(5, 1, 5)));

        project.moveTo(ConstructionState.PREPARING);
        project.moveTo(ConstructionState.BUILDING);

        try {
            VillageColonyMod.CONSTRUCTIONS.register(project);

            Optional<GradeSurvey.Survey> survey = GradeSurvey.read(world, colony);

            context.assertTrue(survey.isPresent(), "o plano não foi lido");
            context.assertTrue(survey.get().work().stream().anyMatch(item -> item[0] == base.getX() + 15
                            && item[1] == base.getZ() + 13 && item[2] == base.getY() + 2),
                    "Regra 368: a cova funda perto da obra ficou fora da fila do Zelador");
        } finally {
            VillageColonyMod.CONSTRUCTIONS.removeOfColony(colonyId);
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
