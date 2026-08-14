package com.villagecolony.fabric.adapter;

import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.worker.model.ToolType;
import com.villagecolony.fabric.integration.TreeSpecies;
import com.villagecolony.core.type.ColonyPos;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

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
    public static Optional<Item> toItem(ToolType tool) {
        return switch (tool) {
            case NONE -> Optional.empty();
            case WOODEN_AXE -> Optional.of(Items.WOODEN_AXE);
            case WOODEN_HOE -> Optional.of(Items.WOODEN_HOE);
        };
    }
}
