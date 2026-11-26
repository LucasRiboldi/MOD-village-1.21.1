package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A coleira da cama — Regra 372 (autor 26-11, notas do jogo da 0.4.145, J1 🅱️: "fundidor completamente perdido, muito
 * longe do centro da vila, inaceitável"; "nenhuma outra profissão pode se afastar mais de XX blocos ... para voltar
 * para sua cama a noite e não ficar travado em terrenos desconhecidos", com o raio contado da cama). Fora o mineiro, o
 * lenhador e os que trabalham pela vila toda (o Zelador, o pastor e o construtor, emenda de 26-11, K1 🅰️), ninguém busca
 * alvo a mais de {@value #RANGE} blocos da própria cama, e quem fica longe dela larga a tarefa e anda de volta. Ninguém
 * é teletransportado ({@code VC_LEASH}).
 */
public final class HomeLeash {

    /** O raio, em blocos na horizontal, a partir da cama. */
    static final int RANGE = 48;

    /** Quantas passadas o teste roda: a caminhada de volta é pedida em cada uma. */
    static final int TRIES = 3;

    /** Uma passada a cada dez segundos. */
    static final int EVERY = 200;

    private static final float SPEED = 0.6F;

    private HomeLeash() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ServerWorld world = server.getOverworld();
            long now = world.getTime();

            if (now % EVERY != EVERY / 4) {
                return;
            }

            for (Colony colony : List.copyOf(VillageColonyMod.COLONIES.all())) {
                if (colony.isActive() && colony.isAttended(now)) {
                    pass(world, colony);
                }
            }
        });
    }

    /** Fora da coleira: o mineiro e o lenhador, que trabalham longe, e o Zelador, o pastor e o construtor (K1 🅰️). */
    static boolean isFree(Optional<ProfessionType> profession) {
        return profession.filter(trade -> trade == ProfessionType.MINER || trade == ProfessionType.LUMBERJACK
                || trade == ProfessionType.GROUNDSKEEPER || trade == ProfessionType.SHEPHERD
                || trade == ProfessionType.BUILDER).isPresent();
    }

    /** Se o alvo fica a até {@value #RANGE} blocos da cama deste aldeão; sem cama, ou mineiro e lenhador, sempre. */
    public static boolean within(ServerWorld world, UUID villagerId, BlockPos target) {
        Optional<Worker> worker = VillageColonyMod.WORKERS.find(villagerId);

        if (worker.isEmpty() || isFree(worker.get().profession())
                || !(world.getEntity(villagerId) instanceof VillagerEntity villager)) {
            return true;
        }

        return home(world, villager).map(bed -> squared(bed, target) <= (long) RANGE * RANGE).orElse(true);
    }

    /** Uma passada nesta colônia: quem está longe da cama volta. Diz quantos foram mandados ou levados de volta. */
    static int pass(ServerWorld world, Colony colony) {
        int recalled = 0;

        for (Worker worker : VillageColonyMod.WORKERS.ofColony(colony.id())) {
            if (isFree(worker.profession()) || !(world.getEntity(worker.villagerId()) instanceof VillagerEntity villager)) {
                continue;
            }

            Optional<BlockPos> bed = home(world, villager);

            if (bed.isEmpty() || squared(bed.get(), villager.getBlockPos()) <= (long) RANGE * RANGE) {
                continue;
            }

            recalled += bringBack(villager, bed.get());
        }

        return recalled;
    }

    private static int bringBack(VillagerEntity villager, BlockPos bed) {
        long distance = squared(bed, villager.getBlockPos());

        List.copyOf(VillageColonyMod.TASKS.assignedTo(villager.getUuid()))
                .forEach(task -> task.release("more than " + RANGE + " blocks from its bed (Regra 372)"));
        villager.getBrain().remember(MemoryModuleType.WALK_TARGET, new WalkTarget(bed, SPEED, 1));
        VillageColonyMod.LOGGER.info("VC_LEASH worker={} walks back to its bed at {}, {} blocks away rule=372",
                villager.getUuid().toString().substring(0, 8), bed.toShortString(), (int) Math.sqrt((double) distance));
        return 1;
    }

    private static Optional<BlockPos> home(ServerWorld world, VillagerEntity villager) {
        return villager.getBrain().getOptionalRegisteredMemory(MemoryModuleType.HOME)
                .filter(home -> home.dimension().equals(world.getRegistryKey()))
                .map(GlobalPos::pos);
    }

    private static long squared(BlockPos a, BlockPos b) {
        long dx = (long) a.getX() - b.getX();
        long dz = (long) a.getZ() - b.getZ();

        return dx * dx + dz * dz;
    }
}
