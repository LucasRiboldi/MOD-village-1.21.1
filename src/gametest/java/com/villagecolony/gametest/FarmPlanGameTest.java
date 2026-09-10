package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
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

    /**
     * <b>A roça que não cabe cede a vez, e a casa passa</b> — 2026-09-09,
     * o E42.
     *
     * <p><b>O que ele mede, e por que nenhum outro teste o media.</b> O
     * mecanismo do adiamento tem duas metades. A primeira é o relógio de
     * {@code FarmPlans} — {@code postpone} grava, {@code postponed}
     * responde —, e ela tem seis testes em {@code FarmPostponementTest}.
     * A segunda é <b>o planejador usar esse relógio para abrir uma
     * casa</b>, e ela não tinha nenhum: {@code FarmPostponementTest}
     * nunca chama {@link ConstructionPlanner#plan}, e
     * {@code theFarmLotStaysWithinTheFarmersReach} chama
     * {@link ConstructionPlanner#withinTheFarmersReach} <b>direto</b> —
     * afirma o predicado fora do fluxo que o consome, que é a mesma
     * armadilha da lição de 2026-09-05: <i>teste que valida uma cópia da
     * regra não valida a regra</i>.
     *
     * <p><b>A lacuna foi medida, e não suposta.</b> Removida da produção
     * a consulta {@code !FarmPlans.postponed(...)} — a mutação que
     * reproduz exatamente o defeito da sessão —, <b>681 unitários e os
     * 274 testes de jogo de então passam</b>: medido em 2026-09-09,
     * antes deste teste existir. A bateria inteira ficava verde com a
     * vila parada. Com ele, a mesma mutação falha aqui, e só aqui.
     *
     * <p><b>O defeito que ele guarda</b>, medido na sessão de 09-09 na
     * colônia {@code 634bf5cc}: o lote livre estava fora do alcance do
     * fazendeiro, a recusa daquele lote <b>encerrava a passagem</b>, e o
     * lote de amanhã é o mesmo de hoje — uma hora de jogo, <b>108
     * passagens do planejador e nenhuma obra aberta</b>, com 78 ciclos
     * dizendo {@code assigned 0 tasks (0 open)}. Sem obra não há pedido
     * de tábua nem de pedra, então o construtor e o mineiro também
     * ficavam parados. A queixa do autor foi <i>"não vi os trabalhadores
     * trabalhando"</i>.
     *
     * <p><b>Como o cenário força a condição</b>, em vez de esperá-la: o
     * alcance do fazendeiro é encurtado a <b>-1</b>, então <b>todo</b>
     * lote que a varredura ache fica fora dele — a arena não precisa ter
     * trinta e dois blocos. <b>Zero não serve</b>, e isto custou uma
     * rodada: a conta é {@code Math.max(dx, dz) <= reach}, então com
     * alcance zero o lote sobre o próprio centro tem distância zero e
     * <b>passa</b> — o impasse não se montava e a primeira afirmação
     * ficava verde por outro motivo.
     *
     * <p>A cota de roça é aberta com quinze aldeões e nenhuma roça
     * construída, e a arena tem de caber a roça de planície, que é
     * <b>13x9</b> — com o raio de 6 da primeira tentativa a varredura
     * não achava lote nenhum, e o teste falhava sem que houvesse
     * defeito. A primeira passagem tem então de recusar o lote, e a
     * segunda — a que este teste existe para afirmar — tem de abrir
     * <b>uma casa</b>.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "farm_standoff")
    public void theHouseGoesUpAfterTheFarmStepsAside(TestContext context) {
        BlockPos center = new BlockPos(16, 1, 16);

        // Chão liso com rua no meio: é o que a varredura procura.
        for (int dx = -SCAN_RADIUS; dx <= SCAN_RADIUS; dx++) {
            for (int dz = -SCAN_RADIUS; dz <= SCAN_RADIUS; dz++) {
                context.setBlockState(
                        center.add(dx, 0, dz), Blocks.GRASS_BLOCK.getDefaultState());
            }
        }

        context.setBlockState(center, Blocks.DIRT_PATH.getDefaultState());

        Colony colony = Colony.create(
                UUID.randomUUID(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(center)));

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);

        // Alcance -1: qualquer lote que a varredura ache está fora
        // dele, que é a condição da sessão sem precisar da distância
        // dela. Zero não serve — `Math.max(dx, dz) <= 0` aceita o lote
        // sobre o próprio centro.
        FarmerWork.shortenSearchTo(-1);

        try {
            // Quinze aldeões e nenhuma roça: a cota pede a primeira. Um
            // deles constrói, senão o planejador para antes em
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

            context.assertTrue(
                    FarmPlans.owedToThePopulation(colony.id()),
                    "o cenário não pediu roça — sem isso o teste não mede o impasse");

            // A primeira passagem topa com o lote fora do alcance e
            // recusa. Ela não abre obra, e é assim que tem de ser.
            Optional<ConstructionProject> refused =
                    ConstructionPlanner.plan(context.getWorld(), colony);

            context.assertTrue(
                    refused.isEmpty(),
                    "a primeira passagem abriu obra — o lote devia estar fora do alcance"
                            + " do fazendeiro, e o cenário não montou o impasse");

            // E a segunda tem de abrir uma CASA. É a afirmação que
            // faltava: sem a consulta ao adiamento, esta passagem
            // recusa o mesmo lote de novo, para sempre.
            Optional<ConstructionProject> opened =
                    ConstructionPlanner.plan(context.getWorld(), colony);

            context.assertTrue(
                    opened.isPresent(),
                    "a segunda passagem não abriu obra nenhuma — é a vila de 09-09 parada,"
                            + " 108 passagens e nenhum trabalhador trabalhando");

            context.assertFalse(
                    FarmPlans.isFarm(opened.get().blueprint().id()),
                    "a segunda passagem abriu outra roça, e ela não cabe:"
                            + " o adiamento não cedeu a vez para a casa");
        } finally {
            FarmerWork.restoreSearch();

            VillageColonyMod.CONSTRUCTIONS.removeOfColony(colony.id());

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

    private static boolean isCrop(BlueprintBlock block) {
        return MinecraftTypeAdapter.toBlock(block.block())
                .map(found -> found instanceof CropBlock)
                .orElse(false);
    }
}
