package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ColonyChests;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.UUID;

/**
 * A trava da tosquia ({@code ReservationGate.WOOL_ON_A_SHEEP}) — escolha do autor, 2026-10-28: a tarefa de
 * lã só é reservada com ovelha lanuda na área do pastor e com menos de {@link #CAP} da lã pedida nos baús.
 * Sem isso o pastor pegava a tarefa, esperava 30 s sem achar ovelha com lã e a soltava (28 vezes no jogo
 * da 0.3.99, com 102 lãs no baú).
 */
public final class WoolGate {

    /** Acima disto da lã pedida nos baús, não se tosquia. */
    public static final int CAP = 64;

    /** O raio do pastor em volta do centro, como na tosquia. */
    static final int RADIUS = 32;

    private WoolGate() {
    }

    /** Se a tarefa de lã desta colônia pode ser reservada agora. */
    public static boolean open(ServerWorld world, UUID colonyId, ResourceType wool) {
        Optional<Colony> colony = VillageColonyMod.COLONIES.find(colonyId);

        if (colony.isEmpty()) {
            return false;
        }

        Optional<Item> item = MinecraftTypeAdapter.toItem(wool);

        if (item.isPresent() && ColonyChests.countIn(world,
                ColonyChests.nearestFirst(world, colonyId, colony.get().center()), item.get()) >= CAP) {
            return false;
        }

        BlockPos center = MinecraftTypeAdapter.toBlockPos(colony.get().center());

        return !world.getEntitiesByType(EntityType.SHEEP, ShepherdFlock.shearingArea(colony.get(), center, RADIUS),
                sheep -> sheep.isAlive() && sheep.isShearable()).isEmpty();
    }
}
