package com.villagecolony.fabric.world;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.StructureTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;

import java.util.Optional;

/**
 * O que o mundo diz da vila sem o mod perguntar ao resto do mod — ADR-003
 * Emenda 6, 2026-09-30.
 *
 * <p>Pacote à parte e de propósito: as quatro partes da camada Fabric
 * ({@code adapter}, {@code integration}, {@code event}, {@code work}) estão
 * em ciclo congelado no ArchUnit, e qualquer chamada nova entre elas reprova.
 * Esta classe só depende do jogo e do Core, e por isso todas podem usá-la.
 * Recebe coordenada em inteiro, sem passar pelo adaptador.
 */
public final class GeneratedVillages {

    private GeneratedVillages() {
    }

    /**
     * A caixa da vila gerada que alcança esta coluna, se houver e o chunk
     * estiver carregado — pedir um chunk descarregado o carregaria no tique.
     */
    public static Optional<VillageBounds> boundsAt(ServerWorld world, int x, int z) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;

        if (!world.isChunkLoaded(chunkX, chunkZ)) {
            return Optional.empty();
        }

        var registry = world.getRegistryManager().get(RegistryKeys.STRUCTURE);
        VillageBounds found = null;

        for (StructureStart start : world.getStructureAccessor().getStructureStarts(
                new ChunkPos(chunkX, chunkZ),
                structure -> registry.getEntry(structure).isIn(StructureTags.VILLAGE))) {
            if (!start.hasChildren()) {
                continue;
            }

            // As peças, e não a caixa da estrutura — 2026-10-03, autor: "o
            // tamanho original do Vanilla". A caixa da estrutura soma 12 blocos
            // de folga em cada lado (a vila do autor: 166 × 140 contra 142 ×
            // 116 das peças).
            for (var piece : start.getChildren()) {
                BlockBox box = piece.getBoundingBox();
                VillageBounds bounds = new VillageBounds(
                        box.getMinX(), box.getMinY(), box.getMinZ(),
                        box.getMaxX(), box.getMaxY(), box.getMaxZ());

                found = found == null ? bounds : found.union(bounds);
            }
        }

        return Optional.ofNullable(found);
    }

    /**
     * A janela de altura das camas em volta desta coluna: a da vila medida que
     * a contém (com a folga de identidade), ou a da descoberta.
     *
     * @return {mínimo, máximo}
     */
    public static int[] bedWindowAround(ServerWorld world, int x, int z) {
        VillageBounds known = null;

        for (Colony colony : VillageColonyMod.COLONIES.all()) {
            Optional<VillageBounds> bounds = colony.bounds();

            if (bounds.isPresent()
                    && bounds.get().containsColumn(x, z, VillageBounds.IDENTITY_MARGIN)) {
                known = known == null ? bounds.get() : known.union(bounds.get());
            }
        }

        return known != null
                ? new int[] {known.bedFloor(), known.bedCeiling()}
                : bedWindowAt(world, x, z);
    }

    /**
     * A janela de altura em que uma cama pode ser de vila, quando ainda não se
     * sabe de que vila — a descoberta.
     *
     * <p>Com vila gerada no lugar, é a faixa dela; sem, vai de 16 abaixo a
     * {@link VillageBounds#BEDS_ABOVE} acima da superfície da coluna. Quem
     * está numa caverna ou numa Trial Chamber continua procurando pela
     * superfície acima dele, e não em volta de si.
     *
     * @return {mínimo, máximo}
     */
    public static int[] bedWindowAt(ServerWorld world, int x, int z) {
        Optional<VillageBounds> generated = boundsAt(world, x, z);

        if (generated.isPresent()) {
            return new int[] {generated.get().bedFloor(), generated.get().bedCeiling()};
        }

        int surface = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);

        return new int[] {surface - 16, surface + VillageBounds.BEDS_ABOVE};
    }
}
