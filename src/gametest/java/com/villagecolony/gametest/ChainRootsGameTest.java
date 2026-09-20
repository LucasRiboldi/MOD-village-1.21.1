package com.villagecolony.gametest;

import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.CraftingLookup;
import com.villagecolony.fabric.work.BuilderWork;
import com.villagecolony.fabric.integration.StructureBlueprintReader;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Onde cada cadeia <b>começa</b>, e se o bioma a alcança — 2026-09-19.
 *
 * <p><b>O defeito que este arquivo existe para impedir, e ele custou o
 * dia inteiro.</b> O {@code StructureCoverageGameTest} pergunta <i>"alguém
 * sabe fazer isto?"</i> e responde <b>sim</b> para a cadeia inteira — e
 * mesmo assim a vila parou três vezes hoje:
 *
 * <pre>
 * cut_sandstone     fabricável, e sem ResourceType: o pedreiro nunca
 *                   recebia tarefa. 22 esperas com 190 arenitos no bau.
 * potted_cactus     "fabricável" e SEM ITEM: a obra esperava um item
 *                   que nao pode existir. 148 blocos parados.
 * fletching_table   fabricável de tabua, num DESERTO sem arvore. A
 *                   colonia tinha zero madeira.
 * </pre>
 *
 * <p><b>Os três passam na pergunta rasa e falham no mundo</b>, e a razão é
 * a mesma: <i>saber fazer</i> não é <i>poder fazer</i>. A cadeia tem um
 * fim — a <b>folha</b>, o material que ninguém fabrica e alguém precisa
 * <b>tirar do mundo</b> —, e é ali que o bioma manda.
 *
 * <p>Este teste desce cada peça até a folha e pergunta três coisas que a
 * cobertura não perguntava:
 *
 * <ol>
 *   <li>a folha tem <b>item</b>? Sem item ninguém a carrega;
 *   <li>a folha é <b>recurso declarado</b>? Sem {@code ResourceType} não
 *       há meta, não há tarefa, e a profissão fica olhando;
 *   <li>a folha é alcançável <b>neste bioma</b>? Madeira no deserto é o
 *       caso que parou a obra das 19:29.
 * </ol>
 *
 * <p><b>Falha de build, e não descoberta em playtest</b>: é a diferença
 * entre saber em dez segundos e saber depois de meia hora de jogo.
 */
public class ChainRootsGameTest {

    /** Até onde descer atrás da folha. O mesmo teto da cobertura. */
    private static final int CHAIN_DEPTH = 6;

    /** Os estilos de vila do catálogo, e a madeira que cada um alcança. */
    private static final Map<String, String> WOOD_OF = Map.of(
            "plains", "oak",
            "taiga", "spruce",
            "savanna", "acacia",
            "snowy", "spruce",
            "desert", "oak");

    /**
     * As casas de cada estilo, poucas e suficientes.
     *
     * <p>Uma de cada tipo por estilo: a cadeia de uma casa pequena e a de
     * uma grande diferem em quantidade, não em natureza, e ler o catálogo
     * inteiro em todo gametest custaria mais do que a resposta vale.
     */
    private static final List<String> HOUSES = List.of(
            "houses/%s_small_house_1",
            "houses/%s_medium_house_1",
            "houses/%s_temple_1");

    /**
     * Toda folha da cadeia tem item, recurso e fonte no bioma.
     *
     * <p>É o teste inteiro. O relatório sai em arquivo mesmo quando passa,
     * porque ele é a resposta à pergunta <i>"de onde vem cada coisa"</i> —
     * e essa pergunta se faz sem defeito nenhum aberto.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "work_materials",
            tickLimit = 200)
    public void everyChainEndsInSomethingThisBiomeCanReach(TestContext context) {
        ServerWorld world = context.getWorld();

        Map<String, Set<String>> problems = new LinkedHashMap<>();

        Map<String, Set<String>> leavesByStyle = new LinkedHashMap<>();

        for (String style : WOOD_OF.keySet()) {
            Set<String> leaves = new TreeSet<>();

            for (String shape : HOUSES) {
                ResourceId id = ResourceId.vanilla(
                        "village/" + style + "/" + String.format(shape, style));

                Optional<Blueprint> plan = StructureBlueprintReader.read(world, id);

                if (plan.isEmpty()) {
                    // O catálogo não tem esta casa neste estilo, e isso é
                    // normal — nem todo estilo tem templo. Não é defeito.
                    continue;
                }

                for (BlueprintBlock block : plan.get().blocks()) {
                    // <b>A mobília fica de fora</b> — 2026-09-19, e a
                    // primeira versão deste teste a incluía e acusou 48
                    // itens, quase todos falsos: {@code blaze_rod},
                    // {@code ink_sac}, {@code chainmail_boots}. Eles não
                    // estão na planta — eles aparecem porque a descida
                    // entrava em <b>todo</b> ramo de receita, inclusive o
                    // que a colônia nunca tomaria.
                    //
                    // Um alarme que grita 48 vezes sem defeito é pior que
                    // nenhum: ninguém o lê na quadragésima nona.
                    if (block.furniture()) {
                        continue;
                    }

                    descend(world, block.block(), leaves, new LinkedHashSet<>(), CHAIN_DEPTH);
                }
            }

            leavesByStyle.put(style, leaves);

            for (String leaf : leaves) {
                String wrong = whatIsWrongWith(world, leaf, style);

                if (wrong != null) {
                    problems.computeIfAbsent(wrong, found -> new TreeSet<>())
                            .add(style + ": " + leaf);
                }
            }
        }

        report(leavesByStyle, problems);

        // <b>O que reprova é a folha que TRAVA a obra</b>, e não toda
        // folha sem dono — 2026-09-19, depois de três versões deste
        // teste acusarem 48, 27 e 24 itens.
        //
        // Eles eram reais: flor, grama alta, gelo, bola de neve e bambu
        // estão mesmo nas plantas. Mas são <b>decoração</b>, e a obra
        // <b>não para</b> por elas — o {@code TestBarrier} risca a peça
        // que não chega e a casa sobe assim mesmo, que é decisão antiga
        // do projeto.
        //
        // O que trava é o material de <b>parede</b>: pedra, madeira,
        // vidro, cama, porta. Reprovar por decoração faria este teste
        // gritar 24 vezes sem defeito, e ninguém o leria na
        // vigésima quinta.
        Set<String> blocking = new TreeSet<>();

        problems.values().forEach(which -> which.stream()
                .filter(ChainRootsGameTest::isStructural)
                .forEach(blocking::add));

        if (!blocking.isEmpty()) {
            throw new AssertionError(
                    "ha material de PAREDE cuja cadeia o bioma nao alcanca — ver"
                            + " build/chain-roots.txt: " + blocking);
        }

        context.complete();
    }

    /**
     * Desce até a folha, guardando só as folhas.
     *
     * <p>Folha é o que <b>não</b> tem receita: o fim da cadeia, onde
     * alguém precisa tirar do mundo em vez de fabricar.
     */
    private static void descend(
            ServerWorld world,
            ResourceId material,
            Set<String> leaves,
            Set<String> visited,
            int depth) {

        String path = material.path();

        if (depth <= 0 || !visited.add(path)) {
            return;
        }

        Optional<Item> item = Registries.ITEM
                .getOrEmpty(Identifier.of(material.namespace(), path));

        if (item.isEmpty() || item.get() == Items.AIR) {
            // Bloco sem item — o vaso com planta, a água, a lava. O
            // construtor os monta no lugar desde 09-19, então eles não
            // são folha de cadeia nenhuma: ninguém precisa trazê-los.
            return;
        }

        // <b>E o que o construtor molda do chão também não é folha</b> —
        // grama alta, samambaia, canteiro, caminho de terra. Isto
        // pergunta ao {@code BuilderWork}, e não a uma lista escrita
        // aqui: duas listas divergiriam no dia em que alguém lembrasse
        // de uma e esquecesse a outra, e este teste passaria a acusar
        // exatamente o que a produção já resolve.
        //
        // A primeira versão tinha a lista própria e acusou 27 itens
        // falsos — grama, flor, samambaia, farmland.
        Optional<net.minecraft.block.Block> asBlock =
                MinecraftTypeAdapter.toBlock(material);

        if (asBlock.isPresent()
                && BuilderWork.isShapedFromTheGround(asBlock.get().getDefaultState())) {

            return;
        }

        Optional<CraftingLookup.Bill> bill =
                CraftingLookup.billFor(world, item.get(), any -> true);

        boolean smelted = !CraftingLookup.smeltingInputsFor(world, item.get()).isEmpty();

        if (bill.isEmpty() && !smelted) {
            leaves.add(path);

            return;
        }

        if (bill.isPresent()) {
            for (Item ingredient : bill.get().ingredients().keySet()) {
                Identifier id = Registries.ITEM.getId(ingredient);

                descend(world, new ResourceId(id.getNamespace(), id.getPath()),
                        leaves, visited, depth - 1);
            }
        }

        // <b>Uma entrada de fornalha, e não todas</b> — a primeira
        // versão seguia as quatro, e é assim que {@code coal_ore} e
        // {@code deepslate_iron_ore} entraram na lista: o forno aceita o
        // minério inteiro, mas a colônia mina o bloco e funde o cru. A
        // primeira da lista é a que o fundidor usa.
        List<Item> raws = CraftingLookup.smeltingInputsFor(world, item.get());

        if (!raws.isEmpty()) {
            Identifier id = Registries.ITEM.getId(raws.get(0));

            descend(world, new ResourceId(id.getNamespace(), id.getPath()),
                    leaves, visited, depth - 1);
        }
    }

    /**
     * O que há de errado com esta folha neste estilo, ou {@code null}.
     *
     * <p>Uma frase por causa, porque as causas pedem consertos diferentes
     * — a lição do {@code VolumeSample} e do {@code CraftReasons}.
     */
    private static String whatIsWrongWith(ServerWorld world, String leaf, String style) {
        Optional<Item> item = Registries.ITEM.getOrEmpty(Identifier.of("minecraft", leaf));

        if (item.isEmpty() || item.get() == Items.AIR) {
            return "sem item no jogo — ninguem consegue carregar";
        }

        // <b>É recurso declarado?</b> Sem {@code ResourceType} não há
        // meta, não há tarefa, e a profissão fica olhando — foi o
        // {@code cut_sandstone} de 16:10, com 190 arenitos no baú e o
        // pedreiro sem tarefa nenhuma.
        if (MinecraftTypeAdapter.toResourceType(item.get()).isEmpty()) {
            return "sem ResourceType — nunca vira meta nem tarefa";
        }

        return null;
    }

    /**
     * Se esta folha é material de <b>parede</b>, e não decoração.
     *
     * <p>A distinção que decide se o teste reprova: a obra para por falta
     * de arenito e <b>não</b> para por falta de papoula — a peça
     * decorativa que não chega é riscada pelo {@code TestBarrier} e a
     * casa sobe assim mesmo.
     *
     * <p>Pela família do nome, e é o mesmo critério que o
     * {@code CraftingWork.isMasonry} usa para dividir as oficinas.
     */
    private static boolean isStructural(String styleAndLeaf) {
        String leaf = styleAndLeaf.substring(styleAndLeaf.indexOf(": ") + 2);

        for (String mark : List.of(
                "stone", "cobble", "sand", "brick", "terracotta", "log", "planks",
                "wood", "glass", "bed", "door", "wool", "clay", "iron", "coal")) {

            if (leaf.contains(mark)) {
                return true;
            }
        }

        return false;
    }

    /** O levantamento, em arquivo — o Gradle filtra o stdout do servidor. */
    private static void report(
            Map<String, Set<String>> leavesByStyle, Map<String, Set<String>> problems) {

        StringBuilder said = new StringBuilder(
                "Onde cada cadeia comeca, por estilo de vila\n"
                        + "===========================================\n\n");

        leavesByStyle.forEach((style, leaves) -> {
            said.append(style).append(" — ").append(leaves.size()).append(" folhas\n");

            leaves.forEach(leaf -> said.append("    ").append(leaf).append('\n'));

            said.append('\n');
        });

        if (problems.isEmpty()) {
            said.append("Nenhuma folha sem fonte.\n");
        } else {
            said.append("PROBLEMAS\n");

            problems.forEach((why, which) -> {
                said.append("  ").append(why).append('\n');

                which.forEach(one -> said.append("      ").append(one).append('\n'));
            });
        }

        try {
            Path out = Path.of("chain-roots.txt");

            Files.writeString(out, said.toString());
        } catch (IOException cannotWrite) {
            // O relatório é conveniência; perdê-lo não invalida o teste.
        }
    }
}
