package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.ai.pathing.PathNode;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Como o aldeão da colônia escolhe o trajeto — autor, 2026-10-28, depois do jogo da 0.3.104 (4 de 7 teleportes
 * noturnos saíram do meio do lago; o aldeão cortava pelo gramado). Escolhas: travessia curta de água é válida, e
 * a rua continua preferível. A navegação é a do jogo ({@code findPathTo}); o mod só muda o custo dos nós.
 */
public class ColonyPathingGameTest implements FabricGameTest {

    /**
     * Canal de 1 de largura em z 3, de x 0 a 12, com volta a pé pela ponta (x 13..14). No custo do Vanilla nadar sai
     * por 30 (6 de distância, 8 na água e 8 em cada margem) e a volta por ~34: o Vanilla nada. Na colônia a água só
     * serve sem caminho seco (Regra 278): ele dá a volta. Tudo dentro de x 0..14: além disso a arena não tem chão
     * carregado e a volta não existe.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "colony_pathing_water", tickLimit = 40)
    public void theColonyVillagerWalksAroundShortWaterWhenThereIsADryWay(TestContext context) {
        floor(context, Blocks.STONE, 14);

        for (int x = 0; x <= 12; x++) {
            context.setBlockState(new BlockPos(x, 1, 3), Blocks.WATER.getDefaultState());
        }

        Scene scene = scene(context, new BlockPos(0, 2, 1));

        context.waitAndRun(5, () -> {
            Path path = null;
            try {
                waterState("before-original", context, scene.villager());
                path = scene.pathTo(context.getAbsolutePos(new BlockPos(0, 2, 7)));
                waterState("after-original", context, scene.villager());

                context.assertTrue(path != null && path.reachesTarget(), "sem caminho até o outro lado: " + path);

                Set<Long> channel = columns(context, 0, 12, 3, 3);

                boolean crossesChannel = false;
                for (int i = 0; i < path.getLength(); i++) {
                    BlockPos at = path.getNode(i).getBlockPos();
                    crossesChannel |= channel.contains(column(at));
                }
                context.assertFalse(crossesChannel, "Regra 278: nadou com caminho seco ao lado (custo "
                        + scene.villager().getPathfindingPenalty(net.minecraft.entity.ai.pathing.PathNodeType.WATER)
                        + "): " + nodes(path));
            } finally {
                try {
                    diagnoseWater(context, scene, path);
                } finally {
                    scene.owned.cleanUp();
                }
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

    /** Mesma geometria fechada nas posições observadas; nenhuma entidade é movida entre as buscas. Regra 278: em todas, a volta seca. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "colony_pathing_water_translation", tickLimit = 40)
    public void theDryDetourIsTranslationInvariantAtTheObservedCoordinates(TestContext context) {
        Map<BlockPos, BlockState> saved = new HashMap<>();
        Map<BlockPos, Scene> probes = new java.util.LinkedHashMap<>();
        Set<Long> forced = new HashSet<>();
        Runnable cleanup = () -> {
            probes.values().forEach(probe -> probe.owned.cleanUp());
            saved.forEach((at, state) -> context.getWorld().setBlockState(at, state, Block.NOTIFY_LISTENERS));
            forced.forEach(chunk -> {
                var at = new net.minecraft.util.math.ChunkPos(chunk);
                context.getWorld().setChunkForced(at.x, at.z, false);
            });
        };
        try {
        for (BlockPos origin : new BlockPos[]{new BlockPos(3363646, -60, 4792765),
                new BlockPos(11504738, -60, 14397879), new BlockPos(8977781, -60, 13733688)}) {
                int chunkX = origin.getX() >> 4;
                int chunkZ = origin.getZ() >> 4;
                for (int cx = chunkX - 1; cx <= chunkX + 2; cx++) {
                    for (int cz = chunkZ - 1; cz <= chunkZ + 2; cz++) {
                        long chunk = net.minecraft.util.math.ChunkPos.toLong(cx, cz);
                        if (!context.getWorld().getForcedChunks().contains(chunk)) {
                            context.getWorld().setChunkForced(cx, cz, true);
                            forced.add(chunk);
                        }
                    }
                }
                // Caixa fechada evita comparar terrenos externos diferentes nas três posições.
                for (int x = -1; x <= 15; x++) {
                    for (int z = -1; z <= 9; z++) {
                        for (int y = -1; y <= 5; y++) {
                            BlockPos at = origin.add(x, y, z);
                            saved.put(at, context.getWorld().getBlockState(at));
                            boolean wall = x == -1 || x == 15 || z == -1 || z == 9 || y == -1 || y == 5;
                            Block block = wall ? Blocks.BARRIER : y == 1 ? Blocks.STONE : Blocks.AIR;
                            if (!wall && y == 1 && z == 3 && x <= 12) block = Blocks.WATER;
                            context.getWorld().setBlockState(at, block.getDefaultState(), Block.NOTIFY_LISTENERS);
                        }
                    }
                }
                probes.put(origin, sceneAt(context, origin.add(0, 2, 1)));
        }
        } catch (RuntimeException failure) {
            cleanup.run();
            throw failure;
        }

        context.waitAndRun(10, () -> {
            Map<BlockPos, Path> paths = new java.util.LinkedHashMap<>();
            try {
            for (var entry : probes.entrySet()) {
                BlockPos origin = entry.getKey();
                Scene probe = entry.getValue();
                Map<Integer, BlockPos> hashes = new HashMap<>();
                int aliases = 0;
                for (BlockPos at : BlockPos.iterate(origin.add(-1, -1, -1), origin.add(15, 5, 9))) {
                    at = at.toImmutable();
                    BlockPos previous = hashes.putIfAbsent(PathNode.hash(at.getX(), at.getY(), at.getZ()), at);
                    if (previous != null && !previous.equals(at)) aliases++;
                }
                boolean aliasControl = PathNode.hash(origin.getX(), origin.getY(), origin.getZ())
                        == PathNode.hash(origin.getX(), origin.getY(), origin.getZ() + 256);
                Path path = probe.pathTo(origin.add(0, 2, 7));
                paths.put(origin, path);
                VillageColonyMod.LOGGER.info("WATER_TRANSLATION origin={} hashAliases={} z256AliasControl={} distance={} ground={}",
                        origin.toShortString(), aliases, aliasControl,
                        new PathNode(origin.getX(), origin.getY() + 2, origin.getZ() + 1)
                                .getDistance(new PathNode(origin.getX(), origin.getY() + 2, origin.getZ() + 7)),
                        probe.villager().isOnGround());
                waterRoute("translation=" + origin.toShortString(), path, origin);
                context.assertTrue(aliases == 0 && aliasControl, "hash local colidiu ou controle z+256 não colidiu");
            }
            for (var entry : paths.entrySet()) {
                BlockPos origin = entry.getKey();
                Path path = entry.getValue();
                context.assertTrue(probes.get(origin).villager().isOnGround(),
                        "diagnóstico sem apoio físico em " + origin.toShortString());
                context.assertTrue(path != null && path.reachesTarget(), "sem caminho na translação " + origin.toShortString());
                boolean crossesWater = false;
                for (int i = 0; i < path.getLength(); i++) {
                    crossesWater |= path.getNode(i).type == net.minecraft.entity.ai.pathing.PathNodeType.WATER;
                }
                context.assertFalse(crossesWater, "Regra 278: nadou com caminho seco ao lado: " + origin.toShortString()
                        + " início=" + path.getNode(0).getBlockPos().subtract(origin).toShortString()
                        + " aldeão=" + probes.get(origin).villager().getBlockPos().subtract(origin).toShortString());
            }
            } finally {
                cleanup.run();
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

    /** Diagnóstico limitado, após as assertivas: consulta o mundo sem mover o aldeão ou alterar a arena. */
    private static void diagnoseWater(TestContext context, Scene scene, Path original) {
        BlockPos origin = context.getAbsolutePos(BlockPos.ORIGIN);
        VillagerEntity villager = scene.villager();
        VillageColonyMod.LOGGER.info("WATER_DIAG origin={} xAxis={} zAxis={} time={} uuid={} pos={} ground={} follow={}",
                origin.toShortString(), context.getAbsolutePos(new BlockPos(1, 0, 0)).subtract(origin).toShortString(),
                context.getAbsolutePos(new BlockPos(0, 0, 1)).subtract(origin).toShortString(),
                context.getWorld().getTime(), villager.getUuid(), villager.getPos(), villager.isOnGround(),
                villager.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.GENERIC_FOLLOW_RANGE));
        waterRoute("original", original, origin);
        diagnoseColdAndWarm(context, scene, origin);

        // As duas bordas e a ponta seca, incluindo uma coluna externa em cada lado.
        for (int x : new int[]{-1, 0, 12, 13, 14, 15}) {
            StringBuilder cells = new StringBuilder();
            for (int z = 0; z <= 8; z++) {
                BlockPos feet = context.getAbsolutePos(new BlockPos(x, 2, z));
                cells.append(z).append(':');
                for (int y = 0; y <= 4; y++) {
                    var state = context.getWorld().getBlockState(context.getAbsolutePos(new BlockPos(x, y, z)));
                    String id = Registries.BLOCK.getId(state.getBlock()).getPath();
                    cells.append(switch (id) {
                        case "air" -> "A";
                        case "stone" -> "S";
                        case "water" -> "W";
                        default -> "[" + id + "]";
                    });
                }
                cells.append(villager.getNavigation().isValidPosition(feet) ? '+' : '-').append(' ');
            }
            VillageColonyMod.LOGGER.info("WATER_TERRAIN x={} cells[z:y0..4,validSupport]={}", x, cells);
        }

        // Todas partem da posição real original; não são teletransportes nem segmentos concatenados.
        for (BlockPos waypoint : new BlockPos[]{new BlockPos(13, 2, 1), new BlockPos(14, 2, 3),
                new BlockPos(13, 2, 5), new BlockPos(-1, 2, 3), new BlockPos(0, 2, 7)}) {
            BlockPos absolute = context.getAbsolutePos(waypoint);
            waterRoute("to=" + waypoint.toShortString() + "/valid="
                    + villager.getNavigation().isValidPosition(absolute), scene.pathTo(absolute), origin);
        }
    }

    /** Comparações no mesmo tique, após as assertivas: sem Brain ou física entre as consultas. */
    private static void diagnoseColdAndWarm(TestContext context, Scene scene, BlockPos origin) {
        VillagerEntity villager = scene.villager();
        BlockPos target = context.getAbsolutePos(new BlockPos(0, 2, 7));
        float originalPenalty = villager.getPathfindingPenalty(PathNodeType.WATER);
        try {
            villager.setPathfindingPenalty(PathNodeType.WATER, PathNodeType.WATER.getDefaultPenalty());
            waterState("cold8-before", context, villager);
            waterRoute("cold8", scene.pathTo(target), origin);
            waterState("cold8-after", context, villager);

            villager.setPathfindingPenalty(PathNodeType.WATER, 48.0f);
            waterRoute("warm48", scene.pathTo(target), origin);
            waterState("warm48-after", context, villager);

            // O cache mundial guarda tipos comuns de bloco; o cache de nós da busca já é limpo por init.
            for (int x = -2; x <= 16; x++) {
                for (int z = -2; z <= 10; z++) {
                    for (int y = -2; y <= 5; y++) {
                        context.getWorld().getPathNodeTypeCache().invalidate(context.getAbsolutePos(new BlockPos(x, y, z)));
                    }
                }
            }
            waterRoute("warm48-worldTypesInvalidated", scene.pathTo(target), origin);
            waterState("invalidated-after", context, villager);
        } finally {
            villager.setPathfindingPenalty(PathNodeType.WATER, originalPenalty);
        }
    }

    /** Estado curto suficiente para distinguir busca prévia, passeio natural e penalidade lazy. */
    private static void waterState(String phase, TestContext context, VillagerEntity villager) {
        Path current = villager.getNavigation().getCurrentPath();
        VillageColonyMod.LOGGER.info(
                "WATER_STATE phase={} time={} age={} pos={} velocity={} ground={} water={} navNodes={} brainPathNodes={} walk={}",
                phase, context.getWorld().getTime(), villager.age, villager.getPos(), villager.getVelocity(),
                villager.isOnGround(), villager.getPathfindingPenalty(PathNodeType.WATER),
                current == null ? -1 : current.getLength(),
                villager.getBrain().getOptionalRegisteredMemory(MemoryModuleType.PATH).map(Path::getLength).orElse(-1),
                villager.getBrain().getOptionalRegisteredMemory(MemoryModuleType.WALK_TARGET)
                        .map(walk -> walk.getLookTarget().getBlockPos().toShortString()).orElse("none"));
    }

    /** Coordenadas em deslocamentos absolutos da origem; rota limitada a 700 caracteres. */
    private static void waterRoute(String label, Path path, BlockPos origin) {
        if (path == null) {
            VillageColonyMod.LOGGER.info("WATER_ROUTE {} null", label);
            return;
        }
        StringBuilder route = new StringBuilder();
        double distance = 0;
        double penalties = 0;
        int water = 0;
        for (int i = 0; i < path.getLength(); i++) {
            var node = path.getNode(i);
            if (i > 0) {
                distance += node.getDistance(path.getNode(i - 1));
                penalties += node.penalty;
            }
            if (node.type == net.minecraft.entity.ai.pathing.PathNodeType.WATER) water++;
            if (route.length() < 700) {
                route.append(node.getBlockPos().subtract(origin).toShortString()).append('/')
                        .append(node.type).append('/').append(node.penalty)
                        .append("/g=").append(node.penalizedPathLength)
                        .append("/f=").append(node.heapWeight)
                        .append("/h=").append(node.distanceToNearestTarget)
                        .append("/l=").append(node.pathLength).append(';');
            }
        }
        if (route.length() > 700) route.setLength(700);
        VillageColonyMod.LOGGER.info("WATER_ROUTE {} reached={} n={} distance={} penalties={} water={} end={} deltaNodes={}",
                label, path.reachesTarget(), path.getLength(), distance, penalties, water,
                path.getEnd() == null ? "none" : path.getEnd().getBlockPos().subtract(origin).toShortString(), route);
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
        return sceneAt(context, context.getAbsolutePos(start));
    }

    private static Scene sceneAt(TestContext context, BlockPos from) {
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
