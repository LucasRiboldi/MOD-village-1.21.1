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
     * Sem porta e sem encaixe, vale a {@link #sunkenBase camada 0 esparsa}.
     *
     * @return vazio quando não há camada da rua dentro da planta: sem porta,
     *     encaixe nem base enterrada (a BigHouseMOD, as plantas de teste), ou
     *     porta na camada 0
     */
    static OptionalInt of(StructureTemplate template, List<BlueprintBlock> blocks) {
        OptionalInt door = blocks.stream()
                .filter(block -> block.block().path().endsWith("_door"))
                .mapToInt(block -> block.offset().y())
                .min();

        if (door.isPresent()) {
            return door.getAsInt() >= 1 ? OptionalInt.of(door.getAsInt() - 1) : OptionalInt.empty();
        }

        OptionalInt connector = streetConnectorLayerOf(template);

        if (connector.isPresent()) {
            return connector;
        }

        // A camada 0 que é só solo é o chão de pisar, não piso enterrado: ela vai no nível
        // da rua, e o que sobe dela (a cerca do curral) fica acima — bug 0.3.52, o animal_pen
        // assentou com o chão de dentro um bloco abaixo da rua, virando uma cova. O sunkenBase
        // não o pegava: a camada de grama é densa (30 peças contra 21 da cerca).
        OptionalInt ground = soilBase(blocks);

        return ground.isPresent() ? ground : sunkenBase(blocks);
    }

    /** Terreno natural que é chão de pisar, não piso construído. */
    private static final java.util.Set<String> SOIL = java.util.Set.of(
            "grass_block", "dirt", "coarse_dirt", "rooted_dirt", "podzol", "mycelium",
            "sand", "red_sand", "gravel", "snow_block", "farmland", "dirt_path");

    /**
     * A camada 0 que é só solo natural (grama, terra): é o chão de pisar e fica no nível da
     * rua, com o que sobe dela acima — o curral (animal_pen), cujo piso é uma camada de grama.
     * Decisão do autor, 2026-10-13: nivelar pela camada útil toda estrutura com piso de solo.
     *
     * @return a camada 0, ou vazio quando ela tem peça construída (não é só solo)
     */
    static OptionalInt soilBase(List<BlueprintBlock> blocks) {
        List<BlueprintBlock> bottom = blocks.stream()
                .filter(block -> block.offset().y() == 0)
                .toList();

        if (bottom.isEmpty() || blocks.stream().noneMatch(block -> block.offset().y() >= 1)) {
            return OptionalInt.empty();
        }

        boolean allSoil = bottom.stream().allMatch(block -> SOIL.contains(block.block().path()));

        return allSoil ? OptionalInt.of(0) : OptionalInt.empty();
    }

    /**
     * A camada 0 que é subterrânea: menos peças que metade da camada 1.
     *
     * <p>O {@code barn_majest} tem 5 peças na camada 0 (funis e feno debaixo
     * do piso) e 85 na 1, onde as paredes começam. Posta um acima do chão, a
     * obra ficava suspensa com um vão de um bloco sob as paredes e os alçapões
     * na altura da cabeça, e os lenhadores encalhavam junto dela. Com a rua na
     * camada 0, a camada 0 vai no chão e as paredes saem do nível de andar.
     *
     * @return a camada 0, ou vazio quando ela é piso de verdade
     */
    static OptionalInt sunkenBase(List<BlueprintBlock> blocks) {
        long bottom = blocks.stream().filter(block -> block.offset().y() == 0).count();
        long above = blocks.stream().filter(block -> block.offset().y() == 1).count();

        return bottom * 2 < above ? OptionalInt.of(0) : OptionalInt.empty();
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
