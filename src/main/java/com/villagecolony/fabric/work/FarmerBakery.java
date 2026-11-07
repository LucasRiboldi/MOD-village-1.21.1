package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.ChestWithdrawer;
import com.villagecolony.fabric.integration.ColonyChests;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;

import java.util.List;

/**
 * O fazendeiro faz o pão — decisão do autor, 2026-09-30: <i>"comida é
 * feita pelo fazendeiro"</i>.
 *
 * <p>O cardápio da {@link VillageMeals} põe o pão primeiro (vale 4 pontos
 * de disposição, contra 1 de cenoura, batata e beterraba), e ninguém o
 * fazia: o trigo, que é a maior colheita, não servia de comida.
 *
 * <p>A receita é a do jogo — três trigos, um pão —, feita no baú do
 * fazendeiro depois de cada colheita guardada. <b>Uma reserva de trigo
 * fica crua</b>: o pastor precisa dela para o rebanho procriar
 * ({@link ShepherdFlock}), e a obra pode pedir fardo.
 */
public final class FarmerBakery {

    /** Trigo que nunca vira pão: o do pastor (ADR-037 C7 c; eram 32). */
    public static final int WHEAT_RESERVE = 8;

    /** Trigos por pão, como na bancada do jogo. */
    public static final int WHEAT_PER_BREAD = 3;

    /** Pães por passada, para o custo por tique continuar pequeno. */
    private static final int MOST_PER_PASS = 8;

    private FarmerBakery() {
    }

    /**
     * Assa o trigo que passa da reserva.
     *
     * @return quantos pães entraram no baú
     */
    public static int bakeSurplus(ServerWorld world, ColonyPos chest) {
        int wheat = ChestWithdrawer.countIn(world, chest, Items.WHEAT);
        int loaves = Math.min(MOST_PER_PASS, Math.max(0, wheat - WHEAT_RESERVE) / WHEAT_PER_BREAD);

        loaves = Math.min(loaves, ChestDepositor.freeSpaceFor(world, chest, Items.BREAD));

        if (loaves <= 0) {
            return 0;
        }

        int taken = ChestWithdrawer.withdraw(world, chest, Items.WHEAT, loaves * WHEAT_PER_BREAD);
        int baked = taken / WHEAT_PER_BREAD;
        int crumbs = taken - baked * WHEAT_PER_BREAD;

        if (crumbs > 0) {
            ChestDepositor.deposit(world, chest, Items.WHEAT, crumbs);
        }

        if (baked > 0) {
            ChestDepositor.deposit(world, chest, Items.BREAD, baked);

            VillageColonyMod.LOGGER.info(
                    "Farmer at {} baked {} bread from {} wheat", chest, baked, baked * WHEAT_PER_BREAD);
        }

        return baked;
    }

    /**
     * A ceia assa o trigo de qualquer baú da vila — Regra 217: a colheita fica espalhada, e só o baú do próprio fazendeiro assava.
     *
     * @param most quantos pães a ceia ainda precisa
     * @return quantos pães entraram nos baús
     */
    public static int bakeForSupper(ServerWorld world, List<ColonyPos> chests, int most) {
        int spare = Math.max(0, ColonyChests.countIn(world, chests, Items.WHEAT) - WHEAT_RESERVE);
        int left = Math.min(most, spare / WHEAT_PER_BREAD);
        int baked = 0;

        for (ColonyPos chest : chests) {
            if (left <= 0) {
                break;
            }

            int loaves = Math.min(
                    Math.min(left, ChestWithdrawer.countIn(world, chest, Items.WHEAT) / WHEAT_PER_BREAD),
                    ChestDepositor.freeSpaceFor(world, chest, Items.BREAD));

            if (loaves > 0) {
                ChestWithdrawer.withdraw(world, chest, Items.WHEAT, loaves * WHEAT_PER_BREAD);
                ChestDepositor.deposit(world, chest, Items.BREAD, loaves);
                left -= loaves;
                baked += loaves;
            }
        }

        return baked;
    }
}
