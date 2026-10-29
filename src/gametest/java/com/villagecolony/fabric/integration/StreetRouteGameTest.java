package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.gametest.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * O trajeto longo vai pela rede de ruas — autor, 2026-10-28, depois do jogo da 0.3.104 (escolha "as duas juntas"). O
 * cérebro do jogo pede o caminho até o destino e recebe o próximo trecho ({@link ColonyPathing#legToward}); a regra do
 * trecho é a do {@code StreetRoute}.
 */
public class StreetRouteGameTest implements FabricGameTest {

    /**
     * Rua em L a 3 blocos do aldeão, até perto de um destino a 62 blocos: o primeiro trecho é a coluna de rua mais
     * perto dele, no chão. Trajeto curto e aldeão que não é da colônia vão direto.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "street_route", tickLimit = 20)
    public void aLongTripFirstHeadsForTheNearestStreet(TestContext context) {
        var world = context.getWorld();

        for (int x = 0; x <= 12; x++) {
            for (int z = 0; z <= 12; z++) {
                context.setBlockState(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK);
            }
        }

        BlockPos at = context.getAbsolutePos(new BlockPos(4, 2, 6));
        Colony colony = Colony.create(UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(at));

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);
        VillagerEntity villager = EntityType.VILLAGER.create(world);
        VillagerEntity stranger = EntityType.VILLAGER.create(world);

        villager.refreshPositionAndAngles(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        stranger.refreshPositionAndAngles(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id()).assign(ProfessionType.MASON);
        owned.owning(villager.getUuid());

        List<Long> columns = new ArrayList<>();

        for (int i = 0; i <= 60; i++) {
            columns.add(ColonyRoads.column(at.getX() + i, at.getZ() - 3));
            columns.add(ColonyRoads.column(at.getX() + 60, at.getZ() - 3 + i));
        }

        SweepState.ROADS.put(colony.id(), new ColonyRoads(colony.id(), MinecraftTypeAdapter.toColonyPos(at), columns));

        try {
            BlockPos far = at.add(62, 0, 50);
            BlockPos leg = ColonyPathing.legToward(villager, far);

            context.assertTrue(leg.getX() == at.getX() && leg.getZ() == at.getZ() - 3 && leg.getY() == at.getY(),
                    "o primeiro trecho devia ser a rua a 3 blocos, de pé em " + at.getX() + ", " + at.getY() + ", "
                            + (at.getZ() - 3) + "; foi " + leg.toShortString());

            BlockPos near = at.add(10, 0, 0);
            context.assertTrue(ColonyPathing.legToward(villager, near).equals(near), "o trajeto curto devia ir direto");
            context.assertTrue(ColonyPathing.legToward(stranger, far).equals(far),
                    "o aldeão de fora da colônia devia ir direto");
        } finally {
            SweepState.ROADS.remove(colony.id());
            owned.cleanUp();
        }

        context.complete();
    }
}
