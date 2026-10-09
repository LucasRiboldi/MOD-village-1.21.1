package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageInventory;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.StructureBlueprintReader;
import com.villagecolony.fabric.integration.BuildSiteScanner;
import com.villagecolony.fabric.integration.SweepPersistence;
import com.villagecolony.fabric.integration.VillageInventoryObserver;
import com.villagecolony.fabric.integration.VillageStructures;
import com.villagecolony.fabric.work.ConstructionDemand;
import com.villagecolony.fabric.work.ColonyFarms;
import com.villagecolony.fabric.work.ConstructionPlanner;
import com.villagecolony.fabric.work.FarmPlans;
import com.villagecolony.fabric.work.FarmerWork;
import com.villagecolony.fabric.work.HousePlans;
import com.villagecolony.fabric.work.PlanPlacement;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.test.GameTest;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.test.TestContext;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A roça que a colônia levanta é a do jogo, e ela sai vazia.
 *
 * <p>Decisão do autor, 2026-09-05: <i>"precisam construir o espaço de
 * plantação padrão e idêntico aos que já vêm na vila do Minecraft"</i>.
 *
 * <p>Até essa data quem abria roça era o fazendeiro, arando toda terra
 * hidratada que achasse: a sessão das 20:30 deixou <b>trinta e quatro
 * blocos arados em trinta e três posições distintas</b>, espalhados por
 * catorze blocos de vila.
 */
public class FarmPlanGameTest implements FabricGameTest {

    /** A gente dos cenários de rodízio: vinte camas e vinte aldeões. */
    private static final int POPULATION = 20;

    /**
     * O raio do chão que o cenário do impasse calça — folgado o
     * bastante para a varredura achar lote, e pequeno para caber na
     * arena vazia.
     */
    private static final int SCAN_RADIUS = 16;

    /** A roça de planície, lida do catálogo do jogo. */
    private static Optional<Blueprint> plainsFarm(TestContext context) {
        List<ResourceId> farms = VillageStructures.farmsFor("plains");

        return farms.isEmpty()
                ? Optional.empty()
                : StructureBlueprintReader.read(context.getWorld(), farms.get(0));
    }

    /**
     * A planta é mesmo uma roça: ela traz terra arada.
     *
     * <p>É a afirmação de que o catálogo entregou o que se pediu, e não
     * uma casa cujo nome por acaso contém {@code farm}.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "farm_plan")
    public void theVillageFarmPlanCarriesFarmland(TestContext context) {
        Blueprint farm = plainsFarm(context).orElse(null);

        context.assertTrue(farm != null, "o catálogo não devolveu roça de planície nenhuma");

        ResourceId farmland = MinecraftTypeAdapter.toResourceId(Blocks.FARMLAND);

        context.assertTrue(
                farm.blocks().stream().anyMatch(block -> block.block().equals(farmland)),
                "a planta da roça não tem um canteiro sequer");

        context.complete();
    }

    /**
     * <b>E o canteiro nasce vazio</b> — a obra faz o espaço, o fazendeiro
     * planta.
     *
     * <p>É a divisão que o autor descreveu, e ela também evita a obra
     * parada: cobrar trigo, cenoura, batata e beterraba do baú faria a
     * roça esperar semente que a colônia talvez não tenha, que é o
     * defeito do vão do teto de 09-04 noutro lugar.
     *
     * <p>A planta do jogo <b>tem</b> lavoura — este teste só vale porque
     * o {@code FarmPlans} a tira. Sem essa retirada ele reprova.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "farm_plan")
    public void thePlantedCropsAreLeftToTheFarmer(TestContext context) {
        Blueprint raw = plainsFarm(context).orElse(null);

        context.assertTrue(raw != null, "o catálogo não devolveu roça de planície nenhuma");

        context.assertTrue(
                raw.blocks().stream().anyMatch(FarmPlanGameTest::isCrop),
                "a roça do jogo veio sem lavoura, e aí este teste não mede nada");

        Colony colony = Colony.create(
                UUID.randomUUID(),
                MinecraftTypeAdapter.toColonyPos(
                        context.getAbsolutePos(new BlockPos(1, 2, 1))));

        List<Blueprint> plans = FarmPlans.plansFor(context.getWorld(), colony);

        context.assertTrue(!plans.isEmpty(), "a colônia não recebeu planta de roça nenhuma");

        for (Blueprint plan : plans) {
            context.assertTrue(
                    plan.blocks().stream().noneMatch(FarmPlanGameTest::isCrop),
                    "a planta da roça saiu com lavoura plantada, e a obra vai esperar semente");
        }

        context.complete();
    }

    /**
     * A roça fica acima da rua — pedido do autor, 2026-10-02: ela precisa da
     * altura para receber a água. A planta do jogo tem camada da rua; a da
     * colônia não, e a lavoura assenta um acima do chão.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "farm_plan")
    public void theFarmSitsAboveTheStreet(TestContext context) {
        Blueprint raw = plainsFarm(context).orElse(null);

        context.assertTrue(raw != null && raw.hasStreetLayer(),
                "a roça do jogo veio sem camada da rua, e aí este teste não mede nada");

        Colony colony = Colony.create(
                UUID.randomUUID(),
                MinecraftTypeAdapter.toColonyPos(
                        context.getAbsolutePos(new BlockPos(1, 2, 1))));

        ColonyPos floor = new ColonyPos(10, 70, 10);

        for (Blueprint plan : FarmPlans.plansFor(context.getWorld(), colony)) {
            context.assertFalse(plan.hasStreetLayer(), plan.id() + " ficou na altura da rua");
            context.assertTrue(plan.originFor(floor).equals(floor),
                    plan.id() + ": a lavoura devia assentar um acima do chão, no piso do lote");
        }

        context.complete();
    }

    /**
     * A roça relida do save é a mesma que foi planejada — playtest de
     * 2026-10-07: retomada, ela voltava com o trigo (91 peças contra 64) e com
     * a camada da rua, a trava media a fundação no ar e nenhum construtor a
     * reservava. Reparo e peças da colônia releem pelo mesmo caminho.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "farm_plan")
    public void aReReadFarmIsTheFarmThatWasPlanned(TestContext context) {
        Colony colony = Colony.create(
                UUID.randomUUID(),
                MinecraftTypeAdapter.toColonyPos(
                        context.getAbsolutePos(new BlockPos(1, 2, 1))));

        List<Blueprint> plans = FarmPlans.plansFor(context.getWorld(), colony);

        context.assertTrue(!plans.isEmpty(), "a colônia não recebeu planta de roça nenhuma");

        for (Blueprint planned : plans) {
            Blueprint reRead = PlanPlacement.blueprintOf(
                    context.getWorld(), colony.id(), planned.id(),
                    MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(1, 2, 1))))
                    .orElseThrow(() -> new AssertionError(planned.id() + " não foi relida"));

            context.assertTrue(reRead.blockCount() == planned.blockCount(),
                    planned.id() + " relida tem " + reRead.blockCount() + " peças, planejada "
                            + planned.blockCount());
            context.assertFalse(reRead.hasStreetLayer(),
                    planned.id() + " relida voltou com a camada da rua");
            context.assertTrue(reRead.blocks().stream().noneMatch(FarmPlanGameTest::isCrop),
                    planned.id() + " relida voltou com lavoura");
        }

        context.complete();
    }

    /**
     * <b>A roça nasce dentro da vila, e não na ponta da estrada</b> —
     * 2026-09-05, visto em jogo.
     *
     * <p>A primeira roça da colônia nasceu em {@code x=1517, z=113} com o
     * centro em {@code x=1435, z=47}: <b>105 blocos</b>, porque o lote
     * veio da ponta da estrada que a vila estava esticando. O fazendeiro
     * procura lavoura a {@code FarmerWork.reach()} do centro, então ele
     * nunca a viu — e a linha logo depois de ela ficar pronta era
     * exatamente {@code no empty plot within 32 blocks of the village}.
     *
     * <p><b>E o pedido de roça nunca se fechava:</b> a colônia levantou
     * duas em quatro minutos, a caminho de encher o mapa.
     *
     * <p>O autor pediu <i>"um espaço livre <b>dentro da vila</b>"</i>, e é
     * essa a conta que esta guarda faz.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "farm_plan")
    public void theFarmLotStaysWithinTheFarmersReach(TestContext context) {
        Colony colony = Colony.create(
                UUID.randomUUID(), new ColonyPos(0, 64, 0));

        int reach = FarmerWork.reach();

        ColonyPos size = new ColonyPos(5, 3, 5);

        BuildSiteScanner.Site near = new BuildSiteScanner.Site(
                new ColonyPos(reach, 64, 0),
                Direction.NORTH,
                size);

        BuildSiteScanner.Site far = new BuildSiteScanner.Site(
                new ColonyPos(reach + 1, 64, 0),
                Direction.NORTH,
                size);

        context.assertTrue(
                ConstructionDemand.withinTheFarmersReach(colony, near),
                "o lote na borda do alcance do fazendeiro foi recusado");

        context.assertFalse(
                ConstructionDemand.withinTheFarmersReach(colony, far),
                "um lote fora do alcance do fazendeiro passou — é a roça de 105 blocos"
                        + " que ninguém planta, de volta");

        context.complete();
    }

    /**
     * <b>Uma roça por fazendeiro</b> — Regra 52 (autor, 2026-10-08): <i>"cada
     * fazendeiro deve ter uma fazenda"</i>. Era uma a cada vinte aldeões, e a
     * conta não sabia quantos fazendeiros havia.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "farm_quota")
    public void oneFarmForEveryFarmer(TestContext context) {
        Colony colony = Colony.create(UUID.randomUUID(), new ColonyPos(0, 64, 0));

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);

        try {
            for (int villager = 0; villager < POPULATION; villager++) {
                UUID id = UUID.randomUUID();

                VillageColonyMod.WORKERS.register(id, colony.id());
                owned.owning(id);
            }

            context.assertFalse(ColonyFarms.owedToTheFarmers(context.getWorld(), colony),
                    "vinte aldeões sem fazendeiro pediram roça");

            UUID first = UUID.randomUUID();
            VillageColonyMod.WORKERS.register(first, colony.id()).assign(ProfessionType.FARMER);
            owned.owning(first);

            context.assertTrue(ColonyFarms.owedToTheFarmers(context.getWorld(), colony),
                    "um fazendeiro sem roça, e a colônia não pediu a dele");

            VillageColonyMod.BUILDINGS.register(new Building(UUID.randomUUID(), colony.id(),
                    ResourceId.vanilla("village/plains/houses/plains_small_farm_1"),
                    new ColonyPos(10, 64, 10), new ColonyPos(18, 66, 22)));

            context.assertFalse(ColonyFarms.owedToTheFarmers(context.getWorld(), colony),
                    "o fazendeiro tem roça e a colônia pediu outra");

            UUID second = UUID.randomUUID();
            VillageColonyMod.WORKERS.register(second, colony.id()).assign(ProfessionType.FARMER);
            owned.owning(second);

            context.assertTrue(ColonyFarms.owedToTheFarmers(context.getWorld(), colony),
                    "dois fazendeiros e uma roça, e a colônia não pediu a segunda");
        } finally {
            VillageColonyMod.BUILDINGS.removeOfColony(colony.id());
            owned.cleanUp();
        }

        context.complete();
    }

    /** A obra seguinte a uma casa deve ser uma infraestrutura — 2026-09-20. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "construction_rotation")
    public void theNextTurnAfterAHouseIsNonResidential(TestContext context) {
        BlockPos center = new BlockPos(16, 1, 16);

        // Chão liso com rua no meio: é o que a varredura procura.
        for (int dx = -SCAN_RADIUS; dx <= SCAN_RADIUS; dx++) {
            for (int dz = -SCAN_RADIUS; dz <= SCAN_RADIUS; dz++) {
                context.setBlockState(
                        center.add(dx, 0, dz), Blocks.GRASS_BLOCK.getDefaultState());
            }
        }

        context.setBlockState(center, Blocks.DIRT_PATH.getDefaultState());

        UUID colonyId = UUID.randomUUID();
        BlockPos absoluteRoad = context.getAbsolutePos(center);
        SweepPersistence.restore(new ColonyRoads(
                colonyId,
                MinecraftTypeAdapter.toColonyPos(absoluteRoad),
                List.of(ColonyRoads.column(absoluteRoad.getX(), absoluteRoad.getZ()))));

        Colony colony = Colony.create(
                colonyId,
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(center)));

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);

        try {
            // Um deles constrói, senão o planejador para antes em
            // NO_WORKER e o teste mediria outra coisa.
            UUID builderId = UUID.randomUUID();

            VillageColonyMod.WORKERS.register(builderId, colony.id())
                    .assign(ProfessionType.BUILDER);

            owned.owning(builderId);

            for (int villager = 1; villager < POPULATION; villager++) {
                UUID id = UUID.randomUUID();

                // Um fazendeiro sem roça: a roça só entra no rodízio por ele — Regra 52.
                var worker = VillageColonyMod.WORKERS.register(id, colony.id());
                if (villager == 1) {
                    worker.assign(ProfessionType.FARMER);
                }
                owned.owning(id);
            }

            // O cenário mede o rodízio, não uma vila sem capacidade de
            // moradia. A observação completa devolve a condição equivalente
            // a vinte camas que o detector já confirmou no mundo.
            colony.observe(colony.center(), POPULATION, true);

            // A vila já ergueu a primeira casa, e ela está <b>de pé no
            // mundo</b> — 2026-09-22.
            //
            // Registrar só a caixa não serve mais desde o reparo cíclico
            // (P0.10): ConstructionPlanner.plan roda
            // BuildingRepairPlanner.open ANTES da alternância, e o reparo
            // compara a planta com o mundo. Uma casa que existe apenas no
            // registro tem zero bloco de pé contra os 151 da planta, então
            // o reparo a adotava e devolvia a própria casa — a obra que
            // esta asserção lia como "outra casa". O cenário nunca chegava
            // a medir o rodízio.
            //
            // Assentando a planta, o reparo encontra a casa inteira, passa
            // adiante, e a vez cai onde este teste quer medi-la.
            ResourceId houseId =
                    ResourceId.vanilla("village/plains/houses/plains_small_house_1");

            // O reparador só considera blocos em chunks carregados. A borda
            // do raio de busca fica além da área carregada pelo GameTest,
            // portanto a casa de referência precisa permanecer no cenário.
            ColonyPos built = MinecraftTypeAdapter.toColonyPos(
                    context.getAbsolutePos(center.add(8, 0, 8)));

            Blueprint house = PlanPlacement.blueprintOf(
                    context.getWorld(), colony.id(), houseId, built).orElse(null);

            context.assertTrue(
                    house != null,
                    "o catálogo não devolveu a casa que estabelece a vez da sequência");

            for (BlueprintBlock block : house.blocks()) {
                MinecraftTypeAdapter.toBlock(block.block()).ifPresent(expected ->
                        context.getWorld().setBlockState(
                                new BlockPos(
                                        built.x() + block.offset().x(),
                                        built.y() + block.offset().y(),
                                        built.z() + block.offset().z()),
                                expected.getDefaultState()));
            }

            ColonyPos size = house.size();

            VillageColonyMod.BUILDINGS.register(new Building(
                    colony.id(),
                    colony.id(),
                    houseId,
                    built,
                    new ColonyPos(
                            built.x() + size.x() - 1,
                            built.y() + size.y() - 1,
                            built.z() + size.z() - 1)));

            Optional<ConstructionProject> opened =
                    ConstructionPlanner.plan(context.getWorld(), colony);

            context.assertTrue(
                    opened.isPresent(),
                    "a passagem seguinte à casa não abriu uma infraestrutura");

            context.assertFalse(
                    HousePlans.isDwelling(opened.get().blueprint().id()),
                    "a passagem seguinte abriu outra casa, quebrando a alternância");
        } finally {
            VillageColonyMod.CONSTRUCTIONS.removeOfColony(colony.id());

            VillageColonyMod.BUILDINGS.removeOfColony(colony.id());

            // Sem FarmPlans.clearAll() de propósito: ele limpa READ e
            // POSTPONED <b>globais</b> — o javadoc dele diz "chamado ao
            // parar o servidor" —, e esvaziar o cache de plantas lidas
            // no meio da bateria é a interferência entre gametests que
            // já custou rodada a este projeto. O adiamento que este
            // teste grava fica preso ao UUID sorteado aqui, que morre
            // com ele, e não alcança colônia de mais ninguém.
            owned.cleanUp();
        }

        context.complete();
    }

    /**
     * Observar o inventário não pode mudar a alternância de casas —
     * decisão 11A, 2026-09-24.
     *
     * <p><b>É a garantia que dá nome à decisão.</b> O mesmo cenário de
     * {@link #theNextTurnAfterAHouseIsNonResidential} — casa de pé,
     * segunda passagem abre infraestrutura —, mas com
     * {@link VillageInventoryObserver#observe} chamado duas vezes ao
     * redor de {@code ConstructionPlanner.plan}: antes e depois. Se
     * observar mudasse alguma coisa, a segunda observação divergiria da
     * primeira, ou a alternância pararia de valer.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "construction_rotation")
    public void observingInventoryDoesNotChangeHouseAlternation(TestContext context) {
        BlockPos center = new BlockPos(16, 1, 16);

        for (int dx = -SCAN_RADIUS; dx <= SCAN_RADIUS; dx++) {
            for (int dz = -SCAN_RADIUS; dz <= SCAN_RADIUS; dz++) {
                context.setBlockState(
                        center.add(dx, 0, dz), Blocks.GRASS_BLOCK.getDefaultState());
            }
        }

        context.setBlockState(center, Blocks.DIRT_PATH.getDefaultState());

        UUID colonyId = UUID.randomUUID();
        BlockPos absoluteRoad = context.getAbsolutePos(center);
        SweepPersistence.restore(new ColonyRoads(
                colonyId,
                MinecraftTypeAdapter.toColonyPos(absoluteRoad),
                List.of(ColonyRoads.column(absoluteRoad.getX(), absoluteRoad.getZ()))));

        Colony colony = Colony.create(
                colonyId,
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(center)));

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);

        try {
            UUID builderId = UUID.randomUUID();

            VillageColonyMod.WORKERS.register(builderId, colony.id())
                    .assign(ProfessionType.BUILDER);

            owned.owning(builderId);

            for (int villager = 1; villager < POPULATION; villager++) {
                UUID id = UUID.randomUUID();

                // Um fazendeiro sem roça: a roça só entra no rodízio por ele — Regra 52.
                var worker = VillageColonyMod.WORKERS.register(id, colony.id());
                if (villager == 1) {
                    worker.assign(ProfessionType.FARMER);
                }
                owned.owning(id);
            }

            // Sem esta observação, vinte adultos e zero camas devem abrir
            // uma casa. Este teste precisa da condição oposta para exercitar
            // somente a invariância da observação de inventário.
            colony.observe(colony.center(), POPULATION, true);

            ResourceId houseId =
                    ResourceId.vanilla("village/plains/houses/plains_small_house_1");

            ColonyPos built = MinecraftTypeAdapter.toColonyPos(
                    context.getAbsolutePos(center.add(8, 0, 8)));

            Blueprint house = PlanPlacement.blueprintOf(
                    context.getWorld(), colony.id(), houseId, built).orElse(null);

            context.assertTrue(
                    house != null,
                    "o catálogo não devolveu a casa que estabelece a vez da sequência");

            for (BlueprintBlock block : house.blocks()) {
                MinecraftTypeAdapter.toBlock(block.block()).ifPresent(expected ->
                        context.getWorld().setBlockState(
                                new BlockPos(
                                        built.x() + block.offset().x(),
                                        built.y() + block.offset().y(),
                                        built.z() + block.offset().z()),
                                expected.getDefaultState()));
            }

            ColonyPos size = house.size();

            VillageColonyMod.BUILDINGS.register(new Building(
                    colony.id(),
                    colony.id(),
                    houseId,
                    built,
                    new ColonyPos(
                            built.x() + size.x() - 1,
                            built.y() + size.y() - 1,
                            built.z() + size.z() - 1)));

            VillageInventory before = VillageInventoryObserver
                    .observe(context.getWorld(), colony.id())
                    .orElseThrow(() -> new AssertionError("a colônia observada não existe"));

            Optional<ConstructionProject> opened =
                    ConstructionPlanner.plan(context.getWorld(), colony);

            VillageInventory after = VillageInventoryObserver
                    .observe(context.getWorld(), colony.id())
                    .orElseThrow(() -> new AssertionError("a colônia observada não existe"));

            context.assertTrue(
                    opened.isPresent(),
                    "a passagem seguinte à casa não abriu uma infraestrutura,"
                            + " mesmo com a observação no meio");

            context.assertFalse(
                    HousePlans.isDwelling(opened.get().blueprint().id()),
                    "observar o inventário quebrou a alternância — abriu outra casa");

            // As duas fotografias concordam em tudo que a observação
            // sozinha não poderia ter mudado: quem trabalha e quantas
            // camas a colônia tem não se alteram por um planejador rodar
            // uma vez no meio.
            context.assertTrue(
                    before.adults() == after.adults(),
                    "observar mudou a contagem de adultos");
            context.assertTrue(
                    before.beds() == after.beds(),
                    "observar mudou a contagem de camas");
        } finally {
            VillageColonyMod.CONSTRUCTIONS.removeOfColony(colony.id());

            VillageColonyMod.BUILDINGS.removeOfColony(colony.id());

            owned.cleanUp();
        }

        context.complete();
    }

    /**
     * A PRIMEIRA obra de toda vila é uma casa — 2026-09-19.
     *
     * <p><b>Decisão do autor:</b> <i>"em todas vilas a primeira
     * construção deve ser uma casa, depois variantes mais úteis para a
     * vila"</i>.
     *
     * <p>Sem a guarda a roça passava na frente: a cota de roça (uma por
     * fazendeiro, Regra 52) abre cedo, e uma vila que nasce com fazendeiro
     * abria a roça <b>antes da primeira casa</b>, gastando a obra mais cara de conseguir no que não abriga
     * ninguém.
     *
     * <p>É o cenário irmão do {@code theHouseGoesUpAfterTheFarmStepsAside},
     * e a diferença é uma só: <b>a colônia não construiu nada ainda</b>.
     * Lá o prédio existe e a roça pode ser pedida; aqui não existe, e a
     * primeira obra tem de ser casa mesmo com a cota de roça aberta.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "farm_standoff")
    public void theFirstBuildOfAVillageIsAlwaysAHouse(TestContext context) {
        BlockPos center = new BlockPos(16, 1, 16);

        for (int dx = -SCAN_RADIUS; dx <= SCAN_RADIUS; dx++) {
            for (int dz = -SCAN_RADIUS; dz <= SCAN_RADIUS; dz++) {
                context.setBlockState(
                        center.add(dx, 0, dz), Blocks.GRASS_BLOCK.getDefaultState());
            }
        }

        context.setBlockState(center, Blocks.DIRT_PATH.getDefaultState());

        UUID colonyId = UUID.randomUUID();
        BlockPos absoluteRoad = context.getAbsolutePos(center);
        SweepPersistence.restore(new ColonyRoads(
                colonyId,
                MinecraftTypeAdapter.toColonyPos(absoluteRoad),
                List.of(ColonyRoads.column(absoluteRoad.getX(), absoluteRoad.getZ()))));

        Colony colony = Colony.create(
                colonyId,
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(center)));

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);

        try {
            UUID builderId = UUID.randomUUID();

            VillageColonyMod.WORKERS.register(builderId, colony.id())
                    .assign(ProfessionType.BUILDER);

            owned.owning(builderId);

            for (int villager = 1; villager < POPULATION; villager++) {
                UUID id = UUID.randomUUID();

                VillageColonyMod.WORKERS.register(id, colony.id());
                owned.owning(id);
            }

            // A cota de roça ESTÁ aberta (um fazendeiro sem roça), e é isso
            // que dá valor ao cenário: sem ela a casa sairia por falta de
            // alternativa, e o teste ficaria verde sem medir a regra.
            UUID farmerId = UUID.randomUUID();
            VillageColonyMod.WORKERS.register(farmerId, colony.id()).assign(ProfessionType.FARMER);
            owned.owning(farmerId);

            context.assertTrue(
                    ColonyFarms.owedToTheFarmers(context.getWorld(), colony),
                    "o cenário não pediu roça — sem isso a casa sai por falta de"
                            + " alternativa e a regra não é medida");

            context.assertTrue(
                    VillageColonyMod.BUILDINGS.ofColony(colony.id()).isEmpty(),
                    "a colônia já tinha prédio — o cenário não é o da PRIMEIRA obra");

            Optional<ConstructionProject> first =
                    ConstructionPlanner.plan(context.getWorld(), colony);

            context.assertTrue(
                    first.isPresent(),
                    "a primeira passagem não abriu obra nenhuma");

            context.assertFalse(
                    FarmPlans.isFarm(first.get().blueprint().id()),
                    "a PRIMEIRA obra da vila foi uma roça: a colônia gastou a obra mais"
                            + " cara de conseguir no que não abriga ninguém");
        } finally {
            VillageColonyMod.CONSTRUCTIONS.removeOfColony(colony.id());

            VillageColonyMod.BUILDINGS.removeOfColony(colony.id());

            owned.cleanUp();
        }

        context.complete();
    }

    private static boolean isCrop(BlueprintBlock block) {
        return MinecraftTypeAdapter.toBlock(block.block())
                .map(found -> found instanceof CropBlock)
                .orElse(false);
    }
}
