package com.villagecolony.fabric.integration;

import com.villagecolony.core.construction.model.BlueprintKind;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.ServerMemory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.tag.StructureTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.structure.pool.StructurePoolElement;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.ChunkPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * As roças que a vila gerada já tinha — Regra 52 (autor, 2026-10-08): cada
 * fazendeiro tem uma roça, e a roça da vila conta como da colônia.
 *
 * <p>Lidas das peças da estrutura de vila, não do chão: a peça diz que planta
 * é ({@code plains_small_farm_1}) e onde fica. Só chunks já carregados, para a
 * leitura nunca gerar terreno.
 */
public final class VillageFarms {

    static {
        ServerMemory.register(VillageFarms.class, VillageFarms::clearAll);
    }

    /** Uma roça da vila gerada: a planta e a caixa dela no mundo. */
    public record Farm(ResourceId plan, BlockBox box) {
    }

    /** As peças mudam só quando a vila cresce; reler a cada cinco minutos basta. */
    private static final long REREAD_TICKS = 6_000;

    private record Read(long at, List<Farm> farms) {
    }

    private static final Map<UUID, Read> READ = new HashMap<>();

    private VillageFarms() {
    }

    /** As roças da vila gerada que tocam esta área. */
    public static List<Farm> within(ServerWorld world, UUID colonyId, BlockBox area) {
        Read read = READ.get(colonyId);

        if (read == null || world.getTime() - read.at() >= REREAD_TICKS) {
            read = new Read(world.getTime(), scan(world, area));
            READ.put(colonyId, read);
        }

        return read.farms();
    }

    private static List<Farm> scan(ServerWorld world, BlockBox area) {
        var villages = world.getRegistryManager().get(RegistryKeys.STRUCTURE);
        Set<StructureStart> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        List<Farm> farms = new ArrayList<>();

        for (int chunkX = area.getMinX() >> 4; chunkX <= area.getMaxX() >> 4; chunkX++) {
            for (int chunkZ = area.getMinZ() >> 4; chunkZ <= area.getMaxZ() >> 4; chunkZ++) {
                if (!world.isChunkLoaded(chunkX, chunkZ)) {
                    continue;
                }

                List<StructureStart> starts = world.getStructureAccessor().getStructureStarts(
                        new ChunkPos(chunkX, chunkZ),
                        structure -> villages.getEntry(structure).isIn(StructureTags.VILLAGE));

                for (StructureStart start : starts) {
                    if (!seen.add(start) || !start.hasChildren()) {
                        continue;
                    }

                    for (StructurePiece piece : start.getChildren()) {
                        planOf(world, piece)
                                .filter(BlueprintKind::isFarm)
                                .filter(plan -> piece.getBoundingBox().intersects(area))
                                .ifPresent(plan -> farms.add(new Farm(plan, piece.getBoundingBox())));
                    }
                }
            }
        }

        return List.copyOf(farms);
    }

    /**
     * A planta de uma peça de vila — o {@code location} do elemento, lido pelo
     * codec do próprio jogo, porque o campo é protegido.
     */
    public static Optional<ResourceId> planOf(ServerWorld world, StructurePiece piece) {
        if (!(piece instanceof PoolStructurePiece pool)) {
            return Optional.empty();
        }

        return StructurePoolElement.CODEC
                .encodeStart(RegistryOps.of(NbtOps.INSTANCE, world.getRegistryManager()), pool.getPoolElement())
                .result()
                .filter(NbtCompound.class::isInstance)
                .map(NbtCompound.class::cast)
                .map(nbt -> nbt.getString("location"))
                .filter(location -> location.contains(":"))
                .map(ResourceId::parse);
    }

    /** Esquece as leituras de uma colônia, ou de todas. */
    public static void forget(UUID colonyId) {
        READ.remove(colonyId);
    }

    public static void clearAll() {
        READ.clear();
    }
}
