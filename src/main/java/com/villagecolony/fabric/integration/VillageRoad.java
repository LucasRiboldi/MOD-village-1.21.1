package com.villagecolony.fabric.integration;

import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.construction.model.VillagePalette;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.content.ModContent;
import com.villagecolony.fabric.content.SandPathBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.Biome;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.Set;

/**
 * Materiais oficiais de pavimentacao da colonia.
 *
 * <p>P0.7 fixa o conjunto de materiais da estrada. O bloco, sozinho, nao
 * identifica uma rua: {@link BuildSiteScanner} tambem exige que a coluna
 * esteja na area espacial reservada pela colonia.
 */
public final class VillageRoad {

    static {
        ServerMemory.register(VillageRoad.class, VillageRoad::clearAll);
    }

    private static final ResourceId DEFAULT_PAVING = VillagePalette.DIRT_PATH;

    private static final Set<Block> ROAD_MATERIALS = Set.of(
            Blocks.DIRT_PATH,
            Blocks.GRAVEL,
            Blocks.TERRACOTTA);

    /**
     * Os caminhos de bioma ({@link #pavingOn}) que não são rua em qualquer lugar: o arenito liso é
     * a parede das casas Vanilla do deserto (2.066 blocos em 28 casas, 350 no nível do chão), e o
     * gelo compactado é o pico do bioma de gelo. Só contam como rua onde a colônia os assentou —
     * a coluna no índice de ruas ({@link #isPaving(ServerWorld, BlockPos)}).
     */
    private static final Set<Block> BIOME_PATHS = Set.of(
            Blocks.SMOOTH_SANDSTONE,
            Blocks.SMOOTH_RED_SANDSTONE,
            Blocks.PACKED_MUD,
            Blocks.PACKED_ICE);

    private static final RegistryKey<Biome> DESERT = BiomeKeys.DESERT;

    /** Os biomas cujo chão é rocha: só neles a pedra vira caminho de cascalho. */
    private static final Set<net.minecraft.registry.RegistryKey<net.minecraft.world.biome.Biome>> ROCKY = Set.of(
            BiomeKeys.STONY_PEAKS, BiomeKeys.STONY_SHORE, BiomeKeys.JAGGED_PEAKS,
            BiomeKeys.WINDSWEPT_HILLS, BiomeKeys.WINDSWEPT_GRAVELLY_HILLS);

    private VillageRoad() {
    }

    /** O material que a colonia usa ao criar um novo trecho de rua. */
    public static Optional<ResourceId> pavingFor(ServerWorld world, String style) {
        return Optional.of(DEFAULT_PAVING);
    }

    /**
     * O caminho natural do chão que a rua cobre — pedido do autor, 2026-10-20: <i>"cada bioma
     * possua blocos de caminho naturais do bioma"</i>.
     *
     * <p>O chão é o do bioma, e o caminho sai dele: terra, grama, podzol, micélio e musgo viram o
     * caminho de terra do Vanilla ({@code fallback}); areia vira arenito liso, como a rua da vila
     * do deserto (as ruas das outras quatro vilas Vanilla são de caminho de terra, conferido nas
     * peças do jar); areia vermelha, arenito vermelho liso; lama, lama compactada; neve e gelo,
     * gelo compactado; pedra e cascalho, cascalho; terracota fica terracota.
     */
    public static Block pavingOn(BlockState ground, Block fallback) {
        if (ground.isOf(Blocks.RED_SAND)) {
            return Blocks.SMOOTH_RED_SANDSTONE;
        }

        if (ground.isIn(BlockTags.SAND)) {
            return Blocks.SMOOTH_SANDSTONE;
        }

        if (ground.isOf(Blocks.MUD)) {
            return Blocks.PACKED_MUD;
        }

        if (ground.isOf(Blocks.SNOW_BLOCK) || ground.isIn(BlockTags.ICE)) {
            return Blocks.PACKED_ICE;
        }

        if (ground.isIn(BlockTags.BASE_STONE_OVERWORLD) || ground.isOf(Blocks.GRAVEL) || ground.isOf(Blocks.CLAY)) {
            return Blocks.GRAVEL;
        }

        if (ground.isIn(BlockTags.TERRACOTTA)) {
            return Blocks.TERRACOTTA;
        }

        return fallback;
    }

    /** O mesmo, com o caminho de terra como o da grama. */
    public static Block pavingOn(BlockState ground) {
        return pavingOn(ground, Blocks.DIRT_PATH);
    }

    /**
     * O caminho que a colônia assenta neste lugar — autor, 2026-10-22: no deserto é sempre o caminho de
     * areia ({@link SandPathBlock}), o caminho exclusivo da vila do deserto, sobre qualquer chão que vire
     * caminho; fora dele, o caminho natural do chão ({@link #pavingOn(BlockState, Block)}).
     */
    public static Block pavingOn(ServerWorld world, BlockPos at, BlockState ground, Block fallback) {
        return isDesert(world, at) ? ModContent.SAND_PATH : pavingOn(ground, fallback);
    }

    /** O mesmo, com o caminho de terra como o da grama. */
    public static Block pavingOn(ServerWorld world, BlockPos at, BlockState ground) {
        return pavingOn(world, at, ground, Blocks.DIRT_PATH);
    }

    private static boolean isDesert(ServerWorld world, BlockPos at) {
        return world.getBiome(at).getKey().filter(DESERT::equals).isPresent();
    }

    /** O bloco de caminho — o de terra do Vanilla ou o de areia do deserto. */
    public static boolean isPathBlock(BlockState state) {
        return state.isOf(Blocks.DIRT_PATH) || state.getBlock() instanceof SandPathBlock;
    }

    /**
     * Se este chão pode virar caminho: o chão do bioma, mas na vila gerada só o solo — o arenito e
     * a pedra de uma peça Vanilla continuam dela (Regra 3). Pedra só nos biomas de rocha: fora
     * deles a rua não sobe na montanha.
     */
    public static boolean isPavableGround(ServerWorld world, BlockPos at, BlockState state) {
        if (BlockProtection.isVillageOriginal(world, at)) {
            return LotGround.isNaturalGround(state);
        }

        if (state.isIn(BlockTags.BASE_STONE_OVERWORLD)) {
            return world.getBiome(at).getKey().filter(ROCKY::contains).isPresent();
        }

        return LotGround.isBiomeGround(world, at) || LotGround.isNaturalGround(state);
    }

    /** Se o bloco é um caminho de bioma, que só é rua onde a colônia o assentou. */
    public static boolean isBiomePath(BlockState state) {
        return BIOME_PATHS.contains(state.getBlock());
    }

    /**
     * Se o bloco desta posição é rua: o material oficial em qualquer lugar, e o caminho de bioma só
     * onde a colônia o assentou (índice de ruas de qualquer colônia) ou na peça de rua da vila gerada.
     */
    public static boolean isPaving(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);

        return isPaving(world, state) || (isBiomePath(state)
                && (RoadIndex.isLaidByAColony(pos) || BlockProtection.isVillageStreet(world, pos)));
    }

    /** Se o bloco pertence ao conjunto oficial de materiais de rua, com o caminho de areia. */
    public static boolean isPaving(ServerWorld world, BlockState state) {
        return ROAD_MATERIALS.contains(state.getBlock()) || state.getBlock() instanceof SandPathBlock;
    }

    /**
     * Se há calçamento colado na caixa desta obra — a um ou dois blocos das
     * bordas, na altura do piso. Sessão longa de 2026-09-26: o planejador pôs a
     * casa encostada numa rua, o índice de ruas ainda não estava montado, e o
     * guarda de alcance mediu do centro e largou a obra um minuto depois. A
     * rua que o lote encosta é a resposta que o índice daria.
     */
    public static boolean besidePaving(ServerWorld world, BlockPos min, int sizeX, int sizeY, int sizeZ) {
        for (int x = min.getX() - 2; x <= min.getX() + sizeX + 1; x++) {
            for (int z = min.getZ() - 2; z <= min.getZ() + sizeZ + 1; z++) {
                boolean inside = x >= min.getX() && x < min.getX() + sizeX
                        && z >= min.getZ() && z < min.getZ() + sizeZ;

                if (inside || !world.getChunkManager().isChunkLoaded(x >> 4, z >> 4)) {
                    continue;
                }

                for (int y = min.getY() - 2; y <= min.getY() + 3; y++) {
                    if (isPaving(world, new BlockPos(x, y, z))) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /** Nao ha catalogo transitivo para limpar, mantido como gancho de ciclo de vida. */
    public static void clearAll() {
    }
}
