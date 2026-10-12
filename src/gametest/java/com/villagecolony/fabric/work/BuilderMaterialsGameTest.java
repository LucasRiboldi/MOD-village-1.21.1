package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.construction.model.MaterialRequest;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskType;
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
 * O ingrediente sem rota aparece no baú do artesão depois de quatro despachos
 * de ciclo — Regra 40, ADR-040. Ingrediente de drop (linha,
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
     * <b>A linha não consome quatro despachos</b> — 2026-09-30. Ela é
     * ingrediente de drop ({@code DropIngredients}): tem rota sempre, e
     * aparece no baú quando o carpinteiro fabrica o tear. O caminho dos quatro
     * despachos, que é o da peça sem rota, não conjura nada para o tear.
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
                    "a linha foi conjurada antes da quarta espera");

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

    /**
     * Calcita tem rota física de mineração antes do abastecimento terminal
     * (ADR-022). Folha não entra: a regra do lenhador é não colhê-la.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "builder_materials")
    public void calciteKeepsAMiningRouteBeforeFallback(TestContext context) {
        Setup calcite = setUp(context, context.getAbsolutePos(new BlockPos(2, 2, 2)), Items.CALCITE);

        try {
            assertMiningRouteWithoutFallback(context, calcite, Items.CALCITE);
        } finally {
            calcite.cleanUp();
        }

        context.complete();
    }

    /** Longe das praias das outras arenas: sem areia, o vidro aparece para a vidraça. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "builder_materials")
    public void theGlassForAPaneAppearsInTheSmelterChestWithoutABeach(TestContext context) {
        BlockPos far = context.getAbsolutePos(new BlockPos(2, 2, 2)).add(-3_000, 0, 3_000);
        Setup setup = setUp(context, far, Items.GLASS_PANE);
        // A vidraça é do fundidor desde a divisão por material (CraftFamily, 2026-10-11).
        VillageColonyMod.WORKERS.find(setup.carpenter()).orElseThrow().assign(ProfessionType.SMELTER);

        try {
            for (int attempt = 1; attempt <= 4; attempt++) {
                BuilderMaterials.hasOrStocksConstructionMaterial(
                        context.getWorld(), setup.project(), Items.GLASS_PANE);
            }

            for (int dispatch = 1; dispatch <= com.villagecolony.fabric.integration.BiomeConstructionSupply.DISPATCHES_BEFORE_STOCKING; dispatch++) {
                ConstructionSupplyDispatches.recordCycleDispatch(
                        setup.project().id(), Items.GLASS_PANE, dispatch);
            }

            BuilderMaterials.hasOrStocksConstructionMaterial(
                    context.getWorld(), setup.project(), Items.GLASS_PANE);

            context.assertTrue(count(context, setup.carpenterChest(), Items.GLASS) == 6,
                    "o vidro da vidraça não apareceu no baú do fundidor: "
                            + count(context, setup.carpenterChest(), Items.GLASS));
            context.assertTrue(count(context, setup.carpenterChest(), Items.SAND) == 0,
                    "a areia, que é natural, apareceu do nada");
        } finally {
            setup.cleanUp();
        }

        context.complete();
    }

    /**
     * Rota no papel não basta: se a profissão não entregou depois de quatro
     * esperas, a peça que trava a obra aparece no baú do construtor.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "builder_materials")
    public void aProfessionRouteThatDoesNotDeliverFallsBackToTheBuilderChest(TestContext context) {
        Setup setup = setUp(context, context.getAbsolutePos(new BlockPos(2, 2, 2)), Items.OAK_PLANKS);

        try {
            context.assertTrue(BiomeConstructionSupply.hasRouteInBiome(
                            context.getWorld(), setup.project().colonyId(), Items.OAK_PLANKS),
                    "a tábua deveria ter rota local para testar a rota só no papel");

            for (int attempt = 1; attempt <= 3; attempt++) {
                context.assertFalse(BuilderMaterials.hasMaterialForNextBlock(context.getWorld(), setup.project()),
                        "a rota profissional foi contornada antes da quarta espera: " + attempt);

                MaterialRequest request = MaterialRequests.of(setup.project().id()).orElse(null);
                context.assertTrue(request != null && request.state() == MaterialRequest.State.RESOLVING
                                && request.source() == MaterialRequest.Source.PROFESSION,
                        "a falta deveria continuar priorizando a profissão antes da quarta espera: " + request);
            }

            // Consultas não gastam tentativa: só o despacho do ciclo conta — ADR-040 §2.
            for (int dispatch = 1; dispatch <= com.villagecolony.fabric.integration.BiomeConstructionSupply.DISPATCHES_BEFORE_STOCKING; dispatch++) {
                ConstructionSupplyDispatches.recordCycleDispatch(setup.project().id(), Items.OAK_PLANKS, dispatch);
            }

            context.assertTrue(BuilderMaterials.hasMaterialForNextBlock(context.getWorld(), setup.project()),
                    "o quarto despacho da rota só no papel não abasteceu o construtor");
            context.assertTrue(count(context, setup.builderChest(), Items.OAK_PLANKS) == 1,
                    "a peça faltante não apareceu no baú do construtor");

            MaterialRequest fourth = MaterialRequests.of(setup.project().id()).orElse(null);
            context.assertTrue(fourth != null && fourth.state() == MaterialRequest.State.DELIVERED
                            && fourth.source() == MaterialRequest.Source.STOCKED,
                    "o quarto despacho deveria ficar registrado como estoque de resgate: " + fourth);
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
            for (int dispatch = 1; dispatch <= 3; dispatch++) {
                ConstructionSupplyDispatches.recordCycleDispatch(
                        setup.project().id(), Items.OAK_PLANKS, dispatch);
            }

            Inventory carpenterChest = (Inventory) context.getWorld().getBlockEntity(
                    MinecraftTypeAdapter.toBlockPos(setup.carpenterChest()));
            carpenterChest.setStack(0, new ItemStack(Items.OAK_PLANKS, 2));

            boolean has = BuilderMaterials.hasMaterialForNextBlock(context.getWorld(), setup.project());
            MaterialRequest request = MaterialRequests.of(setup.project().id()).orElse(null);

            context.assertTrue(has && request != null
                            && request.state() == MaterialRequest.State.DELIVERED
                            && request.source() == MaterialRequest.Source.CHEST,
                    "a tábua no baú devia ficar registrada como entregue pelo baú: " + request);

            ConstructionSupplyDispatches.recordCycleDispatch(setup.project().id(), Items.OAK_PLANKS, 4);
            context.assertFalse(ConstructionSupplyDispatches.fallbackDue(setup.project().id(), Items.OAK_PLANKS),
                    "a entrega física no baú não reiniciou os despachos da obra");
        } finally {
            setup.cleanUp();
        }

        context.complete();
    }

    /**
     * Vidraça sem praia e sem britador — P2d, 2026-10-11 (substitui a regra de
     * ADR-036 item 6 para a vidraça): o vidro não tem rota, a janela vira cerca
     * de madeira, que o carpinteiro faz. O pedido segue pela profissão do começo
     * ao fim, e o vidro não aparece no baú.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "builder_materials")
    public void aPaneWithoutABeachAsksTheCarpenterForAFence(TestContext context) {
        BlockPos far = context.getAbsolutePos(new BlockPos(2, 2, 2)).add(-3_000, 0, -3_000);
        Setup setup = setUp(context, far, Items.GLASS_PANE);

        try {
            BuilderMaterials.hasMaterialForNextBlock(context.getWorld(), setup.project());
            MaterialRequest first = MaterialRequests.of(setup.project().id()).orElse(null);

            BuilderMaterials.hasMaterialForNextBlock(context.getWorld(), setup.project());
            BuilderMaterials.hasMaterialForNextBlock(context.getWorld(), setup.project());
            BuilderMaterials.hasMaterialForNextBlock(context.getWorld(), setup.project());

            for (int attempt = 1; attempt <= 4; attempt++) {
                ConstructionSupplyDispatches.recordCycleDispatch(
                        setup.project().id(), Items.GLASS_PANE, attempt);
            }

            BuilderMaterials.hasMaterialForNextBlock(context.getWorld(), setup.project());
            MaterialRequest fourth = MaterialRequests.of(setup.project().id()).orElse(null);

            context.assertTrue(first != null && first.state() == MaterialRequest.State.RESOLVING
                            && first.source() == MaterialRequest.Source.PROFESSION,
                    "sem rota de vidro a janela devia pedir a cerca ao carpinteiro: " + first);
            context.assertTrue(fourth != null && fourth.state() == MaterialRequest.State.RESOLVING
                            && fourth.source() == MaterialRequest.Source.PROFESSION,
                    "as quatro tentativas de vidraça não podem criá-la do nada: " + fourth);
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

            // Playtest 0.3.31: o que a vila tem é levado, e não criado de novo.
            context.assertTrue(count(context, setup.otherChest(), Items.CLAY_BALL) == 0,
                    "o clay ball surgiu no baú do fundidor em vez de vir do outro baú da vila");
        } finally {
            setup.cleanUp();
        }

        context.complete();
    }

    /** Consultar o baú não equivale a abrir quatro trabalhos na colônia. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "builder_materials")
    public void onlyCycleDispatchesSpendConstructionFallbackAttempts(TestContext context) {
        Setup setup = setUp(context, context.getAbsolutePos(new BlockPos(2, 2, 2)), Items.WHITE_TERRACOTTA);

        try {
            for (int query = 1; query <= 4; query++) {
                context.assertFalse(BuilderMaterials.hasMaterialForNextBlock(context.getWorld(), setup.project()),
                        "a consulta " + query + " do construtor gastou uma tentativa de despacho");
            }

            context.assertTrue(count(context, setup.builderChest(), Items.WHITE_TERRACOTTA) == 0,
                    "a terracota apareceu antes dos despachos do ciclo");

            for (int dispatch = 1; dispatch <= com.villagecolony.fabric.integration.BiomeConstructionSupply.DISPATCHES_BEFORE_STOCKING; dispatch++) {
                ConstructionSupplyDispatches.recordCycleDispatch(
                        setup.project().id(), Items.WHITE_TERRACOTTA, dispatch);
            }

            context.assertTrue(BuilderMaterials.hasMaterialForNextBlock(context.getWorld(), setup.project()),
                    "os despachos distintos não liberaram a contingência terminal");
            context.assertTrue(count(context, setup.builderChest(), Items.WHITE_TERRACOTTA) == 1,
                    "a contingência terminal não entrou no baú do construtor");
        } finally {
            setup.cleanUp();
        }

        context.complete();
    }

    /** Rota sem caminho nenhum conta todo ciclo: nenhuma tarefa vai nascer para ela (Regra 51). */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "builder_materials")
    public void aPieceWithNoRouteAtAllCountsEveryCycle(TestContext context) {
        Setup setup = setUp(context, context.getAbsolutePos(new BlockPos(2, 2, 2)), Items.FLETCHING_TABLE);

        setup.project().moveTo(ConstructionState.PREPARING);
        setup.project().moveTo(ConstructionState.BUILDING);
        setup.project().moveTo(ConstructionState.WAITING_RESOURCES);
        VillageColonyMod.CONSTRUCTIONS.register(setup.project());
        ConstructionSupplyDispatches.renewForCycle(context.getWorld(), setup.colony());

        context.waitAndRun(1, () -> {
            try {
                ConstructionSupplyDispatches.renewForCycle(context.getWorld(), setup.colony());

                context.assertTrue(ConstructionSupplyDispatches.fallbackDue(
                                setup.project().id(), Items.FLETCHING_TABLE),
                        "a peça sem rota nenhuma não contou os ciclos da obra");
            } finally {
                setup.cleanUp();
            }

            context.complete();
        });
    }

    /**
     * Uma rota explicada pelo livro de receitas ainda nao e trabalho. Quatro
     * leituras do ciclo sem uma tarefa da cadeia nao podem materializar a
     * peca; a contagem comeca quando a colonia efetivamente abriu o pedido.
     * Regra 40, ADR-040.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "builder_materials")
    public void dispatchCountsOnlyAfterTheSupplyChainHasAnOpenTask(TestContext context) {
        Setup setup = setUp(context, context.getAbsolutePos(new BlockPos(2, 2, 2)), Items.OAK_PLANKS);

        try {
            setup.project().moveTo(ConstructionState.PREPARING);
            setup.project().moveTo(ConstructionState.BUILDING);
            setup.project().moveTo(ConstructionState.WAITING_RESOURCES);
            VillageColonyMod.CONSTRUCTIONS.register(setup.project());

            for (int cycle = 1; cycle <= 4; cycle++) {
                ConstructionSupplyDispatches.renewForCycle(context.getWorld(), setup.colony());
            }

            context.assertFalse(ConstructionSupplyDispatches.fallbackDue(
                            setup.project().id(), Items.OAK_PLANKS),
                    "a rota sem tarefa aberta gastou os despachos da obra");

            VillageColonyMod.TASKS.create(setup.colony().id(), TaskType.CRAFT_WOOD_MATERIAL,
                    TaskPriority.PRODUCTION, ResourceType.OAK_PLANKS, 1);

            ConstructionSupplyDispatches.renewForCycle(context.getWorld(), setup.colony());
            long tick = context.getWorld().getTime();
            for (int cycle = 1; cycle < com.villagecolony.fabric.integration.BiomeConstructionSupply.DISPATCHES_BEFORE_STOCKING; cycle++) {
                ConstructionSupplyDispatches.recordCycleDispatch(
                        setup.project().id(), Items.OAK_PLANKS, tick + cycle);
            }

            context.assertTrue(ConstructionSupplyDispatches.fallbackDue(
                            setup.project().id(), Items.OAK_PLANKS),
                    "quatro ciclos com trabalho aberto nao registraram o despacho");
        } finally {
            VillageColonyMod.CONSTRUCTIONS.removeOfColony(setup.colony().id());
            VillageColonyMod.TASKS.clear();
            setup.cleanUp();
        }

        context.complete();
    }

    private record Setup(Colony colony, UUID builder, UUID carpenter, ColonyPos builderChest,
            ColonyPos carpenterChest, ConstructionProject project, Item piece) {

        void cleanUp() {
            ConstructionSupplyDispatches.delivered(project.id(), piece);
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

    private static void assertMiningRouteWithoutFallback(TestContext context, Setup setup, Item item) {
        SupplyRoute route = BiomeConstructionSupply.routeInBiome(
                context.getWorld(), setup.project().colonyId(), item);
        context.assertTrue(route.available() && route.source() == SupplySource.BIOME_RESOURCE,
                item + " devia ter rota de mineracao: " + route);

        for (int attempt = 1; attempt <= 4; attempt++) {
            context.assertFalse(BuilderMaterials.hasOrStocksConstructionMaterial(
                            context.getWorld(), setup.project(), item),
                    item + " nao devia aparecer no bau na tentativa " + attempt);
        }

        context.assertTrue(count(context, setup.builderChest(), item) == 0,
                item + " apareceu instantaneamente apesar de existir rota fisica");
    }
}
