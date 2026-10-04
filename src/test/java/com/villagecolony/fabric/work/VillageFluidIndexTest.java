package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.model.VillageBounds;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VillageFluidIndexTest {

    @AfterEach
    void clearIndexes() {
        VillageFluidIndex.clearAll();
    }

    @Test
    void marksOnlyLoadedFluidColumnsInsideTheExactBounds() {
        UUID colonyId = UUID.randomUUID();
        VillageBounds bounds = new VillageBounds(0, 0, 0, 2, 4, 1);
        Set<BlockPos> fluids = Set.of(new BlockPos(0, 0, 0), new BlockPos(2, 0, 1));

        VillageFluidIndex.refresh(
                colonyId, bounds,
                column -> column.getX() != 1,
                column -> fluids.contains(new BlockPos(column.getX(), 0, column.getZ())));

        assertTrue(VillageFluidIndex.skip(colonyId, bounds, new BlockPos(0, 50, 0)));
        assertTrue(VillageFluidIndex.skip(colonyId, bounds, new BlockPos(2, -20, 1)));
        assertFalse(VillageFluidIndex.skip(colonyId, bounds, new BlockPos(1, 0, 0)),
                "a coluna não carregada não pode ser pulada");
        assertFalse(VillageFluidIndex.skip(colonyId, bounds, new BlockPos(3, 0, 1)),
                "uma coluna fora da vila não pertence ao índice");
    }

    @Test
    void rebuildsIncrementallyAndAChangedVillageStartsEmpty() {
        UUID colonyId = UUID.randomUUID();
        VillageBounds bounds = new VillageBounds(0, 0, 0, 39, 4, 39);
        BlockPos last = new BlockPos(39, 0, 39);

        VillageFluidIndex.refresh(
                colonyId, bounds, column -> true,
                column -> column.getX() == last.getX() && column.getZ() == last.getZ());
        assertFalse(VillageFluidIndex.skip(colonyId, bounds, last),
                "a primeira passagem leu além do orçamento incremental");

        VillageFluidIndex.refresh(
                colonyId, bounds, column -> true,
                column -> column.getX() == last.getX() && column.getZ() == last.getZ());
        assertTrue(VillageFluidIndex.skip(colonyId, bounds, last));

        VillageBounds grown = new VillageBounds(-1, 0, -1, 39, 4, 39);
        assertFalse(VillageFluidIndex.skip(colonyId, grown, last),
                "a caixa expandida reutilizou o índice antigo");
    }

    @Test
    void invalidationForcesAFullRebuildForTheColony() {
        UUID colonyId = UUID.randomUUID();
        VillageBounds bounds = new VillageBounds(0, 0, 0, 0, 4, 0);
        BlockPos fluid = new BlockPos(0, 0, 0);

        VillageFluidIndex.refresh(colonyId, bounds, column -> true, column -> true);
        assertTrue(VillageFluidIndex.skip(colonyId, bounds, fluid));

        VillageFluidIndex.invalidate(colonyId);

        assertFalse(VillageFluidIndex.skip(colonyId, bounds, fluid));
    }

    @Test
    void revisitsAColumnThatWasUnavailableWhenItsChunkLaterLoads() {
        UUID colonyId = UUID.randomUUID();
        VillageBounds bounds = new VillageBounds(0, 0, 0, 0, 4, 0);
        BlockPos column = new BlockPos(0, 0, 0);

        VillageFluidIndex.refresh(colonyId, bounds, ignored -> false, ignored -> true);
        assertFalse(VillageFluidIndex.skip(colonyId, bounds, column));

        VillageFluidIndex.refresh(colonyId, bounds, ignored -> true, ignored -> true);
        assertTrue(VillageFluidIndex.skip(colonyId, bounds, column),
                "a coluna ignorada por chunk descarregado nunca foi revisitada");
    }
}
