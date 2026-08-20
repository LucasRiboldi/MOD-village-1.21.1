package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.service.VillageDetector;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ColonyHut;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.construction.service.ConstructionService;
import com.villagecolony.core.coordination.PatienceClock;
import com.villagecolony.core.coordination.IdleReason;
import com.villagecolony.core.coordination.WorkAssignment;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.type.Side;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.BuildSiteScanner;
import com.villagecolony.fabric.integration.SitePreparation;
import com.villagecolony.fabric.integration.VillageBiomes;
import com.villagecolony.fabric.integration.StructureBlueprintReader;
import net.minecraft.block.Block;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Optional;

/**
 * A colônia decide construir — TASK-033.
 *
 * <p>Uma obra por colônia de cada vez, e só quando há quem a execute, e
 * só onde a Regra 6 deixa. Não põe bloco algum: quem põe é
 * {@link BuilderWork}.
 *
 * <p><b>A torneira por último.</b> Não se abre obra sem construtor na
 * vila. Uma obra sem executor ficaria aberta para sempre, e a meta de
 * tábua da Regra 5 passaria a apontar para uma casa que ninguém levanta
 * — o fabricante encheria os baús de tábua por causa de um canteiro
 * fantasma. É a lição do §11, e é a mesma ordem que a Fase 9 seguiu.
 *
 * <p><b>Quando a vila para de crescer.</b> Por regra, nunca: o autor
 * decidiu em 2026-08-14 que se constrói enquanto houver material e
 * espaço. O freio é o mundo — só há obra onde há lote livre encostado em
 * rua, e é {@code BuildSiteScanner} quem responde isso.
 */
public final class ConstructionPlanner {

    /**
     * Como esta fase aparece na linha de {@link IdleLog}.
     *
     * <p>O motivo de não haver obra existe por causa da sessão de
     * 2026-08-14, à noite: a Fase 10 não produziu linha nenhuma — nem
     * obra, nem recusa — e havia cinco caminhos silenciosos por onde ela
     * podia ter saído. Do lado de fora, "não tem construtor", "o jogo não
     * tem essa casa" e "não há lote" eram o mesmo silêncio. É o §11: a
     * linha que expõe o defeito precisa existir antes de alguém
     * desconfiar dele.
     *
     * <p>A memória do último motivo, que morava aqui, virou
     * {@link IdleLog} em 2026-08-15 — o lenhador e o fabricante
     * precisavam da mesma regra.
     */
    private static final String SUBJECT = "building";

    /**
     * A casa pequena, lida do disco uma vez por sessão.
     *
     * <p>Esquecida ao parar o servidor, junto com o resto — ver
     * {@link #clearAll()}.
     */
    private static Optional<Blueprint> smallHouse;

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

    private ConstructionPlanner() {
    }

    /**
     * Registra por que não houve obra, uma vez por motivo.
     *
     * <p>A memória do último motivo saiu daqui em 2026-08-15 e virou
     * {@link IdleLog}: o lenhador e o fabricante precisavam da mesma
     * regra, e ela estava escrita só aqui. O que ficou é a tradução das
     * cinco recusas desta fase para o vocabulário do {@link IdleReason}.
     *
     * @return sempre vazio, para servir de {@code return} das recusas
     */
    private static Optional<ConstructionProject> silent(
            Colony colony, IdleReason why, String detail) {

        IdleLog.record(colony.id(), SUBJECT, why, detail);

        return Optional.empty();
    }

    /** Esquece o motivo guardado. Chamado ao parar o servidor. */
    public static void clearAll() {
        IdleLog.clearAll();

        WAITING_SINCE.clear();

        smallHouse = null;
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
    private static void wakeIfSupplied(ServerWorld world, ConstructionProject project) {
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
    private static boolean giveUpIfStalled(
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

    /**
     * Garante que a obra aberta tenha uma tarefa por onde alguém a pegue.
     *
     * <p><b>É o defeito que a sessão de 2026-08-15 achou, e o último do
     * MVP.</b> A obra existia, o construtor existia com baú, e as duas
     * casas ficaram em {@code 151 blocks left} por cinco horas e quarenta
     * minutos com {@code 0 working}. A corrente arrebentava aqui: nada em
     * produção criava tarefa de construção.
     *
     * <p>{@code tasks.create} só era chamado de {@code ColonyCycle
     * .requestMissing}, que traduz falta de recurso em tarefa; e
     * {@code typeFor} só devolve {@code BUILD} para recurso de categoria
     * {@code CONSTRUCTION}, que <b>nenhum {@code ResourceType} tem</b>.
     * A tarefa de obra era estruturalmente impossível, e por isso
     * {@code BuilderWork} nunca teve o que fazer — em vila nenhuma.
     *
     * <p>Os 82 testes verdes não pegaram porque {@code BuilderGameTest}
     * criava a tarefa à mão. É o §11 pela segunda vez, com a mesma frase
     * que o E10 rendeu: a pergunta não é "este código funciona?", é
     * <em>"quem põe esta coisa aqui, em jogo?"</em>.
     *
     * <p>Quem põe passou a ser este método, e não o ciclo: a obra não é
     * uma falta de recurso — é um projeto aberto precisando de mão. O
     * ciclo continua dono do que nasce de falta, e a vida desta tarefa
     * pertence ao projeto. Ver {@code TaskType.isResourceRequest}.
     *
     * <p>Uma tarefa por vez, e só enquanto a obra está em
     * {@link ConstructionState#BUILDING}: em {@code WAITING_RESOURCES} não
     * há o que colocar, e abrir tarefa ali poria um construtor a andar
     * até um canteiro para não fazer nada.
     */
    private static void ensureTask(Colony colony, ConstructionProject project) {
        if (project.state() != ConstructionState.BUILDING) {
            return;
        }

        for (Task task : VillageColonyMod.TASKS.ofColony(colony.id())) {
            if (task.type() == TaskType.BUILD && task.isOpen()) {
                return;
            }
        }

        int blocks = project.remainingCount();

        if (blocks == 0) {
            // A obra acabou e ainda não foi encerrada. Quem a fecha é o
            // construtor ao pôr o último bloco; abrir tarefa para zero
            // blocos seria ocupar uma mão por nada — e Task.create
            // recusaria, com razão.
            return;
        }

        VillageColonyMod.TASKS.create(
                colony.id(),
                TaskType.BUILD,
                TaskPriority.CONSTRUCTION,
                // Nominal, e é seguro que seja: a tarefa de obra não é
                // pedido de recurso, e quem paga cada bloco é
                // BuilderWork.takeMaterial, lendo o projeto. O que este
                // campo carrega de útil é o número — quantos blocos
                // faltam —, que aparece no log.
                ResourceType.OAK_PLANKS,
                blocks);

        VillageColonyMod.LOGGER.info(
                "Colony {} opened a build task — {} blocks left of {}",
                colony.id(),
                blocks,
                project.blueprint().id());
    }

    /**
     * Decide, se for o caso, a próxima obra desta colônia.
     *
     * @return a obra recém-planejada, quando nasce uma agora
     */
    public static Optional<ConstructionProject> plan(ServerWorld world, Colony colony) {
        resume(world, colony);

        Optional<ConstructionProject> open = VillageColonyMod.CONSTRUCTIONS.openOf(colony.id());

        if (open.isPresent()) {
            wakeIfSupplied(world, open.get());

            // A obra que esperou demais sai da frente, e o planejamento
            // segue nesta mesma passagem: fazer a colônia esperar mais um
            // ciclo depois de já ter esperado vinte não serve a ninguém.
            if (!giveUpIfStalled(world, colony, open.get())) {
                ensureTask(colony, open.get());

                return silent(colony, IdleReason.ALREADY_OPEN, "");
            }
        }

        int builders = WorkAssignment.countCapableOf(
                colony.id(), TaskType.BUILD.required(), VillageColonyMod.WORKERS);

        if (builders == 0) {
            // Sem detalhe: a frase do motivo já diz "no worker in the
            // village can do it", e o assunto da linha já diz que o
            // trabalho é de construção. Repetir "no builder" aqui só
            // alonga a linha.
            return silent(colony, IdleReason.NO_WORKER, "");
        }

        // A planta provisória serve só para medir: o tamanho da cabana
        // não muda com a parede em que a porta fica, e é o tamanho que a
        // busca de lote precisa saber antes de haver lote.
        // A planta desta vila. Em planície é a casa pequena do próprio
        // jogo, por decisão do autor em 2026-08-19; nos outros biomas
        // continua a cabana do mod, na madeira do bioma (Regra 20).
        List<Blueprint> plans = plansFor(world, colony);

        Blueprint blueprint = plans.get(0);

        Optional<BuildSiteScanner.Site> site = BuildSiteScanner.find(
                world, colony.id(), colony.center(), VillageDetector.SEARCH_RADIUS,
                plans.stream().map(Blueprint::size).toList());

        if (site.isEmpty()) {
            // Duas respostas, e a diferença importa: uma diz que não há
            // lote, a outra diz que ninguém terminou de olhar. A linha
            // anterior dizia a primeira nos dois casos, e no segundo isso
            // era mentira — ver o E14 do §17.
            //
            // Sem o número do anel de propósito: IdleLog só registra
            // quando o motivo muda, e um anel diferente por ciclo faria a
            // linha voltar toda vez.
            return BuildSiteScanner.sweepPausedAt(colony.id()).isPresent()
                    ? silent(colony, IdleReason.SWEEP_INCOMPLETE, "looking for a lot")
                    : silent(
                            colony,
                            IdleReason.NO_TARGET,
                            "no free lot beside a road in the whole "
                                    + VillageDetector.SEARCH_RADIUS + "-block radius of "
                                    + colony.center() + " that fits "
                                    + blueprint.size());
        }

        if (VillageColonyMod.BUILDINGS.isColonyInfrastructure(site.get().origin())) {
            // O lote caiu sobre casa que a própria colônia levantou. O
            // scanner não conhece o registro de construções — ele olha o
            // mundo, e uma casa de madeira sobre terra continua parecendo
            // terreno pelo topo do bloco. A próxima passagem tenta outro
            // anel.
            return silent(
                    colony,
                    IdleReason.SITE_REFUSED,
                    "the lot at " + site.get().origin() + " falls on a colony building");
        }

        // A planta que coube naquele lote — a maior das oferecidas que
        // serviu ali. Decisão do autor de 2026-08-20.
        Blueprint chosen = plans.stream()
                .filter(plan -> plan.size().equals(site.get().size()))
                .findFirst()
                .orElse(blueprint);

        // Agora que há lote, a planta é virada para a rua: é a Regra 17,
        // e o lado sai de quem achou o lote.
        Blueprint facingTheRoad = turnedToTheRoad(
                chosen, MinecraftTypeAdapter.toSide(site.get().doorSide()));

        ConstructionProject project = ConstructionProject.plan(
                colony.id(), facingTheRoad, site.get().origin());

        VillageColonyMod.CONSTRUCTIONS.register(project);

        // PREPARING deixou de passar em branco em 2026-08-19. O lote é
        // escolhido sem bloco sólido dentro (Regra 22), mas grama alta e
        // flor não reprovam lote nenhum — e o miolo da planta não põe
        // bloco, então elas ficariam dentro da casa para sempre. Quem as
        // tira é a preparação.
        project.moveTo(ConstructionState.PREPARING);

        SitePreparation.clear(world, project);

        project.moveTo(ConstructionState.BUILDING);

        ensureTask(colony, project);

        IdleLog.clear(colony.id(), SUBJECT);

        VillageColonyMod.LOGGER.info(
                "Colony {} planned {} at {} — {} blocks, {} builders",
                colony.id(),
                project.blueprint().id(),
                project.origin(),
                project.blueprint().blockCount(),
                builders);

        return Optional.of(project);
    }

    /**
     * Faz renascer a obra que o save trouxe.
     *
     * <p>O save guarda identidade, estrutura, lugar e estado — e não o
     * progresso. **Quem sabe o que já está de pé é o mundo**, e é a ele
     * que se pergunta: cada bloco do projeto cujo lugar já contém o bloco
     * certo sai da lista.
     *
     * <p>Sai mais barato no disco e sai mais certo. Uma lista de posições
     * gravada juraria que a parede está lá; se o jogador a derrubou entre
     * uma sessão e outra, a colônia a levanta de novo — e essa é a
     * resposta que se quer.
     *
     * <p>Custa uma leitura de bloco por peça do projeto, uma vez por
     * colônia por sessão. Cento e cinquenta leituras de vetor no primeiro
     * ciclo, e nada depois.
     *
     * <p>Roda dentro de {@link #plan}, antes de tudo: uma obra que voltou
     * do save é uma obra aberta, e planejar outra por cima dela abriria
     * dois canteiros na mesma vila.
     */
    private static void resume(ServerWorld world, Colony colony) {
        Optional<ConstructionService.Pending> pending =
                VillageColonyMod.CONSTRUCTIONS.pendingOf(colony.id());

        if (pending.isEmpty()) {
            return;
        }

        ConstructionService.Pending saved = pending.get();

        Optional<Blueprint> blueprint = blueprintOf(world, saved.blueprint(), saved.origin());

        if (blueprint.isEmpty()) {
            // O jogo não conhece mais essa estrutura — datapack que saiu,
            // versão que mudou. Desistir da obra é melhor que tentar a
            // cada ciclo: a casa pela metade fica no mundo, e o lote
            // ocupado impede a colônia de construir por cima dela.
            VillageColonyMod.LOGGER.warn(
                    "Colony {} had a project of {}, which this game no longer has — dropped",
                    colony.id(),
                    saved.blueprint());

            VillageColonyMod.CONSTRUCTIONS.dropPending(colony.id());

            return;
        }

        ConstructionProject project = ConstructionProject.restore(
                saved.id(), saved.colonyId(), blueprint.get(), saved.origin(), saved.state());

        // O jogador pode ter plantado no canteiro entre uma sessão e
        // outra — a Regra 23: o que já foi olhado se olha de novo.
        SitePreparation.clear(world, project);

        int standing = 0;

        for (BlueprintBlock block : project.blueprint().blocks()) {
            BlockPos where = MinecraftTypeAdapter.toBlockPos(project.worldPositionOf(block));

            Optional<Block> expected = MinecraftTypeAdapter.toBlock(block.block());

            if (expected.isPresent() && world.getBlockState(where).isOf(expected.get())) {
                project.markPlaced(block);

                standing++;
            }
        }

        ResourceId target = houseFor(world, colony).id();

        if (project.isSupersededBy(target)) {
            // Obra de uma planta que não é mais o alvo, e sem um bloco de
            // pé. Nada se perde ao abandoná-la — e mantê-la trava a
            // colônia para sempre, porque `plan` não abre obra nova
            // enquanto houver uma aberta.
            //
            // Foi o que a sessão das 22:01 de 2026-08-15 mostrou. A Regra
            // 13 trocou a obra do MVP pela cabana, e a colônia continuou
            // presa à casa de planície gravada no save: quinze ciclos de
            // "waiting for minecraft:stripped_oak_log", que ninguém
            // produz. A cabana nunca chegou a ser planejada.
            //
            // **E aconteceu de novo, ao contrário.** A correção daquele
            // dia perguntava se a planta era a cabana, com o id escrito
            // no código. A Regra 24 devolveu a casa do jogo às vilas de
            // planície em 2026-08-19, e a pergunta passou a proteger
            // exatamente a obra que devia sair: a cabana gravada no save
            // era retomada, e o alvo novo nunca chegava a ser planejado.
            // Por isso o alvo agora é perguntado à colônia — `houseFor` é
            // a mesma resposta que `plan` usa uma linha abaixo.
            //
            // Com bloco de pé é o contrário: casa pela metade é do
            // jogador, e abandoná-la deixaria um esqueleto no mundo com o
            // lote ocupado. Essa continua de onde parou.
            VillageColonyMod.LOGGER.info(
                    "Colony {} drops the untouched {} — the target is now {}",
                    colony.id(),
                    project.blueprint().id(),
                    target);

            VillageColonyMod.CONSTRUCTIONS.dropPending(colony.id());

            return;
        }

        VillageColonyMod.CONSTRUCTIONS.register(project);
        VillageColonyMod.CONSTRUCTIONS.dropPending(colony.id());

        VillageColonyMod.LOGGER.info(
                "Colony {} resumed {} at {} — {} blocks already standing, {} to go",
                colony.id(),
                project.blueprint().id(),
                project.origin(),
                standing,
                project.remainingCount());
    }

    /**
     * Quantas tábuas a obra em curso ainda pede.
     *
     * <p>É o número que a Regra 5 usa para substituir a metade do
     * armazém. Zero quando não há obra — e aí volta a valer a metade.
     */
    public static int planksNeededBy(ResourceId planks, Colony colony) {
        return VillageColonyMod.CONSTRUCTIONS.openOf(colony.id())
                .map(project -> project.remainingMaterials().getOrDefault(planks, 0))
                .orElse(0);
    }

    /**
     * A casa que esta vila levanta.
     *
     * <p>Decidido pelo autor em 2026-08-19: <b>vila de planície constrói
     * a casa pequena do próprio jogo</b>, e não mais a cabana do mod. O
     * arquivo dela é um schema do mod — ver
     * {@code data/villagecolony/structure/houses/} —, então não depende
     * de o jogo continuar gerando aquela peça com aquele nome.
     *
     * <p>Nos outros biomas continua a cabana, na madeira do bioma. Não é
     * esquecimento: a casa de cada bioma existe no catálogo e ainda não
     * foi escolhida uma por bioma, e o autor pediu "por hora, em testes,
     * só a casa básica pequena".
     *
     * <p><b>O que isso custa, e é preciso dizer.</b> A casa do jogo pede
     * 43 pedregulhos, 16 troncos descascados e 3 vidraças, e a colônia
     * não minera, não funde e não descasca. Pela segunda metade da
     * Regra 13 a obra não é impossível — o jogador guarda no baú o que a
     * colônia não faz, e o construtor tira dali —, mas ela <b>não sobe
     * sozinha</b> como a cabana subia. O relatório diz o que falta, uma
     * peça por vez.
     */
    private static Blueprint houseFor(ServerWorld world, Colony colony) {
        return plansFor(world, colony).get(0);
    }

    /**
     * O que esta colônia sabe levantar, da maior planta para a menor.
     *
     * <p><b>Por que é uma lista desde 2026-08-20.</b> A vila do autor
     * varreu o raio de 64 inteiro sem achar lugar para a casa de
     * planície, tendo três cabanas de pé ali dentro: 49 colunas no nível
     * exato da rua pedem muito mais espaço que 25, e a vila parou de
     * crescer. Exigir a planta grande em toda parte era transformar a
     * Regra 24 num travamento.
     *
     * <p>A cabana fecha a lista sempre, e é de propósito: ela é a planta
     * que a colônia levanta sozinha, sem o jogador guardar nada em baú.
     * Enquanto ela couber em algum lugar, a vila continua crescendo — que
     * é a Regra 13 outra vez, agora sobre espaço em vez de material.
     *
     * <p>Fora da planície a lista tem um item só: a casa do jogo é de
     * planície, e a Regra 20 manda a cabana ser da madeira do bioma.
     */
    private static List<Blueprint> plansFor(ServerWorld world, Colony colony) {
        ResourceId wood = VillageBiomes.woodAt(world, colony.center())
                .orElse(ColonyHut.OAK_PLANKS);

        Blueprint hut = ColonyHut.blueprint(wood, Side.NORTH);

        if (!ColonyHut.OAK_PLANKS.equals(wood)) {
            return List.of(hut);
        }

        return smallHouse(world)
                .map(house -> List.of(house, hut))
                .orElseGet(() -> List.of(hut));
    }

    /**
     * A casa pequena, lida uma vez e guardada.
     *
     * <p>Ler um template é abrir e decodificar um arquivo de trezentos e
     * quarenta e três blocos, e o ciclo pergunta pela planta a cada
     * passagem. O aviso está no cabeçalho de
     * {@code StructureBlueprintReader}: quem chama guarda o resultado.
     */
    private static Optional<Blueprint> smallHouse(ServerWorld world) {
        if (smallHouse == null) {
            smallHouse = StructureBlueprintReader.read(
                    world, StructureBlueprintReader.SMALL_HOUSE);
        }

        return smallHouse;
    }

    /**
     * A planta virada para a rua — a Regra 17, agora por giro.
     *
     * <p>A cabana do mod é quadrada e resolvia a porta mudando duas
     * coordenadas. A casa do jogo não: a porta está onde o arquivo a
     * pôs — a um bloco da parede oeste, na casa de planície —, e a única
     * forma de virá-la para a rua é girar a planta inteira.
     *
     * <p>Planta sem porta passa reta: cerca e poço não têm por onde
     * entrar, e girá-los não faria diferença nenhuma.
     */
    private static Blueprint turnedToTheRoad(Blueprint house, Side road) {
        return house.doorSide()
                .map(door -> house.rotated(door.turnsTo(road)))
                .orElse(house);
    }

    /**
     * A planta deste id, venha ela do mod ou do jogo.
     *
     * <p>Existe para {@link #resume}, que carrega obra gravada em sessão
     * anterior e só tem o id em mãos. {@link HouseFurnishing} usa a
     * mesma resposta pelo mesmo motivo: uma casa terminada guarda o id e
     * o canto, e onde a mobília dela vai está na planta — girada como a
     * casa foi levantada, que é o que este método reconstrói. A cabana da colônia é escrita em
     * código e o leitor de estrutura não a acharia; a casa do jogo é o
     * contrário. Perguntar aos dois é o que deixa um save antigo — com a
     * casa de planície pela metade — continuar de onde parou.
     */
    static Optional<Blueprint> blueprintOf(
            ServerWorld world, ResourceId id, ColonyPos origin) {

        if (ColonyHut.ID.equals(id)) {
            // A parede da porta é perguntada ao mundo, e não ao save —
            // ver BuildSiteScanner.roadSideOf. Sem rua em volta, a casa
            // fica com a porta ao norte, que é onde a planta antiga a
            // punha: obra de save velho continua de onde parou.
            ResourceId wood = VillageBiomes.woodAt(world, origin)
                    .orElse(ColonyHut.OAK_PLANKS);

            return Optional.of(ColonyHut.blueprint(
                    wood, roadSideOf(world, origin, ColonyHut.blueprint(wood, Side.NORTH))));
        }

        // Planta lida de arquivo: ela volta como o arquivo a gravou, e
        // precisa ser virada de novo para a rua. Sem isto a obra que
        // volta do save mede o mundo com a planta na orientação errada,
        // conclui que nada está de pé e reconstrói por cima, torto.
        return StructureBlueprintReader.read(world, id)
                .map(house -> turnedToTheRoad(house, roadSideOf(world, origin, house)));
    }

    /**
     * Para que lado fica a rua desta obra, lida do mundo.
     *
     * <p>O lado não é gravado no save de propósito: ele é uma leitura do
     * mundo, e o mundo é a única fonte que continua certa depois de o
     * jogador mexer nele. Sem rua em volta — o jogador arrancou o
     * caminho —, fica o norte, que é onde a planta antiga punha a porta.
     */
    private static Side roadSideOf(ServerWorld world, ColonyPos origin, Blueprint house) {
        return BuildSiteScanner.roadSideOf(world, origin, house.size())
                .map(MinecraftTypeAdapter::toSide)
                .orElse(Side.NORTH);
    }
}
