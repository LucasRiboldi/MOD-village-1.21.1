package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.coordination.PatienceClock;
import com.villagecolony.core.coordination.WorkAssignment;
import com.villagecolony.core.coordination.WorkClock;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.fabric.integration.ColonySupply;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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
     * A peça que a obra espera não é recurso, e por isso ninguém a fazia
     * — P1.1, 2026-09-17.
     *
     * <p><b>O defeito, medido.</b> Playtest de 2026-09-17 às 08:43: a
     * biblioteca ficou oito minutos em {@code WAITING_RESOURCES} com
     * <b>628 de 628</b> blocos, esperando {@code cobblestone_stairs}, e a
     * colônia tinha <b>69 pedregulhos</b> no baú, um pedreiro com
     * cortador de pedra e a receita do próprio jogo. O log do mesmo ciclo
     * dizia {@code no mason work: no task open for it}.
     *
     * <p><b>Por que ninguém abria a tarefa.</b> Quem as cria é
     * {@code ColonyCycle.requestMissing}, e ele itera
     * {@code Map<ResourceType, Integer>}: o pedido nasce de um recurso
     * <b>declarado</b>, e {@code cobblestone_stairs} não é um deles.
     * Escada, porta, cerca e alçapão são peças da planta, não recursos
     * que a colônia conta — então some do planejador de tarefas, por
     * mais pedregulho que haja.
     *
     * <p><b>Por que não bastava declarar o recurso.</b> Seria a terceira
     * vez: {@code SMOOTH_STONE_SLAB} e {@code STONE_BRICKS} entraram
     * assim, e a ADR-009 §2 chama isso pelo nome — <i>"uma solução que
     * pede mais um nome a cada material novo deve ser questionada"</i>.
     * A próxima peça repetiria o defeito. Decisão do autor em 09-17: que
     * a obra abra a tarefa <b>pelo que ela pede</b>.
     *
     * <p><b>O que decide se dá para fazer é o jogo, e não uma lista.</b>
     * {@code ColonySupply.canProvide} consulta o {@code CraftingLookup},
     * que lê as receitas reais e desce até {@code RECIPE_DEPTH} níveis —
     * peça nova entra sozinha, que é a ADR-009 aplicada. Aqui não se
     * fabrica nada: só se abre o pedido, e quem lavra é
     * {@code CraftingWork}, que já sabe achar a peça pela obra.
     *
     * <p><b>Uma tarefa por vez, e não uma por ciclo.</b> A obra fica em
     * espera por muitos ciclos, e abrir um pedido a cada um encheria a
     * fila com a mesma peça — que é o defeito da fila que não esvaziava
     * (§17, E1), por outra porta.
     */
    public static void askForWhatTheWorkIsWaitingOn(ServerWorld world, Colony colony) {
        VillageColonyMod.CONSTRUCTIONS.openOf(colony.id())
                .filter(project -> project.state() == ConstructionState.WAITING_RESOURCES)
                .ifPresent(project -> askTheCraftsmanFor(world, project));
    }

    private static void askTheCraftsmanFor(ServerWorld world, ConstructionProject project) {
        Optional<BlueprintBlock> next = project.nextBlock();

        if (next.isEmpty()) {
            return;
        }

        ResourceId wanted = next.get().block();

        Optional<Item> piece = MinecraftTypeAdapter.toBlock(wanted).map(Block::asItem);

        if (piece.isEmpty()) {
            return;
        }

        // Já é recurso declarado: o ColonyCycle cuida dele, e abrir aqui
        // seria um segundo pedido pela mesma coisa.
        if (MinecraftTypeAdapter.toResourceType(piece.get()).isPresent()) {
            return;
        }

        TaskType type = CraftingWork.isMasonry(wanted)
                ? TaskType.CRAFT_STONE_MATERIAL
                : TaskType.CRAFT_WOOD_MATERIAL;

        for (Task task : VillageColonyMod.TASKS.ofColony(project.colonyId())) {
            if (task.type() == type && task.isOpen()) {
                return;
            }
        }

        if (WorkAssignment.countCapableOf(
                project.colonyId(), type.required(), VillageColonyMod.WORKERS) == 0) {

            // Ninguém sabe lavrar isto. Abrir a tarefa a deixaria na fila
            // para sempre, sem executor possível — mesma razão do
            // ColonyCycle.requestMissing.
            return;
        }

        if (!ColonySupply.canProvide(
                world, project.colonyId(), project.origin(), piece.get())) {

            // A colônia não tem como fazer: falta ingrediente, e quem o
            // traz é o pedido de recurso, não o fabricante.
            return;
        }

        VillageColonyMod.TASKS.create(
                project.colonyId(),
                type,
                TaskPriority.CONSTRUCTION_MATERIAL,
                // Nominal, como na tarefa de obra: o pedido não é de
                // recurso, e quem acha a peça é CraftingWork lendo a
                // própria obra. Ver ConstructionPlanner.ensureTask.
                ResourceType.COBBLESTONE,
                1);

        VillageColonyMod.LOGGER.info(
                "Colony {} asks the {} for {} — the work is waiting on a piece"
                        + " nobody was making",
                project.colonyId(),
                type == TaskType.CRAFT_STONE_MATERIAL ? "mason" : "carpenter",
                wanted);
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

        giveUp(colony, project);

        return true;
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

        if (!PatienceClock.ranOut(0, stalled)) {
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
        // seguinte: a Regra 25 é determinística, e a sessão daquele dia
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

        VillageColonyMod.BUILDINGS.register(Building.of(project));
        VillageColonyMod.CONSTRUCTIONS.forget(project.id());

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
