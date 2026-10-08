package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ColonyChests;
import com.villagecolony.fabric.integration.CraftingLookup;
import net.minecraft.item.Item;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Se há o que fundir para este pedido num baú da colônia — a trava
 * {@code SMELT_INPUT} da reserva (playtest de 2026-10-08). Sem ela o fundidor
 * pegava a tarefa de lingote, não achava minério em nenhum dos 34 baús, soltava
 * — e a pegava de novo no tique seguinte: 191 vezes em 12 minutos, 0 feitas.
 *
 * <p>É a mesma pergunta que o {@code SmelterWork} faz ao começar (o livro de
 * receitas diz o cru, os baús da Regra 45 dizem se ele existe), feita antes de
 * reservar: tarefa sem entrada — nem a do pedido, nem a de peça que as obras usam —
 * não é elegível, e o fundidor fica livre para outra coisa. A resposta vale por
 * tique, por colônia e pedido: a distribuição pergunta por trabalhador.
 */
public final class SmeltInput {

    static {
        ServerMemory.register(SmeltInput.class, SmeltInput::clearAll);
    }

    private record Answer(long tick, boolean present) {
    }

    private static final Map<String, Answer> ASKED = new HashMap<>();

    private SmeltInput() {
    }

    /** Se algum baú da colônia tem um cru que a fornalha transforma neste recurso. */
    public static boolean inChests(ServerWorld world, UUID colonyId, ResourceType wanted) {
        String key = colonyId + "|" + wanted;
        Answer cached = ASKED.get(key);

        if (cached != null && cached.tick() == world.getTime()) {
            return cached.present();
        }

        boolean present = MinecraftTypeAdapter.toItem(wanted)
                .map(made -> hasRaw(world, colonyId, made))
                .orElse(false);

        if (ASKED.size() > 256) {
            ASKED.clear();
        }

        ASKED.put(key, new Answer(world.getTime(), present));

        return present;
    }

    /**
     * O cru do pedido, ou o de alguma peça que as obras usam: com este, o fundidor
     * pega o pedido e trabalha para a obra enquanto o cru dele não chega
     * ({@link SmelterFallback}, pedido do autor de 2026-09-30).
     */
    private static boolean hasRaw(ServerWorld world, UUID colonyId, Item made) {
        Colony colony = VillageColonyMod.COLONIES.find(colonyId).orElse(null);

        if (colony == null) {
            return false;
        }

        List<ColonyPos> chests = ColonyChests.nearestFirst(world, colonyId, colony.center());
        Set<Item> products = new LinkedHashSet<>();

        products.add(made);
        products.addAll(SmelterFallback.wanted(world, colony));

        for (Item product : products) {
            for (Item raw : CraftingLookup.smeltingInputsFor(world, product)) {
                if (ColonyChests.countIn(world, chests, raw) > 0) {
                    return true;
                }
            }
        }

        return false;
    }

    public static void clearAll() {
        ASKED.clear();
    }
}
