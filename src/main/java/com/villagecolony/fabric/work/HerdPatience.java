package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.type.ServerMemory;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A paciência do pastor com o desgarrado — Regra 328. Na ida até o animal, ele desiste se fica {@link #PATIENCE}
 * tiques sem chegar mais perto; rumo ao curral, desiste também depois de {@link #ENTRIES} vezes em que o animal não
 * entrou. O animal largado fica {@link #SET_ASIDE} tiques fora da coleta, e ele passa ao próximo. Com o animal na
 * corda, a condução segue sem prazo (Regra 309).
 */
final class HerdPatience {

    static {
        ServerMemory.register(HerdPatience.class, HerdPatience::clearAll);
    }

    /** Trinta segundos sem chegar mais perto do animal. */
    static final long PATIENCE = 600;

    /** Quantas vezes o animal pode não entrar pelo portão. */
    static final int ENTRIES = 3;

    /** Cinco minutos fora da coleta. */
    static final long SET_ASIDE = 6_000;

    /** Quanto mais perto conta como avanço, em blocos. */
    private static final double STEP = 1.0;

    private static final class Progress {
        final UUID animal;
        double closest;
        long closerAt;
        int failedEntries;

        Progress(UUID animal, double closest, long closerAt) {
            this.animal = animal;
            this.closest = closest;
            this.closerAt = closerAt;
        }
    }

    private static final Map<UUID, Progress> PROGRESS = new HashMap<>();

    /** Animal → até quando fica fora da coleta. */
    private static final Map<UUID, Long> ASIDE = new HashMap<>();

    private HerdPatience() {
    }

    /**
     * A distância de agora até o animal, na ida.
     *
     * @return se ele desiste: {@link #PATIENCE} tiques sem chegar {@link #STEP} bloco mais perto
     */
    static boolean losesPatience(UUID shepherd, UUID animal, double distance, long now) {
        Progress progress = PROGRESS.get(shepherd);

        if (progress == null || !progress.animal.equals(animal)) {
            PROGRESS.put(shepherd, new Progress(animal, distance, now));
            return false;
        }

        if (distance <= progress.closest - STEP) {
            progress.closest = distance;
            progress.closerAt = now;
            return false;
        }

        return now - progress.closerAt > PATIENCE;
    }

    /**
     * O animal não entrou pelo portão desta vez.
     *
     * @return se ele desiste: {@link #ENTRIES} vezes nesta coleta
     */
    static boolean failedEntry(UUID shepherd, UUID animal, long now) {
        Progress progress = PROGRESS.computeIfAbsent(shepherd, ignored -> new Progress(animal, Double.MAX_VALUE, now));

        if (!progress.animal.equals(animal)) {
            progress = new Progress(animal, Double.MAX_VALUE, now);
            PROGRESS.put(shepherd, progress);
        }

        // Cada tentativa recomeça a ida, e com ela a paciência.
        progress.closest = Double.MAX_VALUE;
        progress.closerAt = now;

        return ++progress.failedEntries >= ENTRIES;
    }

    /** Ele largou este animal: fica fora da coleta por {@link #SET_ASIDE}. */
    static void setAside(ServerWorld world, UUID shepherd, UUID animal, String why) {
        PROGRESS.remove(shepherd);
        ASIDE.put(animal, world.getTime() + SET_ASIDE);
        VillageColonyMod.LOGGER.info("Shepherd {} set the {} aside for {} ticks — {}",
                shepherd.toString().substring(0, 8), animal.toString().substring(0, 8), SET_ASIDE, why);
    }

    /** A coleta acabou, entregue ou largada: a conta recomeça na próxima. */
    static void forget(UUID shepherd) {
        PROGRESS.remove(shepherd);
    }

    /** Se este animal ainda está fora da coleta. */
    static boolean isSetAside(ServerWorld world, AnimalEntity animal) {
        Long until = ASIDE.get(animal.getUuid());

        if (until == null) {
            return false;
        }

        if (world.getTime() >= until) {
            ASIDE.remove(animal.getUuid());
            return false;
        }

        return true;
    }

    static void clearAll() {
        PROGRESS.clear();
        ASIDE.clear();
    }
}
