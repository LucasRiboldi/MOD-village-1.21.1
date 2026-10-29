package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.poi.PointOfInterestTypes;

import java.util.List;

/**
 * As camas da vila são as da caixa dela — Regra 145 (autor 2026-10-28). A sonda de 64 blocos via só parte
 * da vila grande, e o número oscilava (15 → 7 → 18 → 10 na taiga simulada). Agora, a cada
 * {@link #EVERY} tiques, contam-se as camas que o jogo registrou ({@code minecraft:home}) dentro da caixa, nos
 * chunks carregados, na faixa de altura das camas da vila ({@link VillageBounds#holdsBedAt}).
 *
 * <p>Chunk descarregado não é lido (nunca forçar carregamento): a contagem só é aceita com todos os chunks da
 * caixa carregados — senão a vila de antes fica, em vez de uma contagem cortada.
 */
public final class BoxBeds {

    /** De quanto em quanto tempo a contagem é refeita: meio minuto. */
    public static final int EVERY = 600;

    private BoxBeds() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ServerWorld world = server.getOverworld();

            if (world.getTime() % EVERY != 150) {
                return;
            }

            for (Colony colony : List.copyOf(VillageColonyMod.COLONIES.all())) {
                colony.bounds().ifPresent(box -> count(world, box).ifPresent(beds -> {
                    if (beds != colony.observedBeds()) {
                        com.google.gson.JsonObject changed = new com.google.gson.JsonObject();
                        changed.addProperty("before", colony.observedBeds());
                        changed.addProperty("now", beds);
                        VillageDiary.event(colony.id(), "beds", changed);
                    }

                    colony.countBeds(beds);
                }));
            }
        });
    }

    /** As camas dentro da caixa; vazio se algum chunk dela não está carregado. */
    public static java.util.OptionalInt count(ServerWorld world, VillageBounds box) {
        PointOfInterestStorage pois = world.getPointOfInterestStorage();
        int beds = 0;

        for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
            for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
                if (!world.isChunkLoaded(cx, cz)) {
                    return java.util.OptionalInt.empty();
                }

                beds += (int) pois.getInChunk(type -> type.matchesKey(PointOfInterestTypes.HOME), new ChunkPos(cx, cz),
                                PointOfInterestStorage.OccupationStatus.ANY)
                        .filter(poi -> box.containsColumn(poi.getPos().getX(), poi.getPos().getZ())
                                && box.holdsBedAt(poi.getPos().getY()))
                        .count();
            }
        }

        return java.util.OptionalInt.of(beds);
    }
}
