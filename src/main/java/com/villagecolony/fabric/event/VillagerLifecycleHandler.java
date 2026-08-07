package com.villagecolony.fabric.event;

import com.villagecolony.VillageColonyMod;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.VillagerEntity;

import java.util.UUID;

/**
 * Tira do registro o aldeão que deixou de existir.
 *
 * <p>Sem isto, uma colônia que perdesse o lenhador numa noite de zumbis
 * continuaria achando que tem um. A vaga nunca reabriria: a contagem de
 * {@code ProfessionAssigner} veria a função preenchida e o próximo
 * aldeão viraria fazendeiro numa colônia sem ninguém cortando madeira.
 * O baú do morto também ficaria reservado para sempre, e ninguém mais
 * poderia usá-lo. Ver Storage-System.md §"Falhas" e
 * Profession-System.md §"Morte de Trabalhadores".
 *
 * <p>Só o evento serve como prova de que o aldeão se foi. Ausência na
 * varredura não serve: um aldeão fora do raio, ou num chunk
 * descarregado, não está morto — apenas não foi visto. É por isso que
 * {@code WorkerService.remove} existia sem quem o chamasse.
 */
public final class VillagerLifecycleHandler {

    private VillagerLifecycleHandler() {
    }

    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register(VillagerLifecycleHandler::onDeath);
        ServerLivingEntityEvents.MOB_CONVERSION.register(VillagerLifecycleHandler::onConversion);
    }

    private static void onDeath(LivingEntity entity, net.minecraft.entity.damage.DamageSource cause) {
        if (entity instanceof VillagerEntity villager) {
            forget(villager.getUuid(), "died");
        }
    }

    /**
     * Zumbificação não passa por morte.
     *
     * <p>O aldeão mordido por um zumbi é convertido, não morto, então
     * {@code AFTER_DEATH} nunca dispara — e é justamente o caso mais
     * comum de perder um trabalhador em jogo.
     *
     * <p>Curar o zumbi devolve um aldeão com identidade nova. Ele será
     * registrado do zero pela varredura e receberá a função de que a
     * colônia mais precisar, que não é necessariamente a que ele tinha.
     * Preservar a antiga exigiria rastrear a conversão nos dois sentidos,
     * e o MVP não faz isso.
     */
    private static void onConversion(MobEntity previous, MobEntity converted, boolean keepEquipment) {
        if (previous instanceof VillagerEntity villager) {
            forget(villager.getUuid(), "was converted");
        }
    }

    /**
     * Esquece o trabalhador e libera o baú dele.
     *
     * <p>Os dois juntos: um baú reservado para quem não existe mais é
     * um baú perdido para a colônia.
     */
    private static void forget(UUID villagerId, String reason) {
        boolean wasWorker = VillageColonyMod.WORKERS.remove(villagerId);
        boolean hadStorage = VillageColonyMod.STORAGES.remove(villagerId);

        if (!wasWorker && !hadStorage) {
            return;
        }

        VillageColonyMod.LOGGER.info(
                "Worker {} {} — profession freed{}",
                villagerId,
                reason,
                hadStorage ? ", storage released" : "");
    }
}
