package com.villagecolony.fabric.brain;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.worker.model.Worker;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Se a tarefa da colônia continua no {@code Brain} de cada trabalhador — AUD-12.
 *
 * <p>Outro mod que troque a IA do aldeão tira o {@link GoToWorkTargetTask} sem
 * erro nenhum: o aldeão continua registrado, recebe alvo e nunca anda até ele. A
 * tarefa marca a hora a cada tique em que o {@code Brain} a consulta; o ciclo da
 * colônia avisa, uma vez por aldeão, quem passou {@link #SILENCE} tiques sem
 * marcar num chunk que simula entidades e com a IA ligada.
 */
public final class BrainHeartbeat {

    static {
        ServerMemory.register(BrainHeartbeat.class, BrainHeartbeat::clearAll);
    }

    /** Quantos tiques sem consulta antes do aviso: 10 s. */
    static final int SILENCE = 200;

    /** O último tique em que o {@code Brain} de cada trabalhador consultou a tarefa. */
    private static final Map<UUID, Long> LAST = new HashMap<>();

    /** Quem já foi avisado: o aviso sai uma vez por aldeão. */
    private static final Set<UUID> WARNED = new HashSet<>();

    private BrainHeartbeat() {
    }

    /** Chamada pela tarefa da colônia a cada consulta do {@code Brain}. */
    static void beat(ServerWorld world, VillagerEntity villager) {
        if (VillageColonyMod.WORKERS.find(villager.getUuid()).isPresent()) {
            LAST.put(villager.getUuid(), world.getTime());
        }
    }

    /** Avisa os trabalhadores desta colônia cujo {@code Brain} não consulta mais a tarefa. */
    public static void inspect(ServerWorld world, Colony colony) {
        long now = world.getTime();

        LAST.keySet().removeIf(id -> VillageColonyMod.WORKERS.find(id).isEmpty());
        WARNED.removeIf(id -> VillageColonyMod.WORKERS.find(id).isEmpty());

        for (Worker worker : VillageColonyMod.WORKERS.ofColony(colony.id())) {
            if (!(world.getEntity(worker.villagerId()) instanceof VillagerEntity villager)
                    || villager.isAiDisabled()
                    || !world.shouldTickEntity(villager.getBlockPos())) {
                continue;
            }

            // A primeira vista conta como consulta: quem acabou de chegar tem o prazo inteiro.
            long last = LAST.computeIfAbsent(villager.getUuid(), id -> now);

            if (now - last >= SILENCE && WARNED.add(villager.getUuid())) {
                VillageColonyMod.LOGGER.warn(
                        "Villager {} works for colony {} but its brain has not run the colony task in {} ticks"
                                + " — another mod may have replaced the villager AI, and it will not walk to colony work",
                        villager.getUuid().toString().substring(0, 8),
                        colony.id().toString().substring(0, 8),
                        now - last);
            }
        }
    }

    /** Se este aldeão já foi avisado. Para a bateria. */
    public static boolean warned(UUID villagerId) {
        return WARNED.contains(villagerId);
    }

    static void clearAll() {
        LAST.clear();
        WARNED.clear();
    }
}
