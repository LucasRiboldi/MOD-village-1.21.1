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
import com.villagecolony.fabric.integration.VillageBiomes;
import net.minecraft.server.world.ServerWorld;

import java.util.List;
import java.util.Optional;

/**
 * O que esta colônia sabe levantar, e virado para onde.
 *
 * <p>Saiu de {@code ConstructionPlanner} em 2026-08-20, quando ele
 * passou de setecentas linhas. É uma pergunta inteira e independente
 * das outras duas daquele arquivo: <b>qual planta</b>, e <b>com a porta
 * para que lado</b>. Quem planeja usa a resposta; quem mobília
 * ({@link HouseFurnishing}) usa a mesma, e usava por dentro do
 * planejador antes desta separação.
 *
 * <p>Três regras do autor moram aqui juntas, e é por isso que elas
 * cabem no mesmo arquivo: a Regra 20 escolhe a madeira pelo bioma, a
 * Regra 24 dá a casa do jogo à planície, e a Regra 25 manda oferecer da
 * maior planta para a menor. A Regra 17 fecha a conta girando a planta
 * para a rua.
 */
public final class HousePlans {

    /**
     * A casa pequena, lida do disco uma vez por sessão.
     *
     * <p>Esquecida ao parar o servidor, junto com o resto — ver
     * {@link #clearAll()}.
     */
    private static Optional<Blueprint> smallHouse;

    private HousePlans() {
    }

    /** Esquece a planta lida do disco. Chamado ao parar o servidor. */
    public static void clearAll() {
        smallHouse = null;
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
    static Blueprint houseFor(ServerWorld world, Colony colony) {
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
    static List<Blueprint> plansFor(ServerWorld world, Colony colony) {
        VillagePalette palette = paletteOf(world, colony.center());

        Blueprint hut = ColonyHut.blueprint(palette, Side.NORTH);

        if (!ColonyHut.OAK_PLANKS.equals(palette.wall())) {
            return List.of(hut);
        }

        return smallHouse(world)
                .map(house -> List.of(house, hut))
                .orElseGet(() -> List.of(hut));
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
                .orElseGet(() -> VillagePalette.ofWood(ColonyHut.OAK_PLANKS));
    }

    /**
     * A casa pequena, lida uma vez e guardada.
     *
     * <p>Ler um template é abrir e decodificar um arquivo de trezentos e
     * quarenta e três blocos, e o ciclo pergunta pela planta a cada
     * passagem. O aviso está no cabeçalho de
     * {@code StructureBlueprintReader}: quem chama guarda o resultado.
     */
    static Optional<Blueprint> smallHouse(ServerWorld world) {
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
    static Blueprint turnedToTheRoad(Blueprint house, Side road) {
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
