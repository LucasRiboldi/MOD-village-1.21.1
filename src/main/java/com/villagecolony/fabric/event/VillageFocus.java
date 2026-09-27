package com.villagecolony.fabric.event;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.service.VillageDetector;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * A colônia só existe para a simulação quando um jogador está nela agora.
 *
 * <p>Planejamento, detecção e trabalho usam a mesma régua horizontal da vila,
 * {@link VillageDetector#SEARCH_RADIUS}. Não há foco memorizado: ao sair, a
 * colônia pausa; ao chegar, retoma de onde parou. Assim um mundo com muitas
 * vilas não consome processamento em regiões sem jogador.
 *
 * <p>Os GameTests chamam alguns trabalhadores diretamente, sem criar jogador.
 * O manipulador de produção não os chama quando o servidor está vazio.
 */
public final class VillageFocus {

    private VillageFocus() {
    }

    /** Um ciclo de jogo: toda colônia presente pode planejar. */
    static List<UUID> planners(List<Colony> active, Set<UUID> present) {
        return active.stream().map(Colony::id).filter(present::contains).toList();
    }

    /** Se a sonda de detecção roda nesta colônia. */
    static boolean isAnalyzed(ServerWorld overworld, Colony colony) {
        return isWithin(overworld, colony, VillageDetector.SEARCH_RADIUS);
    }

    /**
     * As colônias que algum jogador está vendo agora.
     *
     * <p>Elas furam a fila do {@link PlannerTurns}. A cota por ciclo continua
     * protegendo o tique quando vários jogadores estão em vilas diferentes.
     */
    static Set<UUID> coloniesNearPlayers(ServerWorld overworld, List<Colony> active) {
        if (overworld.getPlayers().isEmpty()) {
            return Set.of();
        }

        Set<UUID> near = new HashSet<>();

        for (Colony colony : active) {
            if (isNearAPlayer(overworld, colony)) {
                near.add(colony.id());
            }
        }

        return near;
    }

    /** Se esta colônia tem jogador perto o bastante para planejar ou detectar. */
    static boolean isNearAPlayer(ServerWorld overworld, Colony colony) {
        return isWithin(overworld, colony, VillageDetector.SEARCH_RADIUS);
    }

    /**
     * Se um trabalhador pode avançar no tique atual.
     *
     * <p>O servidor de produção não chama trabalhadores sem jogadores. O
     * retorno verdadeiro nesse caso preserva GameTests que exercitam os
     * trabalhadores diretamente, sem um jogador de teste.
     */
    public static boolean isActiveNearAPlayer(ServerWorld overworld, UUID colonyId) {
        if (overworld == null || overworld.getPlayers().isEmpty()) {
            return true;
        }

        return VillageColonyMod.COLONIES.find(colonyId)
                .filter(Colony::isActive)
                .map(colony -> isNearAPlayer(overworld, colony))
                .orElse(false);
    }

    /** Se há jogador dentro do raio de vila de uma posição ainda não adotada. */
    static boolean isNearAPlayer(ServerWorld overworld, BlockPos position) {
        int radius = VillageDetector.SEARCH_RADIUS;

        for (ServerPlayerEntity player : overworld.getPlayers()) {
            long dx = (long) player.getBlockX() - position.getX();
            long dz = (long) player.getBlockZ() - position.getZ();

            if (dx * dx + dz * dz <= (long) radius * radius) {
                return true;
            }
        }

        return false;
    }

    /** Se algum jogador está dentro deste raio do centro da colônia. */
    static boolean isWithin(ServerWorld overworld, Colony colony, int radius) {
        for (ServerPlayerEntity player : overworld.getPlayers()) {
            long dx = (long) player.getBlockX() - colony.center().x();
            long dz = (long) player.getBlockZ() - colony.center().z();

            if (dx * dx + dz * dz <= (long) radius * radius) {
                return true;
            }
        }

        return false;
    }
}
