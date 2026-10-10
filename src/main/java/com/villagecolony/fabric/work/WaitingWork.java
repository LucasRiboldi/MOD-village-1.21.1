package com.villagecolony.fabric.work;

import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.service.VillageDetector;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.construction.service.RemovalAudit;
import com.villagecolony.core.coordination.PatienceClock;
import com.villagecolony.core.coordination.WorkClock;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.brain.WorkTargets;
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

    static {
        ServerMemory.register(WaitingWork.class, WaitingWork::clearAll);
    }

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

    /**
     * Quantas peças cada obra em curso ainda devia, e desde quando.
     *
     * <p><b>O buraco que a sessão de 09-19 achou.</b> O relógio acima só
     * conta para {@code WAITING_RESOURCES}. Uma obra em
     * {@code BUILDING} que simplesmente <b>não anda</b> não tinha
     * relógio nenhum, e a vaga de obra é única: a casa de
     * {@code -847, 87, 1310} ficou <b>174 peças paradas em 41 das 60
     * leituras</b>, meia hora segurando a fila enquanto os construtores
     * trocavam de ofício embaixo dela.
     *
     * <p>A contagem que importa é a de <b>peças que faltam</b>, e não a
     * de tempo aberta: obra grande demora por ser grande, e puni-la por
     * isso seria trocar um defeito por outro. O que não é normal é a
     * conta <b>não descer</b>.
     */
    private static final Map<UUID, Progress> BUILDING_SINCE = new HashMap<>();

    /** Última leitura e tiques de expediente acumulados sem assentar peça. */
    private record Progress(int left, long at, long timeOfDay, long stalled) {
    }

    private WaitingWork() {
    }

    /** Esquece as esperas. Chamado ao parar o servidor. */
    public static void clearAll() {
        WAITING_SINCE.clear();
        BUILDING_SINCE.clear();
    }

    /** Esquece os relogios de uma obra removida por cancelamento manual. */
    public static void forget(UUID projectId) {
        if (projectId == null) {
            return;
        }

        WAITING_SINCE.remove(projectId);
        BUILDING_SINCE.remove(projectId);
    }

    /**
     * Acorda a obra que esperava material, quando o material chegou.
     *
     * <p>{@code WAITING_RESOURCES} era estado terminal na prática. A
     * única transição para {@code BUILDING} estava na criação do projeto,
     * e {@link ConstructionPlanner#ensureTask} não abre tarefa fora de {@code BUILDING}: a
     * obra que uma vez ficasse sem material não voltava a ser tentada
     * nunca mais, ainda que o baú enchesse no minuto seguinte.
     *
     * <p>Foi o que a sessão das 19:44 de 2026-08-15 mostrou. A casa parou
     * em 149 blocos com 52 tábuas guardadas, dois fabricantes ociosos e
     * a linha {@code builders: 0 working, WAITING_RESOURCES ... — no
     * build task} repetindo até o desligamento. O comentário de
     * {@code BuilderMaterials.waitForResources} já dizia que "quem destrava é
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

        if (!BuilderMaterials.hasMaterialForNextBlock(world, project)) {
            return;
        }

        project.moveTo(ConstructionState.BUILDING);

        VillageColonyMod.LOGGER.info(
                "Project {} has what it was waiting for — back to building, {} blocks left",
                project.id(),
                project.remainingCount());
    }

    /**
     * A obra que esperou material tempo demais tenta se resgatar sem sair da fila.
     *
     * <p><b>O buraco que isto fecha.</b> Quem planeja não abre obra nova
     * enquanto houver uma aberta, e nada tirava da frente uma obra
     * parada em {@code WAITING_RESOURCES}. A decisão atual é mais estrita:
     * antes de liberar uma obra, a colônia precisa tentar substitutos de
     * material, manter a rota profissional priorizada e, depois das tentativas
     * previstas pela ADR-036 item 6, abastecer o baú que atende o construtor.
     *
     * @return se a obra foi abandonada agora
     */
    public static boolean giveUpIfStalled(
            ServerWorld world, Colony colony, ConstructionProject project) {

        if (project.state() != ConstructionState.WAITING_RESOURCES) {
            WAITING_SINCE.remove(project.id());

            return givesUpIfItIsNotMoving(world, colony, project);
        }

        BUILDING_SINCE.remove(project.id());

        long since = WAITING_SINCE.computeIfAbsent(project.id(), id -> world.getTime());

        if (!PatienceClock.ranOut(since, world.getTime())) {
            return false;
        }

        if (BuilderMaterials.hasMaterialForNextBlock(world, project)) {
            WAITING_SINCE.remove(project.id());
            project.moveTo(ConstructionState.BUILDING);

            VillageColonyMod.LOGGER.info(
                    "Project {} recovered the material it was waiting for — back to building",
                    project.id());

            return false;
        }

        return false;
    }

    /**
     * A obra que está aberta e não anda também sai da frente —
     * 2026-09-19.
     *
     * <p><b>É o mesmo argumento do relógio acima, no estado que ele não
     * cobria.</b> Aquele tira da frente a obra parada esperando
     * material; esta tira a que tem material, tem construtor, está em
     * {@code BUILDING} — e mesmo assim não coloca peça. Do lado de fora
     * as duas são a mesma coisa: a vaga única ocupada e a vila sem
     * crescer.
     *
     * <p><b>A medição.</b> Sessão de 09-19, casa de
     * {@code -847, 87, 1310}: {@code 174 blocks left} em 41 das 60
     * leituras, meia hora. A causa foi o rodízio de ofícios — os
     * construtores largavam o ofício antes de assentar peça —, e essa
     * causa está consertada. Este guarda é o <b>fundo de poço</b>: seja
     * qual for o motivo de a conta não descer, a colônia volta a
     * planejar em vez de esperar para sempre.
     *
     * <p>Mede <b>peça assentada</b>, e não tempo aberta: obra grande
     * demora por ser grande, e o relógio recomeça a cada peça. Só a
     * ausência de progresso pela janela inteira da {@link PatienceClock}
     * conta — dez minutos de expediente. A noite pausa a contagem sem
     * zerá-la, como exige a Regra 18.
     *
     * @return se a obra foi abandonada agora
     */
    private static boolean givesUpIfItIsNotMoving(
            ServerWorld world, Colony colony, ConstructionProject project) {

        if (project.state() != ConstructionState.BUILDING) {
            BUILDING_SINCE.remove(project.id());

            return false;
        }

        int left = project.remainingCount();
        long now = world.getTime();
        long timeOfDay = world.getTimeOfDay();

        Progress seen = BUILDING_SINCE.get(project.id());

        if (seen == null || seen.left() != left) {
            // Ou é a primeira leitura, ou a conta mudou: em qualquer dos
            // dois a obra está viva, e o relógio recomeça daqui.
            BUILDING_SINCE.put(project.id(), new Progress(left, now, timeOfDay, 0));

            return false;
        }

        long stalled = seen.stalled() + workingTicksSince(seen, now, timeOfDay);
        BUILDING_SINCE.put(project.id(), new Progress(left, now, timeOfDay, stalled));

        boolean onlyUnsupported = project.nextBlock().isEmpty() && !project.isFinished();

        if (!ranOutOfPatience(onlyUnsupported, stalled)) {
            return false;
        }

        if (onlyUnsupported) {
            BuilderPlacement.reconsiderDeferredPieces(world, project);
            BUILDING_SINCE.put(project.id(), new Progress(left, now, timeOfDay, 0));

            VillageColonyMod.LOGGER.info(
                    "Project {} keeps waiting at {} — only deferred pieces remain, so support and material rescue"
                            + " keep priority before the lot can be released",
                    project.id(),
                    project.origin());

            return false;
        }

        VillageColonyMod.LOGGER.info(
                "Colony {} lets go of {} at {} — it has been at {} pieces for {} work ticks"
                        + " and placed none. The half-built house and its lot stay taken",
                colony.id(),
                project.blueprint().id(),
                project.origin(),
                left,
                stalled);

        BUILDING_SINCE.remove(project.id());

        // A planta não leva a culpa: não faltou material, a obra não
        // andou. Mesmo argumento do lote fora de alcance.
        giveUp(colony, project, false);

        return true;
    }

    /** Janela histórica curta; mantida para o teste garantir que ela não libera mais a obra. */
    static final long UNSUPPORTED_PATIENCE = 2L * VillageDetector.CYCLE_TICKS;

    /**
     * Se a obra parada já esperou o bastante.
     *
     * <p>Peça sem apoio usa a mesma janela longa: ela pode depender de outra
     * peça que foi adiada por material, de substituição, ou do baú do construtor
     * receber a falta. O relógio curto de dois ciclos continua registrado acima
     * para evitar regressão ao abandono prematuro.
     */
    static boolean ranOutOfPatience(boolean onlyUnsupported, long stalled) {
        return PatienceClock.ranOut(0, stalled);
    }

    /** Desconta noites completas e parciais entre duas leituras do planejador. */
    private static long workingTicksSince(Progress seen, long now, long timeOfDay) {
        long elapsed = Math.max(0, now - seen.at());

        if (timeOfDay - seen.timeOfDay() != elapsed) {
            // Sono, /time e ciclo solar congelado não inventam tempo de trabalho.
            // Se a janela mudou, retomamos a medição na próxima leitura.
            return WorkClock.isWorkTime(seen.timeOfDay()) && WorkClock.isWorkTime(timeOfDay)
                    ? elapsed : 0;
        }

        long days = Math.floorDiv(timeOfDay, WorkClock.DAY)
                - Math.floorDiv(seen.timeOfDay(), WorkClock.DAY);
        long end = Math.min(Math.floorMod(timeOfDay, WorkClock.DAY), WorkClock.DUSK);
        long start = Math.min(Math.floorMod(seen.timeOfDay(), WorkClock.DAY), WorkClock.DUSK);
        return days * WorkClock.DUSK + end - start;
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
        giveUp(colony, project, true);
    }

    /**
     * O mesmo, dizendo se a planta leva a culpa.
     *
     * <p><b>A distinção entrou em 2026-09-15</b>, com o abandono por
     * distância. Marcar a planta só faz sentido quando foi <b>ela</b> que
     * não coube no que a colônia alcança: a marca do {@code PlanRefusals}
     * é por material que faltou, e se desfaz quando o material aparecer.
     *
     * <p>A obra que o centro da vila deixou para trás não faltou material
     * nenhum — a do log de 09-15 tinha 2.743 tábuas e 621 pedregulhos em
     * estoque. Marcá-la pelo primeiro item da lista de restantes acusaria
     * um material inocente e faria a colônia pular aquela casa até que
     * esse item aparecesse, por uma razão que nada tem a ver com ele.
     *
     * @param blamePlan se a planta deve ser marcada como a que a colônia
     *     não conseguiu levantar. Falso quando a causa é a posição, e não
     *     a planta
     */
    public static void giveUp(Colony colony, ConstructionProject project, boolean blamePlan) {
        WAITING_SINCE.remove(project.id());
        BUILDING_SINCE.remove(project.id());

        // <b>E a colônia aprende qual planta não conseguiu levantar</b> —
        // 2026-09-12. Sem isto a escolha reoferece a mesma casa no ciclo
        // seguinte: a ordem é estável dentro da semente, e a sessão daquele dia
        // mostrou o laço — plains_butcher_shop_2 escolhida duas vezes,
        // vinte ciclos de espera por smooth_stone_slab cada, e o autor
        // dizendo "não vi nenhuma construção nascendo". A laje está a três
        // degraus de receita e o teto é dois: aquela casa era impossível
        // para aquela colônia, e continuaria sendo.
        //
        // Marcar aqui, e não na escolha, porque é aqui que se sabe o que
        // faltou. Ver PlanRefusals — a marca é por condição, e se desfaz
        // quando a colônia passar a alcançar o material.
        if (blamePlan) {
            PlanRefusals.refused(
                    colony.id(), project.blueprint().id(), project.remainingMaterials());
        }

        VillageColonyMod.BUILDINGS.registerOrMerge(Building.of(project));

        VillageColonyMod.CONSTRUCTIONS.forget(project.id(), RemovalAudit.patienceAbandonment());

        // A vaga de obra é única; suas tarefas não podem sobreviver ao projeto.
        for (Task task : VillageColonyMod.TASKS.ofColony(colony.id())) {
            if (task.type() == TaskType.BUILD && task.isOpen()) {
                task.executor().ifPresent(worker -> {
                    BuilderWork.forget(worker);
                    WorkTargets.clear(worker);
                });
                task.cancel();
            }
        }

        VillageColonyMod.LOGGER.info(
                "Colony {} gives up on {} at {} — {} blocks remain."
                        + " The half-built house and its lot stay taken",
                colony.id(),
                project.blueprint().id(),
                project.origin(),
                project.remainingCount());

        IdleLog.clear(colony.id(), SUBJECT);
    }
}
