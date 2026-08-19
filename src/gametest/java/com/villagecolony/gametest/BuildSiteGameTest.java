package com.villagecolony.gametest;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.BuildSiteScanner;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

/**
 * O lote da próxima casa — a Regra 6 lendo o mundo.
 *
 * <p>Gametest porque é leitura de terreno: mapa de alturas, blocos e
 * chunk carregado. O Core não tem nada disso.
 *
 * <p><b>Raio curto de propósito.</b> A bateria roda concorrente e as
 * estruturas dos outros testes ficam perto. Com raio de poucos blocos, a
 * busca não alcança a rua de ninguém, e o que este teste afirma é sobre o
 * terreno que ele mesmo montou. Ver {@link ColonyFixture}.
 */
public class BuildSiteGameTest implements FabricGameTest {

    /** Curto o bastante para a busca não sair da estrutura do teste. */
    private static final int RADIUS = 3;

    /** Uma casa de dois por dois, que cabe na estrutura vazia. */
    private static final ColonyPos SMALL_HOUSE = new ColonyPos(2, 3, 2);

    /**
     * Chão liso com rua ao lado: a colônia acha onde construir.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "build_site")
    public void aLotBesideTheRoadIsFound(TestContext context) {
        BlockPos center = new BlockPos(3, 1, 3);

        paveGround(context, center);

        context.setBlockState(center, Blocks.DIRT_PATH.getDefaultState());

        Optional<BuildSiteScanner.Site> site = BuildSiteScanner.find(
                context.getWorld(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(center)),
                RADIUS,
                SMALL_HOUSE);

        context.assertTrue(site.isPresent(), "não achou lote ao lado da rua");

        // Assenta sobre o chão, não dentro dele: o chão está em y=1
        // relativo, então a casa começa em y=2.
        context.assertTrue(
                site.get().origin().y() == context.getAbsolutePos(center).getY() + 1,
                "a casa assentou em " + site.get().origin().y() + ", e o chão está em "
                        + context.getAbsolutePos(center).getY());

        context.complete();
    }

    /**
     * Um campo de grama não desqualifica o lote.
     *
     * <p>Escrito depois da sessão de 2026-08-15, 00:42, que fechou o E14:
     * duas varreduras completas por colônia, raio 64 inteiro, e nenhum
     * lote — em duas vilas de planície rodeadas de campo aberto.
     *
     * <p>O mecanismo: {@code groundInColumn} devolve o bloco mais alto que
     * não é ar, e num campo de planície esse bloco é a <b>grama alta</b>,
     * não o bloco de grama. {@code isNaturalGround} então recusa a coluna,
     * porque tufo não é chão. Um lote de sete por sete precisa das
     * quarenta e nove colunas limpas, e em planície isso não acontece.
     *
     * <p>É o buraco que a TASK-047 já registrava por outro lado:
     * Construction-System.md §PREPARING manda limpar grama, flor e neve, e
     * o código pula o estado alegando que o lote só é aceito quando não há
     * nada em cima dele. A alegação é verdadeira e é justamente o defeito.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "build_site_grass")
    public void aFieldOfGrassDoesNotDisqualifyTheLot(TestContext context) {
        BlockPos center = new BlockPos(3, 1, 3);

        paveGround(context, center);

        context.setBlockState(center, Blocks.DIRT_PATH.getDefaultState());

        // O campo inteiro coberto, e não um tufo só: com um tufo, a busca
        // tenta as quatro direções e o lote escapa pelo lado limpo — foi
        // assim que a primeira versão deste teste passou sem provar nada.
        // Planície de verdade é grama em toda parte.
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }

                context.setBlockState(
                        center.add(dx, 1, dz), Blocks.SHORT_GRASS.getDefaultState());
            }
        }

        Optional<BuildSiteScanner.Site> site = BuildSiteScanner.find(
                context.getWorld(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(center)),
                RADIUS,
                SMALL_HOUSE);

        context.assertTrue(
                site.isPresent(),
                "o campo de grama reprovou o lote — em planície isso é todo lote");

        context.complete();
    }

    /**
     * "Não achei" e "não terminei de procurar" são respostas diferentes.
     *
     * <p>Escrito depois da sessão de 2026-08-15, 00:28. A busca tem teto
     * de colunas por chamada e um cursor que retoma no anel onde parou —
     * e {@code find} devolve vazio nos dois casos: quando varreu o raio
     * inteiro sem achar, e quando o orçamento daquele ciclo acabou no meio.
     *
     * <p>A linha de log da Fase 10 dizia "no free lot beside a road within
     * 64 blocks" nos dois, e no segundo caso isso é mentira: um raio de 64
     * são dezesseis mil colunas, mil por ciclo, dezessete ciclos — e a
     * sessão teve quatorze. Ninguém tinha varrido raio nenhum inteiro.
     *
     * <p>O estado que separa os dois já existia dentro do scanner: o
     * cursor só fica gravado quando o orçamento acaba.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "build_site_partial")
    public void anUnfinishedSweepIsNotAnAnswer(TestContext context) {
        BlockPos center = new BlockPos(3, 1, 3);

        paveGround(context, center);

        ColonyPos from = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(center));

        // Raio curto: cabe inteiro no orçamento, e a varredura termina.
        BuildSiteScanner.find(context.getWorld(), from, RADIUS, SMALL_HOUSE);

        context.assertTrue(
                BuildSiteScanner.sweepPausedAt(from).isEmpty(),
                "raio de " + RADIUS + " cabe num ciclo, e a busca disse que parou no meio");

        // Raio de vila de verdade: dezesseis mil colunas, mil por chamada.
        BuildSiteScanner.find(context.getWorld(), from, 64, SMALL_HOUSE);

        context.assertTrue(
                BuildSiteScanner.sweepPausedAt(from).isPresent(),
                "raio de 64 não cabe num ciclo, e a busca disse que varreu tudo");

        BuildSiteScanner.clearAll();

        context.complete();
    }

    /**
     * Sem rua, não há lote.
     *
     * <p>É a Regra 6 ao pé da letra: nunca casa isolada. Um chão liso e
     * vazio no meio do campo é exatamente o lugar onde a colônia <b>não</b>
     * pode construir.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "build_site")
    public void groundWithoutARoadIsNotALot(TestContext context) {
        BlockPos center = new BlockPos(3, 1, 3);

        paveGround(context, center);

        Optional<BuildSiteScanner.Site> site = BuildSiteScanner.find(
                context.getWorld(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(center)),
                RADIUS,
                SMALL_HOUSE);

        context.assertTrue(site.isEmpty(), "achou lote sem rua nenhuma por perto");

        context.complete();
    }

    /**
     * Terreno acidentado é recusado.
     *
     * <p>Um pilar dentro do lote põe o desnível acima de
     * {@link BuildSiteScanner#MAX_SLOPE}, e a casa iria para outro lugar
     * em vez de nascer enterrada de um lado.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "build_site")
    public void brokenGroundIsRefused(TestContext context) {
        BlockPos center = new BlockPos(3, 1, 3);

        paveGround(context, center);

        context.setBlockState(center, Blocks.DIRT_PATH.getDefaultState());

        // Uma torre em cada vizinho da rua: qualquer lote que encoste
        // nela passa do desnível.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }

                for (int dy = 1; dy <= BuildSiteScanner.MAX_SLOPE + 2; dy++) {
                    context.setBlockState(
                            center.add(dx, dy, dz), Blocks.GRASS_BLOCK.getDefaultState());
                }
            }
        }

        Optional<BuildSiteScanner.Site> site = BuildSiteScanner.find(
                context.getWorld(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(center)),
                RADIUS,
                SMALL_HOUSE);

        context.assertTrue(site.isEmpty(), "aceitou lote com desnível de quatro blocos");

        context.complete();
    }

    /**
     * A Regra 19: lote acima do nível da rua é recusado.
     *
     * <p>Sem ela a casa nasce numa varanda sem escada — o lote pode
     * ficar até {@code MAX_SLOPE} acima do caminho e continuar sendo
     * "plano". A porta da Regra 17 daria para o alto de um degrau que
     * ninguém sobe, e a casa ficaria bonita e inútil.
     *
     * <p>Rodado contra a regra desligada: o lote de cima é aceito e a
     * afirmação falha.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "build_site")
    public void aLotAboveTheRoadIsRefused(TestContext context) {
        BlockPos center = new BlockPos(3, 1, 3);

        paveGround(context, center);

        context.setBlockState(center, Blocks.DIRT_PATH.getDefaultState());

        // Tudo em volta da rua sobe um degrau. O lote continua plano
        // entre si, e deixa de estar no nível de quem anda na rua.
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }

                context.setBlockState(
                        center.add(dx, 1, dz), Blocks.GRASS_BLOCK.getDefaultState());
            }
        }

        Optional<BuildSiteScanner.Site> site = BuildSiteScanner.find(
                context.getWorld(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(center)),
                RADIUS,
                SMALL_HOUSE);

        context.assertTrue(
                site.isEmpty(),
                "a Regra 19 recusa lote fora do nível da rua, e este foi aceito em "
                        + site.map(found -> found.origin().toString()).orElse(""));

        context.complete();
    }

    /**
     * A Regra 17: o lote sabe para que lado fica a rua.
     *
     * <p>A direção sempre foi conhecida — a busca testa os quatro lados
     * e o primeiro que serve vence — e era jogada fora depois de
     * calcular o canto. É ela que diz em que parede a porta vai.
     *
     * <p>Rodado contra a regra desligada: não havia o que afirmar, o
     * lote não dizia lado nenhum.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "build_site")
    public void theLotKnowsWhichSideTheRoadIsOn(TestContext context) {
        BlockPos center = new BlockPos(3, 1, 3);

        paveGround(context, center);

        context.setBlockState(center, Blocks.DIRT_PATH.getDefaultState());

        Optional<BuildSiteScanner.Site> found = BuildSiteScanner.find(
                context.getWorld(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(center)),
                RADIUS,
                SMALL_HOUSE);

        context.assertTrue(found.isPresent(), "não achou lote ao lado da rua");

        BuildSiteScanner.Site site = found.get();

        // Andar do lote para o lado que ele aponta tem de dar na rua, de
        // alguma das colunas da face. Se der em qualquer outra coisa, a
        // porta vai para o mato.
        boolean reachesTheRoad = false;

        for (int dx = 0; dx < SMALL_HOUSE.x(); dx++) {
            for (int dz = 0; dz < SMALL_HOUSE.z(); dz++) {
                BlockPos cell = new BlockPos(
                        site.origin().x() + dx, site.origin().y() - 1, site.origin().z() + dz);

                if (context.getWorld().getBlockState(cell.offset(site.doorSide()))
                        .isOf(Blocks.DIRT_PATH)) {

                    reachesTheRoad = true;
                }
            }
        }

        context.assertTrue(
                reachesTheRoad,
                "o lote diz que a rua fica ao " + site.doorSide()
                        + ", e daquele lado não há caminho nenhum");

        context.complete();
    }

    /**
     * Chão de grama em volta do ponto, largo o bastante para caber a
     * casa em qualquer das quatro direções.
     */
    private static void paveGround(TestContext context, BlockPos center) {
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                context.setBlockState(
                        center.add(dx, 0, dz), Blocks.GRASS_BLOCK.getDefaultState());
            }
        }
    }
}
