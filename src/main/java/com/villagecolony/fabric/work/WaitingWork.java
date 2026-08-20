package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.coordination.PatienceClock;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A obra que espera material: o que a acorda e o que a larga.
 *
 * <p>Saiu de {@code ConstructionPlanner} em 2026-08-20, e o corte é
 * pelo estado: tudo aqui é sobre uma obra parada em
 * {@code WAITING_RESOURCES}, e sobre as duas únicas saídas que ela tem.
 * Ou o material chega e ela volta a construir, ou a paciência acaba e
 * ela sai da frente para a colônia não morrer esperando.
 *
 * <p>As duas se leem melhor lado a lado do que espalhadas no meio do
 * planejamento, porque são a mesma decisão vista de dois lados.
 */
public final class WaitingWork {

    /** O assunto destas linhas no registro de ociosidade. */
    private static final String SUBJECT = "building";

    /**
     * Desde quando cada obra espera material.
     *
     * <p>Fora do modelo de propósito: a hora é do mundo, e
     * {@code ConstructionProject} não conhece Minecraft. Esquecida ao
     * parar o servidor — e isso é escolha, não descuido: a paciência
     * recomeça na sessão seguinte, que é quando o jogador tem chance de
     * ter trazido o material.
     */
    private static final Map<UUID, Long> WAITING_SINCE = new HashMap<>();

    private WaitingWork() {
    }

    /** Esquece as esperas. Chamado ao parar o servidor. */
    public static void clearAll() {
        WAITING_SINCE.clear();
    }

    /**
     * Acorda a obra que esperava material, quando o material chegou.
     *
     * <p>{@code WAITING_RESOURCES} era estado terminal na prática. A
     * única transição para {@code BUILDING} estava na criação do projeto,
     * e {@link #ensureTask} não abre tarefa fora de {@code BUILDING}: a
     * obra que uma vez ficasse sem material não voltava a ser tentada
     * nunca mais, ainda que o baú enchesse no minuto seguinte.
     *
     * <p>Foi o que a sessão das 19:44 de 2026-08-15 mostrou. A casa parou
     * em 149 blocos com 52 tábuas guardadas, dois fabricantes ociosos e
     * a linha {@code builders: 0 working, WAITING_RESOURCES ... — no
     * build task} repetindo até o desligamento. O comentário de
     * {@code BuilderWork.waitForResources} já dizia que "quem destrava é
     * o ciclo da colônia" — era intenção que nenhum código cumpria.
     *
     * <p>Só acorda com o material do próximo bloco em mãos. Acordar sem
     * conferir poria o construtor a caminhar até a obra todo ciclo para
     * falhar ao chegar, que é a mesma roda do E16 por outra porta.
     */
    static void wakeIfSupplied(ServerWorld world, ConstructionProject project) {
        if (project.state() != ConstructionState.WAITING_RESOURCES) {
            return;
        }

        if (!BuilderWork.hasMaterialForNextBlock(world, project)) {
            return;
        }

        project.moveTo(ConstructionState.BUILDING);

        VillageColonyMod.LOGGER.info(
                "Project {} has what it was waiting for — back to building, {} blocks left",
                project.id(),
                project.remainingCount());
    }

    /**
     * A obra que esperou material tempo demais sai da frente.
     *
     * <p><b>O buraco que isto fecha.</b> Quem planeja não abre obra nova
     * enquanto houver uma aberta, e nada tirava da frente uma obra
     * parada em {@code WAITING_RESOURCES}. A casa de planície pede 43
     * pedregulhos que a colônia não minera; sem o jogador guardá-los num
     * baú, a vila parava de crescer <b>para sempre</b>. O lenhador já
     * tinha o guarda de travamento desde a Regra 9; a obra não tinha
     * nada equivalente, e a diferença nunca foi deliberada.
     *
     * <p><b>A casa pela metade fica de pé, e o lote fica tomado.</b> Ela
     * é do jogador agora — derrubá-la seria a Regra 3 ao contrário. E a
     * caixa vai para o registro de construções antes de a obra sumir,
     * senão o lote voltaria a parecer livre e a colônia planejaria por
     * cima do que ela mesma levantou.
     *
     * <p><b>O que isto custa, dito por inteiro:</b> a obra não volta. Se
     * o pedregulho aparecer depois, ninguém retoma aquela casa — ela
     * fica como está. A alternativa era a vila inteira parada à espera
     * de uma entrega que pode nunca vir, e entre as duas esta é a que
     * deixa a colônia viva.
     *
     * @return se a obra foi abandonada agora
     */
    static boolean giveUpIfStalled(
            ServerWorld world, Colony colony, ConstructionProject project) {

        if (project.state() != ConstructionState.WAITING_RESOURCES) {
            WAITING_SINCE.remove(project.id());

            return false;
        }

        long since = WAITING_SINCE.computeIfAbsent(project.id(), id -> world.getTime());

        if (!PatienceClock.ranOut(since, world.getTime())) {
            return false;
        }

        giveUp(colony, project);

        return true;
    }

    /**
     * Larga esta obra: a casa fica de pé como está, e o lote com ela.
     *
     * <p>Separado do relógio de propósito. O relógio é de escala de
     * minutos e se afirma fora do jogo, como o {@link
     * com.villagecolony.core.coordination.WorkClock}; a consequência —
     * a caixa virar construção, a obra sair do registro, a colônia
     * voltar a planejar — se afirma dentro dele, sem esperar dez
     * minutos. Juntas as duas metades não deixam buraco.
     *
     * <p>A ordem das duas linhas importa: a construção entra no registro
     * <b>antes</b> de a obra sair. Invertida, haveria um instante em que
     * o lote não pertence a ninguém.
     */
    public static void giveUp(Colony colony, ConstructionProject project) {
        WAITING_SINCE.remove(project.id());

        VillageColonyMod.BUILDINGS.register(Building.of(project));
        VillageColonyMod.CONSTRUCTIONS.forget(project.id());

        VillageColonyMod.LOGGER.info(
                "Colony {} gives up on {} at {} — {} blocks never came in {} cycles."
                        + " The half-built house and its lot stay taken",
                colony.id(),
                project.blueprint().id(),
                project.origin(),
                project.remainingCount(),
                PatienceClock.CYCLES);

        IdleLog.clear(colony.id(), SUBJECT);
    }
}
