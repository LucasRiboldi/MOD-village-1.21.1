package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.LeashKnotEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.List;

/** Os animais que a coleta do pastor pode levar — Regras 255, 283, 299, 327 e 328. */
final class HerdCandidates {

    private HerdCandidates() {
    }

    /** A área da coleta: a caixa da vila, ou 48 blocos em volta do centro na vila sem caixa (Regra 283). */
    static Box areaOf(Colony colony) {
        BlockPos center = MinecraftTypeAdapter.toBlockPos(colony.center());

        return colony.bounds()
                .map(box -> new Box(box.minX(), box.minY() - 8, box.minZ(), box.maxX() + 1, box.maxY() + 8, box.maxZ() + 1))
                .orElseGet(() -> new Box(center).expand(48));
    }

    /** Os animais que a coleta pode levar, o amarrado primeiro e então o mais perto (Regra 255). */
    static List<AnimalEntity> of(ServerWorld world, Colony colony, VillagerEntity shepherd) {
        Box area = areaOf(colony);
        List<AnimalEntity> candidates = new java.util.ArrayList<>();

        for (EntityType<? extends AnimalEntity> species : ShepherdFlock.LIVESTOCK) {
            candidates.addAll(world.getEntitiesByType(species, area, candidate -> candidate.isAlive()
                    && candidate.canBeLeashed()
                    && (!candidate.isLeashed() || candidate.getLeashHolder() instanceof LeashKnotEntity)
                    && !ShepherdFlock.isPenned(world, candidate)
                    && !isInAnyPen(world, colony, candidate)
                    && !sheltersAnimal(world, candidate.getBlockPos())
                    && !HerdPatience.isSetAside(world, candidate)));
        }

        candidates.sort(java.util.Comparator
                .comparing((AnimalEntity animal) -> !(animal.getLeashHolder() instanceof LeashKnotEntity))
                .thenComparingDouble(animal -> animal.squaredDistanceTo(shepherd)));

        return candidates;
    }

    /** A porta de uma casa coberta delimita o recinto, aberta ou fechada. Animais não operam portas. */
    static boolean sheltersAnimal(ServerWorld world, BlockPos feet) {
        if (world.isSkyVisible(feet)) {
            return false;
        }

        var room = FencedIn.check(world, feet, FencedIn.RADIUS, true, true);
        return room.enclosed() && room.reach().stream().anyMatch(at -> {
            for (var way : net.minecraft.util.math.Direction.Type.HORIZONTAL) {
                if (world.getBlockState(at.offset(way)).isIn(net.minecraft.registry.tag.BlockTags.DOORS)) {
                    return true;
                }
            }
            return false;
        });
    }

    /** Se o animal está no chão de dentro de algum curral conhecido da vila: guardado, com a porteira como estiver (Regra 327). */
    static boolean isInAnyPen(ServerWorld world, Colony colony, AnimalEntity animal) {
        return ShepherdPens.pensOf(world, colony).stream().anyMatch(pen -> pen.reach().contains(animal.getBlockPos()));
    }
}
