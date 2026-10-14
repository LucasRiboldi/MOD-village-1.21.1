package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.gametest.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * O pastor colhe o excedente do rebanho — decisão do autor, 2026-10-14. Abate um adulto por ciclo
 * acima de {@link FlockHarvest#HARVEST_KEEP}, deixando sempre a reserva viva, e deposita couro/carne.
 * A regra pura (quanto abater) é exercitada em unidade ({@code FlockHarvestTest}); aqui o efeito no
 * mundo, com a área restrita à arena para o teste ser determinístico (a bateria roda arenas juntas).
 */
public class FlockHarvestGameTest implements FabricGameTest {

    private static final BlockPos CHEST = new BlockPos(2, 2, 2);
    private static final BlockPos STAND = new BlockPos(3, 2, 3);

    /** Acima da reserva (HARVEST_KEEP+1): abate um, deixa a reserva, e põe couro+carne no baú. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "flock_harvest_surplus", tickLimit = 20)
    public void surplusCattleAreButcheredDownToTheReserve(TestContext context) {
        Fixture f = setUp(context);
        int herd = FlockHarvest.HARVEST_KEEP + 1;
        List<CowEntity> cows = spawnCows(context, herd);

        try {
            boolean butchered = FlockHarvest.step(context.getWorld(), f.colony, f.area, f.chests);

            context.assertTrue(butchered, "havia excedente e nada foi abatido");
            long alive = cows.stream().filter(c -> c.isAlive()).count();
            context.assertTrue(alive == FlockHarvest.HARVEST_KEEP,
                    "devia sobrar a reserva viva (" + FlockHarvest.HARVEST_KEEP + "), sobraram " + alive);
            context.assertTrue(countIn(context, f, net.minecraft.item.Items.LEATHER) > 0,
                    "o couro da vaca abatida não entrou no baú");
            context.assertTrue(countIn(context, f, net.minecraft.item.Items.BEEF) > 0
                            || countIn(context, f, net.minecraft.item.Items.COOKED_BEEF) > 0,
                    "a carne da vaca abatida não entrou no baú (crua ou cozida)");
        } finally {
            tearDown(context, f, cows);
        }

        context.complete();
    }

    /** Na reserva (HARVEST_KEEP): nada é abatido. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "flock_harvest_reserve", tickLimit = 20)
    public void theLiveReserveIsNeverButchered(TestContext context) {
        Fixture f = setUp(context);
        List<CowEntity> cows = spawnCows(context, FlockHarvest.HARVEST_KEEP);

        try {
            boolean butchered = FlockHarvest.step(context.getWorld(), f.colony, f.area, f.chests);

            context.assertFalse(butchered, "abateu abaixo/na reserva viva");
            long alive = cows.stream().filter(c -> c.isAlive()).count();
            context.assertTrue(alive == FlockHarvest.HARVEST_KEEP, "a reserva viva foi tocada: " + alive);
        } finally {
            tearDown(context, f, cows);
        }

        context.complete();
    }

    private record Fixture(Colony colony, ColonyFixture owned, Box area, List<ColonyPos> chests, UUID shepherd) {
    }

    private Fixture setUp(TestContext context) {
        for (int dx = 0; dx <= 12; dx++) {
            for (int dz = 0; dz <= 12; dz++) {
                context.setBlockState(new BlockPos(dx, 0, dz), Blocks.STONE.getDefaultState());
            }
        }

        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());
        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));
        Colony colony = Colony.create(UUID.randomUUID(), chest);
        VillageColonyMod.COLONIES.register(colony);
        ColonyFixture owned = ColonyFixture.create().owning(colony);

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, STAND);
        villager.setBreedingAge(0);
        VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id()).assign(ProfessionType.SHEPHERD);
        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chest));
        owned.owning(villager.getUuid());

        // Área restrita à arena: o teste não vê (nem abate) o rebanho de arenas vizinhas.
        Box area = Box.enclosing(context.getAbsolutePos(new BlockPos(0, 0, 0)),
                context.getAbsolutePos(new BlockPos(12, 4, 12)));
        List<ColonyPos> chests = List.of(chest);

        return new Fixture(colony, owned, area, chests, villager.getUuid());
    }

    private List<CowEntity> spawnCows(TestContext context, int count) {
        List<CowEntity> cows = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            CowEntity cow = context.spawnEntity(EntityType.COW, new BlockPos(5 + (i % 5), 2, 5 + (i / 5)));
            cow.setBreedingAge(0);
            cows.add(cow);
        }

        return cows;
    }

    private int countIn(TestContext context, Fixture f, net.minecraft.item.Item item) {
        return com.villagecolony.fabric.integration.ColonyChests.countIn(
                context.getWorld(), f.chests, item);
    }

    private void tearDown(TestContext context, Fixture f, List<CowEntity> cows) {
        cows.forEach(net.minecraft.entity.Entity::discard);
        context.getWorld().getEntitiesByClass(VillagerEntity.class,
                new Box(context.getAbsolutePos(STAND)).expand(2), v -> true).forEach(net.minecraft.entity.Entity::discard);
        context.setBlockState(CHEST, Blocks.AIR.getDefaultState());
        f.owned.cleanUp();
    }
}
