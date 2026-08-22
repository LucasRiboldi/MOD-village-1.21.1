package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.ColonyHut;
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
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Optional;

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
        return catalogPlans(world, paletteOf(world, colony.center()).style());
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
     */
    private static List<Blueprint> catalogPlans(ServerWorld world, String style) {
        List<Blueprint> plans = new ArrayList<>();

        Set<ColonyPos> sizes = new HashSet<>();

        for (ResourceId id : VillageStructures.housesFor(style)) {
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
                .orElseGet(() -> VillagePalette.ofWood("plains", ColonyHut.OAK_PLANKS));
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
     * A cabana da colônia é escrita em
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
            VillagePalette palette = paletteOf(world, origin);

            return Optional.of(ColonyHut.blueprint(
                    palette,
                    roadSideOf(world, origin, ColonyHut.blueprint(palette, Side.NORTH))));
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
    static Side roadSideOf(ServerWorld world, ColonyPos origin, Blueprint house) {
        return BuildSiteScanner.roadSideOf(world, origin, house.size())
                .map(MinecraftTypeAdapter::toSide)
                .orElse(Side.NORTH);
    }
}
