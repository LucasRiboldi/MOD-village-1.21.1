package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.BlockProtection;
import com.villagecolony.fabric.integration.MineMouths;
import com.villagecolony.fabric.integration.TerrainEdits;
import com.villagecolony.fabric.integration.VillageRoad;
import com.villagecolony.fabric.world.GeneratedVillages;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.GZIPOutputStream;

/**
 * A foto do terreno da colônia — Regra 361, ADR-064. Para cada coluna da caixa da vila mais {@value #MARGIN} de margem: a altura do
 * topo, o bloco do topo, o tipo, quem mexeu nela desde que o servidor abriu ({@link TerrainEdits}) e a altura-alvo do
 * plano (Regra 357); mais as caixas das estruturas, a frente das portas e as bocas da mina. Vai para
 * {@code logs/villagecolony-terrain-<colônia>-<rótulo>.json.gz}: {@code start} uma vez, {@code day<N>} no fim do
 * expediente e {@code last} ao salvar e ao fechar o servidor. É telemetria: o jogo nunca lê a foto de volta.
 */
public final class TerrainSnapshot {

    public static final int VERSION = 1;

    /** Quanto a foto passa da caixa da vila, em cada lado. */
    static final int MARGIN = 16;

    /** O maior lado da foto. */
    static final int MAX_SIDE = 512;

    /** A altura da coluna sem chunk carregado, ou fora do plano. */
    static final int NO_HEIGHT = -32768;

    static final int KIND_UNKNOWN = 0;

    static final int KIND_NATURAL = 1;

    static final int KIND_PATH = 2;

    static final int KIND_WATER = 3;

    static final int KIND_COLONY = 4;

    static final int KIND_OTHER = 5;

    private TerrainSnapshot() {
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            ServerWorld world = server.getOverworld();

            for (Colony colony : List.copyOf(VillageColonyMod.COLONIES.all())) {
                save(world, colony, "last", Optional.empty());
            }
        });
    }

    /** A foto do começo, se esta colônia ainda não tem. */
    public static void start(ServerWorld world, Colony colony) {
        if (!Files.exists(fileOf(dirOf(world), colony, "start"))) {
            save(world, colony, "start", Optional.empty());
        }
    }

    /** Grava a foto com este rótulo na pasta de logs; usa o plano já lido, se houver. */
    public static Optional<Path> save(ServerWorld world, Colony colony, String label,
                                      Optional<GradeSurvey.Survey> survey) {
        return write(world, colony, label, dirOf(world), survey);
    }

    static Optional<Path> write(ServerWorld world, Colony colony, String label, Path dir,
                                Optional<GradeSurvey.Survey> survey) {
        Optional<VillageBounds> bounds = colony.bounds();

        if (bounds.isEmpty()) {
            return Optional.empty();
        }

        long began = System.nanoTime();
        VillageBounds box = bounds.get();
        int minX = box.minX() - MARGIN;
        int minZ = box.minZ() - MARGIN;
        int width = Math.min(MAX_SIDE, box.maxX() - box.minX() + 1 + 2 * MARGIN);
        int depth = Math.min(MAX_SIDE, box.maxZ() - box.minZ() + 1 + 2 * MARGIN);
        Optional<GradeSurvey.Survey> plan = survey.isPresent() ? survey : GradeSurvey.read(world, colony);
        String json = json(world, colony, label, box, minX, minZ, width, depth, plan);
        Path file = fileOf(dir, colony, label);

        try {
            Files.createDirectories(dir);

            try (Writer out = new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(file)),
                    StandardCharsets.UTF_8)) {
                out.write(json);
            }
        } catch (IOException failure) {
            VillageColonyMod.LOGGER.warn("VC_TERRAIN_SNAPSHOT could not write {}: {}", file, failure.toString());
            return Optional.empty();
        }

        VillageColonyMod.LOGGER.info("VC_TERRAIN_SNAPSHOT version={} colony={} label={} columns={} ms={} file={}", VERSION,
                colony.id().toString().substring(0, 8), label, width * depth, (System.nanoTime() - began) / 1_000_000,
                file.getFileName());

        return Optional.of(file);
    }

    static Path fileOf(Path dir, Colony colony, String label) {
        return dir.resolve("villagecolony-terrain-" + colony.id().toString().substring(0, 8) + "-" + label + ".json.gz");
    }

    private static Path dirOf(ServerWorld world) {
        return world.getServer().getRunDirectory().resolve("logs");
    }

    private static String json(ServerWorld world, Colony colony, String label, VillageBounds box, int minX, int minZ,
                               int width, int depth, Optional<GradeSurvey.Survey> plan) {
        int[] heights = new int[width * depth];
        int[] kinds = new int[width * depth];
        int[] blocks = new int[width * depth];
        int[] edits = new int[width * depth];
        int[] targets = new int[width * depth];
        Map<String, Integer> palette = new LinkedHashMap<>();
        int[][] planned = plan.map(GradeSurvey.Survey::targets).orElse(new int[0][0]);
        int planX = plan.map(GradeSurvey.Survey::minX).orElse(0);
        int planZ = plan.map(GradeSurvey.Survey::minZ).orElse(0);

        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < depth; dz++) {
                int index = dx * depth + dz;
                int x = minX + dx;
                int z = minZ + dz;

                edits[index] = TerrainEdits.of(x, z);
                targets[index] = targetAt(planned, x - planX, z - planZ);

                if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                    heights[index] = NO_HEIGHT;
                    kinds[index] = KIND_UNKNOWN;
                    blocks[index] = palette.computeIfAbsent("?", name -> palette.size());
                    continue;
                }

                BlockPos top = new BlockPos(x, world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) - 1, z);
                BlockState state = world.getBlockState(top);

                heights[index] = top.getY();
                kinds[index] = kindOf(state, top);
                blocks[index] = palette.computeIfAbsent(Registries.BLOCK.getId(state.getBlock()).toString(),
                        name -> palette.size());
            }
        }

        StringBuilder text = new StringBuilder(width * depth * 16);
        BlockPos center = MinecraftTypeAdapter.toBlockPos(colony.center());

        text.append("{\"version\":").append(VERSION)
                .append(",\"mod\":\"").append(modVersion()).append('"')
                .append(",\"colony\":\"").append(colony.id()).append('"')
                .append(",\"label\":\"").append(label).append('"')
                .append(",\"day\":").append(Math.floorDiv(world.getTimeOfDay(), com.villagecolony.core.coordination.WorkClock.DAY))
                .append(",\"tick\":").append(world.getTime())
                .append(",\"center\":[").append(center.getX()).append(',').append(center.getY()).append(',')
                .append(center.getZ()).append(']')
                .append(",\"box\":[").append(box.minX()).append(',').append(box.minY()).append(',').append(box.minZ())
                .append(',').append(box.maxX()).append(',').append(box.maxY()).append(',').append(box.maxZ()).append(']')
                .append(",\"minX\":").append(minX).append(",\"minZ\":").append(minZ)
                .append(",\"width\":").append(width).append(",\"depth\":").append(depth);
        array(text, "y", heights);
        array(text, "kind", kinds);
        array(text, "block", blocks);
        array(text, "edit", edits);
        array(text, "target", targets);
        text.append(",\"palette\":[");
        appendStrings(text, new ArrayList<>(palette.keySet()));
        text.append(']');
        structures(world, colony, center, text);
        mines(colony, text);

        return text.append('}').toString();
    }

    private static int targetAt(int[][] planned, int x, int z) {
        return x >= 0 && z >= 0 && x < planned.length && planned.length > 0 && z < planned[0].length
                ? planned[x][z] : NO_HEIGHT;
    }

    private static int kindOf(BlockState state, BlockPos top) {
        if (!state.getFluidState().isEmpty()) {
            return KIND_WATER;
        }

        if (VillageRoad.isPathBlock(state)) {
            return KIND_PATH;
        }

        if (BlockProtection.isColonyBuilt(top)) {
            return KIND_COLONY;
        }

        return WorldTerrain.isNaturalGround(state) ? KIND_NATURAL : KIND_OTHER;
    }

    /** As caixas das construções da colônia e das peças da vila gerada, com a frente de cada porta. */
    private static void structures(ServerWorld world, Colony colony, BlockPos center, StringBuilder text) {
        List<String> boxes = new ArrayList<>();
        List<String> doors = new ArrayList<>();

        for (Building building : VillageColonyMod.BUILDINGS.ofColony(colony.id())) {
            BlockPos min = MinecraftTypeAdapter.toBlockPos(building.min());
            BlockPos max = MinecraftTypeAdapter.toBlockPos(building.max());

            structure(world, boxes, doors, min, max, "colony", building.blueprint().path(), building.finished());
        }

        for (BlockBox piece : GeneratedVillages.structuresAt(world, center.getX(), center.getZ())) {
            structure(world, boxes, doors, new BlockPos(piece.getMinX(), piece.getMinY(), piece.getMinZ()),
                    new BlockPos(piece.getMaxX(), piece.getMaxY(), piece.getMaxZ()), "village", "", true);
        }

        text.append(",\"structures\":[").append(String.join(",", boxes)).append(']');
        text.append(",\"doors\":[").append(String.join(",", doors)).append(']');
    }

    private static void structure(ServerWorld world, List<String> boxes, List<String> doors, BlockPos min, BlockPos max,
                                  String source, String blueprint, boolean finished) {
        int index = boxes.size();

        boxes.add("{\"min\":[" + min.getX() + "," + min.getY() + "," + min.getZ() + "],\"max\":[" + max.getX() + ","
                + max.getY() + "," + max.getZ() + "],\"source\":\"" + source + "\",\"blueprint\":\"" + blueprint
                + "\",\"finished\":" + finished + "}");

        if (!finished) {
            return;
        }

        for (BlockPos front : DoorFronts.of(world, min, max)) {
            doors.add("[" + front.getX() + "," + front.getY() + "," + front.getZ() + "," + index + "]");
        }
    }

    private static void mines(Colony colony, StringBuilder text) {
        List<String> mouths = new ArrayList<>();

        for (Map.Entry<BlockPos, Direction> mouth : MineMouths.of(colony.id()).entrySet()) {
            mouths.add("[" + mouth.getKey().getX() + "," + mouth.getKey().getY() + "," + mouth.getKey().getZ() + ",\""
                    + mouth.getValue().asString() + "\"]");
        }

        text.append(",\"mines\":[").append(String.join(",", mouths)).append(']');
    }

    private static void array(StringBuilder text, String name, int[] values) {
        text.append(",\"").append(name).append("\":[");

        for (int index = 0; index < values.length; index++) {
            if (index > 0) {
                text.append(',');
            }

            text.append(values[index]);
        }

        text.append(']');
    }

    private static void appendStrings(StringBuilder text, List<String> values) {
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                text.append(',');
            }

            text.append('"').append(values.get(index).replace("\"", "")).append('"');
        }
    }

    private static String modVersion() {
        return FabricLoader.getInstance().getModContainer(VillageColonyMod.MOD_ID)
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse("?");
    }
}
