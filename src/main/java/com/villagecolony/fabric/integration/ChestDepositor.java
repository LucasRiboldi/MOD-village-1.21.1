package com.villagecolony.fabric.integration;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

/**
 * Põe no baú o que o trabalhador produziu.
 *
 * <p>O par que faltava de {@link ChestInventoryReader}, que só lê. A
 * madeira derrubada vai direto para cá, sem passar por item no chão —
 * decisão do autor em 2026-08-08, porque item no chão despawna, cai
 * n'água e é roubado por mob, e a contagem da colônia passaria a mentir
 * sem avisar.
 *
 * <p>Nunca força carregamento de chunk, pela mesma razão de
 * {@link ChestInventoryReader#read}: chamar de dentro do ciclo do
 * servidor trava a thread.
 */
public final class ChestDepositor {

    private ChestDepositor() {
    }

    /**
     * Guarda o que couber, e devolve o que não coube.
     *
     * <p>Baú cheio não é erro nem perda: o que sobra volta como número, e
     * quem chamou decide o que fazer. Hoje a colônia registra em log e
     * para a tarefa — o jogador precisa esvaziar o baú, e precisa poder
     * descobrir isso.
     *
     * @return quantos itens não couberam
     */
    public static int deposit(ServerWorld world, ColonyPos chest, Item item, int amount) {
        if (amount <= 0) {
            return 0;
        }

        BlockPos position = MinecraftTypeAdapter.toBlockPos(chest);

        WorldChunk chunk = world.getChunkManager()
                .getWorldChunk(position.getX() >> 4, position.getZ() >> 4);

        if (chunk == null) {
            return amount;
        }

        if (!(chunk.getBlockEntity(position) instanceof ChestBlockEntity inventory)) {
            return amount;
        }

        int remaining = amount;

        remaining = fillExistingStacks(inventory, item, remaining);
        remaining = fillEmptySlots(inventory, item, remaining);

        inventory.markDirty();

        return remaining;
    }

    /**
     * Completa as pilhas que já existem.
     *
     * <p>Antes de abrir slot novo: um baú com sete pilhas de madeira pela
     * metade e um slot livre deve encher as sete, não criar a oitava.
     */
    private static int fillExistingStacks(ChestBlockEntity inventory, Item item, int amount) {
        int remaining = amount;

        for (int slot = 0; slot < inventory.size() && remaining > 0; slot++) {
            ItemStack stack = inventory.getStack(slot);

            if (stack.isEmpty() || !stack.isOf(item)) {
                continue;
            }

            int room = stack.getMaxCount() - stack.getCount();

            if (room <= 0) {
                continue;
            }

            int moved = Math.min(room, remaining);

            stack.increment(moved);
            remaining -= moved;
        }

        return remaining;
    }

    private static int fillEmptySlots(ChestBlockEntity inventory, Item item, int amount) {
        int remaining = amount;

        for (int slot = 0; slot < inventory.size() && remaining > 0; slot++) {
            if (!inventory.getStack(slot).isEmpty()) {
                continue;
            }

            ItemStack stack = new ItemStack(item);
            int moved = Math.min(stack.getMaxCount(), remaining);

            stack.setCount(moved);
            inventory.setStack(slot, stack);

            remaining -= moved;
        }

        return remaining;
    }
}
