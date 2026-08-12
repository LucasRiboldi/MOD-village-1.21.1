package com.villagecolony.fabric.integration;

import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.storage.service.StorageRegistry;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceGroup;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.Optional;
import java.util.UUID;

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
     * Quanto deste item ainda cabe no baú.
     *
     * <p>Existe para poder perguntar <b>antes</b> de derrubar. A madeira
     * é removida do mundo sem drop, então tronco derrubado que não cabe
     * no baú é tronco destruído — a árvore sumiria e a colônia não
     * ficaria com nada. Perguntar primeiro é o que cumpre "recolher
     * todos os recursos da árvore".
     *
     * <p>Devolve zero quando o baú não pode ser lido: chunk descarregado
     * ou baú que o jogador quebrou. Zero faz o chamador não derrubar, que
     * é a resposta segura.
     */
    public static int freeSpaceFor(ServerWorld world, ColonyPos chest, Item item) {
        BlockPos position = MinecraftTypeAdapter.toBlockPos(chest);

        WorldChunk chunk = world.getChunkManager()
                .getWorldChunk(position.getX() >> 4, position.getZ() >> 4);

        if (chunk == null) {
            return 0;
        }

        if (!(chunk.getBlockEntity(position) instanceof ChestBlockEntity inventory)) {
            return 0;
        }

        int room = 0;

        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.getStack(slot);

            if (stack.isEmpty()) {
                room += item.getDefaultStack().getMaxCount();
            } else if (stack.isOf(item)) {
                room += stack.getMaxCount() - stack.getCount();
            }
        }

        return room;
    }

    /**
     * Quanto ainda cabe de um grupo de recursos, num baú só.
     *
     * <p>Difere de {@link #freeSpaceFor} por não perguntar por um item:
     * a meta de madeira é do grupo, e um baú com meia pilha de bétula
     * tem espaço para madeira mesmo que o próximo tronco seja de
     * carvalho. Perguntar por um item só faria a colônia enxergar menos
     * espaço do que tem.
     *
     * <p>Slot vazio conta pilha cheia; slot ocupado por recurso do grupo
     * conta o que falta para completá-lo. Slot com qualquer outra coisa
     * não conta — é do jogador, e o mod não mexe no que ele guardou.
     */
    public static int freeSpaceForGroup(
            ServerWorld world, ColonyPos chest, ResourceGroup group) {

        BlockPos position = MinecraftTypeAdapter.toBlockPos(chest);

        WorldChunk chunk = world.getChunkManager()
                .getWorldChunk(position.getX() >> 4, position.getZ() >> 4);

        if (chunk == null) {
            return 0;
        }

        if (!(chunk.getBlockEntity(position) instanceof ChestBlockEntity inventory)) {
            return 0;
        }

        int room = 0;

        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.getStack(slot);

            if (stack.isEmpty()) {
                room += emptySlotCapacity();
            } else if (isOfGroup(stack, group)) {
                room += stack.getMaxCount() - stack.getCount();
            }
        }

        return room;
    }

    /**
     * O espaço de todos os baús registrados de uma colônia.
     *
     * <p>É a medida que vira meta em {@code ColonyGoals}: a colônia
     * colhe até isto chegar a zero.
     *
     * <p>Baú em chunk descarregado entra como zero, e não como erro.
     * Quem decide sobre esta soma já recusa decidir quando a varredura
     * de {@code ChestInventoryReader.survey} vier parcial, então um baú
     * fora de alcance nunca chega a virar meta a menos.
     */
    public static int freeSpaceForGroup(
            ServerWorld world,
            Iterable<UUID> workerIds,
            StorageRegistry storages,
            ResourceGroup group) {

        int room = 0;

        for (UUID workerId : workerIds) {
            Optional<WorkerStorage> storage = storages.of(workerId);

            if (storage.isPresent()) {
                room += freeSpaceForGroup(world, storage.get().chestPosition(), group);
            }
        }

        return room;
    }

    /**
     * Se este stack é do grupo em questão.
     *
     * <p>A pergunta passa pelo adaptador, e não por uma lista de itens
     * aqui: a tabela de espécies é quem sabe quais troncos existem, e
     * uma segunda lista divergiria dela no dia em que alguém lembrasse
     * de uma e esquecesse a outra.
     */
    private static boolean isOfGroup(ItemStack stack, ResourceGroup group) {
        return MinecraftTypeAdapter.toResourceType(stack.getItem())
                .filter(type -> type.group() == group)
                .isPresent();
    }

    /**
     * Quanto cabe num slot vazio.
     *
     * <p>Sai do próprio jogo, e não de um 64 escrito aqui. Todo recurso
     * que a colônia acompanha empilha igual, então o tronco de carvalho
     * responde por todos — e quando entrar um recurso que empilhe
     * diferente, é esta linha que precisa saber de qual item se fala.
     */
    private static int emptySlotCapacity() {
        return Items.OAK_LOG.getDefaultStack().getMaxCount();
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
