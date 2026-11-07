package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceGroup;
import com.villagecolony.core.worker.model.Worker;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;

import java.util.List;

/**
 * O desgaste das ferramentas — Regra 221: a ferramenta que a colônia entrega dura {@value #DAYS} dias e a renovada sai do estoque,
 * {@value #PLANKS} tábuas e {@value #STONE} pedras. Sem material ela fica no último ponto e o trabalho segue: gastar estoque nunca
 * para a vila. A ferramenta que veio do jogador não se gasta.
 */
public final class ToolWear {

    static final int DAYS = 3;
    static final int PLANKS = 2;
    static final int STONE = 3;

    private ToolWear() {
    }

    /**
     * O desgaste de um dia na colônia.
     *
     * @return quantas ferramentas foram renovadas com material do estoque
     */
    public static int pass(ServerWorld world, Colony colony) {
        List<ColonyPos> chests = ColonyChests.nearestFirst(world, colony.id(), colony.center());
        int renewed = 0;

        for (Worker worker : VillageColonyMod.WORKERS.ofColony(colony.id())) {
            if (world.getEntity(worker.villagerId()) instanceof VillagerEntity villager) {
                ItemStack tool = villager.getEquippedStack(EquipmentSlot.MAINHAND);

                if (tool.isDamageable() && WorkerEquipment.isProfessionTool(tool)) {
                    renewed += wear(world, chests, tool);
                }
            }
        }

        return renewed;
    }

    private static int wear(ServerWorld world, List<ColonyPos> chests, ItemStack tool) {
        int most = tool.getMaxDamage();
        int worn = tool.getDamage() + Math.max(1, most / DAYS);

        if (worn < most - 1) {
            tool.setDamage(worn);

            return 0;
        }

        if (ColonyChests.countGroup(world, chests, ResourceGroup.PLANKS) < PLANKS
                || ColonyChests.countGroup(world, chests, ResourceGroup.STONE) < STONE) {
            tool.setDamage(most - 1);

            return 0;
        }

        ColonyChests.withdrawGroup(world, chests, ResourceGroup.PLANKS, PLANKS);
        ColonyChests.withdrawGroup(world, chests, ResourceGroup.STONE, STONE);
        tool.setDamage(0);

        return 1;
    }
}
