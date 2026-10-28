package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Quem mora mais perto do lugar do ofício pega a tarefa primeiro — item 3 das escolhas do autor
 * (2026-10-27). A tarefa só ganha alvo depois de pega, então a régua é o lugar fixo de cada ofício: a boca
 * da mina para o mineiro, a obra aberta para o construtor, o bosque para o lenhador e o baú do próprio
 * aldeão (ao lado da bancada) para os outros.
 */
public final class TradeSites {

    private TradeSites() {
    }

    /** A ordem desta passagem: a distância da cama de cada um ao lugar do ofício, medida uma vez. */
    public static Comparator<Worker> nearFirst(ServerWorld world, Colony colony) {
        Map<UUID, Double> distance = new HashMap<>();

        for (Worker worker : VillageColonyMod.WORKERS.ofColony(colony.id())) {
            Optional<BlockPos> home = world.getEntity(worker.villagerId()) instanceof VillagerEntity villager
                    ? BedTeleport.bedOf(world, villager) : Optional.empty();
            Optional<BlockPos> site = siteOf(world, colony, worker);

            distance.put(worker.villagerId(), home.isPresent() && site.isPresent()
                    ? Math.sqrt(home.get().getSquaredDistance(site.get()))
                    : Double.MAX_VALUE);
        }

        return Comparator.comparingDouble(worker -> distance.getOrDefault(worker.villagerId(), Double.MAX_VALUE));
    }

    /** O lugar do ofício deste trabalhador. */
    static Optional<BlockPos> siteOf(ServerWorld world, Colony colony, Worker worker) {
        ProfessionType profession = worker.profession().orElse(null);

        if (profession == ProfessionType.MINER) {
            return VillageColonyMod.MINES.of(colony.id())
                    .map(mine -> MinecraftTypeAdapter.toBlockPos(MineEntrance.surfaceOf(world, colony.id(), mine)));
        }

        if (profession == ProfessionType.BUILDER) {
            return VillageColonyMod.CONSTRUCTIONS.openOf(colony.id())
                    .map(project -> MinecraftTypeAdapter.toBlockPos(project.origin()));
        }

        if (profession == ProfessionType.LUMBERJACK) {
            List<BlockPos> grove = VillageGrove.of(colony.id());

            if (!grove.isEmpty()) {
                return Optional.of(grove.getFirst());
            }
        }

        return VillageColonyMod.STORAGES.of(worker.villagerId())
                .map(storage -> MinecraftTypeAdapter.toBlockPos(storage.chestPosition()));
    }

}
