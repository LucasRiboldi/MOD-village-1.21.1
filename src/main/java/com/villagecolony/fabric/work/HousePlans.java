package com.villagecolony.fabric.work;

import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.ConstructionPriority;
import com.villagecolony.core.construction.model.VillagePalette;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.integration.ProfessionChestOverflow;
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
 * Regra 24 dá a casa do jogo à planície, e a ordem das plantas não favorece
 * tamanho (ADR-036, que desfez a Regra 25). A Regra 17 fecha a conta girando
 * a planta para a rua.
 */
public final class HousePlans {

    static {
        ServerMemory.register(HousePlans.class, HousePlans::clearAll);
    }

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
     * O que esta colônia sabe levantar, na ordem de {@link PlanOrdering#mixed}.
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
        List<Blueprint> plans = HouseCatalog.catalogPlans(
                world, paletteOf(world, colony.center()).style(), seedFor(colony));

        // <b>Fora as que esta colônia já tentou e não conseguiu</b> —
        // 2026-09-12: a lista não reoferece a casa que morreu esperando
        // material.
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
        return PlanOrdering.smallestFirst(
                PlanOrdering.without(plans, skipped),
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
     * <p><b>Visível ao pacote para o teste</b>, como {@link PlanOrdering#without} e
     * {@link PlanOrdering#smallestFirst}: é decisão, e decisão se afirma sem mundo.
     */
    static boolean hasNoHouseYet(List<Building> buildings) {
        return buildings.stream()
                .noneMatch(building -> building.finished() && isDwelling(building.blueprint()));
    }

    /**
     * Se a próxima construção precisa ser uma casa — 2026-09-20.
     *
     * <p>Usa a última obra <b>tentada</b>: casa abre a vez de outro tipo;
     * qualquer tipo não residencial devolve a vez para casa.
     *
     * <p><b>Obra abandonada passou a contar — E48, 2026-09-24, decisão do
     * autor.</b> Até aqui só a obra terminada entrava, e o playtest de 24-09
     * mostrou o custo: uma casa terminada e depois o {@code plains_temple_4}
     * abandonado treze vezes. O rodízio via só a casa, a vez continuava
     * com "não residencial", o templo não era excluído — e foi escolhido
     * de novo nas treze. Nenhuma casa em seis horas e meia.
     *
     * <p>{@link #hasNoHouseYet} continua olhando só a terminada, e de
     * propósito: ali a pergunta é "existe casa de pé", não "de quem é a vez".
     */
    static boolean nextConstructionIsHouse(List<Building> buildings) {
        return nextConstructionPriority(buildings, 0, 0).requiresHouse();
    }

    /**
     * O mesmo, e casa sempre que faltar cama — E48, 2026-09-24, decisão do
     * autor: <i>"sempre buscando a jogabilidade mais natural"</i>.
     *
     * <p>Uma vila de verdade não levanta templo com gente dormindo ao
     * relento. Enquanto houver mais adultos do que camas, a vez é da casa;
     * com cama para todos, o rodízio volta a decidir.
     *
     * @param adults os trabalhadores adultos da colônia
     * @param beds as camas que a detecção de vila contou
     */
    static boolean nextConstructionIsHouse(List<Building> buildings, int adults, int beds) {
        return nextConstructionPriority(buildings, adults, beds).requiresHouse();
    }

    /**
     * Motivo atual da família de plantas da próxima obra.
     *
     * <p>Esta adaptação conserva no Fabric a pergunta sobre a planta ser uma
     * casa. A precedência entre déficit, primeira obra e rodízio fica no Core,
     * onde também é exercitada sem mundo.
     */
    static ConstructionPriority nextConstructionPriority(
            List<Building> buildings, int adults, int beds) {
        Optional<Building> last = lastAttempted(buildings);

        return ConstructionPriority.decide(
                last.isPresent(),
                last.map(building -> isHouse(building.blueprint())).orElse(false),
                adults,
                beds);
    }

    static int effectiveBedsForPriority(UUID colonyId, int adults, int observedBeds) {
        if (!WorkerHousingNeeds.needsHouse(colonyId) || adults == 0) {
            return observedBeds;
        }

        return Math.min(observedBeds, adults - 1);
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
     * Se é a vez do tipo desta planta no rodízio — 2026-09-25, decisão do
     * autor.
     *
     * <p><b>Para a obra abandonada.</b> O {@code BuildingRepairPlanner} roda
     * antes do rodízio e reabria a obra largada no mesmo segundo em que a
     * colônia desistia dela: o playtest de 24-09 tem "gives up on
     * plains_temple_4" e "starts repair sweep for plains_temple_4" na mesma
     * origem e no mesmo segundo, e o de 25-09 reabriu um templo abandonado
     * de 301 blocos assim que o outro terminou. A vila ficava presa a
     * templos, e a vez da casa nunca chegava.
     *
     * <p>A regra é a do próprio rodízio: casa na vez da casa; outra obra na
     * vez de outra, e nunca do mesmo tipo da última não residencial tentada.
     * Como a obra abandonada conta como tentada (E48), a que acabou de ser
     * largada nunca é a vez dela.
     */
    static boolean isTurnOf(List<Building> buildings, int adults, int beds, ResourceId blueprint) {
        boolean houseTurn = nextConstructionIsHouse(buildings, adults, beds);

        if (isHouse(blueprint)) {
            return houseTurn;
        }

        return !houseTurn
                && !constructionType(blueprint).equals(lastNonHouseType(buildings).orElse(""));
    }

    /**
     * A próxima família de plantas da colônia, com a regra de alternância.
     * O lote continua sendo escolhido pelo mesmo scanner para qualquer
     * família retornada aqui.
     */
    static List<Blueprint> plansForNext(ServerWorld world, Colony colony) {
        List<Building> buildings = VillageColonyMod.BUILDINGS.ofColony(colony.id());
        int adults = VillageColonyMod.WORKERS.countOfColony(colony.id());

        int beds = effectiveBedsForPriority(colony.id(), adults, colony.observedBeds());
        String previous = lastNonHouseType(buildings).orElse("");

        // Sem baú livre na vila, o armazém vem antes de tudo — ADR-036 9.
        if (ProfessionChestOverflow.needsStorage(colony.id())) {
            List<Blueprint> storage = StoragePlans.forColony(world, colony, buildings);
            if (!storage.isEmpty()) {
                return storage;
            }
        }

        // A casa do ofício que falta, sem falta de cama — Regra 49. Sem planta
        // de oficina possível agora, o rodízio de sempre segue.
        if (ConstructionTurn.of(buildings, adults, beds, ConstructionTurn.workshopMissing(world, colony, buildings))
                == ConstructionPriority.WORKSHOP) {
            List<Blueprint> workshops = nonHousePlansFor(world, colony, previous, buildings);
            if (!workshops.isEmpty()) {
                return workshops;
            }
        }

        if (nextConstructionPriority(buildings, adults, beds).requiresHouse()) {
            return plansFor(world, colony);
        }

        return nonHousePlansFor(world, colony, previous, buildings);
    }

    /** O tipo semântico de uma planta, usado para o rodízio A/B. */
    static String constructionType(ResourceId id) {
        if (isHouse(id)) {
            return "house";
        }

        // Oficina do jogo — N9: cada uma é o seu próprio tipo, para a vez
        // de "outra" poder escolher a oficina do ofício que falta.
        Optional<String> shop = ConstructionOrder.shopType(id);

        if (shop.isPresent()) {
            return shop.get();
        }

        for (String type : NON_DWELLING_TYPES) {
            if (id.path().contains(type)) {
                return type;
            }
        }

        return "other";
    }

    /** A última obra tentada, terminada ou abandonada — ver E48. */
    private static Optional<Building> lastAttempted(List<Building> buildings) {
        return buildings.isEmpty()
                ? Optional.empty()
                : Optional.of(buildings.get(buildings.size() - 1));
    }

    /**
     * O último tipo não residencial tentado, terminado ou abandonado — E48.
     *
     * <p>É o tipo que a vez seguinte exclui. Deixar a obra abandonada de fora
     * foi o que devolveu o mesmo templo treze vezes no playtest de 24-09.
     */
    private static Optional<String> lastNonHouseType(List<Building> buildings) {
        for (int index = buildings.size() - 1; index >= 0; index--) {
            Building building = buildings.get(index);

            if (!isHouse(building.blueprint())) {
                return Optional.of(constructionType(building.blueprint()));
            }
        }

        return Optional.empty();
    }

    /**
     * As plantas da vez de "outra": a família que {@link ConstructionOrder}
     * escolhe — oficina do ofício que falta primeiro —, sem a anterior.
     */
    private static List<Blueprint> nonHousePlansFor(
            ServerWorld world, Colony colony, String previousType, List<Building> buildings) {

        String style = paletteOf(world, colony.center()).style();
        Map<String, List<Blueprint>> byType = new LinkedHashMap<>();
        boolean farmPostponed = FarmPlans.postponed(colony.id(), world.getTime());

        // A lâmpada Vanilla fica na raiz de `village/<style>/`, fora de
        // `houses/`; as demais oficinas e roças continuam na pasta de lotes.
        for (ResourceId id : VillageStructures.buildableFor(style)) {
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

        Map<String, Optional<ProfessionType>> professionOfType = new HashMap<>();

        byType.forEach((type, plans) -> professionOfType.put(
                type, ConstructionOrder.professionOf(plans.get(0).id())));

        Optional<String> chosen = ConstructionOrder.nextType(
                buildings, byType.keySet(), professionOfType, HousePlans::constructionType);

        if (chosen.isPresent()) {
            List<Blueprint> plans = byType.get(chosen.get());
            List<Blueprint> mixed = PlanOrdering.mixed(plans, seedFor(colony));

            Set<ResourceId> skipped = new HashSet<>();

            for (Blueprint plan : mixed) {
                if (PlanRefusals.skip(world, colony.id(), colony.center(), plan.id())) {
                    skipped.add(plan.id());
                }
            }

            return PlanOrdering.without(mixed, skipped);
        }

        return List.of();
    }

    /** As plantas lidas, por id. Ler um template não é barato. */
    static final Map<ResourceId, Optional<Blueprint>> READ = new HashMap<>();

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
            "animal_pen", "meeting_point", "temple", "stable", "accessory", "farm", "lamp");

    /**
     * Se a vez da casa pode oferecer esta planta — N9, 2026-09-24.
     *
     * <p>Moradia que não é oficina. As oficinas do jogo têm cama, e por isso
     * {@link #isDwelling} as aceita; mas a vila do autor levanta casa na vez
     * da casa, e a oficina na vez dela — ver {@link ConstructionOrder}.
     */
    public static boolean isHouse(ResourceId id) {
        return isDwelling(id) && !ConstructionOrder.isShop(id);
    }

    /** Uma moradia elegível para expansão precisa oferecer ao menos uma cama. */
    public static boolean hasBed(Blueprint plan) {
        return plan.blocks().stream().anyMatch(block -> block.block().path().endsWith("_bed"));
    }

    /** Se esta peça é casa de morar, e não cerca, poço ou templo. */
    public static boolean isDwelling(ResourceId id) {
        for (String other : NON_DWELLING_TYPES) {
            if (id.path().contains(other)) {
                return false;
            }
        }

        return true;
    }

    /** A semente da ordem das plantas desta colônia — ADR-036 item 5. */
    private static long seedFor(Colony colony) {
        return PlanOrdering.seedFor(colony.id(), VillageColonyMod.BUILDINGS.ofColony(colony.id()).size());
    }

    static int volumeOf(Blueprint plan) {
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

}
