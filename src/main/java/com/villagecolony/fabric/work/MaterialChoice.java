package com.villagecolony.fabric.work;

import com.villagecolony.core.resource.service.ResourceSubstitution;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.type.Substitution;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.state.property.Property;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * O que o construtor pode assentar no lugar do bloco pedido — 2026-08-26.
 *
 * <p><b>A Regra 27 abriu para pedra, e este arquivo é a abertura.</b> Ela
 * é imutável desde 2026-08-20 e manda o construtor <i>aguardar a
 * existência do específico tipo de bloco que ele precisa</i>. O autor a
 * emendou em 08-26 com três palavras — <i>abre para pedra só</i> — e
 * de novo em 09-05, com a madeira: o que se substitui é a
 * <b>espécie</b>, e nunca a peça. Fora dessas duas famílias o construtor
 * continua esperando o exato — vidraça é vidraça, e tocha é tocha.
 *
 * <p><b>Só {@link Substitution#ALTERNATIVE} entra aqui</b>, e é essa a
 * diferença entre os dois níveis do meio. {@code ACCEPTABLE} conta para
 * a meta da colônia — abeto responde por carvalho quando a pergunta é
 * "tenho tronco?" — e não vai para a parede. {@code ALTERNATIVE} vai.
 *
 * <p><b>O preferido primeiro, sempre.</b> A casa sai com o bloco certo
 * enquanto ele existir; o substituto entra quando o certo acabou. É a
 * frase da ADR-009 §3.10: <i>deserto prefere arenito; isso não quer
 * dizer que só possa arenito</i>.
 *
 * <p><b>Por que isto não ressuscita o defeito de 2026-08-22.</b> Naquele
 * dia a conta aceitava pedregulho por arenito e o construtor não: a
 * colônia declarava a meta cumprida, o mineiro não ia cavar, e a obra
 * dormia esperando o que ninguém buscaria. O defeito era a
 * <b>discordância</b>. Esta classe existe para desfazê-la — o que a
 * conta aceita é o que a parede aceita, e as duas leem a mesma tabela.
 */
public final class MaterialChoice {

    private MaterialChoice() {
    }

    /**
     * Os itens que servem para este bloco, do preferido ao último.
     *
     * <p>Nunca vazio: o primeiro é sempre o item do próprio bloco, mesmo
     * quando a colônia não acompanha aquele recurso — e é o caso da
     * esmagadora maioria deles.
     *
     * <p>A ordem sai de {@code ResourceSubstitution.byPreference}, e não
     * daqui: duas ordens diferentes seriam duas políticas.
     */
    public static List<Item> forBlock(Block wanted) {
        Item exact = wanted.asItem();

        List<Item> order = new ArrayList<>();

        order.add(exact);

        Optional<ResourceType> resource = MinecraftTypeAdapter.toResourceType(exact);

        if (resource.isEmpty()) {
            // Bloco que a colônia não conta — escada, porta, vidraça. Não
            // há substituição declarada para o que não é recurso, e é
            // pela família de madeira que ela chega aqui.
            return sameShapeInAnotherWood(exact, order);
        }

        for (ResourceType candidate : ResourceSubstitution.byPreference(resource.get())) {
            if (ResourceSubstitution.levelOf(resource.get(), candidate)
                    != Substitution.ALTERNATIVE) {

                // PREFERRED é o próprio, e já entrou. ACCEPTABLE conta
                // para a meta e não vai para a parede — a Regra 27 só
                // abriu para o nível de baixo.
                continue;
            }

            MinecraftTypeAdapter.toItem(candidate).ifPresent(item -> {
                if (!order.contains(item)) {
                    order.add(item);
                }
            });
        }

        return List.copyOf(order);
    }

    /**
     * As peças de madeira que se substituem por espécie — 2026-09-05.
     *
     * <p>São as marcações do próprio jogo, e não uma lista escrita aqui:
     * uma escada de madeira é o que a tag {@code wooden_stairs} diz que
     * é, em qualquer versão e com qualquer mod instalado. Escrever
     * "carvalho, bétula, abeto, cerejeira…" seria refazer, com risco de
     * errar, uma tabela que o Vanilla já mantém — o mesmo argumento do
     * {@code ToolUpgrade}.
     */
    private static final List<TagKey<Item>> WOODEN_SHAPES = List.of(
            ItemTags.WOODEN_STAIRS,
            ItemTags.WOODEN_SLABS,
            ItemTags.WOODEN_DOORS,
            ItemTags.WOODEN_TRAPDOORS,
            ItemTags.WOODEN_FENCES,
            ItemTags.WOODEN_BUTTONS,
            ItemTags.WOODEN_PRESSURE_PLATES);

    /**
     * A mesma peça, na madeira que a colônia tiver — 2026-09-05.
     *
     * <p><b>O buraco no meio do teto.</b> Decisão do autor depois da
     * sessão de 2026-09-04: <i>"a construção das Small House fica um
     * buraco no centro do teto"</i>. A causa não era bloco faltando na
     * planta — a camada de cima da casa de planície é uma calota 3×3, oito
     * {@code oak_stairs} em volta de uma tábua, e a camada de baixo tem um
     * vão de um bloco no centro <b>de propósito</b>, porque a calota o
     * tapa. A obra parava antes da calota:
     *
     * <pre>
     * WAITING_RESOURCES ... 13 blocks left, waiting for minecraft:oak_stairs
     * Builder stopped — no minecraft:oak_stairs in the colony chests
     * </pre>
     *
     * <p>E não era falta de madeira: a colônia tinha <b>1.257 tábuas de
     * cerejeira</b> e duas de carvalho. A escada de carvalho sai de tábua
     * de carvalho, os lenhadores daquela vila cortam cerejeira, e a obra
     * ficou esperando uma espécie que ninguém ia trazer — até a paciência
     * acabar e a casa ser dada por feita com o vão à mostra.
     *
     * <p><b>Isto não é política nova.</b> {@code WOOD}, {@code PLANKS} e
     * {@code STONE} já são {@code INTERCHANGEABLE_IN_THE_WALL} desde
     * 2026-08-26 — a parede da casa já sai de espécies misturadas. O que
     * faltava é que escada, porta e alçapão não são {@link ResourceType},
     * então esta classe devolvia só o item exato para elas e a regra
     * parava na peça <b>feita</b> de tábua. A mesma frase da ADR-009
     * §3.10 vale para as duas: <i>prefere carvalho; isso não quer dizer
     * que só possa carvalho</i>.
     *
     * <p>O preferido continua primeiro, e o {@link #dressedLike} veste o
     * substituto com o {@code facing}, o {@code half} e o {@code shape}
     * da planta — sem isso a calota sairia com os degraus virados para
     * qualquer lado.
     *
     * <p>Só madeira. Pedra tem substituição declarada e passa pelo
     * caminho de cima; vidraça e tocha não têm família nenhuma, e
     * inventar uma para elas seria alargar a Regra 27 sem defeito medido
     * que peça isso.
     */
    private static List<Item> sameShapeInAnotherWood(Item exact, List<Item> order) {
        for (TagKey<Item> shape : WOODEN_SHAPES) {
            if (!Registries.ITEM.getEntry(exact).isIn(shape)) {
                continue;
            }

            Registries.ITEM.getEntryList(shape).ifPresent(family -> family.forEach(entry -> {
                if (!order.contains(entry.value())) {
                    order.add(entry.value());
                }
            }));

            // Uma família por peça: uma escada não é também um alçapão, e
            // seguir procurando seria varrer seis tags para nada.
            break;
        }

        return List.copyOf(order);
    }

    /**
     * Se este item é o bloco que a planta pediu, e não um substituto.
     */
    public static boolean isExact(Block wanted, Item taken) {
        return wanted.asItem() == taken;
    }

    /**
     * O substituto vestindo o estado que a planta pediu — 2026-08-26.
     *
     * <p><b>O tronco tem eixo, e a tábua não.</b> Uma viga deitada de
     * carvalho trocada por bétula no estado padrão viraria uma viga
     * <b>em pé</b> de bétula: a casa sai torta, e o defeito é do tipo que
     * só se vê olhando. Por isso o que se copia são as propriedades, e
     * não o bloco.
     *
     * <p>Copia só o que existe nos dois. Pedra não tem eixo, tábua não
     * tem nada, e nesses casos isto devolve o estado padrão — que é o
     * certo para eles.
     */
    public static BlockState dressedLike(BlockState blueprint, Item taken) {
        BlockState substitute = Block.getBlockFromItem(taken).getDefaultState();

        for (Property<?> property : blueprint.getProperties()) {
            substitute = copy(blueprint, substitute, property);
        }

        return substitute;
    }

    private static <T extends Comparable<T>> BlockState copy(
            BlockState from, BlockState to, Property<T> property) {

        return to.contains(property) ? to.with(property, from.get(property)) : to;
    }
}
