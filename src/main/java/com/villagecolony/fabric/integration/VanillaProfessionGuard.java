package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.worker.model.Worker;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.village.VillagerData;
import net.minecraft.village.VillagerProfession;

import java.util.Optional;

/**
 * Aldeão com profissão do mod não tem profissão do Vanilla — decisão do
 * autor, 2026-09-30 (ADR-029).
 *
 * <p>Os dois sistemas corriam em paralelo: o lenhador da colônia podia ser
 * ferreiro do jogo ao mesmo tempo, andar até a bigorna no expediente e
 * segurar a estação de trabalho de outro aldeão. Agora o ofício é um só.
 *
 * <p>Duas portas, e é preciso as duas:
 *
 * <ul>
 *   <li>{@link #filter}, chamado pelo mixin em
 *       {@code VillagerEntity.setVillagerData}: quando o Vanilla tenta dar
 *       ofício a um trabalhador da colônia, o dado sai sem ofício;</li>
 *   <li>{@link #strip}, chamado pela varredura da colônia: o aldeão que já
 *       tinha ofício do jogo antes de ser contratado perde o ofício e solta
 *       a estação de trabalho.</li>
 * </ul>
 *
 * <p>Nitwit fica como está: o jogo nunca lhe dá emprego, e a colônia também
 * não ({@code VillagerScanner.canWork}).
 */
public final class VanillaProfessionGuard {

    private VanillaProfessionGuard() {
    }

    /** O dado que o aldeão recebe: sem ofício Vanilla, se ele é da colônia. */
    public static VillagerData filter(VillagerEntity villager, VillagerData data) {
        if (villager.getWorld().isClient() || !blocks(villager, data.getProfession())) {
            return data;
        }

        return data.withProfession(VillagerProfession.NONE);
    }

    /**
     * Tira o ofício Vanilla e a estação de quem tem ofício da colônia.
     *
     * @return se o aldeão perdeu alguma coisa agora
     */
    public static boolean strip(VillagerEntity villager) {
        if (!hasColonyProfession(villager)) {
            return false;
        }

        boolean changed = false;
        VillagerProfession vanilla = villager.getVillagerData().getProfession();

        if (vanilla != VillagerProfession.NONE && vanilla != VillagerProfession.NITWIT) {
            villager.setVillagerData(
                    villager.getVillagerData().withProfession(VillagerProfession.NONE));
            changed = true;

            VillageColonyMod.LOGGER.info(
                    "Villager {} leaves the vanilla trade {} — it works for the colony",
                    villager.getUuid(), vanilla);
        }

        if (villager.getBrain().hasMemoryModule(MemoryModuleType.JOB_SITE)
                && villager.getBrain().getOptionalRegisteredMemory(MemoryModuleType.JOB_SITE)
                        .isPresent()) {
            villager.releaseTicketFor(MemoryModuleType.JOB_SITE);
            villager.getBrain().forget(MemoryModuleType.JOB_SITE);
            changed = true;
        }

        if (villager.getBrain().hasMemoryModule(MemoryModuleType.POTENTIAL_JOB_SITE)
                && villager.getBrain()
                        .getOptionalRegisteredMemory(MemoryModuleType.POTENTIAL_JOB_SITE)
                        .isPresent()) {
            villager.releaseTicketFor(MemoryModuleType.POTENTIAL_JOB_SITE);
            villager.getBrain().forget(MemoryModuleType.POTENTIAL_JOB_SITE);
            changed = true;
        }

        return changed;
    }

    private static boolean blocks(VillagerEntity villager, VillagerProfession profession) {
        return profession != VillagerProfession.NONE
                && profession != VillagerProfession.NITWIT
                && hasColonyProfession(villager);
    }

    private static boolean hasColonyProfession(VillagerEntity villager) {
        Optional<Worker> worker = VillageColonyMod.WORKERS.find(villager.getUuid());

        return worker.flatMap(Worker::profession).isPresent();
    }
}
