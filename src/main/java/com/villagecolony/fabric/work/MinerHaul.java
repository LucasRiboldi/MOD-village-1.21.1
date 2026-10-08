package com.villagecolony.fabric.work;

import com.villagecolony.core.storage.model.ChestSlotCap;
import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.ChestWithdrawer;
import com.villagecolony.fabric.integration.ColonyChests;
import com.villagecolony.fabric.integration.ProfessionChestOverflow;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Onde vai o que o mineiro cava — a Regra 30.
 *
 * <p>Saiu de {@code MinerWork} em 2026-08-22, quando ele cruzou as
 * quinhentas linhas. É uma pergunta inteira e separada de "o que cavar"
 * e "como cavar".
 *
 * <p>Desde 2026-09-15 o destino primário é o baú do próprio mineiro. Quando
 * ele fica cheio, libera primeiro os dez últimos slots para baús comunitários
 * da mesma vila; nunca usa o baú de outra profissão.
 */
final class MinerHaul {

    /**
     * Quantos de cada tipo, fora o pedido, o baú do mineiro guarda: os três
     * compartimentos de 64 de todo baú da colônia (ADR-036 9, ADR-037 C2).
     */
    static final int TYPE_CAP = ChestSlotCap.MAX_SLOTS_PER_ITEM * 64;

    private MinerHaul() {
    }

    /** O baú cheio de verdade: nada entrou e algo ficou no chão. É o único caso que pausa o mineiro. */
    static boolean chestIsFull(Haul haul) {
        return haul.stored() == 0 && haul.spilled() > 0;
    }

    /**
     * O que entrou no baú, e quanto disso era o que a tarefa pediu.
     *
     * <p><b>São dois números porque são duas perguntas</b>, e confundi-las
     * foi o E3 da sessão de 2026-09-06: o relatório do mineiro dizia
     * <i>"105 of 32 so far"</i> comparando <b>tudo o que ele guardou</b>
     * — pedregulho, terra, carvão, minério — com a meta de <b>um</b>
     * recurso. Os dois lados da frase nem falavam da mesma coisa.
     *
     * @param stored tudo o que coube no baú, de qualquer item
     * @param wanted quanto disso era o recurso que a tarefa pediu
     * @param spilled o que não coube e ficou no chão — o baú cheio de verdade. O
     *     que passou do {@link #TYPE_CAP} sai da galeria de propósito e não conta
     *     aqui: confundir os dois pausava o mineiro que cava pedra atrás de
     *     minério (playtest de 2026-10-08, "chest is full" com o baú com espaço)
     */
    record Haul(int stored, int wanted, int spilled) {
    }

    /**
     * Guarda o que caiu no baú do mineiro. Se ele encher, tenta liberar os
     * slots reservados da profissão antes de deixar o excedente no mundo.
     *
     * @param wanted o item que a tarefa pediu, para a conta sair separada
     *     — nulo quando o pedido não vira item deste jogo, e aí o
     *     {@link Haul#wanted()} sai zero
     * @return o que entrou, separado em tudo e no que foi pedido
     */
    static Haul deposit(
            ServerWorld world,
            UUID colonyId,
            WorkerStorage storage,
            List<ItemStack> drops,
            BlockPos dropPosition,
            Item wanted) {
        return deposit(world, Optional.of(colonyId), storage, drops, dropPosition, wanted);
    }

    private static Haul deposit(
            ServerWorld world,
            Optional<UUID> colonyId,
            WorkerStorage storage,
            List<ItemStack> drops,
            BlockPos dropPosition,
            Item wanted) {

        ColonyPos chest = storage.chestPosition();

        int stored = 0;
        int asked = 0;
        int spilled = 0;

        for (ItemStack drop : drops) {
            boolean isAsked = wanted != null && drop.isOf(wanted);
            int before = stored;

            if (!isAsked) {
                // Teto por tipo (TYPE_CAP): passado ele, o bloco sai da
                // galeria e o drop não é guardado. O pedido só tem o teto do baú.
                int room = Math.max(0, TYPE_CAP
                        - ChestWithdrawer.countIn(world, chest, drop.getItem()));

                if (room < drop.getCount()) {
                    if (room == 0) {
                        continue;
                    }

                    drop = new ItemStack(drop.getItem(), room);
                }
            }

            // Devolve quantos **não** couberam, e não quantos entraram.
            // Ler ao contrário foi o defeito que este mineiro cometeu no
            // primeiro teste dele: todo pedregulho guardado virava uma
            // linha de "filled up" com o baú vazio ao lado.
            //
            int leftOver = ChestDepositor.deposit(
                    world, chest, drop.getItem(), drop.getCount());

            if (leftOver > 0 && colonyId.isPresent()) {
                int relieved = ProfessionChestOverflow.relieve(
                        world,
                        colonyId.get(),
                        ColonyChests.nearestFirst(world, colonyId.get(), chest),
                        professionChests(colonyId.get()));

                if (relieved > 0) {
                    leftOver = ChestDepositor.deposit(world, chest, drop.getItem(), leftOver);
                    VillageColonyMod.LOGGER.info(
                            "Miner chest at {} released {} items to village storage before retrying {}",
                            chest, relieved, drop.getItem());
                }
            }

            stored += drop.getCount() - leftOver;

            spilled += leftOver;

            if (leftOver > 0) {
                world.spawnEntity(new ItemEntity(
                        world,
                        dropPosition.getX() + 0.5,
                        dropPosition.getY() + 0.5,
                        dropPosition.getZ() + 0.5,
                        new ItemStack(drop.getItem(), leftOver)));

                VillageColonyMod.LOGGER.warn(
                        "Miner chest at {} is full — dropped {} of {} at {}",
                        chest,
                        leftOver,
                        drop.getItem(),
                        dropPosition);
            }

            if (isAsked) {
                asked += stored - before;
            }
        }

        return new Haul(stored, asked, spilled);
    }

    /** Variante sem colônia para a prova isolada do teto por tipo. */
    static Haul deposit(
            ServerWorld world,
            WorkerStorage storage,
            List<ItemStack> drops,
            BlockPos dropPosition,
            Item wanted) {
        return deposit(world, Optional.empty(), storage, drops, dropPosition, wanted);
    }

    /** Baús reservados não são destino do transbordo de outro profissional. */
    private static Set<ColonyPos> professionChests(UUID colonyId) {
        Set<ColonyPos> chests = new LinkedHashSet<>();

        for (var worker : VillageColonyMod.WORKERS.ofColony(colonyId)) {
            VillageColonyMod.STORAGES.of(worker.villagerId())
                    .map(WorkerStorage::chestPosition)
                    .ifPresent(chests::add);
        }

        return chests;
    }
}
