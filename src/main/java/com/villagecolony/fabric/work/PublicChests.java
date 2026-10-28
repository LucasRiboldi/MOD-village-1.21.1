package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.MineMouth;
import com.villagecolony.fabric.integration.VillageChests;
import net.minecraft.item.Item;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Os baús públicos da vila: os que não são baú de nenhuma profissão — regra do autor,
 * 2026-10-12. Madeira e pedra cruas podem passar dos limites do baú do ofício e do teto
 * por tipo; o excedente vai para eles em vez de cair no chão ou se perder (playtest
 * 0.3.37: 267 toras caídas pelo baú cheio do lenhador). O baú público não tem o teto de
 * compartimentos por item e enche antes do seguinte — Regra 143.
 */
final class PublicChests {

    private PublicChests() {
    }

    /** Os baús da vila que nenhum ofício reivindica, do mais perto para o mais longe. */
    static List<ColonyPos> of(ServerWorld world, UUID colonyId, BlockPos near) {
        ColonyPos from = MinecraftTypeAdapter.toColonyPos(near);
        // O baú da boca da mina é do minério (Regra 30): não é público.
        Optional<ColonyPos> mouth = VillageColonyMod.MINES.of(colonyId)
                .map(mine -> MinecraftTypeAdapter.toBlockPos(mine.shaft().entry()))
                .flatMap(at -> MineMouth.chestAt(world, at))
                .map(MinecraftTypeAdapter::toColonyPos);

        // Fora da caixa é fora de alcance, como em ColonyChests: a varredura lê chunks inteiros.
        return VillageColonyMod.COLONIES.find(colonyId)
                .map(colony -> VillageChests.around(world, colony, List.of()).stream()
                        .filter(chest -> colony.bounds().map(box -> VillageChests.isInside(box, chest)).orElse(true))
                        .toList())
                .orElse(List.of()).stream()
                .filter(chest -> mouth.map(at -> !at.equals(chest)).orElse(true))
                .sorted(Comparator.comparingLong((ColonyPos chest) -> chest.horizontalDistanceSquared(from)))
                .toList();
    }

    /**
     * Guarda nos baús públicos o que não coube no baú do ofício.
     *
     * @return quanto não coube em nenhum
     */
    static int store(ServerWorld world, UUID colonyId, BlockPos near, Item item, int amount) {
        if (amount <= 0) {
            return 0;
        }

        // Sem teto de compartimentos e do mais perto para o mais longe: um baú enche antes do seguinte
        // (Regra 143). O lenhador largava toras no chão com a vila a 38% (taiga simulada, 2026-10-28).
        int left = amount;

        for (ColonyPos chest : of(world, colonyId, near)) {
            if (left <= 0) {
                break;
            }

            left = ChestDepositor.deposit(world, chest, item, left, true);
        }

        if (left < amount) {
            VillageColonyMod.LOGGER.info("Colony {} — {} {} went to the village's public chests",
                    colonyId.toString().substring(0, 8), amount - left, item);
        }

        return left;
    }
}
