package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.ColonyState;
import com.villagecolony.core.colony.service.VillageDetector;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Onde a simulação gasta o processamento — a vila em que o jogador está.
 *
 * <p><b>ADR-003 Emenda 6 e ADR-036 item 11.</b> Uma colônia trabalha só com
 * o jogador <b>dentro da caixa dela há mais de um minuto</b>
 * ({@link Colony#SETTLE_TICKS}); saiu, parou. Fora disso nada roda — nem
 * planejar, nem detectar, nem os ofícios, a refeição, a fuga ou a placa — e a
 * vila não gasta processamento.
 *
 * <p>Colônia ainda não medida (save antigo, chunk do centro descarregado)
 * usa a régua de antes, jogador a até {@link VillageDetector#SEARCH_RADIUS}
 * do centro, só até ser medida.
 *
 * <p>Os GameTests chamam alguns trabalhadores diretamente, sem criar jogador.
 * O manipulador de produção não os chama quando o servidor está vazio.
 */
public final class VillageFocus {

    /** De quanto em quanto tempo conferir quem está dentro de qual vila. */
    public static final int EVERY_TICKS = 20;

    private VillageFocus() {
    }

    /**
     * Marca as vilas com jogador dentro, e diz no log quando uma começa ou
     * para de trabalhar.
     */
    public static void attend(ServerWorld overworld) {
        long now = overworld.getTime();

        for (Colony colony : VillageColonyMod.COLONIES.all()) {
            boolean wasAttended = colony.isAttended(now);

            if (hasAPlayerInside(overworld, colony)) {
                colony.attend(now);

                if (!wasAttended && colony.isAttended(now)) {
                    VillageColonyMod.LOGGER.info(
                            "Colony {} is attended — a player has been inside the village {} for a minute",
                            colony.id(),
                            colony.bounds().map(Object::toString).orElse("(not measured yet)"));
                }
            } else if (colony.stoppedWithin(now, EVERY_TICKS)) {
                VillageColonyMod.LOGGER.info(
                        "Colony {} rests — the player left the village;"
                                + " no automatic work until one stays inside for a minute",
                        colony.id());
            }
        }
    }

    /** Se há jogador dentro da vila — a caixa, ou o raio antigo se ainda não medida. */
    public static boolean hasAPlayerInside(ServerWorld overworld, Colony colony) {
        for (ServerPlayerEntity player : overworld.getPlayers()) {
            if (isInside(colony, player.getBlockX(), player.getBlockZ())) {
                return true;
            }
        }

        return false;
    }

    public static boolean isInside(Colony colony, int x, int z) {
        if (colony.bounds().isPresent()) {
            return colony.bounds().get().containsColumn(x, z);
        }

        long dx = (long) x - colony.center().x();
        long dz = (long) z - colony.center().z();
        long radius = VillageDetector.SEARCH_RADIUS;

        return dx * dx + dz * dz <= radius * radius;
    }

    /** As colônias que trabalham agora: ativas e com jogador dentro há até 5 minutos. */
    public static Set<UUID> attended(ServerWorld overworld, List<Colony> active) {
        if (overworld.getPlayers().isEmpty()) {
            return Set.of();
        }

        long now = overworld.getTime();
        Set<UUID> found = new HashSet<>();

        for (Colony colony : active) {
            if (colony.isAttended(now)) {
                found.add(colony.id());
            }
        }

        return found;
    }

    /** Um ciclo de jogo: toda colônia atendida pode planejar. */
    public static List<UUID> planners(List<Colony> active, Set<UUID> present) {
        return active.stream().map(Colony::id).filter(present::contains).toList();
    }

    /** Se a sonda de detecção roda nesta colônia. */
    public static boolean isAnalyzed(ServerWorld overworld, Colony colony) {
        return colony.isAttended(overworld.getTime());
    }

    /** Se há jogador dentro do raio de vila de uma posição ainda não adotada — a descoberta. */
    public static boolean isNearAPlayer(ServerWorld overworld, BlockPos position) {
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

    /**
     * Se um trabalhador desta colônia pode avançar no tique atual: colônia
     * {@code ACTIVE} (chunk do centro simulando, ADR-002) e atendida.
     *
     * <p>O servidor de produção não chama trabalhadores sem jogadores. O
     * retorno verdadeiro nesse caso preserva GameTests que exercitam os
     * trabalhadores diretamente, sem um jogador de teste.
     */
    public static boolean isWorking(ServerWorld overworld, UUID colonyId) {
        if (overworld == null || overworld.getPlayers().isEmpty()) {
            // Sem jogador (GameTest): a vila trabalha, salvo se abandonada
            // (ADR-036 item 10). O chunk dormente não conta aqui: cenário
            // distante fica dormente para a sonda e mesmo assim é testado.
            return VillageColonyMod.COLONIES.find(colonyId)
                    .map(colony -> colony.state() != ColonyState.ABANDONED)
                    .orElse(true);
        }

        return VillageColonyMod.COLONIES.find(colonyId)
                .filter(colony -> colony.canWork() && colony.isAttended(overworld.getTime()))
                .isPresent();
    }
}
