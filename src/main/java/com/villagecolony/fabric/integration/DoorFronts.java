package com.villagecolony.fabric.integration;

import com.mojang.serialization.JsonOps;
import com.villagecolony.core.construction.model.ColonyRoads;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.structure.pool.StructurePoolElement;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.chunk.Chunk;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A entrada de cada porta da vila gerada — Regras 303 e 305 (autor, 2026-11-16 e 2026-11-17): na geração, antes de as
 * peças serem postas, as portas são lidas do molde de cada peça (na posição e no giro dela); a primeira coluna à frente de
 * cada porta sem bloco da estrutura fica no piso da porta — o chão é cortado ou aterrado até ele, sem nada acima — e
 * ganha um bloco de caminho no topo. É ali que fica o nó de entrada do grafo.
 */
final class DoorFronts {

    private static final List<Block> DOORS = Registries.BLOCK.stream()
            .filter(block -> block.getDefaultState().isIn(BlockTags.WOODEN_DOORS)).toList();

    private DoorFronts() {
    }

    /** Até quantas colunas da porta para fora se procura a primeira sem bloco da estrutura. */
    static final int MAX_WALK = 16;

    /**
     * A coluna de entrada de cada porta desta peça, com o piso da porta (o bloco de baixo dela) — Regra 305: a primeira
     * coluna à frente da porta sem nenhum bloco da estrutura do piso para cima, mesmo dentro da caixa do molde. Dos dois
     * lados da porta vale o mais curto; no empate, o de fora (o {@code facing} da porta aponta para dentro). Peça sem
     * molde (enfeite de feature) não tem porta.
     *
     * @return coluna empacotada ({@link ColonyRoads#column}) → altura do piso
     */
    static Map<Long, Integer> of(PoolStructurePiece piece, StructureTemplateManager templates,
                                 DynamicRegistryManager registries) {
        Map<Long, Integer> fronts = new HashMap<>();
        Optional<StructureTemplate> template = templateOf(piece.getPoolElement(), templates, registries);

        if (template.isEmpty()) {
            return fronts;
        }

        StructurePlacementData placement = new StructurePlacementData().setRotation(piece.getRotation());
        Map<Long, Integer> highest = highestBlocks(template.get(), placement, piece.getPos());

        for (Block door : DOORS) {
            for (StructureTemplate.StructureBlockInfo info : template.get().getInfosForBlock(piece.getPos(), placement, door)) {
                BlockState state = info.state();

                if (!state.contains(DoorBlock.HALF) || state.get(DoorBlock.HALF) != DoubleBlockHalf.LOWER) {
                    continue;
                }

                int floor = info.pos().getY() - 1;
                Direction inward = state.get(DoorBlock.FACING);
                Optional<BlockPos> out = firstFree(info.pos(), inward.getOpposite(), highest, floor);
                Optional<BlockPos> in = firstFree(info.pos(), inward, highest, floor);
                Optional<BlockPos> entry = out.isPresent() && (in.isEmpty()
                        || out.get().getManhattanDistance(info.pos()) <= in.get().getManhattanDistance(info.pos())) ? out : in;

                entry.ifPresent(at -> fronts.put(ColonyRoads.column(at.getX(), at.getZ()), floor));
            }
        }

        return fronts;
    }

    /** A primeira coluna, da porta para {@code side}, sem bloco da estrutura no piso ou acima. */
    private static Optional<BlockPos> firstFree(BlockPos door, Direction side, Map<Long, Integer> highest, int floor) {
        for (int step = 1; step <= MAX_WALK; step++) {
            BlockPos at = door.offset(side, step);
            Integer top = highest.get(ColonyRoads.column(at.getX(), at.getZ()));

            if (top == null || top < floor) {
                return Optional.of(at);
            }
        }

        return Optional.empty();
    }

    /** Coluna a coluna, o bloco de estrutura mais alto do molde: ar, vazio e encaixe não contam. */
    private static Map<Long, Integer> highestBlocks(StructureTemplate template, StructurePlacementData placement, BlockPos pos) {
        Map<Long, Integer> highest = new HashMap<>();
        net.minecraft.nbt.NbtCompound nbt = template.writeNbt(new net.minecraft.nbt.NbtCompound());
        net.minecraft.nbt.NbtList palette = nbt.contains("palette") ? nbt.getList("palette", 10)
                : nbt.getList("palettes", 9).getList(0);
        net.minecraft.nbt.NbtList blocks = nbt.getList("blocks", 10);

        for (int i = 0; i < blocks.size(); i++) {
            net.minecraft.nbt.NbtCompound block = blocks.getCompound(i);
            String name = palette.getCompound(block.getInt("state")).getString("Name");

            if (name.equals("minecraft:air") || name.equals("minecraft:structure_void") || name.equals("minecraft:jigsaw")) {
                continue;
            }

            net.minecraft.nbt.NbtList at = block.getList("pos", 3);
            BlockPos world = StructureTemplate.transform(placement, new BlockPos(at.getInt(0), at.getInt(1), at.getInt(2)))
                    .add(pos);

            highest.merge(ColonyRoads.column(world.getX(), world.getZ()), world.getY(), Math::max);
        }

        return highest;
    }

    /** O molde da peça, pelo {@code location} que o codec público dela expõe. */
    private static Optional<StructureTemplate> templateOf(StructurePoolElement element, StructureTemplateManager templates,
                                                          DynamicRegistryManager registries) {
        return StructurePoolElement.CODEC.encodeStart(registries.getOps(JsonOps.INSTANCE), element).result()
                .filter(json -> json.isJsonObject() && json.getAsJsonObject().has("location"))
                .map(json -> json.getAsJsonObject().get("location"))
                .filter(location -> location.isJsonPrimitive())
                .map(location -> Identifier.tryParse(location.getAsString()))
                .flatMap(id -> id == null ? Optional.empty() : templates.getTemplate(id));
    }

    /**
     * Leva a coluna da frente da porta ao piso dela — corta o chão acima e aterra o vão abaixo — e põe o bloco de caminho
     * no topo.
     */
    static void shape(StructureWorldAccess world, Chunk chunk, int x, int z, int top, int floor, VillageSoil soil) {
        VillageGround.column(world, chunk, x, z, top, floor, true, soil);

        BlockPos at = new BlockPos(x, floor, z);

        if (SiteSurroundings.isNaturalGround(chunk.getBlockState(at)) && chunk.getBlockState(at.up()).isAir()) {
            chunk.setBlockState(at, Blocks.DIRT_PATH.getDefaultState(), false);
        }
    }
}
