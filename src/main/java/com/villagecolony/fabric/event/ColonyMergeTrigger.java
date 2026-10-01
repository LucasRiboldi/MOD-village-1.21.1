package com.villagecolony.fabric.event;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.coordination.ColonyIdentity;
import com.villagecolony.core.coordination.ColonyMerge;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.StructureTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Quando duas colônias viram uma — ADR-007, implementada em 2026-09-30.
 *
 * <p>O playtest de 30-09 teve três colônias ({@code 3c358029},
 * {@code e79a3177}, {@code 2fffd4b9}) na mesma vila gerada, com centros a 20–70
 * blocos: disputavam lote e rua, e cada uma via a rua da outra como chão
 * proibido. O autor decidiu: colônias que se tocam ou ocupam o mesmo espaço
 * viram a mesma colônia.
 *
 * <p>Três perguntas, e basta uma dizer sim:
 * <ul>
 *   <li>uma construção de uma encosta numa construção da outra — o gatilho
 *       original da ADR-007 §4;</li>
 *   <li>os centros estão a até {@code OVERLAP_DISTANCE} — a sobreposição que
 *       a detecção só avisava;</li>
 *   <li>os chunks dos dois centros pertencem à mesma vila gerada pelo jogo;</li>
 *   <li>uma construção de uma fica a poucos blocos de uma da outra —
 *       {@link ColonyIdentity#NEIGHBOUR_GAP}, pedido do autor em 30-09;</li>
 *   <li>as caixas das duas vilas se tocam, com 16 blocos de folga — ADR-003
 *       Emenda 6.</li>
 * </ul>
 *
 * <p>O que muda de dono é regra do Core — {@link ColonyMerge}.
 */
final class ColonyMergeTrigger {

    private ColonyMergeTrigger() {
    }

    /**
     * Funde todo par que se toque ou ocupe o mesmo espaço, uma fusão por vez
     * até não sobrar par.
     *
     * @return quantas fusões houve
     */
    static int mergeTouchingColonies(ServerWorld world) {
        int merged = 0;

        for (Optional<ColonyMerge.Result> result = mergeOnePair(world);
                result.isPresent();
                result = mergeOnePair(world)) {
            ColonyMerge.Result done = result.get();

            VillageColonyMod.LOGGER.info(
                    "Colony {} absorbed colony {} — they touched or shared the same village;"
                            + " {} workers, {} tasks, {} builds and {} buildings moved over,"
                            + " mine {}",
                    done.survivor(), done.absorbed(), done.workers(), done.tasks(),
                    done.projects(), done.buildings(),
                    done.mineInherited() ? "inherited" : "kept as it was");

            merged++;
        }

        return merged;
    }

    private static Optional<ColonyMerge.Result> mergeOnePair(ServerWorld world) {
        List<Colony> colonies = new ArrayList<>(VillageColonyMod.COLONIES.all());

        for (int i = 0; i < colonies.size(); i++) {
            for (int j = i + 1; j < colonies.size(); j++) {
                Colony a = colonies.get(i);
                Colony b = colonies.get(j);

                if (shouldMerge(world, a, b)) {
                    return Optional.of(ColonyMerge.merge(
                            a, b,
                            VillageColonyMod.COLONIES,
                            VillageColonyMod.WORKERS,
                            VillageColonyMod.TASKS,
                            VillageColonyMod.CONSTRUCTIONS,
                            VillageColonyMod.BUILDINGS,
                            VillageColonyMod.MINES));
                }
            }
        }

        return Optional.empty();
    }

    static boolean shouldMerge(ServerWorld world, Colony a, Colony b) {
        return VillageColonyMod.COLONIES.overlapping(a).contains(b)
                || buildingsTouch(a.id(), b.id())
                || ColonyIdentity.boundsTouch(a, b)
                || ColonyIdentity.buildingsNear(a.id(), b.id(), VillageColonyMod.BUILDINGS)
                || shareAGeneratedVillage(world, a, b);
    }

    /** Uma construção de {@code a} encosta numa de {@code b} — ADR-007 §4. */
    static boolean buildingsTouch(UUID a, UUID b) {
        for (Building building : VillageColonyMod.BUILDINGS.ofColony(a)) {
            for (Building neighbour : VillageColonyMod.BUILDINGS.foreignNeighboursOf(building)) {
                if (neighbour.colonyId().equals(b)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Os chunks dos dois centros pertencem à mesma vila gerada.
     *
     * <p>Chunk descarregado não responde: pedir por ele carregaria o chunk
     * dentro do tique.
     */
    static boolean shareAGeneratedVillage(ServerWorld world, Colony a, Colony b) {
        Set<ChunkPos> villagesOfA = villagesAt(world, a);

        if (villagesOfA.isEmpty()) {
            return false;
        }

        for (ChunkPos village : villagesAt(world, b)) {
            if (villagesOfA.contains(village)) {
                return true;
            }
        }

        return false;
    }

    /** As vilas geradas cujo espaço alcança o chunk do centro, pela origem de cada uma. */
    private static Set<ChunkPos> villagesAt(ServerWorld world, Colony colony) {
        BlockPos center = MinecraftTypeAdapter.toBlockPos(colony.center());
        Set<ChunkPos> found = new HashSet<>();

        if (!world.isChunkLoaded(center)) {
            return found;
        }

        var registry = world.getRegistryManager().get(RegistryKeys.STRUCTURE);

        for (StructureStart start : world.getStructureAccessor().getStructureStarts(
                new ChunkPos(center),
                structure -> registry.getEntry(structure).isIn(StructureTags.VILLAGE))) {
            if (start.hasChildren()) {
                found.add(start.getPos());
            }
        }

        return found;
    }
}
