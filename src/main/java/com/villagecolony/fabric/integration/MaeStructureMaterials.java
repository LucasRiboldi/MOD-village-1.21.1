package com.villagecolony.fabric.integration;

import com.villagecolony.core.type.ColonyPos;
import net.minecraft.block.Blocks;
import net.minecraft.block.FlowerPotBlock;
import net.minecraft.block.enums.SlabType;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * O material de cada peça do {@code structure_v3}: o item exato da planta (P2) e, no vaso com planta, o vaso e
 * a planta tirados juntos, como o construtor legado monta (ADR-028; escolha do autor de 2026-11-04).
 *
 * <p>Passo com material extra grava {@code contract=2}: leitor que só conhece o contrato 1 recusa a obra em vez
 * de tirar só parte do material.
 */
final class MaeStructureMaterials {
    private MaeStructureMaterials() { }

    /** Itens e quantidades de uma peça, na ordem de retirada; vazio para peça sem origem física. */
    static Map<Item, Integer> of(MaeStructurePieces.Piece piece) {
        var main = piece.main();
        var result = new LinkedHashMap<Item, Integer>();
        if (main.getBlock() instanceof FlowerPotBlock pot && pot.getContent() != Blocks.AIR) {
            Item plant = pot.getContent().asItem();
            if (plant == Items.AIR) return Map.of();
            result.put(Items.FLOWER_POT, 1);
            result.merge(plant, 1, Integer::sum);
            return result;
        }
        Item item = main.getBlock().asItem();
        if (item == Items.AIR) return Map.of();
        result.put(item, main.contains(Properties.SLAB_TYPE) && main.get(Properties.SLAB_TYPE) == SlabType.DOUBLE ? 2 : 1);
        return result;
    }

    /** Grava o primeiro item na chave "material" do passo e os demais em "extra"; diz se houve extra. */
    static boolean write(NbtCompound step, Map<Item, Integer> materials) {
        NbtList extra = new NbtList();
        boolean first = true;
        for (var entry : materials.entrySet()) {
            NbtCompound material = new NbtCompound();
            material.putString("item", Registries.ITEM.getId(entry.getKey()).toString());
            material.putInt("count", entry.getValue());
            if (first) step.put("material", material);
            else extra.add(material);
            first = false;
        }
        if (!extra.isEmpty()) step.put("extra", extra);
        return !extra.isEmpty();
    }

    /** Lê o material do passo; item desconhecido ou quantidade fora de 1–2 recusa o passo. */
    static Map<Item, Integer> read(NbtCompound step) {
        var result = new LinkedHashMap<Item, Integer>();
        add(result, step.getCompound("material"));
        var extra = step.getList("extra", NbtElement.COMPOUND_TYPE);
        if (extra.size() > 1) throw new IllegalStateException("invalid_material_contract");
        for (int i = 0; i < extra.size(); i++) add(result, extra.getCompound(i));
        return result;
    }

    private static void add(Map<Item, Integer> result, NbtCompound material) {
        Identifier id = Identifier.tryParse(material.getString("item"));
        int count = material.getInt("count");
        if (id == null || !Registries.ITEM.containsId(id) || count < 1 || count > 2) {
            throw new IllegalStateException("invalid_material_contract");
        }
        result.merge(Registries.ITEM.get(id), count, Integer::sum);
    }

    /** O primeiro item que os baús não têm na quantidade pedida, ou nulo se todos estão lá. */
    static Item missing(ServerWorld world, List<ColonyPos> chests, Map<Item, Integer> materials) {
        for (var entry : materials.entrySet()) {
            if (ColonyChests.countIn(world, chests, entry.getKey()) < entry.getValue()) return entry.getKey();
        }
        return null;
    }

    /** Tira tudo; qualquer falta depois da intenção é escrita incerta, nunca repetida. */
    static void withdraw(ServerWorld world, List<ColonyPos> chests, Map<Item, Integer> materials) {
        for (var entry : materials.entrySet()) {
            if (ColonyChests.withdraw(world, chests, entry.getKey(), entry.getValue()) != entry.getValue()) {
                throw new IllegalStateException("material_withdraw_failed");
            }
        }
    }
}
