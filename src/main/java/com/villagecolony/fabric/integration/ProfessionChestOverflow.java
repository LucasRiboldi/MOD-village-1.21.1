package com.villagecolony.fabric.integration;

import com.villagecolony.core.type.ColonyPos;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Mantém livres os dez slots finais dos baús reservados às profissões. */
public final class ProfessionChestOverflow {
    private static final int RESERVED_SLOTS = 10;

    private ProfessionChestOverflow() {
    }

    public static int relieve(ServerWorld world, List<ColonyPos> villageChests,
            Set<ColonyPos> professionChests) {
        List<ChestBlockEntity> destinations = new ArrayList<>();
        for (ColonyPos chest : villageChests) {
            if (!professionChests.contains(chest)) {
                ChestBlockEntity inventory = ChestWithdrawer.chestAt(world, chest);
                if (inventory != null) {
                    destinations.add(inventory);
                }
            }
        }

        if (destinations.isEmpty()) {
            return 0;
        }

        int moved = 0;
        for (ColonyPos chest : villageChests) {
            if (!professionChests.contains(chest)) {
                continue;
            }
            ChestBlockEntity source = ChestWithdrawer.chestAt(world, chest);
            if (source != null && isFull(source)) {
                moved += relieveLastSlots(source, destinations);
            }
        }
        return moved;
    }

    private static int relieveLastSlots(ChestBlockEntity source,
            List<ChestBlockEntity> destinations) {
        int moved = 0;
        int firstReservedSlot = Math.max(0, source.size() - RESERVED_SLOTS);

        for (int slot = firstReservedSlot; slot < source.size(); slot++) {
            ItemStack stack = source.getStack(slot);
            if (stack.isEmpty()) {
                continue;
            }

            int before = stack.getCount();
            for (ChestBlockEntity destination : destinations) {
                moveInto(destination, stack);
                if (stack.isEmpty()) {
                    break;
                }
            }

            int transferred = before - stack.getCount();
            if (transferred > 0) {
                source.setStack(slot, stack.isEmpty() ? ItemStack.EMPTY : stack);
                source.markDirty();
                moved += transferred;
            }
        }
        return moved;
    }

    private static void moveInto(ChestBlockEntity destination, ItemStack source) {
        for (int slot = 0; slot < destination.size() && !source.isEmpty(); slot++) {
            ItemStack stored = destination.getStack(slot);
            if (stored.isEmpty() || !ItemStack.areItemsAndComponentsEqual(stored, source)) {
                continue;
            }

            int room = stored.getMaxCount() - stored.getCount();
            if (room > 0) {
                int amount = Math.min(room, source.getCount());
                stored.increment(amount);
                source.decrement(amount);
                destination.markDirty();
            }
        }

        for (int slot = 0; slot < destination.size() && !source.isEmpty(); slot++) {
            if (!destination.getStack(slot).isEmpty()) {
                continue;
            }

            int amount = Math.min(source.getMaxCount(), source.getCount());
            ItemStack inserted = source.copy();
            inserted.setCount(amount);
            destination.setStack(slot, inserted);
            source.decrement(amount);
            destination.markDirty();
        }
    }

    private static boolean isFull(ChestBlockEntity inventory) {
        for (int slot = 0; slot < inventory.size(); slot++) {
            if (inventory.getStack(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

}
