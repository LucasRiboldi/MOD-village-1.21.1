package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.VillagePalette;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.Side;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.BuildSiteScanner;
import com.villagecolony.fabric.integration.StructureBlueprintReader;
import com.villagecolony.fabric.integration.VillageStructures;
import com.villagecolony.fabric.integration.VillageBiomes;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * O que esta colônia sabe levantar, e virado para onde.
 *
 * <p>Saiu de {@code ConstructionPlanner} em 2026-08-20, quando ele
 * passou de setecentas linhas. É uma pergunta inteira e independente
 * das outras duas daquele arquivo: <b>qual planta</b>, e <b>com a porta
 * para que lado</b>. Quem planeja usa a resposta, e usava por dentro do
 * planejador antes desta separação.
 *
 * <p>Três regras do autor moram aqui juntas, e é por isso que elas
 * cabem no mesmo arquivo: a Regra 20 escolhe a madeira pelo bioma, a
 * Regra 24 dá a casa do jogo à planície, e a Regra 25 manda oferecer da
 * maior planta para a menor. A Regra 17 fecha a conta girando a planta
 * para a rua.
 */
public final class HousePlans {

    private HousePlans() {
    }

    /** Esquece a planta lida do disco. Chamado ao parar o servidor. */
    public static void clearAll() {
        READ.clear();

        VillageStructures.clearAll();
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
    static Optional<Blueprint> houseFor(ServerWorld world, Colony colony) {
        List<Blueprint> plans = plansFor(world, colony);

        return plans.isEmpty() ? Optional.empty() : Optional.of(plans.get(0));
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
    static List<Blueprint> plansFor(ServerWorld world, Colony colony) {
        List<Blueprint> plans = catalogPlans(world, paletteOf(world, colony.center()).style());

        // <b>Fora as que esta colônia já tentou e não conseguiu</b> —
        // 2026-09-12. A ordem da Regra 25 fica intacta; o que muda é que a
        // lista <b>desce um degrau</b> em vez de reoferecer a casa que
        // morreu esperando material. É o que o autor pediu — preferir a
        // planta menor que resolve o gargalo — sem inverter a regra dele,
        // que faria a vila nunca mais tentar casa grande.
        //
        // A marca é por condição e não por prazo: a planta volta sozinha
        // quando a colônia passar a alcançar o que faltou. Ver
        // PlanRefusals.
        Set<ResourceId> skipped = new HashSet<>();

        for (Blueprint plan : plans) {
            if (PlanRefusals.skip(world, colony.id(), colony.center(), plan.id())) {
                skipped.add(plan.id());
            }
        }

        // <b>E a primeira casa da colônia é a menor</b> — decisão do autor,
        // 2026-09-15. A pergunta é feita ao registro de construções, que é
        // quem sabe o que já está de pé; obra em curso não conta, senão a
        // casa grande que travou a vila contaria como casa levantada.
        return smallestFirst(
                without(plans, skipped),
                hasNoHouseYet(VillageColonyMod.BUILDINGS.ofColony(colony.id())));
    }

    /**
     * Se esta colônia ainda não levantou nenhuma casa — 2026-09-15.
     *
     * <p><b>Conta só a casa terminada</b>, e é o conserto de um defeito que
     * a investigação de 21:50 achou na correção da véspera. A pergunta era
     * "o registro de construções está vazio?", e a obra <b>abandonada</b>
     * também entra nesse registro: {@code WaitingWork.giveUp} guarda a
     * caixa dela para o lote não voltar a parecer livre.
     *
     * <p>O açougue que a colônia largou às 21:42 virou {@code Building}, a
     * colônia passou a "ter casa" sem ter nenhuma, e a preferência pela
     * planta pequena <b>nunca dispararia</b> ali. O save do mundo do autor
     * tem <b>56 buildings</b> e <b>zero</b> {@code house is up}.
     *
     * <p><b>Visível ao pacote para o teste</b>, como {@link #without} e
     * {@link #smallestFirst}: é decisão, e decisão se afirma sem mundo.
     */
    static boolean hasNoHouseYet(List<Building> buildings) {
        return buildings.stream()
                .noneMatch(building -> building.finished() && isDwelling(building.blueprint()));
    }

    /**
     * Se a próxima construção precisa ser uma casa — 2026-09-20.
     *
     * <p>A sequência olha apenas para obras terminadas e usa a última delas:
     * casa abre a vez de outro tipo; qualquer tipo não residencial devolve a
     * vez para casa. Obra abandonada não participa do rodízio.
     */
    static boolean nextConstructionIsHouse(List<Building> buildings) {
        Optional<Building> last = lastFinished(buildings);

        return last.isEmpty() || !isDwelling(last.get().blueprint());
    }

    /**
     * O próximo tipo não residencial, excluindo o tipo anterior.
     *
     * <p>Um resultado vazio significa que a vez atual é de uma casa ou que
     * o catálogo não oferece outro tipo. A segunda situação é preferível a
     * repetir silenciosamente o mesmo prédio e travar a regra de variedade.
     */
    static Optional<String> nextNonHouseType(
            List<Building> buildings, List<String> availableTypes) {
        if (nextConstructionIsHouse(buildings)) {
            return Optional.empty();
        }

        String previous = lastNonHouseType(buildings).orElse("");

        return availableTypes.stream()
                .filter(type -> !type.equals(previous))
                .findFirst();
    }

    /**
     * A próxima família de plantas da colônia, com a regra de alternância.
     * O lote continua sendo escolhido pelo mesmo scanner para qualquer
     * família retornada aqui.
     */
    static List<Blueprint> plansForNext(ServerWorld world, Colony colony) {
        List<Building> buildings = VillageColonyMod.BUILDINGS.ofColony(colony.id());

        if (nextConstructionIsHouse(buildings)) {
            return plansFor(world, colony);
        }

        String previous = lastNonHouseType(buildings).orElse("");

        return nonHousePlansFor(world, colony, previous);
    }

    /** O tipo semântico de uma planta, usado para o rodízio A/B. */
    static String constructionType(ResourceId id) {
        if (isDwelling(id)) {
            return "house";
        }

        for (String type : NON_DWELLING_TYPES) {
            if (id.path().contains(type)) {
                return type;
            }
        }

        return "other";
    }

    private static Optional<Building> lastFinished(List<Building> buildings) {
        for (int index = buildings.size() - 1; index >= 0; index--) {
            Building building = buildings.get(index);

            if (building.finished()) {
                return Optional.of(building);
            }
        }

        return Optional.empty();
    }

    private static Optional<String> lastNonHouseType(List<Building> buildings) {
        for (int index = buildings.size() - 1; index >= 0; index--) {
            Building building = buildings.get(index);

            if (building.finished() && !isDwelling(building.blueprint())) {
                return Optional.of(constructionType(building.blueprint()));
            }
        }

        return Optional.empty();
    }

    /** As famílias não residenciais, na ordem do catálogo, sem a anterior. */
    private static List<Blueprint> nonHousePlansFor(
            ServerWorld world, Colony colony, String previousType) {

        String style = paletteOf(world, colony.center()).style();
        Map<String, List<Blueprint>> byType = new LinkedHashMap<>();
        boolean farmPostponed = FarmPlans.postponed(colony.id(), world.getTime());

        for (ResourceId id : VillageStructures.housesFor(style)) {
            String type = constructionType(id);

            if ("house".equals(type)
                    || type.equals(previousType)
                    || ("farm".equals(type) && farmPostponed)) {
                continue;
            }

            Optional<Blueprint> plan = READ.computeIfAbsent(
                    id, missing -> StructureBlueprintReader.read(world, missing));

            if (plan.isEmpty()) {
                continue;
            }

            Blueprint prepared = FarmPlans.isFarm(id)
                    ? FarmPlans.withoutTheCrops(plan.get())
                    : plan.get();

            byType.computeIfAbsent(type, ignored -> new ArrayList<>()).add(prepared);
        }

        for (List<Blueprint> plans : byType.values()) {
            plans.sort(Comparator.comparingInt(HousePlans::volumeOf).reversed());

            Set<ResourceId> skipped = new HashSet<>();

            for (Blueprint plan : plans) {
                if (PlanRefusals.skip(world, colony.id(), colony.center(), plan.id())) {
                    skipped.add(plan.id());
                }
            }

            return without(List.copyOf(plans), skipped);
        }

        return List.of();
    }

    /**
     * A menor planta na frente, enquanto a colônia não tem casa.
     *
     * <p><b>Decisão do autor, 2026-09-15:</b> <i>"dar preferência para a
     * primeira ser uma casa pequena"</i>.
     *
     * <p><b>O que o log de 09-15 mediu:</b> às 20:54:35 a colônia abriu
     * {@code plains_butcher_shop_2}, de 382 blocos, e sete minutos e meio
     * depois a obra continuava em <i>"382 blocks left"</i> — nenhum bloco
     * assentado — segurando a vaga única da colônia:
     * <i>"no building work: one is already open"</i>. Era a terceira sessão
     * seguida em que a maior planta do catálogo trava a vila <b>antes de a
     * primeira casa existir</b>.
     *
     * <p><b>A Regra 25 continua valendo, e ganha uma exceção de
     * arranque.</b> Ela manda levantar a maior planta que couber, e o
     * motivo dela é real: em 2026-08-20 exigir a casa grande em toda parte
     * fez a vila parar de crescer, com três cabanas de pé e o raio de 64
     * varrido sem resposta. Inverter a regra de vez faria a vila virar um
     * bairro de cabanas e as casas do jogo nunca subirem.
     *
     * <p>O que muda é só a <b>primeira</b>: sem nenhuma casa de pé, a
     * colônia começa pela planta que ela levanta sozinha, sem o jogador
     * guardar nada em baú — a mesma cabana que a {@link #plansFor} já
     * descreve como o fim da lista. Levantada essa, a Regra 25 volta
     * inteira, e a vila cresce como o autor decidiu em 08-20.
     *
     * <p><b>Reordena, não encurta.</b> As outras plantas continuam na
     * lista, atrás da menor: se a pequena não couber naquele lote, a
     * varredura desce para a seguinte em vez de a colônia ficar sem
     * resposta. Ver a Regra 25 — a escolha é por lote, não por vila.
     *
     * <p><b>Visível ao pacote para o teste</b>, pelo mesmo motivo que
     * {@link #without}: é decisão, e decisão se afirma sem mundo.
     */
    static List<Blueprint> smallestFirst(List<Blueprint> plans, boolean hasNoHouseYet) {
        if (!hasNoHouseYet || plans.size() < 2) {
            return plans;
        }

        List<Blueprint> reordered = new ArrayList<>(plans);

        // A ordem que chega é decrescente pela Regra 25, então a menor é a
        // última. Invertê-la por inteiro poria a segunda maior em segundo
        // lugar; o que o autor pediu é a menor NA FRENTE, e o resto como
        // estava — a Regra 25 intacta atrás dela.
        reordered.add(0, reordered.remove(reordered.size() - 1));

        return List.copyOf(reordered);
    }

    /**
     * A lista sem as plantas marcadas — e nunca vazia.
     *
     * <p><b>Separada de {@link #plansFor} porque é a decisão, e decisão se
     * afirma sem mundo.</b> Perguntar ao {@code PlanRefusals} varre baú;
     * escolher o que fica da lista não precisa de nada. Com as duas juntas,
     * o único teste possível seria de jogo — e a base já registrou o preço
     * de um filtro que nada exercitava: quando a divisão do fabricante
     * entrou, removido o {@code continue}, <b>701 unitários e 275 testes de
     * jogo continuavam verdes</b>.
     *
     * <p><b>Nunca devolve vazio tendo planta no catálogo.</b> Se todas
     * estiverem marcadas, vale a menor — a última, porque a ordem é
     * decrescente pela Regra 25. A alternativa é a vila parar de planejar
     * por completo, e a Regra 25 existe justamente para isso não acontecer.
     * Ela vai morrer esperando material de novo, e a linha de desistência
     * continua dizendo o que falta, que é melhor que silêncio.
     *
     * <p><b>Visível ao pacote para o teste.</b> {@code HousePlansTest}
     * afirma as duas coisas: que a marcada sai, e que a lista não fica
     * vazia.
     */
    static List<Blueprint> without(List<Blueprint> plans, Set<ResourceId> skipped) {
        List<Blueprint> offered = new ArrayList<>();

        for (Blueprint plan : plans) {
            if (!skipped.contains(plan.id())) {
                offered.add(plan);
            }
        }

        if (offered.isEmpty() && !plans.isEmpty()) {
            return List.of(plans.get(plans.size() - 1));
        }

        return List.copyOf(offered);
    }

    /**
     * Quantas plantas a busca de lote experimenta por coluna.
     *
     * <p>A Regra 25 manda oferecer da maior para a menor, e a Regra 27
     * deu trinta e seis casas por bioma. Trinta e seis tamanhos por
     * coluna de estrada seria uma varredura trinta e seis vezes mais
     * cara, e a de hoje já leva dez minutos.
     *
     * <p>Quatro é o corte, e é generoso: os tamanhos são poucos e
     * repetidos — a maioria das casas de um bioma divide a mesma pegada.
     * O que se perde é a casa de tamanho raro num lote apertado, e o que
     * se ganha é a colônia continuar planejando dentro de um tique.
     */
    private static final int PLANS_OFFERED = 4;

    /** As plantas lidas, por id. Ler um template não é barato. */
    private static final Map<ResourceId, Optional<Blueprint>> READ = new HashMap<>();

    /**
     * O que esta vila pode levantar, da maior planta para a menor.
     *
     * <p><b>Só o que está no catálogo</b> — a Regra 27, e ela é imutável.
     * Até 2026-08-20 a colônia levantava uma cabana escrita em código,
     * criada pela Regra 13 porque a casa do jogo era impossível com o que
     * ela produzia. A resposta passou a ser outra: a casa do jogo pede
     * pedra, então a colônia aprendeu a minerar.
     *
     * <p>Tamanhos repetidos entram uma vez só. Oferecer duas casas da
     * mesma pegada faria a busca medir o mesmo lote duas vezes para dar a
     * mesma resposta.
     *
     * <p><b>Mas a irmã descartada não some</b> — 2026-09-18. O corte
     * acima é da <b>busca</b>, e só dela: quem mede lote não ganha nada
     * vendo duas casas 9×9. Quem <b>levanta</b> ganha tudo. Até hoje a
     * vila saía com a mesma estrutura sempre, e a causa era esta linha
     * jogando fora as sete outras {@code small_house} antes de qualquer
     * escolha. Ver {@link #siblingsOf}, que as devolve ao planejador
     * depois de o lote estar achado — custo zero na varredura.
     */
    private static List<Blueprint> catalogPlans(ServerWorld world, String style) {
        List<Blueprint> plans = new ArrayList<>();

        Set<ColonyPos> sizes = new HashSet<>();

        for (ResourceId id : VillageStructures.housesFor(style)) {
            if (!isDwelling(id)) {
                continue;
            }

            Optional<Blueprint> house = READ.computeIfAbsent(
                    id, missing -> StructureBlueprintReader.read(world, missing));

            if (house.isEmpty()) {
                continue;
            }

            plans.add(house.get());
        }

        plans.sort(Comparator.comparingInt(HousePlans::volumeOf).reversed());

        List<Blueprint> offered = new ArrayList<>();

        for (Blueprint plan : plans) {
            if (!sizes.add(plan.size())) {
                continue;
            }

            offered.add(plan);

            if (offered.size() == PLANS_OFFERED) {
                break;
            }
        }

        return List.copyOf(offered);
    }

    /**
     * As peças da pasta {@code houses} que não são moradia — decisão do
     * autor, 2026-09-18.
     *
     * <p>O gerador de vilas do jogo põe na mesma pasta tudo que um lote
     * pode receber, e nem tudo ali é casa: cerca de bicho, ponto de
     * encontro, templo, estábulo e a peça decorativa avulsa da planície.
     * A colônia levanta <b>moradia</b>, e o autor pediu variedade de
     * casas — não um poço no lugar de uma.
     *
     * <p>A roça sai por este mesmo filtro e <b>não</b> pelo catálogo:
     * {@code FarmPlans.farmsFor} lê da mesma pasta e depende dela. O que
     * o filtro diz é "isto não é casa", e não "isto não existe".
     *
     * <p><b>Por substring, e é de propósito.</b> Os nomes do jogo não
     * têm convenção entre estilos — {@code butcher_shop} na planície e
     * {@code butchers_shop} na savana, {@code mason_1} no deserto e
     * {@code masons_house_1} na taiga. Uma lista de nomes exatos
     * quebraria em quatro dos cinco biomas; a substring atravessa os
     * cinco, que é o que {@code FarmPlans.isFarm} já faz desde 09-05.
     *
     * <p>Medido nos cinco estilos: 36→24 na planície, 27→22 na taiga,
     * 31→23 na savana, 30→25 na nevada, 28→21 no deserto. Nenhuma
     * moradia cai.
     */
    private static final List<String> NON_DWELLING_TYPES = List.of(
            "animal_pen", "meeting_point", "temple", "stable", "accessory", "farm");

    /** Se esta peça é casa de morar, e não cerca, poço ou templo. */
    public static boolean isDwelling(ResourceId id) {
        for (String other : NON_DWELLING_TYPES) {
            if (id.path().contains(other)) {
                return false;
            }
        }

        return true;
    }

    /**
     * As casas desta vila com a mesma pegada de uma planta — 2026-09-18.
     *
     * <p><b>O defeito que ela fecha:</b> a vila levantava sempre a mesma
     * estrutura. De 36 peças de planície, {@link #catalogPlans} entrega
     * 4 ao planejador — uma por pegada, cortada em {@link #PLANS_OFFERED}
     * —, e ele levanta a {@code get(0)}. As oito {@code small_house} do
     * jogo colapsavam em <b>uma</b>, e a escolhida era a mesma em toda
     * passagem, toda sessão, toda vila do mesmo bioma.
     *
     * <p><b>Por que aqui e não lá.</b> Devolver as irmãs ao
     * {@code catalogPlans} desfaria a razão do corte: a varredura de lote
     * mediria a mesma pegada oito vezes para dar oito vezes a mesma
     * resposta, e o comentário do {@code PLANS_OFFERED} já registra que
     * ela leva dez minutos. Esta pergunta é feita <b>depois</b> de o lote
     * estar achado, quando a pegada já é conhecida e medir acabou. A
     * varredura não fica um byte mais cara.
     *
     * <p>A leitura é do cache {@link #READ}, então as irmãs de uma
     * pegada já oferecida saem sem tocar o disco.
     *
     * <p><b>A pegada casa nos dois eixos, e isso não é descuido.</b> Quem
     * chama compara o tamanho <b>depois</b> do giro da Regra 17 — é o
     * conserto de 09-16, que existe porque uma casa 13×11 aprovada num
     * lote 13×11 vira 11×13 ao girar e ocupa treze blocos onde só onze
     * foram verificados. Se aqui a comparação fosse só pelo eixo do
     * arquivo, duas coisas quebrariam: a irmã retangular que chega seria
     * descartada logo adiante pelo filtro pós-giro, e — pior — a irmã que
     * <b>só cabe girada</b> nunca chegaria a ser considerada. Quem decide
     * se cabe continua sendo o filtro pós-giro de quem chama; o que esta
     * função faz é não esconder dele a candidata.
     */
    static List<Blueprint> siblingsOf(ServerWorld world, String style, ColonyPos footprint) {
        List<Blueprint> siblings = new ArrayList<>();

        for (ResourceId id : VillageStructures.housesFor(style)) {
            if (!isDwelling(id)) {
                continue;
            }

            Optional<Blueprint> house = READ.computeIfAbsent(
                    id, missing -> StructureBlueprintReader.read(world, missing));

            if (house.isPresent() && fitsEitherWay(house.get().size(), footprint)) {
                siblings.add(house.get());
            }
        }

        return List.copyOf(siblings);
    }

    /**
     * Se duas pegadas são a mesma, de pé ou deitada.
     *
     * <p>A altura tem de bater sempre — girar não muda o que é alto. O
     * que o giro troca são os dois eixos do chão.
     *
     * <p><b>Visível ao pacote para o teste</b>, como {@link #without} e
     * {@link #smallestFirst}: é decisão, e decisão se afirma sem mundo.
     */
    static boolean fitsEitherWay(ColonyPos plan, ColonyPos site) {
        if (plan.y() != site.y()) {
            return false;
        }

        return (plan.x() == site.x() && plan.z() == site.z())
                || (plan.x() == site.z() && plan.z() == site.x());
    }

    private static int volumeOf(Blueprint plan) {
        return plan.size().x() * plan.size().y() * plan.size().z();
    }

    /**
     * A paleta desta vila, com carvalho como reserva.
     *
     * <p>Bioma fora da tabela é vila que o mod não atende, e o ciclo nem
     * chegaria aqui. A reserva existe para o caso de o bioma mudar
     * debaixo de uma colônia já registrada — datapack, versão nova — e
     * para que a resposta nunca seja "não sei".
     */
    public static VillagePalette paletteOf(ServerWorld world, ColonyPos where) {
        return VillageBiomes.paletteAt(world, where)
                .orElseGet(() -> VillagePalette.ofWood("plains"));
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
    static Blueprint turnedToTheRoad(Blueprint house, Side road) {
        return house.doorSide()
                .map(door -> house.rotated(door.turnsTo(road)))
                .orElse(house);
    }

    /**
     * A planta deste id, venha ela do mod ou do jogo.
     *
     * <p>Existe para {@link #resume}, que carrega obra gravada em sessão
     * anterior e só tem o id em mãos — a planta precisa voltar girada
     * como a casa foi levantada, que é o que este método reconstrói.
     *
     * <p>Até 2026-08-21 havia dois caminhos aqui, e o primeiro era a
     * cabana do mod, escrita em código, que o leitor de estrutura não
     * acharia. Ela saiu, e ficou o caminho único: obra gravada aponta
     * para um arquivo do jogo, e é dele que a planta volta.
     */
    public static Optional<Blueprint> blueprintOf(
            ServerWorld world, UUID colonyId, ResourceId id, ColonyPos origin) {

        // Planta lida de arquivo: ela volta como o arquivo a gravou, e
        // precisa ser virada de novo para a rua. Sem isto a obra que
        // volta do save mede o mundo com a planta na orientação errada,
        // conclui que nada está de pé e reconstrói por cima, torto.
        return StructureBlueprintReader.read(world, id)
                .map(house -> turnedToTheRoad(house, roadSideOf(world, colonyId, origin, house)));
    }

    /**
     * Para que lado fica a rua desta obra, lida do mundo.
     *
     * <p>O lado não é gravado no save de propósito: ele é uma leitura do
     * mundo, e o mundo é a única fonte que continua certa depois de o
     * jogador mexer nele. Sem rua em volta — o jogador arrancou o
     * caminho —, fica o norte, que é onde a planta antiga punha a porta.
     */
    static Side roadSideOf(ServerWorld world, UUID colonyId, ColonyPos origin, Blueprint house) {
        return BuildSiteScanner.roadSideOf(world, colonyId, origin, house.size())
                .map(MinecraftTypeAdapter::toSide)
                .orElse(Side.NORTH);
    }
}
