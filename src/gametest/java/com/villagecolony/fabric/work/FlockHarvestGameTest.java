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
import java.util.Map;
import java.util.UUID;

/**
 * O pastor colhe o excedente do rebanho — decisão do autor, 2026-10-14. Abate um adulto por ciclo
 * acima de {@link ShepherdFlock#FLOCK_TARGET}, deixando sempre a reserva viva, e deposita couro/carne.
 * A regra pura (quanto abater) é exercitada em unidade ({@code FlockHarvestTest}); aqui o efeito no
 * mundo, com a área restrita à arena para o teste ser determinístico (a bateria roda arenas juntas).
 */
public class FlockHarvestGameTest implements FabricGameTest {

    // Acima dos blocos de preparação do GameTest para a varredura de superfície achar as porteiras.
    private static final int FEET_Y = 16;
    private static final BlockPos CORNER = new BlockPos(12, 0, 12);
    private static final BlockPos CHEST = new BlockPos(2, FEET_Y, 2);
    private static final BlockPos STAND = new BlockPos(3, FEET_Y, 3);

    /** Acima da reserva (FLOCK_TARGET + 1): abate um, deixa a reserva, e põe couro+carne no baú. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "flock_harvest_surplus", tickLimit = 20)
    public void surplusCattleAreButcheredDownToTheReserve(TestContext context) {
        Fixture f = setUp(context);
        int herd = ShepherdFlock.FLOCK_TARGET + 1;
        List<CowEntity> cows = spawnCows(context, herd);

        try {
            boolean butchered = harvest(context, f);

            context.assertTrue(butchered, "havia excedente e nada foi abatido");
            long alive = cows.stream().filter(c -> c.isAlive()).count();
            context.assertTrue(alive == ShepherdFlock.FLOCK_TARGET,
                    "devia sobrar a reserva viva (" + ShepherdFlock.FLOCK_TARGET + "), sobraram " + alive);
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

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "flock_harvest_loose", tickLimit = 20)
    public void looseAdultsDoNotAuthorizeHarvestAtThePenTarget(TestContext context) {
        Fixture f = setUp(context);
        int target = ShepherdFlock.FLOCK_TARGET;
        List<CowEntity> cows = spawnCows(context, target + 1);
        cows.getLast().setPosition(context.getAbsolutePos(new BlockPos(4, FEET_Y, 3)).toCenterPos());

        try {
            context.assertFalse(harvest(context, f),
                    "uma vaca solta não libera abate com a meta de criação guardada");
            context.assertTrue(cows.stream().allMatch(CowEntity::isAlive), "a meta do curral foi tocada");
        } finally {
            tearDown(context, f, cows);
        }
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "flock_harvest_pens", tickLimit = 20)
    public void villageSurplusKeepsTwoAdultsInTheVictimsPen(TestContext context) {
        Fixture f = setUp(context);
        List<CowEntity> cows = spawnCows(context, ShepherdFlock.FLOCK_TARGET + 2);
        var sheep = context.spawnEntity(EntityType.SHEEP, new BlockPos(6, FEET_Y, 7));
        sheep.setBreedingAge(0);

        try {
            for (int i = 2; i < cows.size() - 1; i++) {
                cows.get(i).setPosition(context.getAbsolutePos(new BlockPos(10, FEET_Y, 5 + i % 3)).toCenterPos());
            }
            cows.getLast().setBreedingAge(-24_000);
            PenFlock flock = PenFlock.read(context.getWorld(), f.colony);
            context.assertTrue(flock.pens().size() == 2
                            && flock.adults(EntityType.COW).size() == ShepherdFlock.FLOCK_TARGET + 1,
                    "o cenário precisa ter excedente de vacas adultas somado entre dois currais");
            context.assertFalse(harvest(context, f), "duas vacas, um filhote e uma ovelha não liberam abate local");
            context.assertTrue(cows.stream().allMatch(CowEntity::isAlive), "a reserva local foi tocada");

            cows.get(2).setPosition(context.getAbsolutePos(new BlockPos(5, FEET_Y, 5)).toCenterPos());
            context.assertTrue(harvest(context, f), "três adultas no primeiro curral liberam o excedente da vila");
            context.assertTrue(cows.subList(0, 3).stream().filter(CowEntity::isAlive).count() == 2,
                    "o primeiro curral precisa manter duas vacas adultas");
            context.assertTrue(cows.subList(3, cows.size()).stream().allMatch(CowEntity::isAlive),
                    "vacas do outro curral e o filhote devem permanecer vivos");
        } finally {
            sheep.discard();
            tearDown(context, f, cows);
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "flock_breed_pens", tickLimit = 20)
    public void pairRequiresTwoAdultsInTheSamePen(TestContext context) {
        Fixture f = setUp(context);
        List<CowEntity> cows = spawnCows(context, 3);
        var first = cows.getFirst();
        var second = cows.get(1);
        var loose = cows.getLast();
        second.setPosition(context.getAbsolutePos(new BlockPos(9, FEET_Y, 5)).toCenterPos());
        loose.setPosition(context.getAbsolutePos(new BlockPos(4, FEET_Y, 3)).toCenterPos());
        var shepherd = context.getWorld().getEntity(f.shepherd);
        shepherd.setPosition(context.getAbsolutePos(new BlockPos(7, FEET_Y, 3)).toCenterPos());

        try {
            context.assertFalse(ShepherdFlock.tend(context.getWorld(), f.colony),
                    "dois currais e um animal solto não formam par");
            context.assertTrue(!first.isInLove() && !second.isInLove() && !loose.isInLove(),
                    "nenhuma vaca deve receber comida entre currais ou solta");
        } finally {
            tearDown(context, f, cows);
        }
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "flock_breed_birth", tickLimit = 20)
    public void looseAdultsDoNotPreventThePennedPairFromBreeding(TestContext context) {
        Fixture f = setUp(context);
        List<CowEntity> cows = spawnCows(context, ShepherdFlock.FLOCK_TARGET + 1);
        var first = cows.getFirst();
        var second = cows.get(1);
        var loose = cows.subList(2, cows.size());
        loose.forEach(cow -> cow.setPosition(context.getAbsolutePos(new BlockPos(4, FEET_Y, 3)).toCenterPos()));
        try {
            PenLog.observe(context.getWorld(), f.colony);
            context.assertTrue(ShepherdFlock.tend(context.getWorld(), f.colony),
                    "o pastor alimenta o par no mesmo curral");
            context.assertTrue(first.isInLove() && second.isInLove() && loose.stream().noneMatch(CowEntity::isInLove),
                    "só o par dentro do curral deve receber comida");
            first.breed(context.getWorld(), second);
            var calves = PenFlock.read(context.getWorld(), f.colony).pens().stream()
                    .flatMap(stock -> stock.animals().stream()).filter(CowEntity.class::isInstance)
                    .filter(animal -> animal.isBaby()).toList();
            context.assertTrue(calves.size() == 1, "o par alimentado deve gerar um filhote no curral");
            var calf = calves.getFirst();
            try {
                calf.damage(context.getWorld().getDamageSources().fall(), 100.0f);
                context.assertTrue(!calf.isAlive(), "a queda deve matar o filhote dentro do curral");
            } finally {
                calf.discard();
            }
        } finally {
            tearDown(context, f, cows);
        }
        context.complete();
    }

    /** Na reserva (FLOCK_TARGET): nada é abatido. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "flock_harvest_reserve", tickLimit = 20)
    public void theLiveReserveIsNeverButchered(TestContext context) {
        Fixture f = setUp(context);
        List<CowEntity> cows = spawnCows(context, ShepherdFlock.FLOCK_TARGET);

        try {
            boolean butchered = harvest(context, f);

            context.assertFalse(butchered, "abateu abaixo/na reserva viva");
            long alive = cows.stream().filter(c -> c.isAlive()).count();
            context.assertTrue(alive == ShepherdFlock.FLOCK_TARGET, "a reserva viva foi tocada: " + alive);
        } finally {
            tearDown(context, f, cows);
        }

        context.complete();
    }

    /** O pastor precisa alcançar o animal antes do abate. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "flock_harvest_distance", tickLimit = 20)
    public void distantShepherdDoesNotButcherCattle(TestContext context) {
        Fixture f = setUp(context);
        List<CowEntity> cows = spawnCows(context, ShepherdFlock.FLOCK_TARGET + 1);
        context.getWorld().getEntity(f.shepherd).setPosition(context.getAbsolutePos(new BlockPos(11, FEET_Y, 11)).toCenterPos());

        try {
            context.assertFalse(harvest(context, f),
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
        List<CowEntity> cows = spawnCows(context, ShepherdFlock.FLOCK_TARGET + 1);
        VillagerEntity shepherd = (VillagerEntity) context.getWorld().getEntity(f.shepherd);
        cows.forEach(cow -> cow.attachLeash(shepherd, true));

        try {
            context.assertFalse(harvest(context, f),
                    "Regra 299: o pastor abateu um animal que levava na corda");
            context.assertTrue(cows.stream().allMatch(CowEntity::isAlive), "Regra 299: um animal da corda morreu");
        } finally {
            tearDown(context, f, cows);
        }

        context.complete();
    }

    private boolean harvest(TestContext context, Fixture f) {
        var world = context.getWorld();
        return FlockHarvest.step(world, f.colony, PenFlock.read(world, f.colony), f.chests,
                Map.of(EntityType.COW, ShepherdFlock.FLOCK_TARGET));
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
