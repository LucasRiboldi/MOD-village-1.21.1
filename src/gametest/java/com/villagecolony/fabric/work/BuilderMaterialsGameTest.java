package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.BiomeConstructionSupply;
import com.villagecolony.fabric.integration.ColonyChests;
import com.villagecolony.fabric.integration.SandNearWater;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

/**
 * O ingrediente sem rota aparece no baú do artesão depois de três tentativas
 * de recolher — decisão do autor, 2026-09-30. Ingrediente de drop (linha,
 * corante) não espera: tem rota sempre (DropIngredients).
 *
 * <p>No playtest das 02:45 a casa do pastor esperou 7 min 45 s pelo tear (a
 * linha não tem fonte na colônia) e 4 min 17 s pela vidraça. A peça pronta
 * não aparece: aparece o que falta para o artesão fazê-la.
 */
public final class BuilderMaterialsGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "builder_materials")
    public void theBuilderTakesTheNeededBlockFromAnotherVillageChest(TestContext context) {
        Setup setup = setUp(context, context.getAbsolutePos(new BlockPos(2, 2, 2)), Items.OAK_PLANKS);

        try {
            Inventory carpenterChest = (Inventory) context.getWorld().getBlockEntity(
                    MinecraftTypeAdapter.toBlockPos(setup.carpenterChest()));
            carpenterChest.setStack(0, new ItemStack(Items.OAK_PLANKS, 2));

            context.assertTrue(BuilderMaterials.takeMaterial(
                            context.getWorld(), setup.project(), Blocks.OAK_PLANKS)
                            .filter(Items.OAK_PLANKS::equals).isPresent(),
                    "o construtor não retirou a tábua do baú remoto da vila");
            context.assertTrue(count(context, setup.carpenterChest(), Items.OAK_PLANKS) == 1,
                    "a retirada não consumiu exatamente uma tábua do baú remoto");
            context.assertTrue(count(context, setup.builderChest(), Items.OAK_PLANKS) == 0,
                    "a tábua apareceu no baú do construtor em vez de ser entregue fisicamente");
        } finally {
            setup.cleanUp();
        }

        context.complete();
    }

    /**
     * <b>A linha deixou de esperar três tentativas</b> — 2026-09-30. Ela é
     * ingrediente de drop ({@code DropIngredients}): tem rota sempre, e
     * aparece no baú quando o carpinteiro fabrica o tear. O caminho das três
     * tentativas, que é o da peça sem rota, não conjura nada para o tear.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "builder_materials")
    public void theStringOfALoomHasARouteAndIsNotStockedByTheAttempts(TestContext context) {
        Setup setup = setUp(context, context.getAbsolutePos(new BlockPos(2, 2, 2)), Items.LOOM);

        try {
            context.assertTrue(BiomeConstructionSupply.hasRouteInBiome(
                            context.getWorld(), setup.project().colonyId(), Items.STRING),
                    "a linha devia ter rota: ela aparece sozinha");

            for (int attempt = 1; attempt <= 3; attempt++) {
                BuilderMaterials.hasOrStocksConstructionMaterial(
                        context.getWorld(), setup.project(), Items.LOOM);
            }

            context.assertTrue(count(context, setup.carpenterChest(), Items.STRING) == 0,
                    "a linha foi conjurada pelo caminho das tres tentativas");

            context.assertTrue(count(context, setup.builderChest(), Items.LOOM) == 0
                            && count(context, setup.carpenterChest(), Items.LOOM) == 0,
                    "o tear apareceu pronto; quem o faz e o carpinteiro");
        } finally {
            setup.cleanUp();
        }

        context.complete();
    }

    /** Longe das praias das outras arenas: sem areia, o vidro aparece para a vidraça. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "builder_materials")
    public void theGlassForAPaneAppearsInTheCarpenterChestWithoutABeach(TestContext context) {
        BlockPos far = context.getAbsolutePos(new BlockPos(2, 2, 2)).add(-3_000, 0, 3_000);
        Setup setup = setUp(context, far, Items.GLASS_PANE);

        try {
            for (int attempt = 1; attempt <= 3; attempt++) {
                BuilderMaterials.hasOrStocksConstructionMaterial(
                        context.getWorld(), setup.project(), Items.GLASS_PANE);
            }

            context.assertTrue(count(context, setup.carpenterChest(), Items.GLASS) == 6,
                    "o vidro da vidraça não apareceu no baú do carpinteiro: "
                            + count(context, setup.carpenterChest(), Items.GLASS));
            context.assertTrue(count(context, setup.carpenterChest(), Items.SAND) == 0,
                    "a areia, que é natural, apareceu do nada");
        } finally {
            setup.cleanUp();
        }

        context.complete();
    }

    private record Setup(Colony colony, UUID builder, UUID carpenter, ColonyPos builderChest,
            ColonyPos carpenterChest, ConstructionProject project, Item piece) {

        void cleanUp() {
            BiomeConstructionSupply.routeDelivered(colony.id(), piece);
            SandNearWater.clearAll();
            VillageColonyMod.STORAGES.remove(builder);
            VillageColonyMod.STORAGES.remove(carpenter);
            VillageColonyMod.WORKERS.remove(builder);
            VillageColonyMod.WORKERS.remove(carpenter);
            VillageColonyMod.COLONIES.remove(colony.id());
        }
    }

    private static Setup setUp(TestContext context, BlockPos at, Item piece) {
        ServerWorld world = context.getWorld();
        BlockPos builderAt = at;
        BlockPos carpenterAt = at.east(3);
        world.setBlockState(builderAt, Blocks.CHEST.getDefaultState());
        world.setBlockState(carpenterAt, Blocks.CHEST.getDefaultState());
        ColonyPos builderChest = MinecraftTypeAdapter.toColonyPos(builderAt);
        ColonyPos carpenterChest = MinecraftTypeAdapter.toColonyPos(carpenterAt);

        Colony colony = Colony.create(UUID.randomUUID(), builderChest);
        UUID builder = UUID.randomUUID();
        UUID carpenter = UUID.randomUUID();

        VillageColonyMod.COLONIES.register(colony);
        VillageColonyMod.WORKERS.register(builder, colony.id()).assign(ProfessionType.BUILDER);
        VillageColonyMod.WORKERS.register(carpenter, colony.id()).assign(ProfessionType.CARPENTER);
        VillageColonyMod.STORAGES.register(WorkerStorage.of(builder, builderChest));
        VillageColonyMod.STORAGES.register(WorkerStorage.of(carpenter, carpenterChest));

        ConstructionProject project = ConstructionProject.plan(colony.id(),
                Blueprint.of(ResourceId.vanilla("village/plains/houses/materials"),
                        List.of(new BlueprintBlock(new ColonyPos(0, 0, 0),
                                MinecraftTypeAdapter.toResourceId(piece)))),
                builderChest);

        return new Setup(colony, builder, carpenter, builderChest, carpenterChest, project, piece);
    }

    private static int count(TestContext context, ColonyPos chest, Item item) {
        return ColonyChests.countIn(context.getWorld(), List.of(chest), item);
    }
}
