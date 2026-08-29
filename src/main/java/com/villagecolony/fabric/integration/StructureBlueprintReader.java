package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.core.type.ColonyPos;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Lê uma estrutura do próprio jogo e devolve um projeto — TASK-031.
 *
 * <p>Mesma escolha que a Fase 9 fez com a receita da tábua: perguntar ao
 * Minecraft em vez de escrever a resposta no mod. A casa que a colônia
 * levanta é a casa da vila porque é literalmente o mesmo arquivo que o
 * gerador de mundo usa. Regra 6, e Construction-System.md §"Fonte das
 * Construções".
 *
 * <p>Toda a conversão para tipos do Core acontece aqui, como manda a
 * ADR-005: sai {@link Blueprint}, que não conhece {@code BlockState}.
 *
 * <p><b>O que é descartado na leitura, e por quê:</b>
 *
 * <ul>
 *   <li><b>ar</b> — um projeto guarda o que há de colocar, não o vazio
 *       dentro da casa. O vazio é assunto do PREPARING;
 *   <li><b>blocos de andaime do gerador</b> — a casa Vanilla é uma peça
 *       de jigsaw, e traz blocos de estrutura e de jigsaw que existem
 *       para o gerador encaixar peças. Colocá-los deixaria blocos de
 *       comando na vila do jogador;
 *   <li><b>o estado do bloco</b> — ver {@link BlueprintBlock}. A escada
 *       sai no padrão, sem a orientação que tinha no arquivo.
 * </ul>
 *
 * <p><b>Custo.</b> Ler um template é abrir e decodificar um arquivo. Uma
 * casa de planície tem algumas centenas de blocos, e isso não pode
 * acontecer a cada ciclo — quem chamar guarda o resultado. O projeto é
 * imutável de propósito, para poder ser guardado sem medo.
 */
public final class StructureBlueprintReader {

    /**
     * A casa do MVP.
     *
     * <p>O jogo tem oito variantes de {@code plains_small_house}, e a
     * vila gerada usa todas. O MVP levanta a primeira — Construction-
     * System.md §"Seleção da Estrutura" diz "apenas Plains Small House",
     * e escolher entre as oito é decisão que não muda nada do que a Fase
     * 10 precisa provar.
     */
    public static final ResourceId PLAINS_SMALL_HOUSE =
            ResourceId.vanilla("village/plains/houses/plains_small_house_1");

    /**
     * A casa pequena, agora como schema do próprio mod.
     *
     * <p>É o mesmo arquivo do jogo, copiado para
     * {@code data/villagecolony/structure/houses/small_house.nbt} em
     * 2026-08-19. Ver o README daquela pasta.
     *
     * <p><b>Por que copiar em vez de apontar para o do jogo.</b> A
     * planta que o mod garante existir não depende de o jogo continuar
     * gerando aquela peça com aquele nome, e pode ser trocada por um
     * datapack sem recompilar nada. É a mesma razão que fez
     * {@code ColonyHut} ser escrita em código.
     *
     * <p>Não é a obra da colônia hoje — ver a Regra 13, e a lista de
     * materiais no README.
     */
    public static final ResourceId SMALL_HOUSE =
            new ResourceId("villagecolony", "houses/small_house");

    /**
     * A chave do nome do bloco dentro de uma entrada da paleta.
     *
     * <p>Escrita aqui porque o jogo não a expõe como constante: é o
     * formato de {@code NbtHelper.fromBlockState}, o mesmo que o bloco
     * de estrutura grava.
     */
    private static final String BLOCK_NAME_KEY = "Name";

    /**
     * A chave das propriedades dentro de uma entrada da paleta.
     *
     * <p>Mesma situação do {@link #BLOCK_NAME_KEY}: é o formato que o
     * jogo grava, e ele não o expõe como constante.
     *
     * <p>O projeto continua descartando o estado do bloco (ADR-005). O
     * que se lê daqui é uma pergunta só, e ela não vira dado do Core:
     * <em>esta entrada é a metade de cima de um bloco de duas partes?</em>
     * Ver {@link #isSecondHalf}.
     */
    private static final String PROPERTIES_KEY = "Properties";

    /**
     * A chave dos dados de bloco de uma entrada de {@code blocks}.
     *
     * <p>É por posição, e não por entrada da paleta: dois encaixes com o
     * mesmo estado podem prometer blocos diferentes, e prometem — na
     * casa de planície um vira tábua e o outro vira degrau.
     */
    private static final String ENTRY_NBT_KEY = "nbt";

    /**
     * O bloco em que um encaixe se transforma quando a vila é gerada.
     *
     * <p>Mesma situação do {@link #BLOCK_NAME_KEY}: é o formato que o
     * jogo grava, e ele não o expõe como constante.
     */
    private static final String FINAL_STATE_KEY = "final_state";

    /** Onde começa o estado dentro de um {@code final_state}. */
    private static final char STATE_OPENS = '[';

    private StructureBlueprintReader() {
    }

    /**
     * O que é mobília numa planta lida do jogo — a Regra 21.
     *
     * <p>Mobília não segura a obra: falta o material dela e a casa
     * termina assim mesmo, com a peça entrando depois. A regra nasceu
     * para a cabana do mod, onde a lista era escrita à mão; numa casa
     * lida de arquivo é preciso reconhecê-la.
     *
     * <p>A lista é curta de propósito, e cada item tem o mesmo motivo:
     * a colônia não sabe fazê-lo e ele não sustenta parede nenhuma.
     * Cama pede lã, tocha e lanterna pedem carvão e ferro. Baú entra
     * porque é mobília por natureza, ainda que a colônia saiba fazê-lo —
     * a casa sem baú continua sendo casa.
     *
     * <p><b>O que fica de fora é o ponto.</b> Pedregulho, vidraça e
     * tronco descascado <b>seguram</b> a obra, e é assim que tem de ser:
     * são parede, e casa sem parede não é casa. Quando a colônia não os
     * produz, a obra espera pelo jogador — e o relatório diz por quê.
     */
    private static boolean isFurniture(ResourceId block) {
        String name = block.path();

        return name.endsWith("_bed")
                || name.equals("torch")
                || name.equals("wall_torch")
                || name.equals("lantern")
                || name.equals("chest");
    }

    /**
     * Se esta entrada da paleta é a <b>segunda</b> metade de um bloco que
     * ocupa dois lugares — a parte de cima de uma porta, a cabeceira de
     * uma cama.
     *
     * <p>É o E8 do §17. No arquivo do jogo uma porta são duas entradas
     * com o mesmo nome, distinguidas só pela propriedade {@code half}; a
     * cama, duas com {@code part}. O projeto guardava as duas, e a obra
     * punha dois blocos independentes no estado padrão — duas metades de
     * baixo empilhadas, dois pés de cama lado a lado.
     *
     * <p>A saída é não guardar a segunda. Quem a põe é o jogo, quando o
     * construtor puser a primeira — ver {@code BuilderWork.placeSecondHalf}.
     * É a ADR-001 outra vez: perguntar ao Minecraft em vez de escrever a
     * resposta aqui.
     *
     * <p>Conserta também uma conta que ninguém tinha notado: a porta
     * custava <b>duas</b> portas ao estoque da colônia, porque cada
     * metade era um bloco do projeto e cada bloco do projeto tira uma
     * peça do baú. Uma porta no Vanilla é um item só.
     *
     * <p>Os nomes de propriedade e de valor vêm do próprio jogo
     * ({@code Properties.DOUBLE_BLOCK_HALF.getName()} e afins) e não de
     * literais escritos aqui — se o Vanilla os renomear, isto acompanha.
     *
     * <p>O que continua fora: a <b>orientação</b>. Escada e porta saem no
     * padrão, e a cabeceira da cama vai para onde o estado padrão apontar
     * e não para onde o arquivo dizia. É a outra metade do E8, e ela é a
     * que exige a decisão da TASK-046.
     */
    private static boolean isSecondHalf(NbtCompound paletteEntry) {
        NbtCompound properties = paletteEntry.getCompound(PROPERTIES_KEY);

        if (properties.isEmpty()) {
            return false;
        }

        return DoubleBlockHalf.UPPER.asString().equals(
                        properties.getString(Properties.DOUBLE_BLOCK_HALF.getName()))
                || BedPart.HEAD.asString().equals(
                        properties.getString(Properties.BED_PART.getName()));
    }

    /**
     * O projeto correspondente a uma estrutura do jogo.
     *
     * @return vazio se o jogo não conhece essa estrutura, ou se ela não
     *     tem bloco algum a colocar. Não é exceção: um id errado ou um
     *     datapack que removeu a estrutura são condições do mundo, não
     *     erro de programação — e derrubar o tick do servidor por causa
     *     disso seria pior que não construir
     */
    public static Optional<Blueprint> read(ServerWorld world, ResourceId structure) {
        Identifier id = MinecraftTypeAdapter.toIdentifier(structure);

        Optional<StructureTemplate> template =
                world.getStructureTemplateManager().getTemplate(id);

        if (template.isEmpty()) {
            VillageColonyMod.LOGGER.warn("The game has no structure named {}", structure);

            return Optional.empty();
        }

        List<BlueprintBlock> blocks = blocksOf(template.get());

        if (blocks.isEmpty()) {
            // Não é o mesmo que a estrutura não existir, e as duas
            // correções são diferentes: id errado se resolve mudando o
            // nome, template que não enumera se resolve mudando a
            // leitura. Uma linha só para os dois casos foi o que fez a
            // primeira falha desta classe custar uma rodada inteira.
            VillageColonyMod.LOGGER.warn(
                    "Structure {} loaded with size {} but yielded no placeable block",
                    structure,
                    template.get().getSize());

            return Optional.empty();
        }

        return Optional.of(Blueprint.of(structure, blocks));
    }

    /**
     * Os blocos do template, já convertidos e filtrados.
     *
     * <p><b>Lido do NBT, e não da API de enumeração.</b> O caminho óbvio
     * seria {@code getInfosForBlock(pos, data, null)} — passar
     * {@code null} no lugar do bloco procurado, na expectativa de que
     * "nenhum filtro" significasse "todos". Na 1.21.1 isso devolve
     * <b>zero</b> blocos, e devolve em silêncio: o template carrega, diz
     * o tamanho certo e entrega uma lista vazia. Custou uma rodada de
     * gametest até a linha de diagnóstico separar "o jogo não tem essa
     * estrutura" de "tem, e não enumerou".
     *
     * <p>{@code writeNbt} é o formato que o próprio jogo grava em disco,
     * e ele é melhor para o que este mod precisa: traz o <b>nome</b> de
     * cada bloco na paleta, que é exatamente o que {@link Blueprint}
     * guarda. O caminho pela API daria {@code BlockState} completo, e o
     * estado é justamente o que se descarta na conversão.
     *
     * <p>Cada entrada de {@code blocks} aponta para um índice da paleta.
     * A paleta é curta — uma casa inteira usa uma dúzia de blocos
     * distintos —, e é por isso que o formato existe.
     */
    private static List<BlueprintBlock> blocksOf(StructureTemplate template) {
        NbtCompound nbt = template.writeNbt(new NbtCompound());

        NbtList palette = nbt.getList(StructureTemplate.PALETTE_KEY, NbtElement.COMPOUND_TYPE);
        NbtList entries = nbt.getList(StructureTemplate.BLOCKS_KEY, NbtElement.COMPOUND_TYPE);

        List<ResourceId> names = new ArrayList<>(palette.size());

        // Paralelo à paleta: quais entradas são a metade de cima de um
        // bloco de duas partes, e por isso não entram no projeto.
        boolean[] secondHalf = new boolean[palette.size()];

        for (int i = 0; i < palette.size(); i++) {
            NbtCompound entry = palette.getCompound(i);

            names.add(ResourceId.parse(entry.getString(BLOCK_NAME_KEY)));
            secondHalf[i] = isSecondHalf(entry);
        }

        List<BlueprintBlock> blocks = new ArrayList<>();

        for (int i = 0; i < entries.size(); i++) {
            NbtCompound entry = entries.getCompound(i);

            int index = entry.getInt(StructureTemplate.BLOCKS_STATE_KEY);

            if (index < 0 || index >= names.size()) {
                // Paleta e blocos discordando é arquivo corrompido.
                // Pular a entrada perde um bloco; aceitar o índice
                // estouraria dentro do tick do servidor.
                continue;
            }

            ResourceId name = names.get(index);

            if (isJigsaw(name)) {
                // <b>Encaixe não é andaime</b> — 2026-08-29. Ele carrega
                // no próprio arquivo o bloco em que vira, e descartá-lo
                // abria buraco: o piso da casa de planície tem nove
                // tábuas, e a do meio é um encaixe. O autor viu em jogo,
                // e a frase dele foi "falta um bloco central no chão".
                Optional<ResourceId> promised = finalStateOf(entry);

                if (promised.isEmpty()) {
                    continue;
                }

                name = promised.get();
            } else if (isScaffolding(name)) {
                continue;
            }

            if (secondHalf[index]) {
                continue;
            }

            blocks.add(isFurniture(name)
                    ? BlueprintBlock.furniture(offsetOf(entry), name)
                    : new BlueprintBlock(offsetOf(entry), name));
        }

        // <b>A mobília por último — a Regra 32, 2026-08-29.</b> Regra do
        // autor depois de ver a casa em jogo: "criar uma regra para
        // adicionar os móveis e cama depois da casa pronta". A sessão
        // mostrou as duas razões, as duas no log: três tochas de parede
        // riscadas com "nothing holds it", porque vinham antes da parede
        // que as segura; e a cama pela metade, porque a cabeceira foi
        // decidida contra um pedregulho que ainda não estava lá.
        //
        // Os dois são o mesmo defeito de ordem. A ordem de baixo para
        // cima garante o que está <b>embaixo</b>, e mobília depende do
        // que está <b>ao lado</b> — só a casa inteira responde isso.
        //
        // De baixo para cima dentro de cada grupo, e a ordem é do
        // projeto, não do construtor: o arquivo do jogo não promete
        // ordem nenhuma, e um construtor que seguisse a ordem do arquivo
        // poria o telhado antes da parede. Dentro da mesma altura, por x
        // e depois por z, para que duas leituras do mesmo arquivo deem
        // exatamente a mesma casa — obra com ordem instável é impossível
        // de depurar (Debugging-Strategy.md).
        blocks.sort(Comparator
                .comparing((BlueprintBlock block) -> block.furniture())
                .thenComparingInt(block -> block.offset().y())
                .thenComparingInt(block -> block.offset().x())
                .thenComparingInt(block -> block.offset().z()));

        return blocks;
    }

    /** A posição relativa de uma entrada, que vem como lista de três ints. */
    private static ColonyPos offsetOf(NbtCompound entry) {
        NbtList pos = entry.getList(StructureTemplate.BLOCKS_POS_KEY, NbtElement.INT_TYPE);

        return new ColonyPos(pos.getInt(0), pos.getInt(1), pos.getInt(2));
    }

    /**
     * Se este bloco existe para o gerador e não para a casa.
     *
     * <p>O ar entra aqui junto com os dois blocos de encaixe: os três
     * são "não é para colocar", e separá-los em duas perguntas só faria
     * quem chama perguntar duas vezes.
     *
     * <p>Compara por nome porque é nome o que a paleta traz. Os três
     * saem do registro em vez de virem escritos: {@code Blocks.AIR} é a
     * fonte, e uma string "minecraft:air" aqui envelheceria sozinha.
     */
    private static boolean isScaffolding(ResourceId name) {
        return name.equals(MinecraftTypeAdapter.toResourceId(Blocks.AIR))
                || name.equals(MinecraftTypeAdapter.toResourceId(Blocks.CAVE_AIR))
                || name.equals(MinecraftTypeAdapter.toResourceId(Blocks.STRUCTURE_BLOCK));
    }

    /**
     * Se este é um bloco de encaixe do gerador de vilas.
     *
     * <p><b>Saiu do {@link #isScaffolding} em 2026-08-29</b>, e a
     * diferença é de natureza. Bloco de estrutura é andaime de verdade:
     * ele não vira nada, e o Vanilla o apaga. Encaixe <b>promete</b> um
     * bloco — o {@code final_state} —, e é esse bloco que a vila gerada
     * tem no lugar dele.
     */
    private static boolean isJigsaw(ResourceId name) {
        return name.equals(MinecraftTypeAdapter.toResourceId(Blocks.JIGSAW));
    }

    /**
     * O bloco que este encaixe promete virar.
     *
     * <p>O {@code final_state} vem como texto de estado completo —
     * {@code minecraft:oak_stairs[facing=east,half=bottom,...]} —, e o
     * que entra no projeto é só o nome: o estado é o que a ADR-005
     * descarta, e a ADR-008 é quem vai devolvê-lo, para este bloco e
     * para todos os outros ao mesmo tempo.
     *
     * @return vazio quando o encaixe não promete nada, ou promete ar —
     *     os dois casos são "aqui não vai bloco", que é o que o leitor
     *     já fazia com o encaixe inteiro
     */
    private static Optional<ResourceId> finalStateOf(NbtCompound entry) {
        String promised = entry.getCompound(ENTRY_NBT_KEY).getString(FINAL_STATE_KEY);

        if (promised.isEmpty()) {
            return Optional.empty();
        }

        int state = promised.indexOf(STATE_OPENS);

        ResourceId name =
                ResourceId.parse(state < 0 ? promised : promised.substring(0, state));

        return isScaffolding(name) || isJigsaw(name) ? Optional.empty() : Optional.of(name);
    }
}
