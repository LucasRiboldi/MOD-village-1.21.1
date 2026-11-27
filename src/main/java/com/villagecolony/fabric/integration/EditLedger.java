package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.coordination.WorkClock;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.content.ModContent;
import com.villagecolony.fabric.world.GeneratedVillages;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.CropBlock;
import net.minecraft.block.FallingBlock;
import net.minecraft.block.SaplingBlock;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * O conflito entre o jogador e o mod — Regra 345, telemetria F1 a F3b. Por posição, guarda a última edição do jogador
 * (o bloco que ele deixou) e a última do mod (o sistema que mexeu). Sai no log:
 * <ul>
 *   <li>{@code VC_PLAYER_EDIT}: cada edição do jogador na vila, com o alvo (construção, obra, rua, plantação, baú,
 *       bancada, peça Vanilla, terreno natural…);</li>
 *   <li>{@code VC_PLAYER_UNDO}: o jogador mexeu num bloco que o mod tinha mudado há pouco (ele desfez o mod);</li>
 *   <li>{@code VC_MOD_OVERRIDE}: o mod mudou um bloco que o jogador tinha deixado há pouco (o mod desfez o jogador);</li>
 *   <li>{@code VC_PLAYER_INSIST}: o jogador refez o bloco que tinha deixado, depois de o mod o mudar — a reafirmação,
 *       o sinal mais forte do que ele quer e o primeiro item a corrigir.</li>
 * </ul>
 * O mod que escreve blocos diz o sistema ({@link #mod}); a mudança sem dono numa posição do jogador é achada pelo vigia
 * (a cada 5 s) e sai como {@code mundo} (crescimento, queda, fogo) ou {@code mod?<alvo>}.
 * O vigia encerra a comparação para transições naturais de grama e caminho da Regra 351. Só registra: nada muda no jogo.
 * O tempo entre os passos vale até um dia de jogo.
 */
public final class EditLedger {

    /** Até quanto tempo os passos ainda se ligam: um dia de jogo. */
    static final long WINDOW = WorkClock.DAY;

    /** Quantas posições ficam guardadas; a mais antiga sai. */
    static final int KEPT = 4096;

    private static final class Spot {

        @Nullable String playerBlock;

        long playerAt = Long.MIN_VALUE;

        @Nullable String modSystem;

        long modAt = Long.MIN_VALUE;

        int insisted;
    }

    private static final Map<Long, Spot> SPOTS = new LinkedHashMap<>(KEPT, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Spot> eldest) {
            return size() > KEPT;
        }
    };

    static {
        ServerMemory.register(EditLedger.class, EditLedger::clear);
    }

    private EditLedger() {
    }

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (world.getRegistryKey().equals(World.OVERWORLD) && world.getTime() % 100 == 0) {
                watch(world);
            }
        });
    }

    /** O jogador mudou este bloco de {@code before} para {@code after}; devolve as linhas que saíram. */
    public static List<String> player(ServerWorld world, BlockPos pos, BlockState before, BlockState after) {
        List<String> lines = new ArrayList<>();

        if (before.getBlock() == after.getBlock()) {
            return lines;
        }

        long now = world.getTime();
        String block = nameOf(after);
        String target = targetOf(world, pos, before);

        TerrainEdits.record(pos, TerrainEdits.PLAYER); // ADR-064
        BlockJournal.record(now, "player", pos, nameOf(before), block); // Regra 384
        Spot spot = SPOTS.computeIfAbsent(pos.asLong(), ignored -> new Spot());

        lines.add(log("VC_PLAYER_EDIT version=1 action=" + actionOf(before, after) + " block=" + block + " was="
                + nameOf(before) + " target=" + target + " at=" + pos.toShortString()));

        boolean modAfterPlayer = spot.modSystem != null && now - spot.modAt <= WINDOW && spot.modAt >= spot.playerAt;

        if (modAfterPlayer && block.equals(spot.playerBlock) && now - spot.playerAt <= 2 * WINDOW) {
            spot.insisted++;
            lines.add(log("VC_PLAYER_INSIST version=1 system=" + spot.modSystem + " block=" + block + " target=" + target
                    + " count=" + spot.insisted + " gap=" + (now - spot.modAt) + " at=" + pos.toShortString()));
        } else if (modAfterPlayer) {
            lines.add(log("VC_PLAYER_UNDO version=1 system=" + spot.modSystem + " block=" + block + " was="
                    + nameOf(before) + " target=" + target + " gap=" + (now - spot.modAt) + " at=" + pos.toShortString()));
        }

        spot.playerBlock = block;
        spot.playerAt = now;

        if (world == world.getServer().getOverworld()) {
            for (Colony colony : VillageColonyMod.COLONIES.all()) {
                if (containsColumn(colony, pos)) {
                    lines.forEach(line -> SnapshotHistory.remember(colony.id(), line));
                }
            }
        }
        return lines;
    }

    /** O sistema {@code system} do mod deixou este bloco em {@code after}; devolve as linhas que saíram. */
    public static List<String> mod(ServerWorld world, BlockPos pos, BlockState after, String system) {
        List<String> lines = new ArrayList<>();
        long now = world.getTime();
        Spot spot = SPOTS.get(pos.asLong());

        TerrainEdits.record(pos, "mundo".equals(system) ? TerrainEdits.WORLD : TerrainEdits.MOD); // ADR-064
        BlockJournal.record(now, "mod:" + system, pos, "", nameOf(after)); // Regra 384

        if (spot == null) {
            spot = new Spot();
            SPOTS.put(pos.asLong(), spot);
        }

        if (spot.playerBlock != null && now - spot.playerAt <= WINDOW && spot.playerAt > spot.modAt
                && !nameOf(after).equals(spot.playerBlock)) {
            lines.add(log("VC_MOD_OVERRIDE version=1 system=" + system + " block=" + nameOf(after) + " was="
                    + spot.playerBlock + " gap=" + (now - spot.playerAt) + " at=" + pos.toShortString()));
        }

        spot.modSystem = system;
        spot.modAt = now;

        return lines;
    }

    /** As posições do jogador que mudaram sem ninguém dizer quem: o mundo, ou um sistema do mod sem nome. */
    static void watch(ServerWorld world) {
        long now = world.getTime();

        for (Iterator<Map.Entry<Long, Spot>> spots = new ArrayList<>(SPOTS.entrySet()).iterator(); spots.hasNext(); ) {
            Map.Entry<Long, Spot> entry = spots.next();
            Spot spot = entry.getValue();

            if (spot.playerBlock == null || now - spot.playerAt > WINDOW || spot.modAt >= spot.playerAt) {
                continue;
            }

            BlockPos pos = BlockPos.fromLong(entry.getKey());

            if (!world.isChunkLoaded(pos)) {
                continue;
            }

            BlockState state = world.getBlockState(pos);

            if (isGrassTransition(spot.playerBlock, state)) {
                spot.playerBlock = null;
            } else if (!nameOf(state).equals(spot.playerBlock)) {
                mod(world, pos, state, isNatural(state) ? "mundo" : "mod?" + targetOf(world, pos, state));
            }
        }
    }

    /** Regra 351: caminho vira terra antes de poder receber grama novamente. */
    private static boolean isGrassTransition(String before, BlockState after) {
        return switch (before) {
            case "dirt" -> after.isOf(Blocks.GRASS_BLOCK);
            case "grass_block" -> after.isOf(Blocks.DIRT);
            case "dirt_path" -> after.isOf(Blocks.DIRT) || after.isOf(Blocks.GRASS_BLOCK);
            default -> false;
        };
    }

    /** O que o jogador atingiu: o que dá sentido à edição. */
    static String targetOf(ServerWorld world, BlockPos pos, BlockState before) {
        Block block = before.getBlock();
        var at = com.villagecolony.fabric.adapter.MinecraftTypeAdapter.toColonyPos(pos);

        if (block instanceof ChestBlock) {
            return "bau";
        }
        if (Registries.BLOCK.getId(block).getNamespace().equals(VillageColonyMod.MOD_ID) && block.toString().contains("bench")) {
            return "bancada";
        }
        if (VillageColonyMod.BUILDINGS.anythingBuiltInside(at, at)) {
            return "construcao";
        }
        if (block instanceof CropBlock || before.isOf(net.minecraft.block.Blocks.FARMLAND)) {
            return "plantacao";
        }
        if (before.isOf(net.minecraft.block.Blocks.DIRT_PATH) || block == ModContent.SAND_PATH) {
            return "rua";
        }
        if (!GeneratedVillages.structuresAt(world, pos.getX(), pos.getZ()).stream().filter(box -> box.contains(pos)).toList().isEmpty()) {
            return "vanilla";
        }
        if (SiteSurroundings.isNaturalOrOre(before) || before.isIn(BlockTags.LOGS) || before.isIn(BlockTags.LEAVES)) {
            return "natural";
        }
        return "outro";
    }

    private static boolean isNatural(BlockState state) {
        Block block = state.getBlock();

        return block instanceof CropBlock || block instanceof SaplingBlock || block instanceof FallingBlock
                || state.isIn(BlockTags.LEAVES) || state.isIn(BlockTags.LOGS) || state.isIn(BlockTags.FIRE)
                || !state.getFluidState().isEmpty();
    }

    private static String actionOf(BlockState before, BlockState after) {
        if (after.isAir()) {
            return "broke";
        }
        return before.isAir() || before.isReplaceable() ? "placed" : "changed";
    }

    private static String nameOf(BlockState state) {
        var id = Registries.BLOCK.getId(state.getBlock());

        return id.getNamespace().equals("minecraft") ? id.getPath() : id.toString();
    }

    private static String log(String line) {
        VillageColonyMod.LOGGER.info(line);
        return line;
    }

    /** Esquece tudo. */
    static void clear() {
        SPOTS.clear();
    }

    /** Se a posição está na caixa de alguma vila (onde a edição do jogador interessa). */
    public static boolean inAnyVillage(BlockPos pos) {
        for (Colony colony : VillageColonyMod.COLONIES.all()) {
            if (containsColumn(colony, pos)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsColumn(Colony colony, BlockPos pos) {
        return colony.bounds().map(box -> box.containsColumn(pos.getX(), pos.getZ())).orElse(false);
    }
}
