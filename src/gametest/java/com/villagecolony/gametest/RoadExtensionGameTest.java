package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.BuildSiteScanner;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.integration.RoadExtension;
import com.villagecolony.fabric.work.ConstructionPlanner;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

/**
 * A rua cresce com a vila — a Regra 15, 2026-08-21.
 *
 * <p><b>O "para" que esta regra tira.</b> O cabeçalho do
 * {@code BuildSiteScanner} dizia desde 08-14: "a vila cresce enquanto
 * houver beira de rua livre, e para quando não houver". Uma vila que
 * ocupasse toda a beira da rua que o jogo gerou parava de crescer para
 * sempre, e o log dizia apenas que não havia lote — o que era verdade, e
 * escondia que a colônia podia fazer um.
 *
 * <p><b>O cenário destes testes é uma faixa de um bloco de largura.</b> É
 * o jeito mais direto de montar uma vila sem beira livre: a rua existe, e
 * não há nenhum lugar plano ao lado dela onde uma casa caiba. Foi para
 * exatamente esse caso que a regra foi escrita.
 */
public class RoadExtensionGameTest implements FabricGameTest {

    /** Curto o bastante para a varredura caber numa passagem só. */
    private static final int RADIUS = 3;

    /** Uma casa de dois por dois, que não cabe numa faixa de um. */
    private static final ColonyPos SMALL_HOUSE = new ColonyPos(2, 3, 2);

    /** Onde a rua começa. É daqui que a colônia mede a distância. */
    private static final BlockPos ROAD_START = new BlockPos(2, 1, 2);

    /** A ponta da rua: daqui em diante é terra por calçar. */
    private static final BlockPos ROAD_END = new BlockPos(4, 1, 2);

    /**
     * Sem beira livre, a colônia calça o trecho seguinte.
     *
     * <p>A ponta escolhida é a <b>mais distante do centro</b>, e o cenário
     * tem duas: a rua acaba dos dois lados, e o começo dela está a zero do
     * centro enquanto o fim está a quatro. Estrada que cresce pelo meio
     * racha a vila, e é por isso que a distância decide.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "road_extension")
    public void withoutAFreeLotTheRoadGrows(TestContext context) {
        UUID colony = UUID.randomUUID();

        strip(context);

        // A varredura é quem anota a ponta, e é ela que precisa falhar:
        // é o "não há mais lote" dela que autoriza a rua a crescer.
        context.assertTrue(
                BuildSiteScanner.find(
                        context.getWorld(),
                        colony,
                        MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(ROAD_START)),
                        RADIUS,
                        SMALL_HOUSE).isEmpty(),
                "a faixa de um bloco não podia ter cabido uma casa de dois");

        RoadExtension.Outcome outcome = RoadExtension.extend(
                context.getWorld(), colony, ResourceId.vanilla("dirt_path"));

        context.assertTrue(
                outcome == RoadExtension.Outcome.EXTENDED,
                "a rua não cresceu: " + outcome);

        for (int step = 1; step <= 3; step++) {
            BlockPos laid = ROAD_END.add(step, 0, 0);

            context.assertTrue(
                    context.getBlockState(laid).isOf(Blocks.DIRT_PATH),
                    "o bloco " + step + " depois da ponta não virou rua");
        }

        context.complete();
    }

    /**
     * A rua não cresce enquanto houver onde construir.
     *
     * <p>É a metade que impede a regra de virar outra coisa: rua que
     * cresce sozinha vira rua sem nada em volta. A colônia só calça
     * quando a varredura inteira terminou sem lote, e uma que ache lote
     * apaga a ponta que tinha anotado.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "road_extension")
    public void withAFreeLotTheRoadStaysAsItIs(TestContext context) {
        UUID colony = UUID.randomUUID();

        // Chão largo em volta: agora há beira de rua de sobra.
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                context.setBlockState(
                        ROAD_START.add(dx, 0, dz), Blocks.GRASS_BLOCK.getDefaultState());
            }
        }

        for (int step = 0; step <= 2; step++) {
            context.setBlockState(
                    ROAD_START.add(step, 0, 0), Blocks.DIRT_PATH.getDefaultState());
        }

        context.assertTrue(
                BuildSiteScanner.find(
                        context.getWorld(),
                        colony,
                        MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(ROAD_START)),
                        RADIUS,
                        SMALL_HOUSE).isPresent(),
                "o cenário não montou: era para haver lote ao lado da rua");

        context.assertTrue(
                RoadExtension.extend(
                        context.getWorld(), colony, ResourceId.vanilla("dirt_path"))
                        == RoadExtension.Outcome.NO_END,
                "a rua cresceu com a vila ainda tendo onde construir");

        context.complete();
    }

    /**
     * Rua não se prolonga sobre o que já está de pé.
     *
     * <p>A Regra 3, e aqui ela morde: a vila gerada é feita de bloco que
     * passaria por chão. Uma ponta que dê contra pedra para de calçar em
     * vez de cavar.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "road_extension")
    public void theRoadStopsAtWhatItMayNotPave(TestContext context) {
        UUID colony = UUID.randomUUID();

        strip(context);

        // Pedra à frente da ponta: não é chão de vila, e não se calça.
        context.setBlockState(ROAD_END.add(1, 0, 0), Blocks.STONE.getDefaultState());

        BuildSiteScanner.find(
                context.getWorld(),
                colony,
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(ROAD_START)),
                RADIUS,
                SMALL_HOUSE);

        context.assertTrue(
                RoadExtension.extend(
                        context.getWorld(), colony, ResourceId.vanilla("dirt_path"))
                        == RoadExtension.Outcome.BLOCKED,
                "a rua passou por cima da pedra");

        context.assertTrue(
                context.getBlockState(ROAD_END.add(1, 0, 0)).isOf(Blocks.STONE),
                "a pedra virou rua");

        context.complete();
    }

    /**
     * Uma ponta murada não para a vila — E27, 2026-08-25.
     *
     * <p>A colônia guardava <b>uma</b> ponta, a mais distante, e nenhuma
     * outra. Quando essa não se deixava calçar, {@code pave} devolvia
     * zero e a vila não tinha alternativa: a varredura seguinte gastava
     * mais oito minutos para reescolher a mesma ponta e bater no mesmo
     * bloco. Foi o que a sessão de 2026-08-25 mostrou, às 23:14:27, na
     * colônia {@code 56c5b68d}.
     *
     * <p>O cenário tem as duas pontas da mesma faixa: a de leste é a mais
     * distante do centro e está murada de pedra; a de oeste está mais
     * perto e tem chão. A afirmação é que a segunda foi tentada.
     *
     * <p>Rodado contra a regra desligada: a saída é {@code BLOCKED} e
     * nenhum bloco de rua aparece a oeste.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "road_extension")
    public void aWalledEndDoesNotStopTheRoadWhenAnotherServes(TestContext context) {
        UUID colony = UUID.randomUUID();

        strip(context);

        // A ponta mais distante, murada. É a que a colônia escolhia, e
        // era a única que ela tentava.
        context.setBlockState(ROAD_END.add(1, 0, 0), Blocks.STONE.getDefaultState());

        // A outra ponta da mesma faixa, com chão de sobra. Mais perto do
        // centro, então é a segunda da fila.
        for (int step = 1; step <= 2; step++) {
            context.setBlockState(
                    ROAD_START.add(-step, 0, 0), Blocks.DIRT.getDefaultState());
        }

        BuildSiteScanner.find(
                context.getWorld(),
                colony,
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(ROAD_START)),
                RADIUS,
                SMALL_HOUSE);

        RoadExtension.Outcome outcome = RoadExtension.extend(
                context.getWorld(), colony, ResourceId.vanilla("dirt_path"));

        context.assertTrue(
                outcome == RoadExtension.Outcome.EXTENDED,
                "a ponta murada parou a vila em vez de a colônia tentar a próxima: " + outcome);

        context.assertTrue(
                context.getBlockState(ROAD_START.add(-1, 0, 0)).isOf(Blocks.DIRT_PATH),
                "a segunda ponta não foi tentada");

        context.assertTrue(
                context.getBlockState(ROAD_END.add(1, 0, 0)).isOf(Blocks.STONE),
                "a pedra virou rua");

        context.complete();
    }

    /**
     * O planejador manda calçar quando não tem onde construir.
     *
     * <p>Os testes acima provam a mecânica; este prova a <b>ligação</b>.
     * A Regra 15 mora numa linha só do planejamento — a que antes se
     * limitava a dizer "no free lot beside a road" e desistir —, e uma
     * ligação que não estivesse ligada passaria por todos os outros.
     *
     * <p>O raio é encurtado porque a regra só age quando a varredura
     * <b>termina</b>: sessenta e quatro blocos são dezessete passagens de
     * mil colunas, e um teste que quisesse ver a rua crescer teria de
     * rodar as dezessete — sobre as arenas dos vizinhos.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "road_planner")
    public void thePlannerIsWhatOrdersThePaving(TestContext context) {
        strip(context);

        ColonyPos where = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(ROAD_START));

        Colony colony = Colony.create(UUID.randomUUID(), where);

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);

        VillagerEntity villager = context.spawnEntity(
                EntityType.VILLAGER, ROAD_START.add(0, 1, 0));
        villager.setBreedingAge(0);

        VillageColonyMod.WORKERS
                .register(villager.getUuid(), colony.id())
                .assign(ProfessionType.BUILDER);

        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), where));

        owned.owning(villager.getUuid());

        ConstructionPlanner.shortenSearchTo(RADIUS);

        try {
            ConstructionPlanner.plan(context.getWorld(), colony);

            context.assertTrue(
                    context.getBlockState(ROAD_END.add(1, 0, 0)).isOf(Blocks.DIRT_PATH),
                    "o planejamento terminou sem lote e não mandou calçar nada");
        } finally {
            ConstructionPlanner.restoreSearch();

            owned.cleanUp();
        }

        context.complete();
    }

    /**
     * Uma faixa de um bloco: rua até a ponta, terra depois dela.
     *
     * <p>Estreita de propósito. É o que garante que não há lote: uma casa
     * de dois por dois não cabe em largura um, e a varredura termina o
     * raio inteiro sem achar nada — que é a condição da regra.
     */
    private static void strip(TestContext context) {
        for (int step = 0; step <= 2; step++) {
            context.setBlockState(
                    ROAD_START.add(step, 0, 0), Blocks.DIRT_PATH.getDefaultState());
        }

        for (int step = 1; step <= 3; step++) {
            context.setBlockState(
                    ROAD_END.add(step, 0, 0), Blocks.DIRT.getDefaultState());
        }
    }
    /**
     * O lote nasce no trecho que a colônia acabou de calçar — E26.
     *
     * <p>A conta que a sessão de 2026-08-25 tornou concreta: uma resposta
     * de lote custa dezessete ciclos, oito minutos e meio. Quando a Regra
     * 15 calça um trecho, a colônia <b>sabe</b> onde nasceu beira nova —
     * e mandá-la redescobrir isso varrendo o raio inteiro é pagar oito
     * minutos por uma informação que ela tem na mão.
     *
     * <p>O cenário separa as duas coisas: terra larga só ao lado da
     * segunda coluna calçada. A primeira não tem onde caber casa, e é ela
     * que prova que a busca não se contenta com a primeira da lista.
     *
     * <p><b>O que este teste não alcança</b>, e fica dito: a ligação com
     * {@code ConstructionPlanner} não é exercitada aqui. O planejamento
     * usa as casas de verdade do jogo — sete por sete —, e um lote desse
     * tamanho não cabe na arena da bateria junto com a rua.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "road_extension")
    public void theLotIsBornOnTheStretchTheColonyJustPaved(TestContext context) {
        UUID colony = UUID.randomUUID();

        strip(context);

        // As três colunas que uma extensão calçaria, e o chão largo só a
        // partir da segunda.
        for (int step = 1; step <= 3; step++) {
            context.setBlockState(
                    ROAD_END.add(step, 0, 0), Blocks.DIRT_PATH.getDefaultState());
        }

        for (int x = 6; x <= 7; x++) {
            for (int z = 0; z <= 4; z++) {
                context.setBlockState(new BlockPos(x, 1, z), Blocks.DIRT.getDefaultState());
            }
        }

        // A rua por cima da terra larga continua sendo rua.
        for (int step = 2; step <= 3; step++) {
            context.setBlockState(
                    ROAD_END.add(step, 0, 0), Blocks.DIRT_PATH.getDefaultState());
        }

        ColonyPos center = MinecraftTypeAdapter.toColonyPos(
                context.getAbsolutePos(ROAD_START));

        ColonyPos tight = MinecraftTypeAdapter.toColonyPos(
                context.getAbsolutePos(ROAD_END.add(1, 0, 0)));

        ColonyPos roomy = MinecraftTypeAdapter.toColonyPos(
                context.getAbsolutePos(ROAD_END.add(2, 0, 0)));

        context.assertTrue(
                BuildSiteScanner.findBeside(
                        context.getWorld(), colony, center,
                        List.of(SMALL_HOUSE), List.of(tight)).isEmpty(),
                "achou lote encostado na coluna que não tem chão ao lado");

        context.assertTrue(
                BuildSiteScanner.findBeside(
                        context.getWorld(), colony, center,
                        List.of(SMALL_HOUSE), List.of(tight, roomy)).isPresent(),
                "a colônia acabou de calçar este trecho e não achou o lote que ele abriu");

        context.complete();
    }

    /**
     * A ponta que rendeu é retomada, sem pagar outra varredura.
     *
     * <p><b>O gargalo que a sessão de 2026-08-26, às 03:11, mediu.</b> A
     * rua cresceu <b>um</b> bloco — o {@code pave} para no primeiro que
     * recusa — e um bloco de beira nova não abre lote para uma casa de
     * sete por sete. A colônia então voltava a varrer o raio inteiro
     * para, oito minutos e meio depois, calçar mais um.
     *
     * <p>Três coisas se afirmam aqui, e são as três que mudaram:
     *
     * <ul>
     *   <li>calçar <b>deixa a ponta em crescimento</b> — antes ela era
     *       consumida e esquecida;
     *   <li>a ponta em crescimento é <b>retomada</b> sem varredura;
     *   <li>quando ela para de render, a colônia <b>para de insistir</b>
     *       e a varredura volta a mandar.
     * </ul>
     *
     * <p>O que este teste <b>não</b> mede é o ganho em blocos: a arena da
     * bateria tem oito de lado, e a pista acaba antes de a insistência
     * ter onde render. O ganho é aritmético e está no {@code MAX_RUN}.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "road_extension")
    public void theEndThatPaidIsResumedWithoutAnotherSweep(TestContext context) {
        UUID colony = UUID.randomUUID();

        strip(context);

        BuildSiteScanner.find(
                context.getWorld(),
                colony,
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(ROAD_START)),
                RADIUS,
                SMALL_HOUSE);

        context.assertTrue(
                RoadExtension.extend(
                        context.getWorld(), colony, ResourceId.vanilla("dirt_path"))
                        == RoadExtension.Outcome.EXTENDED,
                "a montagem falhou: a rua nem chegou a crescer");

        context.assertTrue(
                RoadExtension.isGrowing(colony),
                "a ponta foi consumida e esquecida — a colônia vai pagar outra varredura"
                        + " inteira pelo bloco seguinte");

        // A pista acaba na borda da arena, então a retomada bate no fim.
        // O que importa é que ela foi tentada, e que a colônia desistiu
        // de insistir em vez de repetir para sempre.
        RoadExtension.Outcome again = RoadExtension.keepGrowing(
                context.getWorld(), colony, ResourceId.vanilla("dirt_path"));

        context.assertTrue(
                again != RoadExtension.Outcome.NO_END,
                "a ponta em crescimento nem chegou a ser retomada");

        context.assertTrue(
                !RoadExtension.isGrowing(colony),
                "a ponta parou de render e a colônia continuou insistindo nela");

        context.complete();
    }

    /**
     * Varredura nova apaga a insistência.
     *
     * <p>A autorização para a rua crescer é filha de uma varredura que
     * terminou sem lote. Uma varredura nova refaz a pergunta, e a
     * resposta velha sai junto — senão a colônia calçaria por causa de
     * uma decisão que já não vale.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "road_extension")
    public void aFreshSweepDropsTheGrowingEnd(TestContext context) {
        UUID colony = UUID.randomUUID();

        strip(context);

        BuildSiteScanner.find(
                context.getWorld(),
                colony,
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(ROAD_START)),
                RADIUS,
                SMALL_HOUSE);

        RoadExtension.extend(context.getWorld(), colony, ResourceId.vanilla("dirt_path"));

        context.assertTrue(
                RoadExtension.isGrowing(colony),
                "a montagem falhou: era para haver ponta em crescimento");

        // A varredura recomeça do centro, e é ela que chama forgetEnds.
        BuildSiteScanner.find(
                context.getWorld(),
                colony,
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(ROAD_START)),
                RADIUS,
                SMALL_HOUSE);

        context.assertTrue(
                !RoadExtension.isGrowing(colony),
                "a varredura nova não apagou a insistência da anterior");

        context.complete();
    }
}
