package com.villagecolony.fabric.integration;

import com.villagecolony.core.storage.model.ChestSlotCap;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Mantém livres os dez slots finais dos baús reservados às profissões — ADR-036
 * item 9: baú de profissão lotado manda os dez últimos compartimentos para um
 * baú da vila sem profissão; sem baú livre, a colônia fica marcada
 * ({@link #needsStorage}) e a próxima obra é o {@code storage_majest}.
 */
public final class ProfessionChestOverflow {
    private static final int RESERVED_SLOTS = 10;

    static {
        ServerMemory.register(ProfessionChestOverflow.class, ProfessionChestOverflow::clearAll);
    }

    /** Colônias com baú de profissão lotado e nenhum baú livre que o aliviasse. */
    private static final Set<UUID> NEEDS_STORAGE = new HashSet<>();

    private ProfessionChestOverflow() {
    }

    public static void clearAll() {
        NEEDS_STORAGE.clear();
    }

    /** Se, no último alívio, faltou baú livre na vila desta colônia. */
    public static boolean needsStorage(UUID colonyId) {
        return NEEDS_STORAGE.contains(colonyId);
    }

    public static int relieve(ServerWorld world, List<ColonyPos> villageChests,
            Set<ColonyPos> professionChests) {
        return relieve(world, Optional.empty(), villageChests, professionChests);
    }

    /**
     * Mantém espaço nas profissões e, se a vila estiver saturada, usa o
     * armazém comunitário de emergência da mina já concluída.
     */
    public static int relieve(ServerWorld world, UUID colonyId,
            List<ColonyPos> villageChests, Set<ColonyPos> professionChests) {
        return relieve(world, Optional.of(colonyId), villageChests, professionChests);
    }

    private static int relieve(ServerWorld world, Optional<UUID> colonyId,
            List<ColonyPos> villageChests, Set<ColonyPos> professionChests) {
        Map<ColonyPos, ChestInventories.Handle> observed = new LinkedHashMap<>();
        for (ColonyPos chest : villageChests) {
            ChestInventories.at(world, chest)
                    .ifPresent(handle -> observed.putIfAbsent(handle.key(), handle));
        }

        List<Inventory> community = observed.values().stream()
                .filter(handle -> !handle.isProfession(professionChests))
                .map(ChestInventories.Handle::inventory)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));

        // Um baú totalmente vazio recebe primeiro. Assim o alívio preserva
        // pilhas já organizadas e deixa os demais baús comunitários utilizáveis.
        List<Inventory> destinations = new ArrayList<>();
        community.stream().filter(ProfessionChestOverflow::isEmpty).forEach(destinations::add);
        community.stream().filter(inventory -> !isEmpty(inventory)).forEach(destinations::add);

        int moved = 0;
        boolean saturated = false;
        for (ChestInventories.Handle handle : observed.values()) {
            if (!handle.isProfession(professionChests)) {
                continue;
            }
            Inventory source = handle.inventory();
            if (isFull(source)) {
                moved += relieveLastSlots(source, destinations);
                saturated |= hasReservedItems(source);
                if (hasReservedItems(source) && colonyId.isPresent()) {
                    Optional<Inventory> storage = MineOverflowStorage.ensure(world, colonyId.get())
                            .flatMap(chest -> ChestInventories.at(world, chest))
                            .map(ChestInventories.Handle::inventory);
                    if (storage.isPresent()) {
                        moved += relieveLastSlots(source, List.of(storage.get()));
                    }
                }
            }
        }

        if (colonyId.isPresent()) {
            if (saturated) {
                NEEDS_STORAGE.add(colonyId.get());
            } else {
                NEEDS_STORAGE.remove(colonyId.get());
            }
        }

        return moved;
    }

    private static int relieveLastSlots(Inventory source,
            List<Inventory> destinations) {
        int moved = 0;
        int firstReservedSlot = Math.max(0, source.size() - RESERVED_SLOTS);

        for (int slot = firstReservedSlot; slot < source.size(); slot++) {
            ItemStack stack = source.getStack(slot);
            if (stack.isEmpty()) {
                continue;
            }

            int before = stack.getCount();
            for (Inventory destination : destinations) {
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

    private static void moveInto(Inventory destination, ItemStack source) {
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

        // Teto de compartimentos por item também no destino (ADR-036 9).
        int holding = 0;
        for (int slot = 0; slot < destination.size(); slot++) {
            if (destination.getStack(slot).isOf(source.getItem())) {
                holding++;
            }
        }

        for (int slot = 0; slot < destination.size() && !source.isEmpty()
                && holding < ChestSlotCap.MAX_SLOTS_PER_ITEM; slot++) {
            if (!destination.getStack(slot).isEmpty()) {
                continue;
            }

            int amount = Math.min(source.getMaxCount(), source.getCount());
            ItemStack inserted = source.copy();
            inserted.setCount(amount);
            destination.setStack(slot, inserted);
            source.decrement(amount);
            destination.markDirty();
            holding++;
        }
    }

    private static boolean isFull(Inventory inventory) {
        for (int slot = 0; slot < inventory.size(); slot++) {
            if (inventory.getStack(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasReservedItems(Inventory inventory) {
        int firstReservedSlot = Math.max(0, inventory.size() - RESERVED_SLOTS);
        for (int slot = firstReservedSlot; slot < inventory.size(); slot++) {
            if (!inventory.getStack(slot).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static boolean isEmpty(Inventory inventory) {
        for (int slot = 0; slot < inventory.size(); slot++) {
            if (!inventory.getStack(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

}
