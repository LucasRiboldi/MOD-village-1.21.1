package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.worker.model.ProfessionLock;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * O aviso de troca de ofício — Regra 383 (autor 26-11, K3 🅲 do jogo da 0.4.148: "eu vi o personagem zelador deixar de ser
 * o zelador e sumir a placa de identificação, preocupantíssimo"). A cada trinta segundos, quem tinha ofício e agora tem
 * outro, nenhum ou sumiu do registro vivo sai numa linha {@code VC_PROFESSION_CHANGE}, para achar a causa no ato.
 */
public final class ProfessionWatch {

    /** Uma leitura a cada trinta segundos. */
    static final int EVERY = 600;

    /** Colônia → aldeão → o ofício da última leitura. */
    private static final Map<UUID, Map<UUID, ProfessionType>> LAST = new HashMap<>();

    static {
        ServerMemory.register(ProfessionWatch.class, LAST::clear);
    }

    private ProfessionWatch() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ServerWorld world = server.getOverworld();

            if (world.getTime() % EVERY != EVERY / 6) {
                return;
            }

            for (Colony colony : List.copyOf(VillageColonyMod.COLONIES.all())) {
                read(world, colony);
            }
        });
    }

    static void read(ServerWorld world, Colony colony) {
        Map<UUID, ProfessionType> now = new HashMap<>();

        for (Worker worker : VillageColonyMod.WORKERS.ofColony(colony.id())) {
            worker.profession().ifPresent(trade -> now.put(worker.villagerId(), trade));
        }

        Map<UUID, ProfessionType> before = LAST.put(colony.id(), now);

        if (before == null) {
            return;
        }

        ProfessionLock.changes(before, now).forEach((villager, trade) -> {
            Entity entity = world.getEntity(villager);

            // Quem morreu sai do registro: não é troca de ofício.
            if (trade.isEmpty() && (entity == null || !entity.isAlive())) {
                return;
            }

            VillageColonyMod.LOGGER.warn("VC_PROFESSION_CHANGE version=1 colony={} worker={} from={} to={} rule=383",
                    colony.id().toString().substring(0, 8), villager.toString().substring(0, 8), before.get(villager),
                    trade.map(Enum::name).orElse(VillageColonyMod.WORKERS.find(villager).isPresent() ? "NONE" : "GONE"));
        });
    }
}
