package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.coordination.GatheringReach;
import com.villagecolony.core.coordination.ResourceReach;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.data.save.ProfessionPolicySavedData;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * A busca de recurso da vez, por colônia e ofício — ADR-036 item 18. A regra
 * está em {@link ResourceReach}; aqui moram a vez de cada um e as exceções:
 * raio escolhido pelo jogador no menu e raio encurtado pelos testes valem como
 * antes, a partir do centro.
 */
final class ResourceSearches {

    static {
        ServerMemory.register(ResourceSearches.class, ResourceSearches::clearAll);
    }

    /** De onde parte a busca, até onde vai, e o que ela aceita. */
    record Plan(BlockPos origin, int radius, Predicate<BlockPos> inside) {
    }

    private record Key(UUID colonyId, ProfessionType profession) {
    }

    private static final Map<Key, Integer> TURNS = new HashMap<>();

    private ResourceSearches() {
    }

    static void clearAll() {
        TURNS.clear();
    }

    /**
     * A busca da vez, sem passar a vez.
     *
     * @param ceiling o teto do ofício (o raio de antes da caixa)
     * @param shortened se o teto foi encurtado pelos testes
     */
    static Plan current(ServerWorld world, UUID colonyId, ProfessionType profession,
            BlockPos centre, int ceiling, boolean shortened) {

        Optional<Colony> colony = VillageColonyMod.COLONIES.find(colonyId);
        Optional<VillageBounds> box = colony.flatMap(Colony::bounds);
        boolean chosenByPlayer = ProfessionPolicySavedData.get(world.getServer()).policies()
                .policyOf(profession).hasConfiguredRadius();

        if (box.isEmpty() || shortened || chosenByPlayer) {
            int beds = colony.map(Colony::observedBeds).orElse(0);
            int radius = chosenByPlayer
                    ? ProfessionPolicySavedData.get(world.getServer()).policies()
                            .policyOf(profession).searchRadiusOr(ceiling)
                    : GatheringReach.radius(beds, ceiling);

            return new Plan(centre, radius, pos -> true);
        }

        ResourceReach.Search search = ResourceReach.search(
                box.get(), MinecraftTypeAdapter.toColonyPos(centre), TURNS.getOrDefault(new Key(colonyId, profession), 0));

        return new Plan(
                MinecraftTypeAdapter.toBlockPos(search.origin()),
                search.radius(),
                pos -> ResourceReach.within(box.get(), pos.getX(), pos.getZ()));
    }

    /**
     * O alcance a partir do centro: até 10 além da borda com a vila medida;
     * sem ela, o {@link GatheringReach} até {@code ceiling}.
     */
    static int villageReach(UUID colonyId, BlockPos centre, int ceiling) {
        Optional<Colony> colony = VillageColonyMod.COLONIES.find(colonyId);
        Optional<VillageBounds> box = colony.flatMap(Colony::bounds);

        if (box.isPresent()) {
            return ResourceReach.search(box.get(), MinecraftTypeAdapter.toColonyPos(centre), 0).radius();
        }

        return GatheringReach.radius(colony.map(Colony::observedBeds).orElse(0), ceiling);
    }

    /** A próxima busca deste ofício parte do outro lado (centro ↔ borda). */
    static void advance(UUID colonyId, ProfessionType profession) {
        TURNS.merge(new Key(colonyId, profession), 1, Integer::sum);
    }
}
