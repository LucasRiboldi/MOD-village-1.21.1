package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.service.VillageDetector;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ConstructionReach;
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
import com.villagecolony.fabric.integration.StructureBlueprintReader;
import net.minecraft.block.Block;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Abrir a obra num lote achado: qual planta, virada para onde, sorteada entre
 * as irmãs, e o que o log diz sobre isso — separado de
 * {@link ConstructionPlanner} em 2026-09-24, quando ele passou de 500 linhas.
 * Os comentários vieram junto sem mudança.
 */
final class SiteOpening {

    private SiteOpening() {
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
    static List<ColonyPos> sizesOf(List<Blueprint> plans) {
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
     * <p>Separado de {@link ConstructionPlanner#plan} em 2026-08-25, quando passou a haver
     * <b>dois</b> caminhos até um lote: a varredura em anéis, e o atalho
     * de quem acabou de calçar a rua e sabe onde nasceu beira nova. O
     * trecho é o mesmo nos dois, e duas cópias dele seriam duas versões
     * da Regra 17.
     */
    static Optional<ConstructionProject> open(
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
        // conhecida — ver PlanPlacement.siblingsOf, e por que ela não
        // encarece a varredura.
        // A deduplicação é por {@code id} e não por {@code distinct()}:
        // Blueprint é classe sem equals, então distinct compararia
        // referência e deixaria a planta oferecida duas vezes no sorteio
        // — com peso dobrado sobre as irmãs.
        List<Blueprint> candidates = new ArrayList<>(plans);

        if (HousePlans.isHouse(blueprint.id())) {
            candidates.addAll(PlanPlacement.siblingsOf(
                    world, HousePlans.paletteOf(world, colony.center()).style(), site.size()));
        }

        Set<ResourceId> seen = new HashSet<>();

        List<Blueprint> fitting = candidates.stream()
                .filter(plan -> seen.add(plan.id()))
                .map(plan -> PlanPlacement.turnedToTheRoad(plan, road))
                .filter(plan -> plan.size().equals(site.size()))
                .toList();

        Blueprint facingTheRoad = fitting.isEmpty()
                ? PlanPlacement.turnedToTheRoad(blueprint, road)
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

        // <b>A camada da rua vai no chão</b> — sessão de jogo de 2026-09-26.
        // O lote diz onde fica o piso; a planta do jogo diz em que camada a
        // rua está, e é ela que desce ao chão. Ver Blueprint.originFor.
        ConstructionProject project = ConstructionProject.plan(
                colony.id(), facingTheRoad, facingTheRoad.originFor(site.origin()));

        VillageColonyMod.CONSTRUCTIONS.register(project);

        // E o que é chão não vira obra: fundação enterrada e a terra do
        // quintal já estão lá. Ver BuriedPieces.
        BuriedPieces.markHeldByTheGround(world, project);

        // PREPARING deixou de passar em branco em 2026-08-19. O lote é
        // escolhido sem bloco sólido dentro (Regra 22), mas grama alta e
        // flor não reprovam lote nenhum — e o miolo da planta não põe
        // bloco, então elas ficariam dentro da casa para sempre. Quem as
        // tira é a preparação.
        project.moveTo(ConstructionState.PREPARING);

        SitePreparation.clear(world, project);

        project.moveTo(ConstructionState.BUILDING);

        ConstructionPlanner.ensureTask(colony, project);

        IdleLog.clear(colony.id(), ConstructionPlanner.SUBJECT);

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
        // legível na hora. Ver ConstructionReach.isOutOfReach, que usa a
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
                ConstructionPlanner.searchRadius);

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
     * {@code BuildSiteScanner} e a do {@link ConstructionDemand#withinTheFarmersReach}.
     */
    static int squareDistance(ColonyPos origin, ColonyPos centre) {
        return Math.max(
                Math.abs(origin.x() - centre.x()),
                Math.abs(origin.z() - centre.z()));
    }

    /**
     * A distância em linha reta, que é como o guarda de alcance mede — E46.
     *
     * <p>Euclidiana, arredondada: a conta do
     * {@code ConstructionReach.isOutOfReach}. Quando ela passa do raio e
     * a {@link #squareDistance} não passa, a obra nasce condenada.
     */
    static int straightDistance(ColonyPos origin, ColonyPos centre) {
        long dx = (long) origin.x() - centre.x();
        long dz = (long) origin.z() - centre.z();

        return (int) Math.round(Math.sqrt(dx * dx + dz * dz));
    }
}
