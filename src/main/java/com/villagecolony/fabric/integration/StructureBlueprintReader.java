package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.core.type.ColonyPos;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
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
     * A chave do nome do bloco dentro de uma entrada da paleta.
     *
     * <p>Escrita aqui porque o jogo não a expõe como constante: é o
     * formato de {@code NbtHelper.fromBlockState}, o mesmo que o bloco
     * de estrutura grava.
     */
    private static final String BLOCK_NAME_KEY = "Name";

    private StructureBlueprintReader() {
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

        for (int i = 0; i < palette.size(); i++) {
            names.add(ResourceId.parse(palette.getCompound(i).getString(BLOCK_NAME_KEY)));
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

            if (isScaffolding(name)) {
                continue;
            }

            blocks.add(new BlueprintBlock(offsetOf(entry), name));
        }

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
                || name.equals(MinecraftTypeAdapter.toResourceId(Blocks.STRUCTURE_BLOCK))
                || name.equals(MinecraftTypeAdapter.toResourceId(Blocks.JIGSAW));
    }
}
