package com.villagecolony.fabric.integration;

import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.ConstructionProject;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/** Prepara depressões rasas de uma obra já alinhada à rua. */
public final class FoundationPreparation {
    private static final int MIN_SUPPORTED_PERCENT = 50;

    /**
     * Quantas camadas abaixo da base uma coluna pode ter e ainda ser aterrada.
     *
     * <p>Uma, a régua de 28-09. Foi a três por algumas horas em 2026-09-30 e
     * voltou por decisão do autor: a base fica na altura da rua ou um acima,
     * nunca abaixo (LotLevel).
     */
    static final int MAX_FILL_DEPTH = 1;

    private FoundationPreparation() {
    }

    /**
     * Aterra as lacunas da base e diz se a obra pode ser reservada.
     *
     * <p>A mesma régua de {@link #refusal}, com o efeito: só aterra quando a
     * base inteira se qualifica.
     */
    public static boolean prepareIfQualified(ServerWorld world, ConstructionProject project) {
        List<BlockPos> gaps = new ArrayList<>();

        if (scan(world, project, gaps).isPresent()) {
            return false;
        }

        // De baixo para cima: cada camada assenta sobre a anterior.
        gaps.sort((a, b) -> Integer.compare(a.getY(), b.getY()));
        for (BlockPos gap : gaps) world.setBlockState(gap, VillageBiomes.foundationGroundAt(world, gap).getDefaultState());
        return true;
    }

    /**
     * Por que a base desta obra não se qualifica, ou vazio se ela serve —
     * leitura sem efeito.
     *
     * <p>A régua é a do {@link LotLevel}, medida na cota da rua da planta:
     * pelo menos metade das colunas apoiada, e cada outra uma lacuna de até
     * {@link #MAX_FILL_DEPTH} camada, sem fluido. A recusa era muda e a obra
     * ficava "AVAILABLE with nobody" sem causa no log (playtest de
     * 2026-10-07).
     */
    public static Optional<String> refusal(ServerWorld world, ConstructionProject project) {
        return scan(world, project, new ArrayList<>());
    }

    private static Optional<String> scan(
            ServerWorld world, ConstructionProject project, List<BlockPos> gaps) {

        Blueprint blueprint = project.blueprint();
        if (!blueprint.hasStreetLayer()) return Optional.empty();
        int roadY = project.origin().y() + blueprint.streetLayer();
        int width = blueprint.size().x();
        int depth = blueprint.size().z();
        int supported = 0;
        for (int dx = 0; dx < width; dx++) for (int dz = 0; dz < depth; dz++) {
            BlockPos base = new BlockPos(project.origin().x() + dx, roadY, project.origin().z() + dz);
            if (!world.getBlockState(base).getCollisionShape(world, base).isEmpty()) {
                supported++;
                continue;
            }
            OptionalInt fill = fillableDepth(world, base);
            if (fill.isEmpty()) {
                return Optional.of("the base at y=" + roadY + " has a gap deeper than "
                        + MAX_FILL_DEPTH + " or with fluid at " + base.toShortString());
            }
            for (int layer = 0; layer < fill.getAsInt(); layer++) gaps.add(base.down(layer));
        }
        if (supported * 100 < width * depth * MIN_SUPPORTED_PERCENT) {
            return Optional.of("the base at y=" + roadY + " rests on " + supported + " of "
                    + width * depth + " columns, under " + MIN_SUPPORTED_PERCENT + "%");
        }
        return Optional.empty();
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
