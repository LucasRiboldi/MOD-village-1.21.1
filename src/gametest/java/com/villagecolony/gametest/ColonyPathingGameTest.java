package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Como o aldeão da colônia escolhe o trajeto — autor, 2026-10-28, depois do jogo da 0.3.104 (4 de 7 teleportes
 * noturnos saíram do meio do lago; o aldeão cortava pelo gramado). Escolhas: nadar só se não houver volta, e
 * preferir a rua. A navegação é a do jogo ({@code findPathTo}); o mod só muda o custo dos nós.
 */
public class ColonyPathingGameTest implements FabricGameTest {

    /**
     * Canal de 1 de largura em z 3, de x 0 a 12, com volta a pé pela ponta (x 13..14). No custo do Vanilla nadar sai
     * por 30 (6 de distância, 8 na água e 8 em cada margem) e a volta por ~34: ele nada. No da colônia nadar sai por
     * 70 e a volta ganha, mesmo com a folga de 1,5 da busca do jogo. Tudo dentro de x 0..14: além disso a arena não
     * tem chão carregado e a volta não existe.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "colony_pathing_water", tickLimit = 40)
    public void theColonyVillagerWalksAroundTheWaterWhenThereIsAWayAround(TestContext context) {
        floor(context, Blocks.STONE, 14);

        for (int x = 0; x <= 12; x++) {
            context.setBlockState(new BlockPos(x, 1, 3), Blocks.WATER.getDefaultState());
        }

        Scene scene = scene(context, new BlockPos(0, 2, 1));

        context.waitAndRun(5, () -> {
            try {
                Path path = scene.pathTo(context.getAbsolutePos(new BlockPos(0, 2, 7)));

                context.assertTrue(path != null && path.reachesTarget(), "sem caminho até o outro lado: " + path);

                Set<Long> channel = columns(context, 0, 12, 3, 3);

                for (int i = 0; i < path.getLength(); i++) {
                    BlockPos at = path.getNode(i).getBlockPos();

                    context.assertFalse(channel.contains(column(at)), "o caminho atravessa o canal em "
                            + at.toShortString() + ", e há volta a pé pela ponta (custo da água "
                            + scene.villager().getPathfindingPenalty(net.minecraft.entity.ai.pathing.PathNodeType.WATER)
                            + "): " + nodes(path));
                }
            } finally {
                scene.owned.cleanUp();
            }

            context.complete();
        });
    }

    /**
     * Reta de 12 blocos de grama contra uma rua em U de 20 (x 1, z 0..4; z 0, x 1..13; x 13, z 0..4). O Vanilla
     * corta pela grama; o aldeão da colônia, dentro da caixa da vila, vai pela rua.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "colony_pathing_street", tickLimit = 40)
    public void theColonyVillagerTakesTheStreetInsideTheVillage(TestContext context) {
        floor(context, Blocks.GRASS_BLOCK, 14);

        for (int z = 0; z <= 4; z++) {
            context.setBlockState(new BlockPos(1, 1, z), Blocks.DIRT_PATH.getDefaultState());
            context.setBlockState(new BlockPos(13, 1, z), Blocks.DIRT_PATH.getDefaultState());
        }

        for (int x = 1; x <= 13; x++) {
            context.setBlockState(new BlockPos(x, 1, 0), Blocks.DIRT_PATH.getDefaultState());
        }

        Scene scene = scene(context, new BlockPos(1, 2, 4));
        BlockPos min = context.getAbsolutePos(new BlockPos(0, 0, 0));
        BlockPos max = context.getAbsolutePos(new BlockPos(14, 4, 8));
        scene.colony.restoreBounds(new VillageBounds(Math.min(min.getX(), max.getX()), min.getY(),
                Math.min(min.getZ(), max.getZ()), Math.max(min.getX(), max.getX()), max.getY(),
                Math.max(min.getZ(), max.getZ())));

        context.waitAndRun(5, () -> {
            try {
                Path path = scene.pathTo(context.getAbsolutePos(new BlockPos(13, 2, 4)));

                context.assertTrue(path != null && path.reachesTarget(), "sem caminho até a ponta: " + path);

                Set<Long> middle = columns(context, 4, 10, 3, 8);

                for (int i = 0; i < path.getLength(); i++) {
                    BlockPos at = path.getNode(i).getBlockPos();

                    context.assertFalse(middle.contains(column(at)), "o caminho cortou pela grama no meio, em "
                            + at.toShortString() + ", e a rua dá a volta: " + nodes(path));
                }
            } finally {
                scene.owned.cleanUp();
            }

            context.complete();
        });
    }

    private record Scene(Colony colony, VillagerEntity villager, ColonyFixture owned) {

        Path pathTo(BlockPos target) {
            // O aldeão já passeia no tique 3: pare a navegação antes de perguntar (KF-007).
            villager.getNavigation().stop();

            return villager.getNavigation().findPathTo(target, 0);
        }
    }

    /**
     * As colunas absolutas de um retângulo relativo. Em coordenada absoluta porque a arena pode vir girada, e o
     * {@code getRelativePos} de um nó então não volta ao retângulo escrito no teste.
     */
    private static Set<Long> columns(TestContext context, int minX, int maxX, int minZ, int maxZ) {
        Set<Long> columns = new HashSet<>();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                columns.add(column(context.getAbsolutePos(new BlockPos(x, 2, z))));
            }
        }

        return columns;
    }

    private static long column(BlockPos at) {
        return BlockPos.asLong(at.getX(), 0, at.getZ());
    }

    /** Os nós do caminho, para a mensagem de falha. */
    private static String nodes(Path path) {
        StringBuilder text = new StringBuilder();

        for (int i = 0; i < path.getLength(); i++) {
            text.append(path.getNode(i).getBlockPos().toShortString()).append(' ').append(path.getNode(i).type)
                    .append(' ').append(path.getNode(i).penalty).append(" | ");
        }

        return text.toString();
    }

    /** Aldeão de ofício de uma colônia, de pé em {@code start} relativo. */
    private static Scene scene(TestContext context, BlockPos start) {
        BlockPos from = context.getAbsolutePos(start);
        Colony colony = Colony.create(UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(from));

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);
        VillagerEntity villager = EntityType.VILLAGER.create(context.getWorld());

        villager.refreshPositionAndAngles(from.getX() + 0.5, from.getY(), from.getZ() + 0.5, 0, 0);
        villager.setBreedingAge(0);
        context.getWorld().spawnEntity(villager);
        VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id()).assign(ProfessionType.MASON);
        owned.owning(villager.getUuid());

        return new Scene(colony, villager, owned);
    }

    /** Piso de x 0..{@code maxX} por z 0..8 em y 1, ar até y 4. */
    private static void floor(TestContext context, Block ground, int maxX) {
        for (int x = 0; x <= maxX; x++) {
            for (int z = 0; z <= 8; z++) {
                context.setBlockState(new BlockPos(x, 1, z), ground.getDefaultState());

                for (int y = 2; y <= 4; y++) {
                    context.setBlockState(new BlockPos(x, y, z), Blocks.AIR.getDefaultState());
                }
            }
        }
    }
}
