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
 * A frente de cada porta da vila gerada — Regra 303 (autor, 2026-11-16: "todas estruturas devem nascer com um bloco de
 * caminho do lado de fora da porta, na altura do solo"). Na geração, antes de as peças serem postas, as portas são lidas
 * do molde de cada peça (na posição e no giro dela); a coluna logo à frente de cada porta, fora das peças, fica no piso
 * da porta — o chão é cortado ou aterrado até ele, sem degrau de 2 — e ganha um bloco de caminho no topo.
 */
final class DoorFronts {

    private static final List<Block> DOORS = Registries.BLOCK.stream()
            .filter(block -> block.getDefaultState().isIn(BlockTags.WOODEN_DOORS)).toList();

    private DoorFronts() {
    }

    /**
     * As colunas à frente das portas desta peça, com o piso da porta (o bloco de baixo dela). Peça sem molde (enfeite de
     * feature) não tem porta.
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

        for (Block door : DOORS) {
            for (StructureTemplate.StructureBlockInfo info : template.get().getInfosForBlock(piece.getPos(), placement, door)) {
                BlockState state = info.state();

                if (!state.contains(DoorBlock.HALF) || state.get(DoorBlock.HALF) != DoubleBlockHalf.LOWER) {
                    continue;
                }

                Direction facing = state.get(DoorBlock.FACING);

                for (Direction side : List.of(facing, facing.getOpposite())) {
                    BlockPos front = info.pos().offset(side);

                    fronts.put(ColonyRoads.column(front.getX(), front.getZ()), info.pos().getY() - 1);
                }
            }
        }

        return fronts;
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
