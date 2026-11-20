package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.gametest.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.VillagerProfession;

/**
 * Regra 341: quem pegou ofício Vanilla antes de ser do mod perde o ofício e a roupa assim que é contratado. O autor viu
 * um trabalhador do mod com roupa Vanilla: a varredura limpava antes de contratar, e a roupa ficava até a próxima.
 */
public class VanillaTradeAfterHireGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "vanilla_trade_after_hire", tickLimit = 20)
    public void aVillagerHiredWithAVanillaTradeLosesIt(TestContext context) {
        ColonyFixture fixture = ColonyFixture.colonyAt(context, new BlockPos(1, 2, 1));
        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, new BlockPos(3, 2, 3));

        try {
            villager.setAiDisabled(true);
            Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), fixture.colony().id());
            fixture.owning(villager.getUuid());

            // Ainda sem ofício do mod, o Vanilla pode dar o dele.
            villager.setVillagerData(villager.getVillagerData().withProfession(VillagerProfession.FARMER));
            context.assertTrue(villager.getVillagerData().getProfession() == VillagerProfession.FARMER,
                    "o cenário não mede nada: o aldeão sem ofício do mod não pegou o ofício Vanilla");

            worker.assign(ProfessionType.LUMBERJACK);
            VanillaProfessionGuard.sweep(context.getWorld(), fixture.colony().id());

            context.assertTrue(villager.getVillagerData().getProfession() == VillagerProfession.NONE,
                    "Regra 341: o lenhador contratado ficou com o ofício e a roupa de fazendeiro");
        } finally {
            villager.discard();
            fixture.cleanUp();
        }

        context.complete();
    }
}
