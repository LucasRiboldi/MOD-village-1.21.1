package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.coordination.VillageExpansion;
import com.villagecolony.core.coordination.VillageExpansion.SideSurvey;
import com.villagecolony.core.coordination.VillageGrowth;
import com.villagecolony.core.type.Side;
import com.villagecolony.fabric.integration.BlockProtection;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A medida dos quatro lados quando a vila enche — Regra 56 C. O que decide o lado é o
 * chão: plano ganha, água e construção perdem, outra colônia veta. O núcleo da regra
 * está em {@link VillageExpansion}.
 */
public final class VillageExpansionSurvey {

    /** De quantos em quantos blocos se mede uma coluna da faixa. */
    static final int STEP = 2;

    private VillageExpansionSurvey() {
    }

    /** A regra de casa vem das plantas, que só o lado do jogo lê. */
    public static void register() {
        VillageExpansion.useHouseRule(HousePlans::isHouse);
    }

    /**
     * Uma varredura terminou sem lote; na {@link VillageExpansion#FULL_AFTER}ª seguida,
     * a vila anexa a faixa do melhor lado.
     *
     * @return o lado anexado agora
     */
    static Optional<Side> sweptWithoutLot(ServerWorld world, Colony colony) {
        Optional<VillageBounds> box = colony.bounds();

        if (box.isEmpty() || !VillageExpansion.sweptWithoutLot(colony.id())) {
            return Optional.empty();
        }

        List<SideSurvey> surveys = new ArrayList<>();

        for (Side side : Side.values()) {
            surveys.add(survey(world, colony, box.get(), side));
        }

        String measured = surveys.stream()
                .map(survey -> survey.side() + " " + survey.flat() + "/" + survey.columns() + " flat, "
                        + survey.water() + " water, " + survey.built() + " built"
                        + (survey.foreign() ? ", another colony" : ""))
                .collect(Collectors.joining("; "));
        Optional<SideSurvey> best = VillageExpansion.choose(surveys);

        if (best.isEmpty()) {
            VillageColonyMod.LOGGER.info("Colony {} is full and no side can be annexed — {}",
                    colony.id().toString().substring(0, 8), measured);

            return Optional.empty();
        }

        Side side = best.get().side();

        VillageColonyMod.reportGrowth(colony.id(), VillageGrowth.byStrip(
                VillageColonyMod.COLONIES, colony.id(), VillageExpansion.stripOf(box.get(), side)), "an annexed strip");
        VillageExpansion.annexed(colony.id(), side);
        VillageColonyMod.LOGGER.info("Colony {} is full — annexes {} blocks to the {}, the road grows that way — {}",
                colony.id().toString().substring(0, 8), VillageExpansion.STRIP, side, measured);

        return Optional.of(side);
    }

    /** Chão, água e construção na faixa além deste lado. */
    static SideSurvey survey(ServerWorld world, Colony colony, VillageBounds box, Side side) {
        VillageBounds strip = VillageExpansion.stripOf(box, side);
        boolean foreign = VillageColonyMod.COLONIES.all().stream()
                .filter(other -> !other.id().equals(colony.id()))
                .flatMap(other -> other.bounds().stream())
                .anyMatch(other -> other.touches(strip, 0));
        int columns = 0;
        int flat = 0;
        int water = 0;
        int built = 0;

        for (int x = strip.minX(); x <= strip.maxX(); x += STEP) {
            for (int z = strip.minZ(); z <= strip.maxZ(); z += STEP) {
                if (!world.isChunkLoaded(new BlockPos(x, 0, z))) {
                    continue;
                }

                int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
                BlockPos ground = new BlockPos(x, top - 1, z);

                columns++;

                if (!world.getFluidState(ground).isEmpty()) {
                    water++;
                    continue;
                }

                if (BlockProtection.isColonyBuilt(ground) || BlockProtection.isVillageOriginal(world, ground)) {
                    built++;
                }

                int east = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x + 1, z);
                int south = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z + 1);

                if (Math.abs(east - top) <= 1 && Math.abs(south - top) <= 1) {
                    flat++;
                }
            }
        }

        return new SideSurvey(side, columns, flat, water, built, foreign);
    }
}
