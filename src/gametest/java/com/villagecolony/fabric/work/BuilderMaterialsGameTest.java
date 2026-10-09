package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.MaterialRequest;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.BiomeConstructionSupply;
import com.villagecolony.fabric.integration.ColonyChests;
import com.villagecolony.fabric.integration.SandNearWater;
import com.villagecolony.fabric.integration.SupplyRoute;
import com.villagecolony.fabric.integration.SupplySource;
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

    /** O diagnostico de suprimento mostra a cadeia que a obra vai depender. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "builder_materials")
    public void theSupplyRouteExplainsCraftingDependencies(TestContext context) {
        Setup setup = setUp(context, context.getAbsolutePos(new BlockPos(2, 2, 2)), Items.OAK_PLANKS);

        try {
            SupplyRoute route = BiomeConstructionSupply.routeInBiome(
                    context.getWorld(), setup.project().colonyId(), Items.OAK_PLANKS);

            context.assertTrue(route.available(), "tabua de carvalho devia ter rota no bioma");
            context.assertTrue(route.source() == SupplySource.CRAFTING,
                    "a rota devia explicar que a tabua vem de receita: " + route);
            context.assertTrue(route.inputs().stream().anyMatch(input -> input.item().equals(Items.OAK_LOG)
                            && input.source() == SupplySource.BIOME_RESOURCE),
                    "a rota devia mostrar a tora local como dependencia: " + route);
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
            for (int attempt = 1; attempt <= 4; attempt++) {
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

    /** A peça no baú fica registrada como entregue pelo baú — ADR-035 §3. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "builder_materials")
    public void aPieceInTheChestIsRecordedAsDeliveredByTheChest(TestContext context) {
        Setup setup = setUp(context, context.getAbsolutePos(new BlockPos(2, 2, 2)), Items.OAK_PLANKS);

        try {
            Inventory carpenterChest = (Inventory) context.getWorld().getBlockEntity(
                    MinecraftTypeAdapter.toBlockPos(setup.carpenterChest()));
            carpenterChest.setStack(0, new ItemStack(Items.OAK_PLANKS, 2));

            boolean has = BuilderMaterials.hasMaterialForNextBlock(context.getWorld(), setup.project());
            MaterialRequest request = MaterialRequests.of(setup.project().id()).orElse(null);

            context.assertTrue(has && request != null
                            && request.state() == MaterialRequest.State.DELIVERED
                            && request.source() == MaterialRequest.Source.CHEST,
                    "a tábua no baú devia ficar registrada como entregue pelo baú: " + request);
        } finally {
            setup.cleanUp();
        }

        context.complete();
    }

    /**
     * O pedido mostra a sequência real de uma peça sem rota — ADR-035 §3: conta
     * as tentativas e, na quarta (ADR-036 item 6), o vidro vai ao baú do carpinteiro e a
     * vidraça passa a esperar o artesão.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "builder_materials")
    public void aPaneWithoutABeachShowsTheAttemptsAndThenTheCraftsman(TestContext context) {
        BlockPos far = context.getAbsolutePos(new BlockPos(2, 2, 2)).add(-3_000, 0, -3_000);
        Setup setup = setUp(context, far, Items.GLASS_PANE);

        try {
            BuilderMaterials.hasMaterialForNextBlock(context.getWorld(), setup.project());
            MaterialRequest first = MaterialRequests.of(setup.project().id()).orElse(null);

            BuilderMaterials.hasMaterialForNextBlock(context.getWorld(), setup.project());
            BuilderMaterials.hasMaterialForNextBlock(context.getWorld(), setup.project());
            BuilderMaterials.hasMaterialForNextBlock(context.getWorld(), setup.project());
            MaterialRequest fourth = MaterialRequests.of(setup.project().id()).orElse(null);

            context.assertTrue(first != null && first.state() == MaterialRequest.State.RESOLVING
                            && first.source() == MaterialRequest.Source.STOCKED,
                    "na primeira falta a vidraça devia estar contando tentativas: " + first);
            context.assertTrue(fourth != null && fourth.state() == MaterialRequest.State.RESOLVING
                            && fourth.source() == MaterialRequest.Source.CRAFTSMAN,
                    "na quarta a vidraça devia esperar o artesão: " + fourth);
        } finally {
            setup.cleanUp();
        }

        context.complete();
    }

    /** A busca vazia deve abastecer o baú da profissão que procurou o recurso. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "builder_materials")
    public void emptySurfaceSearchStocksTheSmelterChestEvenWhenAnotherChestHasClay(TestContext context) {
        SurfaceSupplySetup setup = setUpSurfaceSupply(context, context.getAbsolutePos(new BlockPos(2, 2, 2)));

        try {
            Inventory otherChest = (Inventory) context.getWorld().getBlockEntity(
                    MinecraftTypeAdapter.toBlockPos(setup.otherChest()));
            otherChest.setStack(0, new ItemStack(Items.CLAY_BALL, 1));

            context.assertTrue(BiomeConstructionSupply.stockAfterEmptySearches(
                            context.getWorld(), setup.colony().id(), setup.colony().center(),
                            Items.CLAY_BALL, 1, ProfessionType.SMELTER),
                    "a busca vazia devia abastecer o baú do fundidor");

            context.assertTrue(count(context, setup.smelterChest(), Items.CLAY_BALL) == 1,
                    "o clay ball já existia na colônia, mas não apareceu no baú do fundidor");
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

    private record SurfaceSupplySetup(Colony colony, UUID smelter, UUID builder,
            ColonyPos smelterChest, ColonyPos otherChest) {

        void cleanUp() {
            VillageColonyMod.STORAGES.remove(smelter);
            VillageColonyMod.STORAGES.remove(builder);
            VillageColonyMod.WORKERS.remove(smelter);
            VillageColonyMod.WORKERS.remove(builder);
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

    private static SurfaceSupplySetup setUpSurfaceSupply(TestContext context, BlockPos at) {
        ServerWorld world = context.getWorld();
        BlockPos smelterAt = at;
        BlockPos otherAt = at.east(3);
        world.setBlockState(smelterAt, Blocks.CHEST.getDefaultState());
        world.setBlockState(otherAt, Blocks.CHEST.getDefaultState());
        ColonyPos smelterChest = MinecraftTypeAdapter.toColonyPos(smelterAt);
        ColonyPos otherChest = MinecraftTypeAdapter.toColonyPos(otherAt);

        Colony colony = Colony.create(UUID.randomUUID(), smelterChest);
        UUID smelter = UUID.randomUUID();
        UUID builder = UUID.randomUUID();

        VillageColonyMod.COLONIES.register(colony);
        VillageColonyMod.WORKERS.register(smelter, colony.id()).assign(ProfessionType.SMELTER);
        VillageColonyMod.WORKERS.register(builder, colony.id()).assign(ProfessionType.BUILDER);
        VillageColonyMod.STORAGES.register(WorkerStorage.of(smelter, smelterChest));
        VillageColonyMod.STORAGES.register(WorkerStorage.of(builder, otherChest));

        return new SurfaceSupplySetup(colony, smelter, builder, smelterChest, otherChest);
    }

    private static int count(TestContext context, ColonyPos chest, Item item) {
        return ColonyChests.countIn(context.getWorld(), List.of(chest), item);
    }
}
