package com.villagecolony.fabric.work;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.TerrainEdits;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

/**
 * Regra 361 (ADR-064): a foto do terreno guarda a altura, o tipo e quem mexeu em cada coluna, e as caixas das construções.
 */
public class TerrainSnapshotGameTest implements FabricGameTest {

    private static final int SIDE = 12;

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "terrain_snapshot", tickLimit = 40)
    public void theSnapshotKeepsEachColumn(TestContext context) {
        ServerWorld world = context.getWorld();
        UUID colonyId = UUID.randomUUID();
        UUID houseId = UUID.randomUUID();
        // Longe da arena: a barreira que a cerca seria o topo das colunas.
        BlockPos base = context.getAbsolutePos(BlockPos.ORIGIN).add(2_900_000, 20, 2_900_000);

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

        world.setBlockState(base.add(3, 2, 3), Blocks.DIRT.getDefaultState());
        world.setBlockState(base.add(5, 1, 5), Blocks.WATER.getDefaultState());
        world.setBlockState(base.add(7, 1, 7), Blocks.DIRT_PATH.getDefaultState());

        Colony colony = Colony.create(colonyId, MinecraftTypeAdapter.toColonyPos(base.add(6, 2, 6)));

        colony.restoreBounds(new VillageBounds(base.getX(), base.getY(), base.getZ(), base.getX() + SIDE - 1,
                base.getY() + 4, base.getZ() + SIDE - 1));
        VillageColonyMod.BUILDINGS.register(new Building(houseId, colonyId, ResourceId.vanilla("house"),
                MinecraftTypeAdapter.toColonyPos(base.add(8, 2, 1)), MinecraftTypeAdapter.toColonyPos(base.add(10, 4, 3))));
        TerrainEdits.record(base.add(3, 2, 3), TerrainEdits.PLAYER);

        context.waitAndRun(2, () -> {
            Path dir = null;

            try {
                dir = Files.createTempDirectory("vc-terrain");
                Optional<Path> file = TerrainSnapshot.write(world, colony, "test", dir, Optional.empty());

                context.assertTrue(file.isPresent(), "Regra 361: a foto não foi gravada");

                JsonObject photo = read(file.get());
                int minX = photo.get("minX").getAsInt();
                int minZ = photo.get("minZ").getAsInt();
                int depth = photo.get("depth").getAsInt();
                JsonArray heights = photo.getAsJsonArray("y");
                JsonArray kinds = photo.getAsJsonArray("kind");
                JsonArray edits = photo.getAsJsonArray("edit");

                context.assertTrue(heights.size() == photo.get("width").getAsInt() * depth
                                && photo.getAsJsonArray("target").size() == heights.size(),
                        "Regra 361: a foto não tem uma altura e um alvo por coluna");

                int bump = (base.getX() + 3 - minX) * depth + (base.getZ() + 3 - minZ);
                int flat = (base.getX() + 1 - minX) * depth + (base.getZ() + 1 - minZ);
                int water = (base.getX() + 5 - minX) * depth + (base.getZ() + 5 - minZ);
                int path = (base.getX() + 7 - minX) * depth + (base.getZ() + 7 - minZ);

                context.assertTrue(heights.get(bump).getAsInt() == base.getY() + 2
                                && heights.get(flat).getAsInt() == base.getY() + 1,
                        "Regra 361: a altura do topo está errada: " + heights.get(bump) + " e " + heights.get(flat));
                context.assertTrue(kinds.get(water).getAsInt() == TerrainSnapshot.KIND_WATER
                                && kinds.get(path).getAsInt() == TerrainSnapshot.KIND_PATH
                                && kinds.get(flat).getAsInt() == TerrainSnapshot.KIND_NATURAL,
                        "Regra 361: o tipo da coluna está errado");
                context.assertTrue((edits.get(bump).getAsInt() & TerrainEdits.PLAYER) != 0
                                && edits.get(flat).getAsInt() == 0,
                        "Regra 361: quem mexeu na coluna se perdeu");
                context.assertTrue(photo.getAsJsonArray("structures").size() == 1,
                        "Regra 361: a casa da colônia não está na foto");
            } catch (IOException | RuntimeException failed) {
                VillageColonyMod.LOGGER.error("VC_TEST_FAIL TerrainSnapshotGameTest: {}", failed.toString());
                throw new IllegalStateException(failed.getMessage(), failed);
            } finally {
                VillageColonyMod.BUILDINGS.remove(houseId);
                force(world, base, false);
                delete(dir);
            }

            context.complete();
        });
    }

    private static JsonObject read(Path file) throws IOException {
        try (Reader in = new InputStreamReader(new GZIPInputStream(Files.newInputStream(file)), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(in).getAsJsonObject();
        }
    }

    private static void delete(Path dir) {
        if (dir == null) {
            return;
        }

        try (var files = Files.list(dir)) {
            for (Path file : files.toList()) {
                Files.deleteIfExists(file);
            }
            Files.deleteIfExists(dir);
        } catch (IOException ignored) {
            // Pasta temporária: o sistema limpa.
        }
    }

    private static void force(ServerWorld world, BlockPos base, boolean forced) {
        for (int cx = (base.getX() >> 4) - 1; cx <= ((base.getX() + SIDE) >> 4) + 1; cx++) {
            for (int cz = (base.getZ() >> 4) - 1; cz <= ((base.getZ() + SIDE) >> 4) + 1; cz++) {
                world.setChunkForced(cx, cz, forced);
            }
        }
    }
}
