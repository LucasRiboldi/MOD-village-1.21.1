package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
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
import com.villagecolony.fabric.integration.VillageStructures;
import com.villagecolony.fabric.work.ConstructionPlanner;
import com.villagecolony.fabric.work.FarmPlans;
import com.villagecolony.fabric.work.FarmerWork;
import com.villagecolony.fabric.work.HousePlans;
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
                ConstructionPlanner.withinTheFarmersReach(colony, near),
                "o lote na borda do alcance do fazendeiro foi recusado");

        context.assertFalse(
                ConstructionPlanner.withinTheFarmersReach(colony, far),
                "um lote fora do alcance do fazendeiro passou — é a roça de 105 blocos"
                        + " que ninguém planta, de volta");

        context.complete();
    }

    /**
     * <b>Uma roça a cada quinze aldeões</b> — decisão do autor,
     * 2026-09-05: <i>"a quantidade de espaços de plantação deve [ser]
     * 1/15 avos da quantidade de aldeões"</i>.
     *
     * <p><b>O que ela fecha.</b> O pedido de roça vinha do fazendeiro —
     * <i>varri o raio e não achei campo</i> —, e um pedido assim não tem
     * teto: a sessão das 21:17 levantou <b>duas roças em quatro
     * minutos</b>, porque a primeira nasceu longe demais para ele ver e o
     * pedido nunca se fechava.
     *
     * <p>Divisão inteira, que é a frase ao pé da letra: catorze aldeões
     * não pedem roça nenhuma, quinze pedem a primeira.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "farm_quota")
    public void oneFarmForEveryFifteenVillagers(TestContext context) {
        Colony colony = Colony.create(UUID.randomUUID(), new ColonyPos(0, 64, 0));

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);

        try {
            context.assertFalse(
                    FarmPlans.owedToThePopulation(colony.id()),
                    "uma vila sem aldeão nenhum pediu roça");

            for (int villager = 0; villager < FarmPlans.VILLAGERS_PER_FARM - 1; villager++) {
                UUID id = UUID.randomUUID();

                VillageColonyMod.WORKERS.register(id, colony.id());
                owned.owning(id);
            }

            context.assertFalse(
                    FarmPlans.owedToThePopulation(colony.id()),
                    "catorze aldeões já pediram roça — a cota é de quinze");

            UUID last = UUID.randomUUID();

            VillageColonyMod.WORKERS.register(last, colony.id());
            owned.owning(last);

            context.assertTrue(
                    FarmPlans.owedToThePopulation(colony.id()),
                    "quinze aldeões e nenhuma roça, e a colônia não pediu a primeira");
        } finally {
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
        BuildSiteScanner.restore(new ColonyRoads(
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

            for (int villager = 1; villager < FarmPlans.VILLAGERS_PER_FARM; villager++) {
                UUID id = UUID.randomUUID();

                VillageColonyMod.WORKERS.register(id, colony.id());
                owned.owning(id);
            }

            // A vila já ergueu a primeira casa. Longe do centro de
            // propósito: o prédio apenas estabelece a vez da sequência.
            ColonyPos built = MinecraftTypeAdapter.toColonyPos(
                    context.getAbsolutePos(center.add(-SCAN_RADIUS, 0, -SCAN_RADIUS)));

            VillageColonyMod.BUILDINGS.register(new Building(
                    colony.id(),
                    colony.id(),
                    ResourceId.vanilla("village/plains/houses/plains_small_house_1"),
                    built,
                    new ColonyPos(built.x() + 1, built.y() + 1, built.z() + 1)));

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
     * A PRIMEIRA obra de toda vila é uma casa — 2026-09-19.
     *
     * <p><b>Decisão do autor:</b> <i>"em todas vilas a primeira
     * construção deve ser uma casa, depois variantes mais úteis para a
     * vila"</i>.
     *
     * <p>Sem a guarda a roça passava na frente: a cota é por população —
     * um campo a cada {@code VILLAGERS_PER_FARM} aldeões —, e uma vila
     * que nasce com gente bastante abria a roça <b>antes da primeira
     * casa</b>, gastando a obra mais cara de conseguir no que não abriga
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
        BuildSiteScanner.restore(new ColonyRoads(
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

            for (int villager = 1; villager < FarmPlans.VILLAGERS_PER_FARM; villager++) {
                UUID id = UUID.randomUUID();

                VillageColonyMod.WORKERS.register(id, colony.id());
                owned.owning(id);
            }

            // A cota de roça ESTÁ aberta, e é isso que dá valor ao
            // cenário: sem ela a casa sairia por falta de alternativa, e
            // o teste ficaria verde sem medir a regra.
            context.assertTrue(
                    FarmPlans.owedToThePopulation(colony.id()),
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
