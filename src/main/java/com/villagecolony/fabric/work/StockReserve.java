package com.villagecolony.fabric.work;

import com.villagecolony.core.coordination.BlockReserve;
import com.villagecolony.core.type.ServerMemory;
import net.minecraft.item.Item;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/** A reserva de um item fabricado nos baús, pela classe dele — ADR-062, Regra 340 ({@link BlockReserve}). */
public final class StockReserve {

    /** Os itens que saem de alguma receita do jogo, lidos uma vez por servidor. */
    private static @Nullable Set<Item> made;

    static {
        ServerMemory.register(StockReserve.class, () -> made = null);
    }

    private StockReserve() {
    }

    /** Quanto deste fabricado se guarda: 4, 12 ou 1; 0 para a bancada do mod, que as profissões não fazem. */
    public static int of(Item item) {
        return BlockReserve.reserveOf(idOf(item), true);
    }

    /** A classe deste item, sabendo se alguma receita do jogo o faz (Regra 342, o registro de blocos). */
    public static BlockReserve.StockClass classOf(ServerWorld world, Item item) {
        return BlockReserve.classOf(idOf(item), madeItems(world).contains(item));
    }

    /** O id completo: {@code minecraft:oak_planks}. */
    public static String idOf(Item item) {
        return Registries.ITEM.getId(item).toString();
    }

    /** O item de um id curto ({@code oak_planks}) ou completo. */
    public static Item itemOf(String key) {
        Identifier id = key.contains(":") ? Identifier.of(key) : Identifier.ofVanilla(key);

        return Registries.ITEM.get(id);
    }

    private static Set<Item> madeItems(ServerWorld world) {
        Set<Item> known = made;

        if (known == null) {
            known = new HashSet<>();

            for (RecipeEntry<?> entry : world.getRecipeManager().sortedValues()) {
                known.add(entry.value().getResult(world.getRegistryManager()).getItem());
            }

            made = known;
        }

        return known;
    }
}
