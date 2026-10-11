package com.villagecolony.fabric.work;

import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.CraftingLookup;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A tábua que as peças de madeira da obra vão consumir — P7c, autossuficiência,
 * 2026-10-11.
 *
 * <p>A demanda de tábua da obra contava só os blocos de tábua. A escada, a laje e
 * a cerca da planta pedem tábua pela receita, e essa tábua não entrava na conta:
 * o portão da madeira (metade fica em tora) barrava o carpinteiro com a obra
 * esperando escada. No playtest de 11-10, 38 pedidos de peça da obra pararam
 * assim, com 0 peça. A receita é a do jogo (ADR-009), lida uma vez por peça.
 */
public final class WoodInPieces {

    static {
        ServerMemory.register(WoodInPieces.class, WoodInPieces::clearAll);
    }

    private static final Map<Item, Double> PER_PIECE = new HashMap<>();

    private WoodInPieces() {
    }

    static void clearAll() {
        PER_PIECE.clear();
    }

    /** As tábuas que as peças desta lista vão consumir pela receita; a tábua posta como bloco fica de fora. */
    public static int planksIn(ServerWorld world, Map<ResourceId, Integer> materials) {
        double total = 0;

        for (Map.Entry<ResourceId, Integer> entry : materials.entrySet()) {
            if (entry.getKey().path().endsWith("_planks")) {
                continue;
            }

            Optional<Item> piece = MinecraftTypeAdapter.toBlock(entry.getKey()).map(Block::asItem);

            if (piece.isPresent()) {
                total += entry.getValue() * PER_PIECE.computeIfAbsent(piece.get(), item -> perPiece(world, item));
            }
        }

        return (int) Math.ceil(total);
    }

    private static double perPiece(ServerWorld world, Item piece) {
        return CraftingLookup.billFor(world, piece, any -> true)
                .map(bill -> bill.ingredients().entrySet().stream()
                        .filter(ingredient -> Registries.ITEM.getId(ingredient.getKey()).getPath().endsWith("_planks"))
                        .mapToInt(Map.Entry::getValue)
                        .sum() / (double) bill.resultCount())
                .orElse(0.0);
    }
}
