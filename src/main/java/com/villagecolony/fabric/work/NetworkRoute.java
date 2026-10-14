package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.MineNetwork;
import com.villagecolony.core.construction.service.MineNetworks;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Andar só pelo que está pronto — ADR-042 D5.
 *
 * <p>No nível em rede, o caminho até a pedra é a rota pelos nós prontos (piso garantido,
 * aberturas vedadas) até o pai do nó em obra; o mineiro anda de centro em centro, cada
 * perna de no máximo {@link MineNetwork#SPACING} blocos, que a navegação do jogo cumpre
 * sem esforço. É a regra da MineColonies: trabalha-se a partir do nó-pai pronto.
 */
final class NetworkRoute {

    /** Perto assim do próximo centro, ele já passou dele: segue para o seguinte. */
    static final int PASSED = 3;

    /** Altura do nível em que o mineiro conta como estando nele. */
    static final int ON_THE_LEVEL = 3;

    private NetworkRoute() {
    }

    /**
     * O próximo centro de nó da rota até a pedra, quando o mineiro está no nível e a
     * pedra é do nó que ele segura. Vazio fora do nível ou sem rota (vale a perna de
     * sempre).
     */
    static Optional<BlockPos> waypoint(UUID colonyId, VillagerEntity villager, BlockPos target) {
        Optional<MineNetwork> level = MineNetworks.of(colonyId);

        if (level.isEmpty()) {
            return Optional.empty();
        }

        MineNetwork network = level.get();
        BlockPos at = villager.getBlockPos();

        if (Math.abs(at.getY() - network.hub().y()) > ON_THE_LEVEL) {
            return Optional.empty();
        }

        Optional<MineNetwork.Node> node = network.claimedBy(villager.getUuid());

        if (node.isEmpty()) {
            return Optional.empty();
        }

        List<ColonyPos> route = network.routeTo(MinecraftTypeAdapter.toColonyPos(at), node.get());

        if (route.isEmpty()) {
            return Optional.empty();
        }

        // O centro da rota mais perto dele; passado esse, o seguinte.
        int nearest = 0;

        for (int index = 1; index < route.size(); index++) {
            if (distance(route.get(index), at) < distance(route.get(nearest), at)) {
                nearest = index;
            }
        }

        if (distance(route.get(nearest), at) <= PASSED) {
            nearest++;
        }

        // Já no pai do nó: a pedra está no corredor à frente.
        return nearest < route.size()
                ? Optional.of(MinecraftTypeAdapter.toBlockPos(route.get(nearest)))
                : Optional.empty();
    }

    private static int distance(ColonyPos center, BlockPos at) {
        return Math.abs(center.x() - at.getX()) + Math.abs(center.z() - at.getZ());
    }
}
