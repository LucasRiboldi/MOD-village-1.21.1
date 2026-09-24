package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.VillagePalette;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.Side;
import com.villagecolony.core.worker.model.ProfessionType;
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
 * A planta posta no lote: girada para a rua, relida do save e trocada por uma
 * irmã da mesma pegada — separado de {@link HousePlans} em 2026-09-24, quando
 * ele passou de 500 linhas.
 *
 * <p>O {@code HousePlans} escolhe <b>qual</b> família levantar; esta classe
 * cuida de como a planta escolhida fica no terreno. Os comentários vieram
 * junto sem mudança.
 */
public final class PlanPlacement {

    private PlanPlacement() {
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
            if (!HousePlans.isHouse(id)) {
                continue;
            }

            Optional<Blueprint> house = HousePlans.READ.computeIfAbsent(
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
