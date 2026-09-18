package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.service.VillageDetector;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.construction.service.ConstructionService;
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
import com.villagecolony.fabric.integration.VillageRoad;
import com.villagecolony.fabric.integration.RoadExtension;
import com.villagecolony.fabric.integration.SweepLog;
import com.villagecolony.fabric.integration.SitePreparation;
import net.minecraft.block.Block;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Predicate;

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
 *
 * <p><b>O que saiu daqui em 2026-08-20</b>, quando este arquivo passou
 * de setecentas linhas. Eram três perguntas independentes morando
 * juntas, e cada uma virou um arquivo com nome:
 *
 * <pre>
 * {@link HousePlans}    qual planta, e virada para que lado
 * {@link WaitingWork}   a obra que espera material: acordar ou largar
 * </pre>
 *
 * <p>O que ficou é a decisão de abrir obra: há construtor, há lote, e
 * então nasce o projeto e a tarefa por onde alguém o pega. Mais a
 * retomada do save, que é a mesma decisão vista de trás para frente.
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
     * Até onde a colônia procura lote. É o raio da vila, menos nos testes.
     *
     * <p>A bateria roda arenas lado a lado no mesmo mundo, e uma
     * varredura de 64 blocos sai da arena e acha a rua do teste vizinho.
     * E há um motivo prático junto: 64 são dezessete passagens de mil
     * colunas, e a Regra 15 só age quando a varredura <b>termina</b> —
     * um teste que quisesse ver a rua crescer teria de rodar as
     * dezessete.
     */
    private static int searchRadius = VillageDetector.SEARCH_RADIUS;

    /** Encurta a busca de lote. Só os testes precisam disso. */
    public static void shortenSearchTo(int blocks) {
        if (blocks <= 0) {
            throw new IllegalArgumentException("Radius must be positive: " + blocks);
        }

        searchRadius = blocks;
    }

    /** Devolve o raio ao valor de jogo. */
    public static void restoreSearch() {
        searchRadius = VillageDetector.SEARCH_RADIUS;
    }

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
            WaitingWork.wakeIfSupplied(world, open.get());

            // <b>E a obra que o centro deixou para trás</b> — 2026-09-15.
            // Vem antes do relógio de paciência porque não é caso dele: ele
            // só conta para WAITING_RESOURCES, e esta obra fica em BUILDING
            // para sempre, calada, com a vaga única ocupada. Ver
            // ConstructionProject.isOutOfReach, que traz a aritmética do log
            // do autor.
            //
            // <b>E quem responde "alcançável" é a rua, não o centro</b> —
            // E46, 2026-09-16. A rua cresce pela ponta mais distante do
            // centro, de propósito, e medir do centro largava a obra que a
            // própria estrada acabara de alcançar. Sem índice de ruas a
            // pergunta cai no centro, que é o comportamento de antes.
            OptionalInt toTheRoad = BuildSiteScanner.roadsOf(colony.id())
                    .map(roads -> roads.blocksToTheNearestRoad(open.get().origin()))
                    .orElseGet(OptionalInt::empty);

            if (ConstructionProject.isOutOfReach(
                    open.get().origin(), colony.center(), searchRadius, toTheRoad)) {

                VillageColonyMod.LOGGER.info(
                        "Colony {} lets go of {} at {} — the village centre is at {},"
                                + " the radius is {}, and the nearest road is {}."
                                + " The half-built house and its lot stay taken",
                        colony.id(),
                        open.get().blueprint().id(),
                        open.get().origin(),
                        colony.center(),
                        searchRadius,
                        toTheRoad.isPresent()
                                ? toTheRoad.getAsInt() + " blocks away"
                                : "not indexed yet");

                // A planta nao leva a culpa: nao faltou material, a obra ficou
                // longe. Ver WaitingWork.giveUp(colony, project, blamePlan).
                WaitingWork.giveUp(colony, open.get(), false);
            } else if (!WaitingWork.giveUpIfStalled(world, colony, open.get())) {
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

        // As plantas desta vila, da maior para a menor — a Regra 25 —, e
        // todas do catálogo do jogo, que é a Regra 27. O mod não inventa
        // casa: se a lista vier vazia, não há o que construir, e dizê-lo
        // é melhor que levantar algo que ninguém pediu.
        // <b>Roça quando o fazendeiro não tem campo</b> — decisão do
        // autor, 2026-09-05: "precisam construir o espaço de plantação
        // padrão e idêntico aos que já vêm na vila do Minecraft" e
        // "precisam de um espaço livre dentro da vila e não colado em
        // outra estrutura".
        //
        // A segunda exigência não custa nada: é exatamente o que a busca
        // de lote abaixo já garante para a casa, e a roça passa pela
        // mesma porta. Nenhuma regra de espaçamento foi escrita duas
        // vezes.
        //
        // <b>Quantas, é a população que diz</b> — decisão do autor,
        // 2026-09-05: "zona de plantação criada a cada 15 aldeões
        // existentes na vila".
        //
        // O pedido vinha do fazendeiro — "varri o raio e não achei
        // campo" — e um pedido assim não tem teto: a sessão das 21:17
        // levantou duas roças em quatro minutos porque a primeira nasceu
        // longe demais para ele ver. A cota fecha isso por construção.
        List<Blueprint> plans = List.of();

        // <b>E a roça que já não coube cede a vez</b> — 2026-09-09. Sem
        // esta segunda pergunta a colônia repetia a mesma recusa para
        // sempre: a vila de 09-09 gastou uma hora em 108 passagens sem
        // abrir obra nenhuma. Ver FarmPlans.postponed.
        if (FarmPlans.owedToThePopulation(colony.id())
                && !FarmPlans.postponed(colony.id(), world.getTime())) {

            plans = FarmPlans.plansFor(world, colony);
        }

        if (plans.isEmpty()) {
            plans = HousePlans.plansFor(world, colony);
        }

        if (plans.isEmpty()) {
            return silent(
                    colony,
                    IdleReason.NOT_IN_GAME,
                    "this game has no village house for the " + colony.id() + " style");
        }

        Blueprint blueprint = plans.get(0);

        // <b>Roça não sai atrás da estrada</b> — 2026-09-05, visto em
        // jogo. A primeira roça da colônia nasceu em {x=1517, z=113} com
        // o centro em {x=1435, z=47}: <b>105 blocos</b>, porque o lote
        // veio da ponta da estrada que a vila estava esticando. O
        // fazendeiro procura lavoura a 32 do centro, então ele nunca a
        // veria — e a linha do log logo depois de ela ficar pronta era
        // exatamente "no empty plot within 32 blocks of the village".
        //
        // Pior: o pedido de roça nunca se fechava, e a colônia levantou
        // <b>duas</b> em quatro minutos, a caminho de encher o mapa.
        //
        // A extensão de rua existe para a vila <b>crescer</b>, e casa
        // nova na ponta é o que ela quer. Roça é o contrário: o autor
        // pediu "um espaço livre <b>dentro da vila</b>", e a varredura em
        // anéis a partir do centro já devolve o lote livre mais perto.
        boolean farming = FarmPlans.isFarm(blueprint.id());

        // A ponta que já rendeu continua rendendo, e isso vem antes da
        // varredura — 2026-08-26. Sem isto a colônia pagava dezessete
        // ciclos por bloco de rua, e a sessão das 03:11 mediu o custo:
        // um bloco calçado, e o lote de sete por sete continuou sem
        // caber. Quem autorizou a rua a crescer foi a varredura que
        // terminou sem lote, e essa autorização vale para o trecho.
        if (!farming && RoadExtension.isGrowing(colony.id())) {
            Optional<ConstructionProject> onTheStretch =
                    keepGrowing(world, colony, blueprint, plans, builders);

            if (onTheStretch.isPresent() || RoadExtension.isGrowing(colony.id())) {
                // Ou nasceu lote no trecho novo, ou a passagem foi gasta
                // crescendo. Nos dois casos a varredura espera.
                return onTheStretch;
            }

            // A ponta parou de render. A varredura volta a mandar, e
            // nesta mesma passagem.
        }

        // O denominador da conta do SweepLog: quantas vezes o planejador
        // chegou a pedir lote. Obra já aberta, falta de construtor e
        // planta ausente saem antes daqui e não são dívida da varredura.
        SweepLog.asked(colony.id());

        Optional<BuildSiteScanner.Site> site = BuildSiteScanner.find(
                world, colony.id(), colony.center(), searchRadius,
                sizesOf(plans));

        if (site.isEmpty()) {
            // Duas respostas, e a diferença importa: uma diz que não há
            // lote, a outra diz que ninguém terminou de olhar. A linha
            // anterior dizia a primeira nos dois casos, e no segundo isso
            // era mentira — ver o E14 do §17.
            //
            // Sem o número do anel de propósito: IdleLog só registra
            // quando o motivo muda, e um anel diferente por ciclo faria a
            // linha voltar toda vez.
            // Os dois jeitos de procurar contam — 2026-09-11. Esta
            // linha perguntava só pelo cursor do quadrado, e desde que a
            // volta pelo índice de ruas também pode parar no meio, ela
            // deixaria a Regra 15 crescer a rua sem ninguém ter visto o
            // raio inteiro. Ver BuildSiteScanner.stillLookingForALot.
            if (BuildSiteScanner.stillLookingForALot(colony.id())) {
                return silent(colony, IdleReason.SWEEP_INCOMPLETE, "looking for a lot");
            }

            // A Regra 15, e é aqui que ela cabe: a varredura terminou o
            // raio inteiro e não há beira de rua livre. Antes desta
            // linha a vila parava para sempre — a rua era algo que a
            // colônia encontrava, e nunca algo que ela produzia.
            //
            // O lote novo não nasce nesta passagem de propósito: a
            // varredura seguinte é que vai encontrá-lo, e ela recomeça
            // do centro no ciclo que vem. Trinta segundos, e a ordem da
            // regra fica respeitada — estrada primeiro, casa depois.
            return extendTheRoad(world, colony, blueprint, plans, builders);
        }

        // A recusa de lote sobre casa da colônia morava aqui, e daqui não
        // funcionava. O comentário dizia "a próxima passagem tenta outro
        // anel", e era falso: achar um lote apaga o cursor da varredura,
        // então a passagem seguinte recomeçava do centro e reencontrava o
        // mesmo lugar. Em 2026-08-20 a vila do autor ficou nesse laço.
        //
        // A pergunta desceu para `BuildSiteScanner.isClearAbove`, que é
        // onde a varredura ainda pode seguir para o anel seguinte.

        if (farming && !withinTheFarmersReach(colony, site.get())) {
            // Lote livre, mas longe demais: o fazendeiro procura lavoura
            // a FarmerWork.reach() do centro, e roça que ele não vê é
            // roça que ninguém planta — e que não fecha o pedido, então
            // a colônia levantaria outra, e outra.
            //
            // <b>E a recusa não pode parar a vila</b> — 2026-09-09. Ela
            // encerrava a passagem inteira, e como o lote de amanhã é o
            // mesmo de hoje, a colônia repetia a recusa para sempre sem
            // nunca tentar uma casa: uma hora de jogo, 108 passagens do
            // planejador, nenhuma obra aberta, e o autor sem ver
            // trabalhador nenhum trabalhando. A roça cede a vez por
            // vinte ciclos e as casas passam — ver FarmPlans.postponed.
            FarmPlans.postpone(colony.id(), world.getTime());

            return silent(
                    colony,
                    IdleReason.NO_TARGET,
                    "the only free lot is outside the farmer's reach"
                            + " — the houses go first for now");
        }

        return open(world, colony, site.get(), plans, blueprint, builders);
    }

    /**
     * Os tamanhos que a varredura precisa testar, sem repetição —
     * 2026-09-09.
     *
     * <p>Cada coluna candidata é testada contra <b>cada</b> tamanho desta
     * lista, e o catálogo do jogo repete muito a pegada: em planície são
     * dezenas de casas com um punhado de tamanhos distintos. Passar a
     * lista crua faria a mesma medida ser refeita dezenas de vezes por
     * coluna, e a varredura tem teto de colunas por passagem — o custo
     * sairia do raio que ela alcança, e não do relógio.
     *
     * <p>Não existia antes porque não fazia falta: até 09-09 a colônia
     * recebia uma casa por estilo, e uma lista de um elemento não tem o
     * que deduplicar. Ela nasce junto com o catálogo inteiro.
     *
     * <p>A ordem se mantém — maior primeiro, que é a Regra 25 —, e é ela
     * que decide o tamanho do lote. A variedade entra depois, entre as
     * plantas que empatam nesse tamanho; ver {@link #open}.
     */
    private static List<ColonyPos> sizesOf(List<Blueprint> plans) {
        // <b>E a pegada girada também</b> — 2026-09-16. A Regra 17 gira a
        // planta para a porta olhar a rua, e numa planta retangular o giro
        // troca os eixos: a 13×11 do arquivo pode precisar de um lote 11×13.
        //
        // Passar só a pegada do arquivo fazia o scanner procurar o lote
        // errado, e o filtro do {@code open} — que hoje compara depois do
        // giro — não acharia planta nenhuma para um lote 13×11 quando
        // todas viram 11×13. A vila deixaria de construir em silêncio.
        //
        // Ambas as orientações entram, e a de origem primeiro: a Regra 25
        // manda tentar a maior antes, e a ordem da lista é que decide.
        List<ColonyPos> sizes = new ArrayList<>();

        for (Blueprint plan : plans) {
            ColonyPos size = plan.size();

            if (!sizes.contains(size)) {
                sizes.add(size);
            }

            ColonyPos turned = new ColonyPos(size.z(), size.y(), size.x());

            if (!sizes.contains(turned)) {
                sizes.add(turned);
            }
        }

        return List.copyOf(sizes);
    }

    /**
     * O lote vira obra aberta.
     *
     * <p>Separado de {@link #plan} em 2026-08-25, quando passou a haver
     * <b>dois</b> caminhos até um lote: a varredura em anéis, e o atalho
     * de quem acabou de calçar a rua e sabe onde nasceu beira nova. O
     * trecho é o mesmo nos dois, e duas cópias dele seriam duas versões
     * da Regra 17.
     */
    private static Optional<ConstructionProject> open(
            ServerWorld world, Colony colony, BuildSiteScanner.Site site,
            List<Blueprint> plans, Blueprint blueprint, int builders) {

        // A planta que coube naquele lote — a maior das oferecidas que
        // serviu ali. Decisão do autor de 2026-08-20.
        //
        // <b>E entre as que empatam no tamanho, o sorteio</b> — decisão
        // do autor, 2026-09-09: <i>"criando uma aleatoriedade simples
        // para todas as construções possíveis do bioma"</i>.
        //
        // O sorteio não disputa com o "maior primeiro", e é por isso que
        // ele mora aqui e não na escolha do lote: quem decide o tamanho é
        // a varredura, que testa cada coluna da planta maior para a
        // menor e já devolve o lote com o tamanho que venceu. O que
        // sobra para sortear são as plantas <b>daquele mesmo tamanho</b>
        // — e em planície são muitas, porque o catálogo do jogo repete a
        // pegada em casas de aparência bem diferente.
        //
        // {@code findFirst} sempre devolvia a mesma, então a colônia
        // levantava a mesma casa a vida inteira: a sessão de 09-09 subiu
        // {@code plains_small_house_1} outra vez, que era a única que a
        // barreira daquele dia deixava passar.
        // <b>Girada ANTES de conferir se cabe</b> — 2026-09-16, e esta
        // ordem é o conserto de um defeito que o autor viu em jogo: <i>"a
        // segunda construção acavalou em cima de uma fazenda da vila,
        // então a verificação do local para construir deve ter dado
        // erro"</i>.
        //
        // <b>O que acontecia.</b> O filtro comparava o tamanho da planta
        // <b>antes</b> do giro, e a Regra 17 girava depois. Numa planta
        // retangular o giro de 90° troca os eixos — ver
        // {@code BlueprintRotationTest}: a casa 13×11 aprovada num lote
        // 13×11 virava 11×13 e ocupava treze blocos de profundidade onde
        // só onze foram verificados.
        //
        // O excedente caía em terreno que ninguém olhou. No log de 03:23
        // caiu na roça: o açougue foi planejado em 2503,63,-3045 e o
        // construtor riscou onze posições de farmland catorze blocos
        // adiante, com a linha "Block{minecraft:farmland} is in the way".
        // A comida da vila virou piso de casa.
        //
        // Girar primeiro faz o filtro ver a pegada que a obra <b>vai</b>
        // ocupar, e não a que ela tinha no arquivo.
        Side road = MinecraftTypeAdapter.toSide(site.doorSide());

        // <b>E o sorteio de 09-09 finalmente tem o que sortear</b> —
        // 2026-09-18. Ele nasceu para acabar com "a colônia levanta a
        // mesma casa a vida inteira", e não acabou: a lista que chega
        // aqui vem do {@code catalogPlans}, que corta as pegadas
        // repetidas <b>antes</b>. Sortear entre plantas de tamanhos todos
        // distintos, filtradas pelo tamanho do lote, é sortear entre uma.
        //
        // As irmãs são pedidas agora, com o lote já achado e a pegada já
        // conhecida — ver HousePlans.siblingsOf, e por que ela não
        // encarece a varredura.
        // A deduplicação é por {@code id} e não por {@code distinct()}:
        // Blueprint é classe sem equals, então distinct compararia
        // referência e deixaria a planta oferecida duas vezes no sorteio
        // — com peso dobrado sobre as irmãs.
        List<Blueprint> candidates = new ArrayList<>(plans);

        candidates.addAll(HousePlans.siblingsOf(
                world, HousePlans.paletteOf(world, colony.center()).style(), site.size()));

        Set<ResourceId> seen = new HashSet<>();

        List<Blueprint> fitting = candidates.stream()
                .filter(plan -> seen.add(plan.id()))
                .map(plan -> HousePlans.turnedToTheRoad(plan, road))
                .filter(plan -> plan.size().equals(site.size()))
                .toList();

        Blueprint facingTheRoad = fitting.isEmpty()
                ? HousePlans.turnedToTheRoad(blueprint, road)
                : fitting.get(world.getRandom().nextInt(fitting.size()));

        // <b>Quantas concorreram, e é o §11</b> — 2026-09-18. O playtest
        // de 02:25 levantou duas plantas diferentes, e isso <b>não</b>
        // provou que o sorteio entre irmãs funcionou: as duas tinham
        // pegadas diferentes — 517 e 382 blocos —, então a variedade
        // podia ter vindo da lista de tamanhos que já existia antes, pela
        // Regra 25 descendo um degrau. Do lado de fora, "sorteou entre
        // oito" e "só havia uma" eram a mesma linha.
        //
        // A conta vai na linha do {@code planned}, logo abaixo, e não numa
        // segunda linha por obra: é a mesma decisão vista de um outro
        // ângulo, e duas linhas por obra seriam ruído no log de uma
        // sessão longa.
        //
        String drawnFrom = drawnFrom(fitting.size());

        ConstructionProject project = ConstructionProject.plan(
                colony.id(), facingTheRoad, site.origin());

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

        // <b>E a linha diz de onde o lote foi medido</b> — E46, 2026-09-16.
        // A obra que nasce aqui pode ser largada no ciclo seguinte pelo
        // guarda de alcance, e o log não permitia saber por quê: ele dizia
        // onde a casa ficava, e não a que distância do centro ela estava
        // nem por qual régua. Duas obras morreram assim no playtest de
        // 21:41, planejadas e abandonadas em trinta segundos, e descobrir
        // que o índice de ruas não tem teto de raio custou ler o scanner
        // inteiro.
        //
        // As duas contas saem juntas de propósito: é a divergência entre
        // elas que condena a obra, e vê-las lado a lado torna o defeito
        // legível na hora. Ver ConstructionProject.isOutOfReach, que usa a
        // euclidiana, e BuildSiteScanner, que varre em quadrado.
        VillageColonyMod.LOGGER.info(
                "Colony {} planned {} at {} — {} blocks, {} builders,"
                        + " drawn from {}."
                        + " Measured from {}: {} blocks square, {} blocks straight,"
                        + " and the radius is {}",
                colony.id(),
                project.blueprint().id(),
                project.origin(),
                project.blueprint().blockCount(),
                builders,
                drawnFrom,
                colony.center(),
                squareDistance(project.origin(), colony.center()),
                straightDistance(project.origin(), colony.center()),
                searchRadius);

        return Optional.of(project);
    }

    /**
     * De quantas plantas saiu a escolha, dito para quem lê o log.
     *
     * <p><b>Lista vazia não é "sorteou entre zero".</b> É o caminho de
     * reserva — nenhuma planta coube na pegada do lote e vale a que veio
     * de fora. Imprimir {@code 0} ali faria um caminho <b>diferente</b>
     * parecer um sorteio degenerado, que é justamente o tipo de silêncio
     * que este log existe para desfazer.
     *
     * <p><b>Visível ao pacote para o teste, e é o motivo de existir
     * separada.</b> A linha do {@code planned} <b>não sai na bateria de
     * jogo</b> — medido em 2026-09-18, zero ocorrências com e sem esta
     * mudança: os gametests montam o projeto sem passar pelo
     * {@code open}. Deixar a formatação embutida no {@code LOGGER.info}
     * seria deixá-la sem nenhuma verificação possível, e esta base já
     * pagou o preço de um trecho que nada exercitava.
     */
    static String drawnFrom(int fitting) {
        return fitting == 0
                ? "none fitting, the offered plan"
                : fitting + " of that footprint";
    }

    /**
     * A distância em quadrado, que é como a varredura mede — E46.
     *
     * <p>Chebyshev: o lado do menor quadrado centrado em {@code centre}
     * que contém {@code origin}. É a conta dos anéis do
     * {@code BuildSiteScanner} e a do {@link #withinTheFarmersReach}.
     */
    private static int squareDistance(ColonyPos origin, ColonyPos centre) {
        return Math.max(
                Math.abs(origin.x() - centre.x()),
                Math.abs(origin.z() - centre.z()));
    }

    /**
     * A distância em linha reta, que é como o guarda de alcance mede — E46.
     *
     * <p>Euclidiana, arredondada: a conta do
     * {@code ConstructionProject.isOutOfReach}. Quando ela passa do raio e
     * a {@link #squareDistance} não passa, a obra nasce condenada.
     */
    private static int straightDistance(ColonyPos origin, ColonyPos centre) {
        long dx = (long) origin.x() - centre.x();
        long dz = (long) origin.z() - centre.z();

        return (int) Math.round(Math.sqrt(dx * dx + dz * dz));
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

        Optional<Blueprint> blueprint = HousePlans.blueprintOf(
                world, colony.id(), saved.blueprint(), saved.origin());

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

        Optional<ResourceId> target = HousePlans.houseFor(world, colony).map(Blueprint::id);

        // <b>Roça não é versão velha de casa</b> — 2026-09-05. O alvo
        // desta pergunta é sempre uma casa, e o pedido de roça é estado
        // de memória que nasce vazio ao ligar o servidor: sem esta
        // ressalva, toda roça planejada e ainda intocada seria descartada
        // no primeiro carregamento do save. São propósitos diferentes, e
        // não duas plantas disputando o mesmo lugar.
        if (target.isPresent()
                && !FarmPlans.isFarm(project.blueprint().id())
                && project.isSupersededBy(target.get())) {
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
                    target.get());

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
     * Se este lote está onde o fazendeiro trabalha.
     *
     * <p>Medido em quadrado e a partir do centro da vila, que é
     * exatamente como {@code CropPatch.survey} varre: usar aqui uma
     * conta diferente da dele poria a roça na borda que ele nunca
     * alcança, e é esse o defeito que esta guarda existe para não
     * repetir.
     *
     * <p><b>Pública para o teste chamá-la</b>, e não por precisar de
     * fora. É o precedente do {@code MinerWork.footingIn}, e pelo mesmo
     * motivo: um teste que reimplementasse esta conta afirmaria a cópia
     * dele, e não a regra — foi assim que a correção do E32 passou sem
     * ninguém ver, em 2026-09-05.
     */
    public static boolean withinTheFarmersReach(Colony colony, BuildSiteScanner.Site site) {
        int dx = Math.abs(site.origin().x() - colony.center().x());
        int dz = Math.abs(site.origin().z() - colony.center().z());

        return Math.max(dx, dz) <= FarmerWork.reach();
    }

    /**
     * Quantas tábuas a obra em curso ainda pede.
     *
     * <p>É o número que a Regra 5 usa para substituir a metade do
     * armazém. Zero quando não há obra — e aí volta a valer a metade.
     */
    public static int planksNeededBy(ResourceId planks, Colony colony) {
        return materialNeededBy(planks, colony);
    }

    /**
     * Continua a rua que esta colônia começou, sem varrer de novo.
     *
     * <p>Irmão de {@link #extendTheRoad}, e a diferença é de onde vem a
     * autorização: lá, de uma varredura que acabou de terminar sem lote;
     * aqui, da mesma varredura, que continua valendo enquanto a ponta
     * render. Ver {@code RoadExtension.MAX_RUN} para onde isso para.
     *
     * @return a obra, quando o trecho novo já coube uma casa
     */
    private static Optional<ConstructionProject> keepGrowing(
            ServerWorld world, Colony colony, Blueprint blueprint,
            List<Blueprint> plans, int builders) {

        Optional<ResourceId> paving = VillageRoad.pavingFor(
                world, HousePlans.paletteOf(world, colony.center()).style());

        if (paving.isEmpty()) {
            return Optional.empty();
        }

        if (RoadExtension.keepGrowing(world, colony.id(), paving.get())
                != RoadExtension.Outcome.EXTENDED) {

            return Optional.empty();
        }

        IdleLog.clear(colony.id(), SUBJECT);

        return BuildSiteScanner.findBeside(
                        world,
                        colony.id(),
                        colony.center(),
                        sizesOf(plans),
                        RoadExtension.justPaved(colony.id()))
                .flatMap(beside -> open(world, colony, beside, plans, blueprint, builders));
    }

    /**
     * Prolonga a rua quando não há mais beira livre — a Regra 15.
     *
     * <p>A ponta já foi escolhida: a varredura que acabou de falhar
     * anotou a mais distante do centro enquanto procurava lote. Aqui só
     * se calça, e o que se decide é o que dizer quando não dá.
     *
     * <p><b>Três respostas, e as três são diferentes no log.</b> Sem rua
     * nenhuma para prolongar, a vila realmente parou e o motivo é o
     * antigo. Com ponta e sem poder calçar — encosta, água, peça de vila
     * — a vila também parou, mas por outra razão, e confundir as duas
     * mandaria o autor procurar no lugar errado.
     */
    private static Optional<ConstructionProject> extendTheRoad(
            ServerWorld world, Colony colony, Blueprint blueprint,
            List<Blueprint> plans, int builders) {

        Optional<ResourceId> paving = VillageRoad.pavingFor(
                world, HousePlans.paletteOf(world, colony.center()).style());

        if (paving.isEmpty()) {
            return silent(
                    colony,
                    IdleReason.NOT_IN_GAME,
                    "this game has no street to say what the road is made of");
        }

        RoadExtension.Outcome outcome =
                RoadExtension.extend(world, colony.id(), paving.get());

        return switch (outcome) {
            case EXTENDED -> {
                // Fala sempre, e não pelo IdleLog: rua nova é coisa que
                // aconteceu, e o log de transições cala o que se repete.
                IdleLog.clear(colony.id(), SUBJECT);

                // E o lote nasce agora — E26. A colônia acabou de criar a
                // beira; mandá-la redescobri-la varrendo o raio inteiro
                // custaria dezessete ciclos por uma informação que ela
                // tem na mão. Vazio aqui não é erro: o trecho novo pode
                // não caber casa, e aí a varredura seguinte decide.
                yield BuildSiteScanner.findBeside(
                                world,
                                colony.id(),
                                colony.center(),
                                sizesOf(plans),
                                RoadExtension.justPaved(colony.id()))
                        .flatMap(beside ->
                                open(world, colony, beside, plans, blueprint, builders));
            }

            case BLOCKED -> silent(
                    colony,
                    IdleReason.NO_TARGET,
                    "none of the road ends this colony can see may be paved — up to "
                            + RoadExtension.CANDIDATES + " were tried, and each one that"
                            + " refused sits out a while before being tried again");

            case NO_END -> silent(
                    colony,
                    IdleReason.NO_TARGET,
                    "no free lot beside a road in the whole "
                            + searchRadius + "-block radius of "
                            + colony.center() + " that fits " + blueprint.size()
                            + ", and no road end to extend either");
        };
    }

    /**
     * Quanto deste material a obra aberta ainda pede.
     *
     * <p>Era só para tábua até 2026-08-20, e virou geral quando o
     * mineiro entrou: a meta da colônia precisa saber que a casa quer 43
     * pedregulhos, senão ninguém abre tarefa de mineração e a obra dorme
     * esperando um material que a colônia já sabe fazer.
     */
    public static int materialNeededBy(ResourceId material, Colony colony) {
        return VillageColonyMod.CONSTRUCTIONS.openOf(colony.id())
                .map(project -> project.remainingMaterials().getOrDefault(material, 0))
                .orElse(0);
    }

    /**
     * Tudo o que a obra aberta ainda pede, material por material.
     *
     * <p>Existe para quem precisa <b>classificar</b> o que falta em vez
     * de somar um nome: a meta de fornalha da ADR-009 pergunta de cada
     * material se ele sai de forno, e para isso precisa vê-los todos.
     *
     * <p>Vazio quando não há obra, que é a resposta certa e não um erro.
     */
    public static Map<ResourceId, Integer> materialsNeededBy(Colony colony) {
        return VillageColonyMod.CONSTRUCTIONS.openOf(colony.id())
                .map(ConstructionProject::remainingMaterials)
                .orElse(Map.of());
    }

    /**
     * O mesmo, para uma família de materiais — 2026-08-21.
     *
     * <p>A cama tem dezesseis cores e a planta grava a que está no
     * arquivo. Perguntar por {@code white_bed} devolve zero numa casa que
     * pede {@code red_bed}, e zero é o pastor sem tarefa — o mesmo
     * defeito que o vidro teve por pedir vidraça.
     *
     * <p>A lã que a colônia produz é branca, e a cama colorida ainda
     * pede tinta que ninguém faz. O que isto conserta é a <b>demanda</b>
     * aparecer; a cor é problema do dia em que a colônia souber tingir.
     */
    public static int materialNeededBy(Predicate<ResourceId> family, Colony colony) {
        return VillageColonyMod.CONSTRUCTIONS.openOf(colony.id())
                .map(project -> project.remainingMaterials().entrySet().stream()
                        .filter(entry -> family.test(entry.getKey()))
                        .mapToInt(Map.Entry::getValue)
                        .sum())
                .orElse(0);
    }
}
