package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.BiomeConstructionSupply;
import com.villagecolony.fabric.integration.ChestWithdrawer;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/**
 * Três buscas vazias põem o material no baú de quem o usa — pedido do autor,
 * 2026-10-03, depois do playtest em que dois fundidores pararam por areia 64
 * vezes numa vila sem areia no alcance.
 */
public final class LocateFallbackGameTest implements FabricGameTest {

    private static final BlockPos SMELTER_CHEST = new BlockPos(2, 2, 2);

    private static final BlockPos BUILDER_CHEST = new BlockPos(5, 2, 2);

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "locate_fallback")
    public void theThirdEmptySearchStocksTheSmeltersSand(TestContext context) {
        ServerWorld world = context.getWorld();
        Scene scene = Scene.of(context);

        try {
            Task task = VillageColonyMod.TASKS.create(scene.colony.id(), TaskType.COLLECT_SURFACE_RESOURCE,
                    TaskPriority.PRODUCTION, ResourceType.SAND, 8);

            context.assertFalse(LocateFallback.afterEmptySearch(world, task, 2),
                    "na segunda busca vazia o aldeão ainda procura");
            context.assertTrue(ChestWithdrawer.countIn(world, scene.smelterChest, Items.SAND) == 0,
                    "areia apareceu antes da terceira busca");

            EmptySweeps.foundNothing(scene.colony.id(), ResourceType.SAND, world.getTime());

            context.assertTrue(LocateFallback.afterEmptySearch(world, task, 3),
                    "na terceira busca vazia a areia devia aparecer");
            context.assertTrue(ChestWithdrawer.countIn(world, scene.smelterChest, Items.SAND) == 8,
                    "a areia devia estar no baú do fundidor, oito como a tarefa pedia; tem "
                            + ChestWithdrawer.countIn(world, scene.smelterChest, Items.SAND));
            context.assertTrue(ChestWithdrawer.countIn(world, scene.builderChest, Items.SAND) == 0,
                    "o construtor não usa areia crua: ela vai para quem a funde");
            context.assertFalse(EmptySweeps.isWaiting(scene.colony.id(), ResourceType.SAND, world.getTime()),
                    "entregue, a contagem recomeça");
        } finally {
            scene.forget();
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "locate_fallback")
    public void whatTheFurnaceDoesNotTakeGoesToTheBuilder(TestContext context) {
        ServerWorld world = context.getWorld();

        context.assertTrue(LocateFallback.userOf(world, Items.SAND) == ProfessionType.SMELTER, "areia vira vidro");
        context.assertTrue(LocateFallback.userOf(world, Items.CLAY_BALL) == ProfessionType.SMELTER,
                "argila vira tijolo");
        context.assertTrue(LocateFallback.userOf(world, Items.DIRT) == ProfessionType.BUILDER,
                "terra é bloco de obra");
        context.complete();
    }

    /**
     * A peça da natureza sem rota no bioma aparece também — revê a decisão de
     * 26-09, que a recusava para sempre.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "locate_fallback")
    public void aNaturalPieceWithNoRouteIsStockedForTheBuild(TestContext context) {
        ServerWorld world = context.getWorld();
        Scene scene = Scene.of(context);

        try {
            context.assertTrue(BiomeConstructionSupply.stockForConstruction(
                            world, scene.colony.id(), scene.builderChest, Items.CACTUS),
                    "a peça da natureza sem rota devia aparecer depois das três tentativas");
            context.assertTrue(ChestWithdrawer.countIn(world, scene.builderChest, Items.CACTUS) == 1,
                    "o cacto devia estar no baú do construtor");
        } finally {
            scene.forget();
        }

        context.complete();
    }

    /**
     * O pastor sem ovelha com lã solta a tarefa, e na terceira busca a lã
     * aparece — playtest de 2026-10-03, meia hora segurando a tarefa calado.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "locate_fallback")
    public void aShepherdWithNoWoollySheepLetsGoAndTheThirdSearchStocksWool(TestContext context) {
        ServerWorld world = context.getWorld();
        Scene scene = Scene.of(context);
        UUID shepherd = UUID.randomUUID();

        try {
            Task task = VillageColonyMod.TASKS.create(scene.colony.id(), TaskType.COLLECT_WOOL,
                    TaskPriority.PRODUCTION, ResourceType.WHITE_WOOL, 4);

            for (int search = 1; search <= 2; search++) {
                task.reserveFor(shepherd);
                EmptyFlock.endSearch(world, task, shepherd, 32);

                context.assertTrue(!task.isHeld() && task.isOpen(),
                        "a busca " + search + " devia soltar a tarefa de volta à fila, e ela está " + task.state());
            }

            context.assertTrue(EmptySweeps.isWaiting(scene.colony.id(), ResourceType.WHITE_WOOL, world.getTime()),
                    "sem ovelha, a lã fica de castigo e ninguém a reserva logo de novo");

            task.reserveFor(shepherd);
            EmptyFlock.endSearch(world, task, shepherd, 32);

            context.assertTrue(ChestWithdrawer.countIn(world, scene.builderChest, Items.WHITE_WOOL) == 4,
                    "na terceira busca vazia as quatro lãs deviam aparecer no baú");
            context.assertFalse(task.isOpen(), "entregue a lã, a tarefa de tosquia acaba");
        } finally {
            EmptySweeps.found(scene.colony.id(), ResourceType.WHITE_WOOL);
            scene.forget();
        }

        context.complete();
    }

    /** Uma colônia com um fundidor e um construtor, cada um com seu baú. */
    private record Scene(Colony colony, UUID smelter, UUID builder, ColonyPos smelterChest, ColonyPos builderChest) {

        static Scene of(TestContext context) {
            context.setBlockState(SMELTER_CHEST, Blocks.CHEST.getDefaultState());
            context.setBlockState(BUILDER_CHEST, Blocks.CHEST.getDefaultState());

            ColonyPos smelterChest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(SMELTER_CHEST));
            ColonyPos builderChest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(BUILDER_CHEST));
            Colony colony = Colony.create(UUID.randomUUID(), smelterChest);
            UUID smelter = UUID.randomUUID();
            UUID builder = UUID.randomUUID();

            VillageColonyMod.COLONIES.register(colony);
            VillageColonyMod.WORKERS.register(smelter, colony.id()).assign(ProfessionType.SMELTER);
            VillageColonyMod.WORKERS.register(builder, colony.id()).assign(ProfessionType.BUILDER);
            VillageColonyMod.STORAGES.register(WorkerStorage.of(smelter, smelterChest));
            VillageColonyMod.STORAGES.register(WorkerStorage.of(builder, builderChest));

            return new Scene(colony, smelter, builder, smelterChest, builderChest);
        }

        void forget() {
            VillageColonyMod.TASKS.ofColony(colony.id()).forEach(task -> {
                if (task.isOpen()) {
                    task.cancel();
                }
            });
            VillageColonyMod.STORAGES.remove(smelter);
            VillageColonyMod.STORAGES.remove(builder);
            VillageColonyMod.WORKERS.remove(smelter);
            VillageColonyMod.WORKERS.remove(builder);
            VillageColonyMod.COLONIES.remove(colony.id());
            EmptySweeps.found(colony.id(), ResourceType.SAND);
        }
    }
}
