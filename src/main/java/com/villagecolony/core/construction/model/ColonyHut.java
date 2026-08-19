package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.Side;

import java.util.ArrayList;
import java.util.List;

/**
 * A cabana que a colônia sabe construir sozinha — decisão de 2026-08-15.
 *
 * <p><b>Por que ela existe.</b> A obra do MVP era
 * {@code plains_village/houses/plains_small_house_1}, lida do próprio
 * jogo. A lista de compras dela, medida em 08-15, explica por que a casa
 * nunca subiu:
 *
 * <pre>
 * 149 blocos, 8 tipos
 *  49 oak_stairs        tábua
 *  43 cobblestone       mineração
 *  33 oak_planks        tronco
 *  16 stripped_oak_log  machado no tronco
 *   3 glass_pane        areia e fornalha
 *   3 wall_torch        graveto e carvão
 *   1 white_bed         tábua e lã
 *   1 oak_door          tábua
 * </pre>
 *
 * <p>Sessenta e seis daqueles blocos pedem cadeias que este mod não tem:
 * minerar, fundir, tosquiar, descascar. A colônia parava no primeiro
 * pedregulho e ficava em {@code WAITING_RESOURCES} para sempre — não por
 * defeito, mas porque a meta era impossível.
 *
 * <p><b>Esta cabana é possível.</b> Tábua e porta, e nada mais. As duas
 * saem de tronco, que é o que o lenhador traz e o fabricante transforma.
 * A colônia levanta esta obra do começo ao fim sem o jogador guardar
 * nada em baú algum — que é o que faltava para o sexto passo do MVP
 * poder ser visto acontecendo.
 *
 * <p><b>O que isto não fecha.</b> A casa do jogo continua sendo o alvo
 * bonito, e o dia em que a colônia minerar e fundir ela volta. O que
 * muda é qual obra o MVP precisa provar.
 *
 * <p>Escrita em código e não lida do jogo, de propósito: uma planta que
 * o mod garante existir não depende de datapack, não muda entre versões
 * e cabe num teste que a soma bloco a bloco.
 */
public final class ColonyHut {

    /** Cinco por cinco. Cabe em lote que a casa do jogo não caberia. */
    public static final int SIDE = 5;

    /** Três de pé-direito, e o teto no quarto nível. */
    public static final int WALL_HEIGHT = 3;

    public static final ResourceId ID = new ResourceId("villagecolony", "hut");

    /** A madeira das vilas de planície, e o padrão de quem não escolhe. */
    public static final ResourceId OAK_PLANKS =
            new ResourceId(ResourceId.VANILLA, "oak_planks");

    /** No meio da parede, seja qual for a parede. */
    private static final int DOOR_AT = SIDE / 2;

    /** O que a última montagem produziu, para não remontar por consulta. */
    private static Blueprint plan;

    private static ResourceId planWood;

    private static Side planSide;

    private ColonyHut() {
    }

    /**
     * A planta desta colônia: a madeira dela, e o lado em que a rua está.
     *
     * <p>Dois parâmetros que antes eram constantes, e cada um é uma
     * regra:
     *
     * <ul>
     *   <li><b>a madeira</b> é a Regra 20 — cada vila constrói no estilo
     *       do seu bioma, e o estilo que a colônia sabe produzir é a
     *       espécie da madeira;
     *   <li><b>o lado</b> é a Regra 17 — a casa se abre para a rua. A
     *       parede da porta deixa de ser sempre o norte e passa a ser a
     *       que encosta no caminho.
     * </ul>
     *
     * <p>Guarda a última montagem porque o ciclo pergunta a planta a
     * cada passagem e a colônia não muda de madeira nem de rua entre uma
     * e outra. Montagem nova só quando a pergunta muda.
     *
     * @param wood a tábua desta colônia. A porta sai da mesma espécie
     * @param doorSide para que lado fica a rua, visto de dentro do lote
     */
    public static Blueprint blueprint(ResourceId wood, Side doorSide) {
        if (plan == null || !wood.equals(planWood) || doorSide != planSide) {
            plan = Blueprint.of(ID, blocks(wood, doorOf(wood), doorSide));
            planWood = wood;
            planSide = doorSide;
        }

        return plan;
    }

    /**
     * A porta da mesma espécie da tábua.
     *
     * <p>{@code spruce_planks} vira {@code spruce_door}. É convenção do
     * jogo e não do mod, e vale para as nove madeiras que existem — o
     * que a torna mais confiável que uma tabela escrita aqui, que
     * envelheceria na próxima madeira que o jogo acrescentasse.
     */
    private static ResourceId doorOf(ResourceId wood) {
        return new ResourceId(wood.namespace(), wood.path().replace("_planks", "_door"));
    }

    /**
     * Paredes, porta e teto, de baixo para cima.
     *
     * <p>A ordem importa: {@code BuilderWork} põe na ordem da lista, e
     * bloco posto no ar cai ou é recusado por {@code canPlaceAt}. Por
     * isso o teto vem depois das paredes que o seguram, e a porta depois
     * do batente.
     *
     * <p>Sem chão. O lote já foi escolhido plano e livre por
     * {@code BuildSiteScanner}, e forrar o terreno gastaria vinte e cinco
     * tábuas para esconder a grama que já estava lá.
     */
    private static List<BlueprintBlock> blocks(
            ResourceId wood, ResourceId door, Side doorSide) {

        List<BlueprintBlock> blocks = new ArrayList<>();

        ColonyPos doorway = doorwayOn(doorSide);

        for (int y = 0; y < WALL_HEIGHT; y++) {
            for (int x = 0; x < SIDE; x++) {
                for (int z = 0; z < SIDE; z++) {
                    if (!isWall(x, z) || isDoorway(x, z, y, doorway)) {
                        continue;
                    }

                    blocks.add(new BlueprintBlock(new ColonyPos(x, y, z), wood));
                }
            }
        }

        // A porta por último entre os blocos baixos: ela precisa do
        // batente de pé dos dois lados para não ficar solta.
        blocks.add(new BlueprintBlock(doorway, door));

        for (int x = 0; x < SIDE; x++) {
            for (int z = 0; z < SIDE; z++) {
                blocks.add(new BlueprintBlock(new ColonyPos(x, WALL_HEIGHT, z), wood));
            }
        }

        return List.copyOf(blocks);
    }

    /** A borda do quadrado. O miolo fica oco — é onde se mora. */
    private static boolean isWall(int x, int z) {
        return x == 0 || z == 0 || x == SIDE - 1 || z == SIDE - 1;
    }

    /**
     * Onde a porta fica, dado o lado em que a rua está — a Regra 17.
     *
     * <p>No meio da parede que encosta no caminho. A cabana é um
     * quadrado, então mudar de parede é mudar duas coordenadas e nada
     * mais: não há o que girar, e é por isso que esta regra saiu barata
     * aqui e será cara no dia em que a planta tiver escada.
     */
    private static ColonyPos doorwayOn(Side doorSide) {
        return switch (doorSide) {
            case NORTH -> new ColonyPos(DOOR_AT, 0, 0);
            case SOUTH -> new ColonyPos(DOOR_AT, 0, SIDE - 1);
            case WEST -> new ColonyPos(0, 0, DOOR_AT);
            case EAST -> new ColonyPos(SIDE - 1, 0, DOOR_AT);
        };
    }

    /** O vão da porta: dois blocos de altura, na parede que dá na rua. */
    private static boolean isDoorway(int x, int z, int y, ColonyPos doorway) {
        return x == doorway.x() && z == doorway.z() && y < 2;
    }
}
