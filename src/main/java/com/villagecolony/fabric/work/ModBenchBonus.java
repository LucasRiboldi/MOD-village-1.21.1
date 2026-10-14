package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.content.ModContent;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.ColonyChests;
import net.minecraft.item.Item;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * O dobro de produção da bancada do mod — decisão do autor, 2026-10-13.
 *
 * <p>Se o titular de uma profissão que tem bancada do mod ({@link ModContent}) trabalha
 * com uma dela perto, cada ciclo de trabalho rende o dobro. É o único ponto que decide o
 * fator; a produção o consulta ao depositar o produto no baú
 * ({@link CraftingSteps}, {@link SmelterWork}).
 */
public final class ModBenchBonus {

    /** O fator quando a bancada do mod está em uso. */
    public static final int FACTOR = 2;

    private ModBenchBonus() {
    }

    /**
     * Quanto multiplicar a produção deste titular: {@link #FACTOR} se ele usa a bancada
     * do mod da sua profissão, senão 1.
     *
     * @param near o baú/ponto de referência de onde a bancada é procurada (raio de
     *     {@link CraftStation#POI_RADIUS})
     */
    public static int factor(ServerWorld world, UUID workerId, BlockPos near) {
        return usesModBench(world, workerId, near) ? FACTOR : 1;
    }

    /** O mesmo, por {@link com.villagecolony.core.type.ColonyPos}. */
    public static int factor(ServerWorld world, UUID workerId, com.villagecolony.core.type.ColonyPos near) {
        return factor(world, workerId, MinecraftTypeAdapter.toBlockPos(near));
    }

    /** Se este titular tem a bancada do mod da sua profissão ao alcance. */
    public static boolean usesModBench(ServerWorld world, UUID workerId, BlockPos near) {
        Optional<ProfessionType> profession =
                VillageColonyMod.WORKERS.find(workerId).flatMap(Worker::profession);

        return profession.isPresent()
                && CraftStation.modBench(world, profession.get(), near).isPresent();
    }

    /**
     * Uma unidade acabou de ser produzida por este titular: se ele usa a bancada do mod, uma
     * cópia extra entra no baú (o dobro por ciclo). No baú da profissão dele se couber, senão
     * num baú da colônia perto de {@code near}; descartada só se não houver espaço em nenhum.
     *
     * @return se a cópia extra foi depositada
     */
    public static boolean grantExtra(ServerWorld world, UUID workerId, Item item, ColonyPos near) {
        BlockPos at = MinecraftTypeAdapter.toBlockPos(near);

        if (!usesModBench(world, workerId, at)) {
            return false;
        }

        Optional<ColonyPos> own = VillageColonyMod.STORAGES.of(workerId).map(WorkerStorage::chestPosition);

        if (own.isPresent() && ChestDepositor.deposit(world, own.get(), item, 1) == 0) {
            return true;
        }

        Optional<UUID> colonyId = VillageColonyMod.WORKERS.find(workerId).map(Worker::colonyId);

        if (colonyId.isEmpty()) {
            return false;
        }

        List<ColonyPos> chests = ColonyChests.nearestFirst(world, colonyId.get(), near);

        for (ColonyPos chest : chests) {
            if (ChestDepositor.deposit(world, chest, item, 1) == 0) {
                return true;
            }
        }

        return false;
    }
}
