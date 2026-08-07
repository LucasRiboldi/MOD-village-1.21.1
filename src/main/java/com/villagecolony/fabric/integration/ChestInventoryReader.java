package com.villagecolony.fabric.integration;

import com.villagecolony.core.resource.model.ColonyResources;
import com.villagecolony.core.resource.model.ResourceTally;
import com.villagecolony.core.resource.model.ResourceType;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.storage.service.StorageRegistry;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Conta o que há nos baús dos trabalhadores.
 *
 * <p>Só lê. Nada aqui move, retira ou reorganiza item — o baú é do
 * jogador tanto quanto do aldeão, e o MVP não mexe no seu conteúdo. Ver
 * Storage-System.md §"Capacidade de Armazenamento".
 *
 * <p>Conta apenas os três recursos que a colônia acompanha; o resto do
 * inventário é ignorado, não apagado. Ver {@link ResourceType}.
 */
public final class ChestInventoryReader {

    private ChestInventoryReader() {
    }

    /**
     * O que há num baú.
     *
     * <p>Devolve vazio, e não erro, quando não há baú na posição: o
     * jogador pode tê-lo quebrado entre o registro e a leitura, e isso
     * é o "Storage Missing" de Storage-System.md §"Falhas", não uma
     * falha do mod.
     *
     * <p>Baú duplo conta só a metade registrada. Cada metade é uma block
     * entity com posição própria, e é uma delas que o trabalhador
     * reivindicou. Contar as duas faria a colônia enxergar o dobro
     * quando o outro lado fosse reivindicado por outro aldeão.
     */
    public static ResourceTally read(ServerWorld world, BlockPos position) {
        if (!(world.getBlockEntity(position) instanceof ChestBlockEntity chest)) {
            return ResourceTally.empty();
        }

        Map<ResourceType, Integer> counts = new EnumMap<>(ResourceType.class);

        for (int slot = 0; slot < chest.size(); slot++) {
            ItemStack stack = chest.getStack(slot);

            if (stack.isEmpty()) {
                continue;
            }

            MinecraftTypeAdapter.toResourceType(stack.getItem()).ifPresent(
                    type -> counts.merge(type, stack.getCount(), Integer::sum));
        }

        return ResourceTally.of(counts);
    }

    /** O que há no baú de um trabalhador, se ele tiver um. */
    public static ResourceTally readOf(
            ServerWorld world, UUID workerId, StorageRegistry storages) {

        return storages.of(workerId)
                .map(storage -> read(
                        world, MinecraftTypeAdapter.toBlockPos(storage.chestPosition())))
                .orElseGet(ResourceTally::empty);
    }

    /**
     * A soma dos baús de vários trabalhadores.
     *
     * <p>É a "visão agregada" de Resource-System.md §"Registro de
     * Recursos", calculada na hora a partir dos baús.
     *
     * <p>Calculada, e não guardada: o jogador pode tirar madeira do baú
     * a qualquer momento, e um total em cache estaria errado sem que
     * nada avisasse. Enquanto a contagem for barata — um punhado de
     * baús, dezenas de slots — vale pagar por ela.
     */
    public static ResourceTally readAll(
            ServerWorld world, Iterable<UUID> workerIds, StorageRegistry storages) {

        ResourceTally total = ResourceTally.empty();

        for (UUID workerId : workerIds) {
            total = total.plus(readOf(world, workerId, storages));
        }

        return total;
    }

    /**
     * O estoque de uma colônia, repartido por baú.
     *
     * <p>Difere de {@link #readAll} por guardar de onde veio cada
     * parcela: o trabalhador vai até o baú, e o total sozinho não diz
     * a ninguém para onde andar. Ver Resource-System.md §"Registro de
     * Recursos".
     */
    public static ColonyResources readColony(
            ServerWorld world, Iterable<UUID> workerIds, StorageRegistry storages) {

        Map<ColonyPos, ResourceTally> byChest = new LinkedHashMap<>();

        for (UUID workerId : workerIds) {
            storages.of(workerId).ifPresent(storage -> byChest.put(
                    storage.chestPosition(),
                    read(world, MinecraftTypeAdapter.toBlockPos(storage.chestPosition()))));
        }

        return ColonyResources.of(byChest);
    }

    /** Atalho para somar tudo o que está registrado. */
    public static ResourceTally readEverything(ServerWorld world, StorageRegistry storages) {
        ResourceTally total = ResourceTally.empty();

        for (WorkerStorage storage : storages.all()) {
            total = total.plus(
                    read(world, MinecraftTypeAdapter.toBlockPos(storage.chestPosition())));
        }

        return total;
    }
}
