package com.villagecolony.fabric.integration;

import net.minecraft.item.Item;

import java.util.List;

/** Uma peca e as dependencias que provam se a vila consegue produzi-la. */
public record SupplyRoute(Item item, boolean available, SupplySource source, List<SupplyRoute> inputs) {

    public SupplyRoute {
        inputs = List.copyOf(inputs);
    }

    public static SupplyRoute available(Item item, SupplySource source, List<SupplyRoute> inputs) {
        return new SupplyRoute(item, true, source, inputs);
    }

    public static SupplyRoute unavailable(Item item, SupplySource source, List<SupplyRoute> inputs) {
        return new SupplyRoute(item, false, source, inputs);
    }
}
