package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;

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

    private static final ResourceId PLANKS = new ResourceId(ResourceId.VANILLA, "oak_planks");

    private static final ResourceId DOOR = new ResourceId(ResourceId.VANILLA, "oak_door");

    /** Onde fica a porta, no meio da parede do norte. */
    private static final int DOOR_X = SIDE / 2;

    private static Blueprint plan;

    private ColonyHut() {
    }

    /**
     * A planta, montada uma vez.
     *
     * <p>Imutável e igual para todo mundo, como a casa lida do jogo —
     * guardar poupa remontar a lista a cada consulta.
     */
    public static Blueprint blueprint() {
        if (plan == null) {
            plan = Blueprint.of(ID, blocks());
        }

        return plan;
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
    private static List<BlueprintBlock> blocks() {
        List<BlueprintBlock> blocks = new ArrayList<>();

        for (int y = 0; y < WALL_HEIGHT; y++) {
            for (int x = 0; x < SIDE; x++) {
                for (int z = 0; z < SIDE; z++) {
                    if (!isWall(x, z) || isDoorway(x, z, y)) {
                        continue;
                    }

                    blocks.add(new BlueprintBlock(new ColonyPos(x, y, z), PLANKS));
                }
            }
        }

        // A porta por último entre os blocos baixos: ela precisa do
        // batente de pé dos dois lados para não ficar solta.
        blocks.add(new BlueprintBlock(new ColonyPos(DOOR_X, 0, 0), DOOR));

        for (int x = 0; x < SIDE; x++) {
            for (int z = 0; z < SIDE; z++) {
                blocks.add(new BlueprintBlock(new ColonyPos(x, WALL_HEIGHT, z), PLANKS));
            }
        }

        return List.copyOf(blocks);
    }

    /** A borda do quadrado. O miolo fica oco — é onde se mora. */
    private static boolean isWall(int x, int z) {
        return x == 0 || z == 0 || x == SIDE - 1 || z == SIDE - 1;
    }

    /** O vão da porta: dois blocos de altura, no meio da parede norte. */
    private static boolean isDoorway(int x, int z, int y) {
        return z == 0 && x == DOOR_X && y < 2;
    }
}
