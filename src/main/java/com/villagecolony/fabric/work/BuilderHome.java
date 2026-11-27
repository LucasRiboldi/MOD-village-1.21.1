package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.VillageFoundation;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.BedPart;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.poi.PointOfInterest;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.poi.PointOfInterestTypes;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * O construtor mora perto da obra — Regra 376 (autor 26-11, notas do jogo da 0.4.145, J7: "o construtor precisa se
 * mudar para a casa livre mais perto da construção se ele estiver ter que caminhar mais de 30 blocos para chegar na
 * construção atual"). Com a obra aberta a mais de {@value #FAR} blocos da cama dele, o construtor passa para a cama livre
 * mais perto da obra, a até {@value #FAR} dela; sem cama livre, troca de cama com o adulto sem profissão que dorme mais
 * perto da obra. A troca da Regra 74, ao amanhecer, segue valendo para os outros ({@code VC_BUILDER_HOME}).
 */
public final class BuilderHome {

    /** A distância, na horizontal, da cama à obra acima da qual o construtor se muda. */
    static final int FAR = 30;

    /** Uma passada a cada trinta segundos. */
    static final int EVERY = 600;

    private BuilderHome() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ServerWorld world = server.getOverworld();
            long now = world.getTime();

            if (now % EVERY != EVERY / 3) {
                return;
            }

            for (Colony colony : List.copyOf(VillageColonyMod.COLONIES.all())) {
                if (colony.isActive() && colony.isAttended(now)) {
                    VillageColonyMod.CONSTRUCTIONS.openOf(colony.id()).ifPresent(project ->
                            settle(world, colony, MinecraftTypeAdapter.toBlockPos(project.origin())));
                }
            }
        });
    }

    /** Muda para perto da obra em {@code site} cada construtor que dorme longe dela; diz quantos mudaram. */
    static int settle(ServerWorld world, Colony colony, BlockPos site) {
        int moved = 0;

        for (Worker worker : VillageColonyMod.WORKERS.ofColony(colony.id())) {
            if (worker.profession().filter(ProfessionType.BUILDER::equals).isEmpty()
                    || !(world.getEntity(worker.villagerId()) instanceof VillagerEntity builder)) {
                continue;
            }

            Optional<BlockPos> home = homeOf(world, builder);

            if (home.isPresent() && near(home.get(), site)) {
                continue;
            }

            if (freeBedNear(world, site).map(head -> move(world, builder, head)).orElse(false)
                    || (home.isPresent() && swapWithIdle(world, colony, builder, home.get(), site))) {
                moved++;
                VillageColonyMod.LOGGER.info("VC_BUILDER_HOME worker={} bed={} site={} rule=376",
                        builder.getUuid().toString().substring(0, 8),
                        homeOf(world, builder).map(BlockPos::toShortString).orElse("-"), site.toShortString());
            }
        }

        return moved;
    }

    /** A cabeceira da cama livre mais perto da obra, a até {@value #FAR} dela. */
    private static Optional<BlockPos> freeBedNear(ServerWorld world, BlockPos site) {
        return world.getPointOfInterestStorage()
                .getInSquare(type -> type.matchesKey(PointOfInterestTypes.HOME), site, FAR,
                        PointOfInterestStorage.OccupationStatus.HAS_SPACE)
                .map(PointOfInterest::getPos)
                .filter(head -> near(head, site))
                .min(Comparator.comparingLong((BlockPos head) -> squared(head, site)));
    }

    /** Troca de cama com o adulto sem profissão da colônia que dorme mais perto da obra, a até {@value #FAR} dela. */
    private static boolean swapWithIdle(ServerWorld world, Colony colony, VillagerEntity builder, BlockPos builderHead,
                                        BlockPos site) {
        Optional<VillagerEntity> idle = VillageColonyMod.WORKERS.ofColony(colony.id()).stream()
                .filter(worker -> worker.profession().isEmpty())
                .map(worker -> world.getEntity(worker.villagerId()))
                .filter(entity -> entity instanceof VillagerEntity villager && villager.isAlive() && !villager.isBaby())
                .map(VillagerEntity.class::cast)
                .filter(villager -> homeOf(world, villager).filter(head -> near(head, site)).isPresent())
                .min(Comparator.comparingLong(villager -> squared(homeOf(world, villager).orElseThrow(), site)));

        if (idle.isEmpty()) {
            return false;
        }

        BlockPos idleHead = homeOf(world, idle.get()).orElseThrow();
        PointOfInterestStorage pois = world.getPointOfInterestStorage();

        // As duas camas ficam livres antes de cada um tomar a do outro.
        builder.getBrain().forget(MemoryModuleType.HOME);
        idle.get().getBrain().forget(MemoryModuleType.HOME);
        pois.releaseTicket(builderHead);
        pois.releaseTicket(idleHead);

        return move(world, builder, idleHead) && move(world, idle.get(), builderHead);
    }

    /** Dá a cama desta cabeceira ao aldeão; falso se ali não há cama. */
    private static boolean move(ServerWorld world, VillagerEntity villager, BlockPos head) {
        BlockState state = world.getBlockState(head);

        if (!(state.getBlock() instanceof BedBlock)) {
            return false;
        }

        BlockPos foot = state.get(BedBlock.PART) == BedPart.HEAD ? head.offset(state.get(BedBlock.FACING).getOpposite())
                : head;

        VillageFoundation.giveHome(world, villager, foot);

        return true;
    }

    private static Optional<BlockPos> homeOf(ServerWorld world, VillagerEntity villager) {
        return villager.getBrain().getOptionalRegisteredMemory(MemoryModuleType.HOME)
                .filter(home -> home.dimension().equals(world.getRegistryKey()))
                .map(GlobalPos::pos);
    }

    private static boolean near(BlockPos a, BlockPos b) {
        return squared(a, b) <= (long) FAR * FAR;
    }

    private static long squared(BlockPos a, BlockPos b) {
        long dx = (long) a.getX() - b.getX();
        long dz = (long) a.getZ() - b.getZ();

        return dx * dx + dz * dz;
    }
}
