package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.gametest.Arena;
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

    // Acima dos blocos de preparação do GameTest para a varredura de superfície achar as porteiras.
    private static final int FEET_Y = 16;
    private static final BlockPos CORNER = new BlockPos(12, 0, 12);
    private static final BlockPos CHEST = new BlockPos(2, FEET_Y, 2);
    private static final BlockPos STAND = new BlockPos(3, FEET_Y, 3);

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

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "flock_harvest_pens", tickLimit = 20)
    public void adultsInOtherPensAndOutsideDoNotMakeASurplus(TestContext context) {
        Fixture f = setUp(context);
        List<CowEntity> cows = spawnCows(context, FlockHarvest.HARVEST_KEEP + 2);
        var sheep = context.spawnEntity(EntityType.SHEEP, new BlockPos(6, FEET_Y, 7));
        sheep.setBreedingAge(0);

        try {
            for (int i = 2; i < cows.size() - 1; i++) {
                BlockPos at = i < 4 ? new BlockPos(9, FEET_Y, 5 + i % 2) : new BlockPos(4, FEET_Y, 3);
                cows.get(i).setPosition(context.getAbsolutePos(at).toCenterPos());
            }
            cows.getLast().setBreedingAge(-24_000);
            var pens = ShepherdPens.pensOf(context.getWorld(), f.colony).stream()
                    .filter(pen -> f.area.contains(pen.gate().toCenterPos())).toList();
            context.assertTrue(pens.size() == 2 && pens.stream()
                    .mapToInt(pen -> ShepherdPens.animalsIn(context.getWorld(), pen)).sum() == 6,
                    "o cenário precisa de dois currais, quatro vacas adultas, um filhote e uma ovelha");
            context.assertFalse(FlockHarvest.step(context.getWorld(), f.colony, f.area, f.chests),
                    "abateu sem três adultos da mesma espécie no curral da vítima");
            context.assertTrue(cows.stream().allMatch(CowEntity::isAlive), "a reserva local foi tocada");

            cows.get(4).setPosition(context.getAbsolutePos(new BlockPos(10, FEET_Y, 5)).toCenterPos());
            context.getWorld().getEntity(f.shepherd)
                    .setPosition(context.getAbsolutePos(new BlockPos(8, FEET_Y, 3)).toCenterPos());
            context.assertTrue(FlockHarvest.step(context.getWorld(), f.colony, f.area, f.chests),
                    "o segundo curral agora tem três vacas adultas e permite um abate");
            context.assertTrue(cows.subList(2, 5).stream().filter(CowEntity::isAlive).count() == 2,
                    "o segundo curral precisa manter duas vacas adultas");
            context.assertTrue(cows.subList(0, 2).stream().allMatch(CowEntity::isAlive)
                            && cows.subList(5, cows.size()).stream().allMatch(CowEntity::isAlive),
                    "somente uma das três vacas adultas do segundo curral pode ser abatida");
        } finally {
            sheep.discard();
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

    /** O pastor precisa alcançar o animal antes do abate. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "flock_harvest_distance", tickLimit = 20)
    public void distantShepherdDoesNotButcherCattle(TestContext context) {
        Fixture f = setUp(context);
        List<CowEntity> cows = spawnCows(context, FlockHarvest.HARVEST_KEEP + 1);
        context.getWorld().getEntity(f.shepherd).setPosition(context.getAbsolutePos(new BlockPos(11, FEET_Y, 11)).toCenterPos());

        try {
            context.assertFalse(FlockHarvest.step(context.getWorld(), f.colony, f.area, f.chests),
                    "pastor distante abateu o rebanho");
            context.assertTrue(cows.stream().allMatch(CowEntity::isAlive), "um animal morreu sem pastor próximo");
        } finally {
            tearDown(context, f, cows);
        }

        context.complete();
    }

    /** Regra 299: o animal que o pastor leva na corda nunca é abatido, mesmo com excedente. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "flock_harvest_led", tickLimit = 20)
    public void theLedAnimalIsNeverButchered(TestContext context) {
        Fixture f = setUp(context);
        List<CowEntity> cows = spawnCows(context, FlockHarvest.HARVEST_KEEP + 1);
        VillagerEntity shepherd = (VillagerEntity) context.getWorld().getEntity(f.shepherd);
        cows.forEach(cow -> cow.attachLeash(shepherd, true));

        try {
            context.assertFalse(FlockHarvest.step(context.getWorld(), f.colony, f.area, f.chests),
                    "Regra 299: o pastor abateu um animal que levava na corda");
            context.assertTrue(cows.stream().allMatch(CowEntity::isAlive), "Regra 299: um animal da corda morreu");
        } finally {
            tearDown(context, f, cows);
        }

        context.complete();
    }

    private record Fixture(Colony colony, ColonyFixture owned, Box area, List<ColonyPos> chests, UUID shepherd) {
    }

    private Fixture setUp(TestContext context) {
        Arena.forceChunks(context, BlockPos.ORIGIN, CORNER, true);
        for (int dx = 0; dx <= 12; dx++) {
            for (int dz = 0; dz <= 12; dz++) {
                context.setBlockState(new BlockPos(dx, FEET_Y - 1, dz), Blocks.STONE.getDefaultState());
            }
        }

        buildPen(context, 4);
        buildPen(context, 8);
        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());
        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));
        Colony colony = Colony.create(UUID.randomUUID(), chest);
        BlockPos low = context.getAbsolutePos(new BlockPos(0, FEET_Y - 1, 0));
        BlockPos high = context.getAbsolutePos(new BlockPos(12, FEET_Y + 2, 12));
        colony.measure(new VillageBounds(Math.min(low.getX(), high.getX()), low.getY(),
                Math.min(low.getZ(), high.getZ()), Math.max(low.getX(), high.getX()), high.getY(),
                Math.max(low.getZ(), high.getZ())));
        VillageColonyMod.COLONIES.register(colony);
        ColonyFixture owned = ColonyFixture.create().owning(colony);

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, STAND);
        villager.setBreedingAge(0);
        VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id()).assign(ProfessionType.SHEPHERD);
        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chest));
        owned.owning(villager.getUuid());

        // Área restrita à arena: o teste não vê (nem abate) o rebanho de arenas vizinhas.
        Box area = Box.enclosing(context.getAbsolutePos(new BlockPos(0, FEET_Y - 1, 0)),
                context.getAbsolutePos(new BlockPos(12, FEET_Y + 2, 12)));
        List<ColonyPos> chests = List.of(chest);

        return new Fixture(colony, owned, area, chests, villager.getUuid());
    }

    private static void buildPen(TestContext context, int startX) {
        for (int x = startX; x <= startX + 3; x++) {
            for (int z = 4; z <= 8; z++) {
                if (x == startX || x == startX + 3 || z == 4 || z == 8) {
                    context.setBlockState(new BlockPos(x, FEET_Y, z), Blocks.OAK_FENCE.getDefaultState());
                }
            }
        }
        context.setBlockState(new BlockPos(startX + 1, FEET_Y, 4), Blocks.OAK_FENCE_GATE.getDefaultState());
    }

    private List<CowEntity> spawnCows(TestContext context, int count) {
        List<CowEntity> cows = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            CowEntity cow = context.spawnEntity(EntityType.COW, new BlockPos(5 + i % 2, FEET_Y, 5 + (i / 2) % 3));
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
                f.area, v -> v.getUuid().equals(f.shepherd)).forEach(net.minecraft.entity.Entity::discard);
        context.setBlockState(CHEST, Blocks.AIR.getDefaultState());
        for (int x = 4; x <= 11; x++) {
            for (int z = 4; z <= 8; z++) {
                context.setBlockState(new BlockPos(x, FEET_Y, z), Blocks.AIR.getDefaultState());
            }
        }
        for (int x = 0; x <= CORNER.getX(); x++) {
            for (int z = 0; z <= CORNER.getZ(); z++) {
                context.setBlockState(new BlockPos(x, FEET_Y - 1, z), Blocks.AIR.getDefaultState());
            }
        }
        f.owned.cleanUp();
        Arena.forceChunks(context, BlockPos.ORIGIN, CORNER, false);
    }
}
