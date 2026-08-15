package com.villagecolony.fabric.integration;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceGroup;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.List;

/**
 * Tira do baú o que o trabalhador vai consumir — Fase 9.
 *
 * <p>É a primeira coisa que este mod faz que **diminui** o que o jogador
 * tem. Até aqui a colônia só somava: contava o baú, punha madeira dentro,
 * e nada nunca saía. Fabricar é transformar, e transformar começa em
 * tirar.
 *
 * <p>Por isso as regras aqui são estreitas:
 *
 * <ul>
 *   <li>só o que a colônia sabe contar — item fora de
 *       {@link ResourceType} continua no baú, e não é tocado. O baú da
 *       colônia costuma ser um baú que o jogador também usa;
 *   <li>nunca força carregamento de chunk, como o resto desta camada;
 *   <li>devolve o que de fato saiu, e não o que foi pedido. Quem chama
 *       precisa saber com o que ficou na mão — a receita depende da
 *       espécie do tronco.
 * </ul>
 *
 * <p>O par simétrico é {@link ChestDepositor}, e os dois compartilham a
 * forma: pergunta ao chunk carregado, mexe no inventário, marca sujo.
 */
public final class ChestWithdrawer {

    private ChestWithdrawer() {
    }

    /**
     * Tira até {@code amount} itens deste tipo, e diz quantos saíram.
     *
     * <p>Menos que o pedido não é erro: o baú pode ter menos, ou nada. O
     * chamador decide o que fazer com a diferença.
     */
    public static int withdraw(ServerWorld world, ColonyPos chest, Item item, int amount) {
        if (amount <= 0) {
            return 0;
        }

        ChestBlockEntity inventory = chestAt(world, chest);

        if (inventory == null) {
            return 0;
        }

        int taken = takeFrom(inventory, stack -> stack.isOf(item), amount, new ArrayList<>());

        if (taken > 0) {
            inventory.markDirty();
        }

        return taken;
    }

    /**
     * Quantos itens deste tipo o baú tem, sem tirar nenhum.
     *
     * <p>A pergunta que {@link #withdraw} responde tirando. Existe para
     * quem precisa saber antes de decidir — a obra parada em
     * {@code WAITING_RESOURCES} perguntando se já pode acordar.
     *
     * <p>Mesma leitura de {@code withdraw}: o mesmo baú, o mesmo teste de
     * item. As duas têm de concordar, senão a obra acorda e volta a
     * dormir no mesmo ciclo.
     */
    public static int countIn(ServerWorld world, ColonyPos chest, Item item) {
        ChestBlockEntity inventory = chestAt(world, chest);

        if (inventory == null) {
            return 0;
        }

        int found = 0;

        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.getStack(slot);

            if (stack.isOf(item)) {
                found += stack.getCount();
            }
        }

        return found;
    }

    /**
     * Tira até {@code amount} itens deste grupo, seja qual for a espécie.
     *
     * <p>É o que a fabricação precisa: o fabricante pega madeira, não
     * carvalho. Qual espécie ele pegou decide qual receita se aplica, e
     * por isso o que volta são os itens, e não um número.
     *
     * <p>Vai do primeiro slot ao último, que é a ordem em que o jogador
     * vê o baú. Escolher por espécie mais abundante, ou pela que rende
     * mais, seria uma decisão de regra que ninguém tomou.
     *
     * @return os itens retirados, já somados por tipo. Vazio quando não
     *     havia nada do grupo, ou quando o baú não pôde ser lido
     */
    public static List<ItemStack> withdrawGroup(
            ServerWorld world, ColonyPos chest, ResourceGroup group, int amount) {

        if (amount <= 0 || group == ResourceGroup.NONE) {
            return List.of();
        }

        ChestBlockEntity inventory = chestAt(world, chest);

        if (inventory == null) {
            return List.of();
        }

        List<ItemStack> taken = new ArrayList<>();

        int count = takeFrom(inventory, stack -> isOfGroup(stack, group), amount, taken);

        if (count > 0) {
            inventory.markDirty();
        }

        return List.copyOf(taken);
    }

    /**
     * Tira do inventário o que o filtro aceitar, até o limite.
     *
     * <p>Guarda em {@code taken} o que saiu, somado por item — quem pede
     * um grupo precisa saber de que espécie era cada peça.
     */
    private static int takeFrom(
            ChestBlockEntity inventory,
            java.util.function.Predicate<ItemStack> accepts,
            int amount,
            List<ItemStack> taken) {

        int remaining = amount;

        for (int slot = 0; slot < inventory.size() && remaining > 0; slot++) {
            ItemStack stack = inventory.getStack(slot);

            if (stack.isEmpty() || !accepts.test(stack)) {
                continue;
            }

            // O item antes de mexer na pilha: pilha que zera devolve AR
            // em getItem, e o chamador ficaria sem saber o que pegou.
            Item item = stack.getItem();

            int fromThisSlot = Math.min(remaining, stack.getCount());

            stack.decrement(fromThisSlot);

            if (stack.isEmpty()) {
                inventory.setStack(slot, ItemStack.EMPTY);
            }

            add(taken, item, fromThisSlot);

            remaining -= fromThisSlot;
        }

        return amount - remaining;
    }

    /**
     * Se este item é da colônia e do grupo pedido.
     *
     * <p>Passa por {@link ResourceType}: o que a colônia não conta, ela
     * não tira. Um item do jogador guardado no mesmo baú fica onde está.
     */
    private static boolean isOfGroup(ItemStack stack, ResourceGroup group) {
        return MinecraftTypeAdapter.toResourceType(stack.getItem())
                .filter(type -> type.group() == group)
                .isPresent();
    }

    /** Soma no que já saiu, para não devolver gota a gota. */
    private static void add(List<ItemStack> taken, Item item, int count) {
        for (ItemStack existing : taken) {
            if (existing.isOf(item)) {
                existing.increment(count);

                return;
            }
        }

        taken.add(new ItemStack(item, count));
    }

    /** O baú, ou {@code null} se o chunk não está carregado. */
    private static ChestBlockEntity chestAt(ServerWorld world, ColonyPos chest) {
        BlockPos position = MinecraftTypeAdapter.toBlockPos(chest);

        WorldChunk chunk = world.getChunkManager()
                .getWorldChunk(position.getX() >> 4, position.getZ() >> 4);

        if (chunk == null) {
            return null;
        }

        return chunk.getBlockEntity(position) instanceof ChestBlockEntity inventory
                ? inventory
                : null;
    }
}
