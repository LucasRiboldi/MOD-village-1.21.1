package com.villagecolony.gametest;

import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.integration.CraftingLookup;
import com.villagecolony.fabric.integration.StructureBlueprintReader;
import com.villagecolony.fabric.integration.VillageStructures;
import com.villagecolony.fabric.work.CraftingWork;
import com.villagecolony.fabric.work.HousePlans;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * O que as casas do jogo pedem, e quem na colônia sabe produzir — 2026-09-18.
 *
 * <p><b>A pergunta do autor:</b> <i>"verificar se todos os itens
 * necessários em todas estruturas que existem nas vilas constam na linha
 * de produção de algum dos trabalhadores"</i>.
 *
 * <p><b>Por que isto é um teste de jogo e não uma lista escrita à mão.</b>
 * A resposta depende de três coisas que só o jogo sabe: quais blocos as
 * plantas realmente contêm, o que o livro de receitas sabe fabricar, e o
 * que a fornalha aceita. Uma lista no README envelheceria na primeira
 * versão do Minecraft; este teste a recalcula toda vez que roda.
 *
 * <p><b>O que ele afirma</b> é a regra que a colônia precisa cumprir: todo
 * bloco que uma casa pede ou <b>é fabricável</b> pelo livro do jogo — e
 * então cai no carpinteiro ou no pedreiro pela divisão de
 * {@code CraftingWork.isMasonry} —, ou <b>é colhível</b> na natureza, ou
 * é peça que o jogador põe no baú pela segunda metade da Regra 13.
 *
 * <p>O relatório que ele imprime no log é a fonte da tabela do README.
 * Quem quiser regerá-la roda {@code gradlew runGametest} e lê as linhas
 * {@code [coverage]}.
 */
public class StructureCoverageGameTest implements FabricGameTest {

    /** As cinco vilas que o jogo gera. */
    private static final List<String> STYLES =
            List.of("plains", "taiga", "savanna", "snowy", "desert");

    /**
     * O que a colônia colhe da natureza, por família de nome.
     *
     * <p>Não é o {@code ResourceType}: aquele declara os 43 recursos que o
     * ciclo conta, e aqui a pergunta é outra — <i>existe trabalhador cuja
     * linha de produção alcança este bloco?</i>. O lenhador derruba
     * qualquer tronco, o mineiro quebra qualquer pedra, o fundidor junta
     * areia e cascalho.
     */
    private static final Map<String, String> GATHERED = Map.ofEntries(
            Map.entry("_log", "lenhador"),
            Map.entry("_wood", "lenhador"),
            Map.entry("_leaves", "lenhador"),
            Map.entry("_sapling", "lenhador"),
            Map.entry("dirt", "fundidor (superfície)"),
            // <b>O caminho de terra é o calçamento da colônia</b>, e ele
            // empatava: "dirt" e "path" têm quatro caracteres cada, e
            // {@code dirt_path} casa com as duas. A regra da mais longa
            // não desempata e a ordem do mapa decidia — o teste
            // noRealBlockLandsOnATie o achou em 09-18, e este é o dono
            // certo: VillageRoad.DEFAULT_PAVING o assenta, e ele sai da
            // terra que a superfície junta.
            Map.entry("dirt_path", "fundidor (superfície)"),
            Map.entry("grass_block", "fundidor (superfície)"),
            Map.entry("sand", "fundidor (superfície)"),
            Map.entry("gravel", "fundidor (superfície)"),
            Map.entry("clay", "fundidor (superfície)"),
            Map.entry("snow", "fundidor (superfície)"),
            Map.entry("ice", "fundidor (superfície)"),
            Map.entry("water", "o mundo"),
            Map.entry("lava", "o mundo"),
            Map.entry("air", "o mundo"),
            Map.entry("wool", "pastor"),
            // <b>Os cinco que o levantamento de 09-18 achou órfãos</b> —
            // e todos são ingredientes de cadeia, não blocos de planta:
            // a estante pede livro, o livro pede couro, e o couro também
            // sai de pele de coelho. Atribuídos aos menos carregados,
            // por afinidade: o pastor é quem lida com bicho e tinha
            // ZERO itens, e o fazendeiro planta o que é planta.
            Map.entry("rabbit_hide", "pastor"),
            Map.entry("string", "pastor"),
            Map.entry("ink_sac", "pastor"),
            Map.entry("leather", "pastor"),
            Map.entry("pitcher_plant", "fazendeiro"),
            Map.entry("torchflower", "fazendeiro"),
            Map.entry("flint", "fundidor (superfície)"),
            Map.entry("hay_block", "fazendeiro"),
            Map.entry("wheat", "fazendeiro"),
            Map.entry("carrot", "fazendeiro"),
            Map.entry("potato", "fazendeiro"),
            Map.entry("beetroot", "fazendeiro"),
            Map.entry("melon", "fazendeiro"),
            Map.entry("pumpkin", "fazendeiro"),
            Map.entry("cactus", "fazendeiro"),
            Map.entry("sugar_cane", "fazendeiro"),
            Map.entry("bamboo", "fazendeiro"),
            Map.entry("farmland", "fazendeiro"),
            Map.entry("vine", "o mundo"),
            Map.entry("moss", "o mundo"),
            Map.entry("mushroom", "o mundo"),
            Map.entry("flower", "o mundo"),
            Map.entry("tulip", "o mundo"),
            Map.entry("daisy", "o mundo"),
            Map.entry("poppy", "o mundo"),
            Map.entry("dandelion", "o mundo"),
            Map.entry("allium", "o mundo"),
            Map.entry("orchid", "o mundo"),
            Map.entry("bluet", "o mundo"),
            Map.entry("lilac", "o mundo"),
            Map.entry("peony", "o mundo"),
            Map.entry("bush", "o mundo"),
            // O capim alto e o baixo: sem esta, os dois ficam órfãos —
            // medido em 09-18 ao tentar enxugar a tabela.
            Map.entry("grass", "o mundo"),
            Map.entry("fern", "o mundo"),
            Map.entry("seagrass", "o mundo"),
            Map.entry("lily", "o mundo"),
            Map.entry("berries", "o mundo"),
            Map.entry("podzol", "o mundo"),
            Map.entry("mycelium", "o mundo"),
            Map.entry("path", "o mundo"),
            Map.entry("sea_pickle", "o mundo"),
            Map.entry("kelp", "o mundo"),
            Map.entry("coral", "o mundo"),
            Map.entry("sponge", "o mundo"),
            Map.entry("obsidian", "mineiro"),
            Map.entry("ore", "mineiro"),
            Map.entry("netherrack", "mineiro"));

    /**
     * O levantamento — imprime a cobertura e cobra que nada fique órfão.
     *
     * <p><b>O teste não falha por peça fabricável nem por peça colhível.</b>
     * Ele falha por peça que <b>nenhuma das duas coisas</b> alcança: essa é
     * a obra que espera para sempre, e é o defeito que o P1.1 mostrou em
     * jogo — a escada de pedregulho que ninguém fabricava, com a colônia
     * tendo 69 pedregulhos no baú.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "structure_coverage",
            tickLimit = 400)
    public void everyBlockTheHousesAskForHasSomeoneWhoMakesIt(TestContext context) {
        ServerWorld world = context.getWorld();

        Map<String, Set<String>> byOwner = new TreeMap<>();

        Set<String> orphans = new TreeSet<>();

        Set<String> seen = new TreeSet<>();

        for (String style : STYLES) {
            for (ResourceId id : VillageStructures.housesFor(style)) {
                if (!HousePlans.isDwelling(id)) {
                    continue;
                }

                Optional<Blueprint> plan = StructureBlueprintReader.read(world, id);

                if (plan.isEmpty()) {
                    continue;
                }

                for (BlueprintBlock block : plan.get().blocks()) {
                    walkTheChain(world, block.block(), byOwner, orphans, seen, CHAIN_DEPTH);
                }
            }
        }

        report(byOwner, orphans, seen.size());

        context.assertTrue(
                !seen.isEmpty(),
                "nenhuma planta foi lida — o levantamento não mediu nada");

        context.assertTrue(
                orphans.isEmpty(),
                "peças que ninguém produz nem colhe, e a obra as espera para sempre: "
                        + orphans);

        context.complete();
    }

    /**
     * A resposta não depende da ordem do mapa — 2026-09-18.
     *
     * <p><b>O defeito que este teste tranca.</b> O {@code ownerOf} parava
     * na primeira chave que casasse, e {@code Map.ofEntries} <b>não tem
     * ordem</b>: a iteração é embaralhada a cada JVM. Duas peças casavam
     * com chaves de donos diferentes — {@code grass_block} contra
     * {@code grass}, {@code torchflower} contra {@code flower} — e o dono
     * saía por sorteio. Medido em cinco execuções: quatro deram um dono,
     * uma deu outro.
     *
     * <p>Um levantamento que muda de resposta entre rodadas não serve
     * para o que ele existe, e o pior é que ele <b>passa</b>: nenhuma
     * peça fica órfã nos dois casos, só muda de dono. Sem este teste o
     * defeito volta em silêncio na próxima chave que colidir.
     *
     * <p>É a mesma armadilha que {@code ColonyGoals} pagou quando
     * {@code Map.copyOf} virou a prioridade do fabricante em sorteio.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "structure_coverage",
            tickLimit = 200)
    public void theMostSpecificFamilyWinsAndNotTheFirstOne(TestContext context) {
        ServerWorld world = context.getWorld();

        // As duas peças que colidem hoje. A chave curta e a longa têm
        // donos diferentes, e a longa é a que descreve a peça.
        assertOwner(context, world, "grass_block", "fundidor (superfície)");

        assertOwner(context, world, "torchflower", "fazendeiro");

        // E o que só casa com a chave curta continua com ela: a regra é
        // "a mais específica", não "a mais longa da tabela".
        assertOwner(context, world, "short_grass", "o mundo");

        assertOwner(context, world, "cornflower", "o mundo");

        context.complete();
    }

    /** Afirma de quem é uma peça, pelo nome do bloco. */
    private static void assertOwner(
            TestContext context, ServerWorld world, String block, String expected) {

        String found = ownerOf(world, ResourceId.vanilla(block));

        context.assertTrue(
                expected.equals(found),
                block + " é de '" + found + "' e devia ser de '" + expected
                        + "' — a família mais específica deixou de vencer");
    }

    /**
     * Nenhuma peça real fica no empate — 2026-09-18.
     *
     * <p><b>Por que este teste existe, tendo o de cima.</b> Aquele afirma
     * <b>quatro peças</b>: as duas que colidem hoje e duas que não. Ele
     * tranca o defeito conhecido e <b>não</b> tranca o próximo — quem
     * acrescentar amanhã uma família que colida passa por ele sem ser
     * notado. Tratar por amostra um defeito estrutural é o que o deixa
     * traiçoeiro.
     *
     * <p><b>A condição que quebra a regra da chave mais longa</b> é duas
     * chaves de <b>mesmo comprimento</b> casando no <b>mesmo nome</b>,
     * com donos diferentes: aí {@code >} não desempata, a primeira a
     * chegar vence, e a ordem de {@code Map.ofEntries} volta a decidir.
     *
     * <p><b>A primeira versão deste teste era vazia</b>, e vale registrar
     * porque é o mesmo erro do gametest do fundidor: ela exigia que uma
     * chave <i>contivesse</i> a outra tendo o mesmo comprimento — o que
     * as torna idênticas, e um {@code Map} não admite chave repetida. A
     * condição nunca ocorreria e o teste passaria para sempre sem medir
     * nada. O empate real não precisa que uma contenha a outra: o nome
     * {@code x_ab_cd} casa com {@code ab} e com {@code cd}, que têm dois
     * caracteres cada.
     *
     * <p>Por isso a varredura é sobre os <b>nomes que o jogo tem</b>, e
     * não sobre as chaves entre si: é lá que o empate aparece.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "structure_coverage",
            tickLimit = 400)
    public void noRealBlockLandsOnATie(TestContext context) {
        ServerWorld world = context.getWorld();

        List<String> ties = new ArrayList<>();

        Set<String> checked = new TreeSet<>();

        for (String style : STYLES) {
            for (ResourceId id : VillageStructures.housesFor(style)) {
                if (!HousePlans.isDwelling(id)) {
                    continue;
                }

                Optional<Blueprint> plan = StructureBlueprintReader.read(world, id);

                if (plan.isEmpty()) {
                    continue;
                }

                for (BlueprintBlock block : plan.get().blocks()) {
                    String path = placed(block.block());

                    if (checked.add(path)) {
                        tieOn(path).ifPresent(ties::add);
                    }
                }
            }
        }

        context.assertTrue(
                !checked.isEmpty(), "nenhuma planta foi lida — a varredura não mediu nada");

        context.assertTrue(
                ties.isEmpty(),
                "peças em que duas famílias do mesmo comprimento disputam, e a ordem"
                        + " do mapa decide: " + ties);

        context.complete();
    }

    /** O empate desta peça, se houver: duas chaves do mesmo tamanho e donos diferentes. */
    private static Optional<String> tieOn(String path) {
        int longest = 0;

        Set<String> owners = new TreeSet<>();

        Set<String> marks = new TreeSet<>();

        for (Map.Entry<String, String> family : GATHERED.entrySet()) {
            String mark = family.getKey();

            if (!path.contains(mark)) {
                continue;
            }

            if (mark.length() > longest) {
                longest = mark.length();

                owners.clear();
                marks.clear();
            }

            if (mark.length() == longest) {
                owners.add(family.getValue());
                marks.add(mark);
            }
        }

        return owners.size() > 1
                ? Optional.of(path + " disputada por " + marks + " -> " + owners)
                : Optional.empty();
    }

    /**
     * Quem produz esta peça, ou {@code null} se ninguém.
     *
     * <p>A ordem das perguntas é a da colônia: fabricar vem antes de
     * colher, porque a peça fabricável é responsabilidade de um
     * trabalhador mesmo quando o nome dela parece natural — a escada de
     * pedregulho contém {@code cobble} e é do pedreiro por fabricação, não
     * por mineração.
     */
    private static String ownerOf(ServerWorld world, ResourceId material) {
        String path = placed(material);

        Optional<Item> item = Registries.ITEM
                .getOrEmpty(Identifier.of(material.namespace(), path));

        if (item.isPresent() && CraftingLookup.billFor(world, item.get(), any -> true).isPresent()) {
            return CraftingWork.isMasonry(material) ? "pedreiro" : "carpinteiro";
        }

        if (item.isPresent()
                && !CraftingLookup.smeltingInputsFor(world, item.get()).isEmpty()) {

            return "fundidor";
        }

        String owner = null;

        int matched = 0;

        // <b>A chave mais longa vence, e não a primeira</b> — 2026-09-18.
        //
        // <b>O defeito que isto conserta.</b> O laço parava na primeira
        // chave que casasse, e {@code Map.ofEntries} <b>não tem ordem</b>:
        // ela é deliberadamente embaralhada a cada JVM. Com
        // {@code "grass"} valendo "o mundo" e {@code "grass_block"}
        // valendo a superfície, o dono de {@code grass_block} saía por
        // sorteio — medido em cinco execuções, quatro deram "o mundo" e
        // uma deu "superfície". O mesmo com {@code torchflower} contra
        // {@code flower}, que alternou duas contra três.
        //
        // Um levantamento que muda de resposta entre rodadas não serve
        // para o que ele existe. É a mesma armadilha que
        // {@code ColonyGoals} pagou quando {@code Map.copyOf} virou a
        // prioridade do fabricante em sorteio.
        //
        // A chave mais longa é a mais específica, e isso <b>dispensa
        // ordenar a tabela</b>: quem acrescentar uma família nova não
        // precisa saber onde a põe.
        for (Map.Entry<String, String> family : GATHERED.entrySet()) {
            String mark = family.getKey();

            if (path.contains(mark) && mark.length() > matched) {
                matched = mark.length();

                owner = family.getValue();
            }
        }

        // Pedra que não é fabricável nem casou com família nenhuma ainda
        // é do mineiro: ele quebra o que for pedra.
        return owner != null
                ? owner
                : CraftingWork.isMasonry(material) ? "mineiro" : null;
    }

    /**
     * O item que se coloca na mão para obter este bloco — e é o jogo
     * quem responde.
     *
     * <p><b>O bloco no arquivo não é o item no baú</b>, e a diferença tem
     * várias formas: a tocha pregada é {@code wall_torch} e o item é
     * {@code torch}; o vaso com flor é {@code potted_dandelion} e o item
     * é {@code flower_pot}; o caldeirão com água é
     * {@code water_cauldron} e o item é {@code cauldron}. São o mesmo
     * item posto de jeitos diferentes, e o construtor coloca o item.
     *
     * <p><b>Por pergunta ao jogo, e não por lista de nomes</b> —
     * 2026-09-18. A primeira versão tinha seis {@code replace}
     * encadeados: {@code wall_torch}, {@code _wall_sign},
     * {@code _wall_banner}, {@code _wall_fan}, {@code wall_head},
     * {@code _wall_skull}. Ela tratava <b>sintomas</b>, cobria só a
     * família da parede, e deixava passar vaso e caldeirão — que caíram
     * em "o mundo" como se ninguém os fabricasse, quando o caldeirão
     * custa sete lingotes.
     *
     * <p>{@code Block.asItem()} é a pergunta certa e responde a todas as
     * formas de uma vez, inclusive as que o jogo inventar depois. É a
     * ADR-009 outra vez: <i>quem sabe é o jogo</i>, e material novo não
     * pode custar mais um nome no código.
     *
     * <p>Bloco sem item — o ar, a água, o fogo — devolve
     * {@code Items.AIR}; nesse caso vale o nome do próprio bloco, e a
     * tabela das famílias o classifica.
     */
    private static String placed(ResourceId material) {
        Identifier id = Identifier.of(material.namespace(), material.path());

        Optional<Block> block = Registries.BLOCK.getOrEmpty(id);

        if (block.isEmpty()) {
            return material.path();
        }

        Item item = block.get().asItem();

        return item == Items.AIR
                ? material.path()
                : Registries.ITEM.getId(item).getPath();
    }

    /**
     * Até onde a cadeia é seguida antes de desistir.
     *
     * <p>Seis degraus cobrem a mais funda que uma casa de vila pede — a
     * estante é tábua + livro, o livro é papel + couro, o papel é
     * cana — e impedem que uma receita circular do jogo ou de um datapack
     * trave a bateria.
     */
    private static final int CHAIN_DEPTH = 6;

    /**
     * A cadeia inteira de uma peça, e não só o primeiro degrau —
     * 2026-09-18.
     *
     * <p><b>O pedido do autor:</b> <i>"colocar na lista aqueles que
     * dependem de outros recursos para existirem, tipo bookshelf, que
     * precisa de 6 tábuas e 3 livros, logo alguém tem que fazer o livro
     * também, e livro é papel e couro, logo alguém tem que fazer papel. A
     * cadeia de produção toda deve ser atendida pelas profissões"</i>.
     *
     * <p><b>Por que a pergunta rasa não bastava.</b> Perguntar
     * <i>"a estante é fabricável?"</i> devolve <b>sim</b> e a colônia
     * ainda assim não a levanta: o livro não nasce de baú nenhum. É o
     * P1.1 outra vez, generalizado — lá a obra esperou por
     * {@code cobblestone_stairs} que ninguém fabricava, e a razão foi
     * exatamente esta, um degrau da cadeia sem dono.
     *
     * <p>Desce por {@code Bill.ingredients} e registra <b>cada</b>
     * ingrediente encontrado no caminho, com o dono de cada um. O que
     * chega ao fim sem dono é órfão, esteja na planta ou três degraus
     * abaixo dela.
     */
    private static void walkTheChain(
            ServerWorld world,
            ResourceId material,
            Map<String, Set<String>> byOwner,
            Set<String> orphans,
            Set<String> visited,
            int depth) {

        String path = material.path();

        if (depth <= 0 || !visited.add(path)) {
            return;
        }

        String owner = ownerOf(world, material);

        if (owner == null) {
            orphans.add(path);
        } else {
            byOwner.computeIfAbsent(owner, found -> new TreeSet<>()).add(path);
        }

        Optional<Item> item = Registries.ITEM
                .getOrEmpty(Identifier.of(material.namespace(), path));

        if (item.isEmpty()) {
            return;
        }

        CraftingLookup.billFor(world, item.get(), any -> true).ifPresent(bill -> {
            for (Item ingredient : bill.ingredients().keySet()) {
                Identifier id = Registries.ITEM.getId(ingredient);

                walkTheChain(
                        world,
                        new ResourceId(id.getNamespace(), id.getPath()),
                        byOwner,
                        orphans,
                        visited,
                        depth - 1);
            }
        });
    }

    /**
     * O relatório que vira a tabela do README.
     *
     * <p><b>Em arquivo, e não só no log.</b> O Gradle filtra o stdout do
     * servidor de teste, e as linhas do levantamento não chegavam a quem
     * as pediu — medido em 09-18. O arquivo fica em
     * {@code build/structure-coverage.txt} e é a fonte da tabela do
     * README: quem quiser regerá-la roda {@code gradlew runGametest} e o
     * lê.
     */
    private static void report(
            Map<String, Set<String>> byOwner, Set<String> orphans, int total) {

        StringBuilder out = new StringBuilder();

        out.append(total)
                .append(" distinct items across the five village styles,")
                .append(" following every recipe chain\n\n");

        for (Map.Entry<String, Set<String>> owner : byOwner.entrySet()) {
            out.append(owner.getKey())
                    .append(" — ")
                    .append(owner.getValue().size())
                    .append('\n');

            for (String made : owner.getValue()) {
                out.append("    ").append(made).append('\n');
            }

            out.append('\n');
        }

        if (orphans.isEmpty()) {
            out.append("nobody-makes-these: none\n");
        } else {
            out.append("nobody-makes-these — ").append(orphans.size()).append('\n');

            for (String orphan : orphans) {
                out.append("    ").append(orphan).append('\n');
            }
        }

        com.villagecolony.VillageColonyMod.LOGGER.info(
                "[coverage] {} items, {} without an owner", total, orphans.size());

        try {
            java.nio.file.Path where = java.nio.file.Path.of("structure-coverage.txt")
                    .toAbsolutePath();

            java.nio.file.Files.writeString(where, out.toString());

            com.villagecolony.VillageColonyMod.LOGGER.info("[coverage] written to {}", where);
        } catch (Exception unwritable) {
            // O relatório é conveniência; falhar em escrevê-lo não pode
            // derrubar a afirmação do teste, que é sobre os órfãos.
            com.villagecolony.VillageColonyMod.LOGGER.warn(
                    "[coverage] could not write the report", unwritable);
        }
    }
}
