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
        if (wanted <= 0) {
            return 0;
        }

        Optional<Item> piece = MinecraftTypeAdapter.toBlock(made).map(Block::asItem);

        if (piece.isEmpty()) {
            return 0;
        }

        Optional<CraftingLookup.Bill> bill =
                CraftingLookup.billFor(world, piece.get(), item -> true);

        if (bill.isEmpty()) {
            return 0;
        }

        int perMaking = bill.get().resultCount();

        if (perMaking <= 0) {
            return 0;
        }

        int perBatch = 0;

        for (Map.Entry<Item, Integer> ingredient : bill.get().ingredients().entrySet()) {
            if (MinecraftTypeAdapter.toResourceType(ingredient.getKey())
                    .filter(part::equals)
                    .isPresent()) {

                perBatch += ingredient.getValue();
            }
        }

        if (perBatch <= 0) {
            return 0;
        }

        int makings = (wanted + perMaking - 1) / perMaking;

        return makings * perBatch;
    }
}
