package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.VillagePalette;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.CraftingLookup;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.server.world.ServerWorld;

import java.util.Optional;

/**
 * Quanto vidro a obra ainda pede — 2026-08-20.
 *
 * <p><b>Por que não basta perguntar por vidro.</b> A casa de planície não
 * pede vidro: pede <b>três vidraças</b>. Perguntar ao projeto quanto
 * vidro falta devolvia zero, e com zero a colônia nunca abria tarefa de
 * fundição — o fundidor existia desde a manhã de 2026-08-20 e não tinha o
 * que fundir em jogo, e a areia não tinha para quem ser colhida.
 *
 * <p>Então a vidraça é <b>decomposta pela receita do próprio jogo</b>:
 * seis vidros dão dezesseis vidraças, e quem diz isso é o livro de
 * receitas, não uma tabela escrita aqui. Três vidraças pedem uma fornada
 * de seis vidros — sobra treze, e sobrar é melhor que faltar: meia
 * fornada não existe.
 *
 * <p>É meio {@code ItemRequest} do backlog, e o meio que dá para fazer
 * hoje: um passo de decomposição, do material da obra para o que o
 * trabalhador sabe produzir. O geral — qualquer receita, em qualquer
 * profundidade — continua sendo o item 9 da fila.
 */
public final class GlassDemand {

    /** A vidraça da janela, que é o que a casa realmente pede. */
    private static final ResourceId GLASS_PANE = ResourceId.vanilla("glass_pane");

    private GlassDemand() {
    }

    /**
     * O vidro que a obra aberta desta colônia ainda consome.
     *
     * <p>O que ela pede de vidro direto mais o que as vidraças que
     * faltam vão custar. Zero quando não há obra, e zero também quando a
     * obra não tem janela — a cabana de deserto não tem.
     */
    public static int of(ServerWorld world, VillagePalette palette, Colony colony) {
        int direct = ConstructionPlanner.materialNeededBy(palette.glass(), colony);

        int panes = ConstructionPlanner.materialNeededBy(GLASS_PANE, colony);

        return direct + glassForPanes(world, palette, panes);
    }

    /**
     * O vidro de uma quantidade de vidraças, em fornadas inteiras.
     *
     * <p>Arredonda para cima porque a bancada não faz meia receita: uma
     * vidraça pedida custa a fornada inteira de vidro, e o resto fica no
     * baú para a próxima janela.
     *
     * <p>Zero quando o jogo não reconhece a vidraça ou não tem receita
     * para ela — datapack pode tirá-la, e inventar um número aqui seria a
     * colônia colhendo areia para nada.
     *
     * <p>Público porque é a metade que pode calar sem quebrar: um zero
     * daqui não derruba nada, só faz a colônia nunca pedir areia. Um
     * teste de jogo o pergunta ao livro de receitas de verdade.
     */
    public static int glassForPanes(ServerWorld world, VillagePalette palette, int panes) {
        if (panes <= 0) {
            return 0;
        }

        Optional<Item> pane = MinecraftTypeAdapter.toBlock(GLASS_PANE).map(Block::asItem);
        Optional<Item> glass = MinecraftTypeAdapter.toBlock(palette.glass()).map(Block::asItem);

        if (pane.isEmpty() || glass.isEmpty()) {
            return 0;
        }

        Optional<CraftingLookup.Bill> bill =
                CraftingLookup.billFor(world, pane.get(), item -> true);

        if (bill.isEmpty()) {
            return 0;
        }

        int perBatch = bill.get().ingredients().getOrDefault(glass.get(), 0);
        int perMaking = bill.get().resultCount();

        if (perBatch <= 0 || perMaking <= 0) {
            return 0;
        }

        int makings = (panes + perMaking - 1) / perMaking;

        return makings * perBatch;
    }
}
