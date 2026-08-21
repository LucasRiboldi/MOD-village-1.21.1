package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.VillagePalette;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.CraftingLookup;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.server.world.ServerWorld;

import java.util.Map;
import java.util.Optional;

/**
 * O que a obra pede, traduzido para o que a colônia sabe produzir.
 *
 * <p><b>O problema que ela resolve.</b> A casa não pede vidro: pede
 * <b>vidraça</b>. Não pede carvão: pede <b>tocha</b>. Perguntar ao
 * projeto quanto vidro ou quanto carvão falta devolvia zero, e com zero
 * a colônia nunca abria tarefa — o fundidor ficava parado, o mineiro não
 * procurava minério, e os dois materiais continuavam por conta do
 * jogador.
 *
 * <p>Então a peça da parede é <b>decomposta pela receita do próprio
 * jogo</b>: seis vidros dão dezesseis vidraças, um carvão dá quatro
 * tochas. Nenhum desses números está escrito aqui — quem os sabe é o
 * livro de receitas, e escrevê-los faria a conta envelhecer no primeiro
 * datapack.
 *
 * <p><b>Um passo, e só um.</b> É meio {@code ItemRequest} do backlog, e o
 * meio que dá para fazer hoje: do material da obra para o que um
 * trabalhador sabe produzir. Cadeia mais funda — o lampião, que pede
 * pepita, que pede lingote — continua sendo o item da fila.
 *
 * <p>Nasceu como {@code GlassDemand} em 2026-08-20 e virou isto no dia
 * seguinte, quando o carvão precisou da mesma conta. Dois casos com a
 * mesma forma pedem um lugar só.
 */
public final class WorkMaterials {

    /** A vidraça da janela, que é o que a casa realmente pede. */
    private static final ResourceId GLASS_PANE = ResourceId.vanilla("glass_pane");

    /** A tocha, nas duas formas em que a planta a grava. */
    private static final ResourceId WALL_TORCH = ResourceId.vanilla("wall_torch");

    private static final ResourceId TORCH = ResourceId.vanilla("torch");

    /** O lampião da Regra 21, que é o que pede ferro nesta colônia. */
    public static final ResourceId LANTERN = ResourceId.vanilla("lantern");

    /**
     * Quantos degraus de receita a conta desce.
     *
     * <p>O mesmo teto do {@code ColonySupply}, e pela mesma razão: é o
     * que o lampião pede — pepita, que sai do lingote — e cada degrau
     * custa uma varredura do livro de receitas inteiro.
     */
    private static final int RECIPE_DEPTH = 2;

    private WorkMaterials() {
    }

    /**
     * O vidro que a obra aberta desta colônia ainda consome.
     *
     * <p>O que ela pede de vidro direto mais o que as vidraças que faltam
     * vão custar. Zero quando não há obra, e zero também quando a obra
     * não tem janela — a cabana de deserto não tem.
     */
    public static int glass(ServerWorld world, VillagePalette palette, Colony colony) {
        int direct = ConstructionPlanner.materialNeededBy(palette.glass(), colony);

        int panes = ConstructionPlanner.materialNeededBy(GLASS_PANE, colony);

        return direct + through(world, GLASS_PANE, ResourceType.GLASS, panes);
    }

    /**
     * O carvão que as tochas da obra vão custar — 2026-08-21.
     *
     * <p>As duas formas contam junto: no arquivo da casa a tocha da
     * parede é {@code wall_torch} e a do chão é {@code torch}, e as duas
     * saem do mesmo item e da mesma receita. Separá-las daria duas contas
     * para uma fornada.
     */
    /**
     * O lingote que os lampiões que faltam vão custar — 2026-08-21.
     *
     * <p><b>Dois degraus, e é o caso que os pediu.</b> O lampião não pede
     * lingote: pede oito <b>pepitas</b>, e a pepita é que sai do lingote,
     * nove de cada. Um degrau só devolveria zero, e com zero o fundidor
     * nunca recebe tarefa de ferro — que foi exatamente o que aconteceu
     * no dia em que ele aprendeu a fundi-lo.
     *
     * <p>Vem da mobília, e não da obra: o lampião é peça da Regra 21, e
     * quem sabe quantas casas estão sem ele é a passagem que as
     * mobiliaria. Ver {@code HouseFurnishing}.
     */
    public static int iron(ServerWorld world, int lanterns) {
        return through(world, LANTERN, ResourceType.IRON_INGOT, lanterns);
    }

    public static int coal(ServerWorld world, Colony colony) {
        int torches = ConstructionPlanner.materialNeededBy(WALL_TORCH, colony)
                + ConstructionPlanner.materialNeededBy(TORCH, colony);

        return through(world, WALL_TORCH, ResourceType.COAL, torches);
    }

    /**
     * Quanto deste material uma quantidade daquela peça custa.
     *
     * <p>Arredonda para cima porque a bancada não faz meia receita: uma
     * vidraça pedida custa a fornada inteira de vidro, e o resto fica no
     * baú para a próxima janela.
     *
     * <p><b>Soma por tipo, e não por item.</b> A receita da tocha aceita
     * carvão ou carvão vegetal, e o livro devolve o primeiro que
     * encontrar; perguntar pelo item exato devolveria zero metade das
     * vezes, e zero aqui é a colônia deixando de pedir <b>em silêncio</b>.
     *
     * <p>Zero quando o jogo não reconhece a peça ou não tem receita para
     * ela — datapack pode tirá-la, e inventar um número seria a colônia
     * minerando para nada.
     *
     * <p>Público porque é a metade que pode calar sem quebrar: um zero
     * daqui não derruba nada. Um teste de jogo o pergunta ao livro de
     * receitas de verdade.
     */
    public static int through(ServerWorld world, ResourceId made, ResourceType part, int wanted) {
        return through(world, made, part, wanted, RECIPE_DEPTH);
    }

    private static int through(
            ServerWorld world, ResourceId made, ResourceType part, int wanted, int depth) {

        Optional<Item> piece = MinecraftTypeAdapter.toBlock(made).map(Block::asItem);

        return piece.map(item -> through(world, item, part, wanted, depth)).orElse(0);
    }

    /**
     * O mesmo, item a item, para descer pela receita.
     *
     * <p>Um degrau: o que a receita pede e é do tipo procurado entra na
     * conta; o que não é vira a mesma pergunta um degrau abaixo. É assim
     * que o lampião chega ao lingote — ele pede <b>pepita</b>, e a pepita
     * é que sai do lingote.
     *
     * <p>O teto é o mesmo do {@code ColonySupply}, e pela mesma razão:
     * cada degrau custa uma varredura do livro de receitas inteiro.
     */
    private static int through(
            ServerWorld world, Item made, ResourceType part, int wanted, int depth) {

        if (wanted <= 0) {
            return 0;
        }

        Optional<CraftingLookup.Bill> bill =
                CraftingLookup.billFor(world, made, item -> true);

        if (bill.isEmpty()) {
            return 0;
        }

        int perMaking = bill.get().resultCount();

        if (perMaking <= 0) {
            return 0;
        }

        int makings = (wanted + perMaking - 1) / perMaking;

        int total = 0;

        for (Map.Entry<Item, Integer> ingredient : bill.get().ingredients().entrySet()) {
            int needed = makings * ingredient.getValue();

            if (MinecraftTypeAdapter.toResourceType(ingredient.getKey())
                    .filter(part::equals)
                    .isPresent()) {

                total += needed;

                continue;
            }

            if (depth > 0) {
                total += through(world, ingredient.getKey(), part, needed, depth - 1);
            }
        }

        return total;
    }
}
