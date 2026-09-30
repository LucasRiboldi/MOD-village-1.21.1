package com.villagecolony.fabric.integration;

import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.ConstructionProject;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

/** Prepara depressões rasas de uma obra já alinhada à rua. */
public final class FoundationPreparation {
    private static final int MIN_SUPPORTED_PERCENT = 50;

    /**
     * Quantas camadas abaixo da rua uma coluna da base pode ter e ainda ser
     * aterrada — pedido do autor, 2026-09-30.
     *
     * <p>Era uma. Na sessão das 02:45, 37% das recusas de lote foram "fora da
     * altura da rua", e o autor pediu que a obra fosse construída por cima
     * da metade que falta. Três é escolha provisória: acomoda a encosta
     * suave de vila sem transformar ravina em plataforma. Medir no jogo.
     */
    static final int MAX_FILL_DEPTH = 3;

    private FoundationPreparation() {
    }

    public static boolean prepareIfQualified(ServerWorld world, ConstructionProject project) {
        Blueprint blueprint = project.blueprint();
        if (!blueprint.hasStreetLayer()) return true;
        int roadY = project.origin().y() + blueprint.streetLayer();
        int width = blueprint.size().x();
        int depth = blueprint.size().z();
        int supported = 0;
        List<BlockPos> gaps = new ArrayList<>();
        for (int dx = 0; dx < width; dx++) for (int dz = 0; dz < depth; dz++) {
            BlockPos base = new BlockPos(project.origin().x() + dx, roadY, project.origin().z() + dz);
            if (!world.getBlockState(base).getCollisionShape(world, base).isEmpty()) {
                supported++;
                continue;
            }
            OptionalInt fill = fillableDepth(world, base);
            if (fill.isEmpty()) return false;
            for (int layer = 0; layer < fill.getAsInt(); layer++) gaps.add(base.down(layer));
        }
        if (supported * 100 < width * depth * MIN_SUPPORTED_PERCENT) return false;
        // De baixo para cima: cada camada assenta sobre a anterior.
        gaps.sort((a, b) -> Integer.compare(a.getY(), b.getY()));
        for (BlockPos gap : gaps) world.setBlockState(gap, VillageBiomes.foundationGroundAt(world, gap).getDefaultState());
        return true;
    }

    static boolean isFillableBaseGap(ServerWorld world, BlockPos base) {
        return fillableDepth(world, base).isPresent();
    }

    /**
     * Quantas camadas vazias há da cota da rua até o chão natural desta
     * coluna, se forem até {@link #MAX_FILL_DEPTH} e nenhuma tiver fluido.
     *
     * <p>Vazio é ar, planta e flor — {@code LotGround.isNothing}: aterrar
     * nunca cobre bloco de estrutura.
     */
    static OptionalInt fillableDepth(ServerWorld world, BlockPos base) {
        for (int layer = 0; layer < MAX_FILL_DEPTH; layer++) {
            BlockPos at = base.down(layer);
            if (!world.isInBuildLimit(at)) return OptionalInt.empty();
            BlockState state = world.getBlockState(at);
            if (!LotGround.isNothing(state) || !state.getFluidState().isEmpty()) return OptionalInt.empty();
            BlockPos below = at.down();
            if (!world.isInBuildLimit(below)) return OptionalInt.empty();
            if (LotGround.isLotGround(world, below)) return OptionalInt.of(layer + 1);
        }
        return OptionalInt.empty();
    }
}
