package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.BiomeBlockSwap;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.LampPostPlan;
import com.villagecolony.core.construction.model.VillagePalette;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Property;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * As estruturas do mod vestidas com o bioma da vila — Regra 262. A troca ({@link BiomeBlockSwap}) vale para toda planta
 * do mod que não tem arquivo próprio do estilo ({@code colony/<estilo>/...}): o arquivo do estilo, onde existe, já é a
 * versão do bioma e fica como está. O bioma é o da vila, o mesmo que escolhe as casas ({@link VillageBiomes#paletteAt}).
 */
public final class BiomeBlueprints {

    /** As pastas de estilo dentro de {@code colony/}: planta ali já vem do bioma. */
    private static final Set<String> STYLES = Set.of("plains", "desert", "savanna", "snowy", "taiga");

    private BiomeBlueprints() {
    }

    /** A troca da vila que está neste lugar: a madeira e a pedra das casas dela, com carvalho e pedregulho de reserva. */
    public static BiomeBlockSwap swapAt(ServerWorld world, ColonyPos where) {
        VillagePalette palette = VillageBiomes.paletteAt(world, where).orElseGet(() -> VillagePalette.ofWood("plains"));
        ResourceId planks = VillageBiomes.woodAt(world, where).orElse(ResourceId.vanilla("oak_planks"));

        return BiomeBlockSwap.of(palette, planks);
    }

    /** Se a planta deste id recebe a troca: do mod, sem arquivo de estilo, e não o poste, que já nasce do bioma. */
    public static boolean applies(ResourceId id) {
        if (!"villagecolony".equals(id.namespace()) || LampPostPlan.isLampPost(id)) {
            return false;
        }

        String path = id.path();

        return STYLES.stream().noneMatch(style -> path.startsWith("colony/" + style + "/"));
    }

    /** A planta vestida com o bioma da vila desta colônia; a mesma, quando a colônia não é conhecida. */
    public static Blueprint styled(ServerWorld world, UUID colonyId, Blueprint plan) {
        if (!applies(plan.id())) {
            return plan;
        }

        return VillageColonyMod.COLONIES.find(colonyId)
                .map(Colony::center)
                .map(center -> styledAt(world, center, plan))
                .orElse(plan);
    }

    /** A planta vestida com o bioma da vila que está neste lugar. */
    public static Blueprint styledAt(ServerWorld world, ColonyPos where, Blueprint plan) {
        return applies(plan.id()) ? swapAt(world, where).apply(plan, BiomeBlueprints::exists) : plan;
    }

    /** O estado trocado, com as mesmas propriedades (eixo, lado, metade) que o bloco novo também tem. */
    public static BlockState swapped(BlockState state, BiomeBlockSwap swap) {
        ResourceId from = MinecraftTypeAdapter.toResourceId(state.getBlock());
        ResourceId to = swap.apply(from);

        if (to.equals(from)) {
            return state;
        }

        Optional<Block> block = MinecraftTypeAdapter.toBlock(to);

        if (block.isEmpty()) {
            return state;
        }

        BlockState swapped = block.get().getDefaultState();

        for (Property<?> property : state.getProperties()) {
            swapped = copy(state, swapped, property);
        }

        return swapped;
    }

    /** Se o que está no mundo faz o papel do bloco pedido: a mesma peça, de qualquer madeira ou pedra trocável. */
    public static boolean sameRole(BlockState now, Block expected) {
        return now.isOf(expected) || BiomeBlockSwap.sameRole(
                MinecraftTypeAdapter.toResourceId(now.getBlock()), MinecraftTypeAdapter.toResourceId(expected));
    }

    private static boolean exists(ResourceId id) {
        return MinecraftTypeAdapter.toBlock(id).isPresent();
    }

    private static <T extends Comparable<T>> BlockState copy(BlockState from, BlockState to, Property<T> property) {
        return to.contains(property) ? to.with(property, from.get(property)) : to;
    }
}
