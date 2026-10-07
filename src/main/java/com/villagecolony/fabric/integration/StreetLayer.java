package com.villagecolony.fabric.integration;

import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.type.ResourceId;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.structure.StructureTemplate;
import java.util.List;
import java.util.OptionalInt;

/**
 * A altura da rua dentro de uma planta — separado de
 * {@link StructureBlueprintReader} em 2026-10-02 pelo teto de 500 linhas; o
 * texto veio sem mudança.
 */
final class StreetLayer {

    private StreetLayer() {
    }

    /**
     * A camada que fica na altura da rua — 2026-09-26.
     *
     * <p><b>A porta manda.</b> O pedido do autor é "o chão e a porta na altura
     * da rua": a camada da rua é aquela em que se pisa ao entrar, uma abaixo da
     * porta mais baixa. A primeira versão lia o encaixe (jigsaw) voltado para a
     * rua, e a bateria mostrou que ele não é régua: na
     * {@code plains_shepherds_house_1} e na {@code plains_big_house_1} ele está
     * no andar da porta, e no {@code plains_temple_4} a porta está na camada 0
     * — o piso do templo fica abaixo da planta, e o templo continua como
     * sempre foi.
     *
     * <p>Planta sem porta — roça, curral, praça — usa o encaixe de rua
     * ({@code west_up}; os de decoração apontam para cima, {@code up_north}).
     *
     * @return vazio quando não há camada da rua dentro da planta: sem porta nem
     *     encaixe (a BigHouseMOD, as plantas de teste), ou porta na camada 0
     */
    static OptionalInt of(StructureTemplate template, List<BlueprintBlock> blocks) {
        OptionalInt door = blocks.stream()
                .filter(block -> block.block().path().endsWith("_door"))
                .mapToInt(block -> block.offset().y())
                .min();

        if (door.isPresent()) {
            return door.getAsInt() >= 1 ? OptionalInt.of(door.getAsInt() - 1) : OptionalInt.empty();
        }

        return streetConnectorLayerOf(template);
    }

    /** A camada do encaixe voltado para a rua, para a planta sem porta. */
    private static OptionalInt streetConnectorLayerOf(StructureTemplate template) {
        NbtCompound nbt = template.writeNbt(new NbtCompound());

        NbtList palette = nbt.getList(StructureTemplate.PALETTE_KEY, NbtElement.COMPOUND_TYPE);
        NbtList entries = nbt.getList(StructureTemplate.BLOCKS_KEY, NbtElement.COMPOUND_TYPE);

        OptionalInt lowest = OptionalInt.empty();

        for (int i = 0; i < entries.size(); i++) {
            NbtCompound entry = entries.getCompound(i);
            int index = entry.getInt(StructureTemplate.BLOCKS_STATE_KEY);

            if (index < 0 || index >= palette.size()) {
                continue;
            }

            NbtCompound state = palette.getCompound(index);

            ResourceId name = ResourceId.parse(state.getString(StructureBlueprintReader.BLOCK_NAME_KEY));
            String orientation = state.getCompound(StructureBlueprintReader.PROPERTIES_KEY).getString("orientation");

            if (!StructureBlueprintReader.isJigsaw(name) || !facesSideways(orientation)) {
                continue;
            }

            int layer = StructureBlueprintReader.offsetOf(entry).y();

            if (lowest.isEmpty() || layer < lowest.getAsInt()) {
                lowest = OptionalInt.of(layer);
            }
        }

        return lowest;
    }

    /** {@code west_up} sim, {@code up_north} não: a frente do encaixe aponta para o lado. */
    private static boolean facesSideways(String orientation) {
        return orientation.startsWith("north_") || orientation.startsWith("south_")
                || orientation.startsWith("east_") || orientation.startsWith("west_");
    }
}
