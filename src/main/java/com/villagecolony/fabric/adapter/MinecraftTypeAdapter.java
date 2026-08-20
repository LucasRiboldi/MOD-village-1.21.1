package com.villagecolony.fabric.adapter;

import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.type.Side;
import com.villagecolony.core.worker.model.ToolType;
import com.villagecolony.fabric.integration.TreeSpecies;
import com.villagecolony.core.type.ColonyPos;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Optional;

/**
 * Converte tipos do Minecraft para tipos do Core e vice-versa.
 *
 * <p>Esta é a fronteira. Nenhuma classe do Core conhece {@code BlockPos};
 * nenhuma conversão acontece fora daqui. Ver ADR-005 §4.
 *
 * <p>{@code Identifier <-> ResourceId} e
 * {@code BlockRotation <-> ColonyRotation} entram quando houver uso.
 */
public final class MinecraftTypeAdapter {

    private MinecraftTypeAdapter() {
    }

    public static ColonyPos toColonyPos(BlockPos pos) {
        return new ColonyPos(pos.getX(), pos.getY(), pos.getZ());
    }

    public static BlockPos toBlockPos(ColonyPos pos) {
        return new BlockPos(pos.x(), pos.y(), pos.z());
    }

    /**
     * O recurso correspondente a um item, se a colônia acompanha algum.
     *
     * <p>Vazio para a esmagadora maioria dos itens, e isso é normal: a
     * colônia conta três coisas, não o inventário inteiro. Ver
     * {@link ResourceType}.
     *
     * <p>Comparação por identidade porque {@code Items.OAK_LOG} é
     * singleton no registro — é a mesma instância para todo stack de
     * carvalho do servidor.
     */
    public static Optional<ResourceType> toResourceType(Item item) {
        // As madeiras vêm da tabela de espécies, e não de uma segunda
        // lista aqui: acrescentar uma árvore lá passaria a contar sozinho
        // no estoque. Duas listas divergiriam no dia em que alguém
        // lembrasse de uma e esquecesse a outra.
        for (TreeSpecies species : TreeSpecies.values()) {
            if (item == species.log().asItem()) {
                return Optional.of(species.resource());
            }

            if (item == species.planks().asItem()) {
                return Optional.of(species.plankResource());
            }
        }

        if (item == Items.COBBLESTONE) {
            return Optional.of(ResourceType.COBBLESTONE);
        }

        // As matérias que a cadeia de produção de 2026-08-20 acrescentou.
        // Areia vermelha conta como areia: as duas fundem em vidro, e a
        // colônia não distingue o que a fornalha não distingue.
        if (item == Items.SANDSTONE) {
            return Optional.of(ResourceType.SANDSTONE);
        }

        if (item == Items.SAND || item == Items.RED_SAND) {
            return Optional.of(ResourceType.SAND);
        }

        if (item == Items.GLASS) {
            return Optional.of(ResourceType.GLASS);
        }

        if (item == Items.WHITE_WOOL) {
            return Optional.of(ResourceType.WHITE_WOOL);
        }

        return Optional.empty();
    }

    /**
     * O item correspondente à ferramenta de uma profissão.
     *
     * <p>Profession-System.md §"Ferramentas das Profissões" diz que o
     * trabalhador recebe a ferramenta inicial ao assumir a função.
     * {@link ToolType} existia e a profissão já a declarava desde a Fase
     * 4; o que faltava era esta conversão, e por isso nada chegava à mão
     * de ninguém.
     *
     * <p>{@link ToolType#NONE} devolve vazio, e não um item vazio: o
     * fabricante e o construtor trabalham de mãos livres por decisão do
     * MVP, e um {@code ItemStack.EMPTY} circulando obrigaria todo mundo a
     * checá-lo.
     *
     * <p>A evolução madeira → pedra → ferro não pertence ao MVP. Quando
     * pertencer, é este método que ganha o nível, não quem o chama.
     */
    /**
     * O nome do jogo, como {@code Identifier}.
     *
     * <p>{@code Identifier.of} valida os caracteres e estoura se o nome
     * for impossível. Deixar estourar é o certo: um id malformado só
     * chega aqui vindo de código, e {@link ResourceId} de propósito não
     * valida o que o jogo aceita — quem sabe disso é o jogo.
     */
    /**
     * O lado do horizonte do jogo, no vocabulário do Core.
     *
     * <p>Cima e baixo não são lado de casa e não chegam aqui: quem
     * pergunta é a escolha de lote, que só percorre
     * {@code Direction.Type.HORIZONTAL}.
     *
     * @throws IllegalArgumentException se vier UP ou DOWN, que seria
     *     defeito de quem chamou e não caso a tratar
     */
    public static Side toSide(Direction direction) {
        return switch (direction) {
            case NORTH -> Side.NORTH;
            case SOUTH -> Side.SOUTH;
            case EAST -> Side.EAST;
            case WEST -> Side.WEST;
            default -> throw new IllegalArgumentException("not a horizontal side: " + direction);
        };
    }

    /** O caminho de volta: o lado do Core na direção do jogo. */
    public static Direction toDirection(Side side) {
        return switch (side) {
            case NORTH -> Direction.NORTH;
            case SOUTH -> Direction.SOUTH;
            case EAST -> Direction.EAST;
            case WEST -> Direction.WEST;
        };
    }

    public static Identifier toIdentifier(ResourceId id) {
        return Identifier.of(id.namespace(), id.path());
    }

    public static ResourceId toResourceId(Identifier id) {
        return new ResourceId(id.getNamespace(), id.getPath());
    }

    /**
     * O nome com que o jogo registra este bloco.
     *
     * <p>Todo bloco tem entrada no registro, inclusive os de outros
     * mods: é assim que o jogo o salva em disco.
     */
    public static ResourceId toResourceId(Block block) {
        return toResourceId(Registries.BLOCK.getId(block));
    }

    /**
     * O bloco que este nome designa, se o jogo o conhece.
     *
     * <p>Vazio para nome que o registro não tem — um projeto lido de um
     * datapack que depois saiu, por exemplo. O registro devolve
     * {@code AIR} para desconhecido, e "ar" é uma resposta que quem
     * constrói não pode confundir com um bloco de verdade.
     */
    public static Optional<Block> toBlock(ResourceId id) {
        Identifier identifier = toIdentifier(id);

        if (!Registries.BLOCK.containsId(identifier)) {
            return Optional.empty();
        }

        return Optional.of(Registries.BLOCK.get(identifier));
    }

    public static Optional<Item> toItem(ToolType tool) {
        return switch (tool) {
            case NONE -> Optional.empty();
            case WOODEN_AXE -> Optional.of(Items.WOODEN_AXE);
            case WOODEN_PICKAXE -> Optional.of(Items.WOODEN_PICKAXE);
            case SHEARS -> Optional.of(Items.SHEARS);
            case WOODEN_HOE -> Optional.of(Items.WOODEN_HOE);
        };
    }
}
