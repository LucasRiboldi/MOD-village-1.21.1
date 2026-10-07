package com.villagecolony.fabric.event;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.ColonyState;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.fabric.integration.ChestSpawner;
import com.villagecolony.fabric.integration.ChestMarker;
import com.villagecolony.fabric.work.BuilderWork;
import com.villagecolony.fabric.work.LumberjackWork;
import com.villagecolony.fabric.work.MinerWork;
import com.villagecolony.fabric.work.FarmerWork;
import com.villagecolony.fabric.work.ShepherdWork;
import com.villagecolony.fabric.work.SmelterWork;
import com.villagecolony.fabric.work.SurfaceGatheringWork;
import com.villagecolony.fabric.work.CraftingWork;
import com.villagecolony.fabric.work.WorkerHousingNeeds;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.VillagerEntity;

import java.util.List;
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
            unmarkChestOf(villager);

            forget(villager.getUuid(), "died");
        }
    }

    /**
     * Tira a marca do baú antes de esquecer o trabalhador.
     *
     * <p>Nesta ordem porque a marca precisa da posição do baú, e
     * {@link #forget} apaga o registro que a guarda.
     *
     * <p>O baú e o que está dentro dele ficam: quem morreu era o dono, e
     * não o conteúdo. O que sai é a promessa de que aquele baú tem dono.
     */
    private static void unmarkChestOf(VillagerEntity villager) {
        if (!(villager.getWorld() instanceof ServerWorld world)) {
            return;
        }

        VillageColonyMod.STORAGES.of(villager.getUuid())
                .ifPresent(storage -> ChestMarker.unmark(world, storage.chestPosition()));
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
            unmarkChestOf(villager);

            forget(villager.getUuid(), "was converted");
        }
    }

    /** Na passagem para abandonada, solta os trabalhadores — ADR-036 item 10. */
    static void dismissIfAbandoned(Colony colony, ColonyState was, ColonyState now) {
        if (now == ColonyState.ABANDONED && was != ColonyState.ABANDONED) {
            VillageColonyMod.LOGGER.info(
                    "Colony {} stops working — {} workers dismissed; chests and buildings stay",
                    colony.id(), dismissColony(colony.id()));
        }
    }

    /**
     * Solta todos os trabalhadores de uma colônia abandonada — ADR-036 item
     * 10: as profissões se desativam e os aldeões voltam a ser comuns; os
     * baús e as construções ficam como estão.
     *
     * @return quantos foram soltos
     */
    static int dismissColony(UUID colonyId) {
        List<UUID> workers = VillageColonyMod.WORKERS.ofColony(colonyId).stream()
                .map(Worker::villagerId)
                .toList();

        workers.forEach(villagerId -> forget(villagerId, "left the abandoned colony " + colonyId));

        return workers.size();
    }

    /**
     * Esquece o trabalhador e libera o baú dele.
     *
     * <p>Os dois juntos: um baú reservado para quem não existe mais é
     * um baú perdido para a colônia.
     */
    private static void forget(UUID villagerId, String reason) {
        VillageColonyMod.WORKERS.find(villagerId).ifPresent(worker ->
                WorkerHousingNeeds.resolve(worker.colonyId(), villagerId));
        ChestSpawner.forget(villagerId);

        // As tarefas primeiro: soltá-las depois de esquecer o
        // trabalhador daria no mesmo hoje, mas deixaria a ordem
        // dependendo de o registro de tarefas não consultar o de
        // trabalhadores — e ele pode vir a consultar.
        int releasedTasks = VillageColonyMod.TASKS.releaseAllOf(villagerId);

        // O destino também: um aldeão zumbificado e depois curado volta
        // com identidade nova, mas o UUID antigo ficaria no mapa para
        // sempre se ninguém o tirasse.
        WorkTargets.clear(villagerId);

        // E a árvore que ele estava quebrando: o plano guarda posições de
        // uma colheita em curso, e sem isto ele ficaria no registro
        // esperando por um aldeão que não volta.
        //
        // São os seis, e o construtor faltava aqui até 2026-08-25 — a
        // lista de dispensa em VillageDetectionHandler o tinha, esta não.
        // O trabalho do construtor morto ficava no registro porque quem
        // o percorre não distingue "morreu" de "está fora de chunk
        // carregado", e explodia quando a obra fechasse.
        MinerWork.forget(villagerId);
        SmelterWork.forget(villagerId);
        SurfaceGatheringWork.forget(villagerId);
        ShepherdWork.forget(villagerId);
        FarmerWork.forget(villagerId);
        LumberjackWork.forget(villagerId);
        CraftingWork.forget(villagerId);
        BuilderWork.forget(villagerId);

        boolean wasWorker = VillageColonyMod.WORKERS.remove(villagerId);
        boolean hadStorage = VillageColonyMod.STORAGES.remove(villagerId);

        if (!wasWorker && !hadStorage && releasedTasks == 0) {
            return;
        }

        VillageColonyMod.LOGGER.info(
                "Worker {} {} — profession freed{}{}",
                villagerId,
                reason,
                hadStorage ? ", storage released" : "",
                releasedTasks > 0 ? ", " + releasedTasks + " tasks requeued" : "");
    }
}
