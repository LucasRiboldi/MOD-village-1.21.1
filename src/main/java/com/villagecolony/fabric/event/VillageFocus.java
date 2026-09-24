package com.villagecolony.fabric.event;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.service.VillageDetector;
import com.villagecolony.core.coordination.ColonyFocus;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Só a vila onde o jogador passa mais tempo evolui — 2026-09-24, decisão do
 * autor: <i>"a maioria pode ficar salva apenas da vila onde o player passa
 * mais tempo, pausando a evolução ou análise de outras vilas para não
 * sobrecarregar"</i>.
 *
 * <p><b>O que pausa nas outras:</b> o planejador (abrir obra, varrer lote,
 * estender rua) e a sonda de detecção pelo centro. É onde estava o custo: o
 * planejador foi 91% do tempo dos ciclos lentos de 24-09. <b>O que continua
 * em todas as vilas perto do jogador:</b> trabalhadores, leitura de baú e
 * tarefas — isso custa menos de 1% e, sem isso, a vila vizinha pararia de
 * trabalhar no meio de uma obra que já estava aberta.
 *
 * <p><b>Qual é a vila foco:</b> a de maior presença acumulada do jogador,
 * com memória que esquece em duas horas de jogo ({@link ColonyFocus}).
 * Enquanto ninguém acumulou presença — mundo recém-aberto —, evolui a vila
 * onde o jogador está. A presença não é gravada no save: ao reabrir o mundo,
 * o foco se refaz em poucos ciclos com o jogador parado na vila dele.
 *
 * <p>Só vale no ciclo de jogo; o ciclo dos testes ({@code runCycleNow}) não
 * tem jogador e continua planejando todas as colônias.
 */
final class VillageFocus {

    private static final ColonyFocus FOCUS = new ColonyFocus();

    private static UUID announced;

    private VillageFocus() {
    }

    /**
     * Um ciclo de jogo: soma a presença e diz quem planeja.
     *
     * @param active as colônias em ciclo agora (jogador a até 128 blocos)
     * @param present as que têm jogador dentro do raio da vila (64 blocos)
     * @return as colônias que podem planejar: a vila foco, se ela está em
     *     ciclo; sem foco ainda, as que têm jogador dentro
     */
    static List<UUID> planners(List<Colony> active, Set<UUID> present) {
        FOCUS.record(VillageColonyMod.COLONIES.all().stream().map(Colony::id).toList(), present);

        Optional<UUID> focus = FOCUS.focus();
        announce(focus);

        List<UUID> activeIds = active.stream().map(Colony::id).toList();

        if (focus.isPresent()) {
            return activeIds.contains(focus.get()) ? List.of(focus.get()) : List.of();
        }

        return activeIds.stream().filter(present::contains).toList();
    }

    /**
     * Se a sonda de detecção roda nesta colônia: a vila foco, ou uma com
     * jogador dentro agora. As outras ficam como estavam até alguém chegar.
     */
    static boolean isAnalyzed(ServerWorld overworld, Colony colony) {
        return FOCUS.focus().map(colony.id()::equals).orElse(false)
                || isWithin(overworld, colony, VillageDetector.SEARCH_RADIUS);
    }

    private static void announce(Optional<UUID> focus) {
        UUID now = focus.orElse(null);

        if (!Objects.equals(now, announced)) {
            announced = now;

            VillageColonyMod.LOGGER.info(
                    "Focus village is now {} (presence {} cycles) — only it plans and scans;"
                            + " the other villages keep working and wait",
                    now, now == null ? 0 : String.format("%.1f", FOCUS.presenceOf(now)));
        }
    }

    /** A vila foco atual, para o log e os testes. */
    static Optional<UUID> focus() {
        return FOCUS.focus();
    }

    /** Esquece a presença — mundo novo, ou teste. */
    static void clearAll() {
        FOCUS.clear();
        announced = null;
    }

    /**
     * As colônias que algum jogador está vendo agora — 2026-09-15.
     *
     * <p>Elas furam a fila do {@link PlannerTurns}, e o motivo é o relato
     * do autor de 09-15: <i>"entrei no jogo, não vi nenhuma casa
     * crescendo"</i>. O log daquela sessão mostrou o sistema funcionando —
     * a rua cresceu três vezes em vinte minutos — e a colônia observada
     * esperando quatro ciclos por vez, enquanto 28 das 29 colônias do
     * mundo estavam dormentes e gastavam a fila sem ter o que fazer.
     *
     * <p><b>A régua é a mesma da vila</b>, {@code SEARCH_RADIUS}: dentro
     * dela o jogador tem os chunks carregados e vê o que a colônia faz.
     * Fora dela, o trabalho acontece sem plateia e pode esperar a vez.
     *
     * <p>Horizontal, como todo raio de vila neste projeto — ver
     * {@code ConstructionReach.isOutOfReach}. O jogador no fundo da mina
     * continua sendo o jogador daquela vila.
     *
     * <p>Custa uma volta pelos jogadores online vezes as colônias ativas,
     * com aritmética de inteiros e nenhuma leitura de mundo. Num servidor
     * cheio isso cresce, e o teto da cota continua sendo o que protege o
     * tique: ver {@code PlannerTurns.PER_CYCLE}.
     */
    static Set<UUID> coloniesNearPlayers(
            ServerWorld overworld, List<Colony> active) {

        if (overworld.getPlayers().isEmpty()) {
            // Servidor sem ninguém online: não há o que priorizar, e o
            // rodízio puro é a resposta certa.
            return Set.of();
        }

        Set<UUID> near = new HashSet<>();

        for (Colony colony : active) {
            if (isWithin(overworld, colony, VillageDetector.SEARCH_RADIUS)) {
                near.add(colony.id());
            }
        }

        return near;
    }

    /**
     * Se esta colônia tem jogador perto o bastante para trabalhar —
     * 2026-09-15.
     *
     * <p>Decisão do autor: <i>"não trabalhar nas vilas que o jogador não
     * está perto"</i>. Ver {@link VillageDetectionHandler#WORKING_DISTANCE}.
     *
     * <p><b>Para o ciclo inteiro</b>, e não só o planejamento: trabalhador,
     * leitura de baú e tarefa. A colônia longe fica inerte até alguém
     * chegar, e retoma de onde parou — os cursores de varredura, mina e
     * índice de ruas são guardados, e nada disso depende de ciclos
     * contínuos.
     *
     * <p><b>Servidor sem ninguém online não trabalha</b>, e isso é a
     * consequência honesta da regra. Antes disto as 29 colônias do mundo do
     * autor ciclavam para sempre; agora o mundo vazio não gasta tique com
     * vila nenhuma.
     */
    static boolean isNearAPlayer(ServerWorld overworld, Colony colony) {
        return isWithin(overworld, colony, VillageDetectionHandler.WORKING_DISTANCE);
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
