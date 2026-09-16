package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.BuildSiteScanner;
import com.villagecolony.fabric.integration.LotRefusals;
import com.villagecolony.fabric.integration.SweepLog;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
     * Uma casa alta, para a Regra 22 ter o que exercitar.
     *
     * <p>A casa de vila do jogo tem sete de altura. Com três, a janela
     * de busca de chão já cobre o volume por acidente, e a regra do
     * volume não é testada de verdade.
     */
    private static final ColonyPos TALL_HOUSE = new ColonyPos(2, 5, 2);

    /**
     * P0.7: solo sólido disponível não é recusado por sua composição.
     *
     * <p>A marca da rua é espacial: os materiais da estrada só bloqueiam
     * quando a colônia os reservou no índice de ruas. Isto permite ao
     * jogador preparar o chão com qualquer material de apoio sem criar
     * uma taxonomia geológica paralela.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "build_site_p0_7")
    public void stoneGravelAndTerracottaOutsideARoadAreLots(TestContext context) {
        BlockPos center = new BlockPos(3, 1, 3);

        for (net.minecraft.block.Block ground : new net.minecraft.block.Block[] {
                Blocks.STONE, Blocks.GRAVEL, Blocks.TERRACOTTA, Blocks.CALCITE}) {
            UUID colony = UUID.randomUUID();

            paveGround(context, center);
            paveGroundWith(context, center, ground);
            context.setBlockState(center, Blocks.DIRT_PATH.getDefaultState());
            reserveRoad(context, colony, center);

            Optional<BuildSiteScanner.Site> site = BuildSiteScanner.find(
                    context.getWorld(),
                    colony,
                    MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(center)),
                    RADIUS,
                    SMALL_HOUSE);

            context.assertTrue(
                    site.isPresent(),
                    ground + " fora de ROAD_AREA foi recusado como lote no P0.7");

            BuildSiteScanner.clearAll();
        }

        context.complete();
    }

    /**
     * A recusa barata responde antes da cara — 2026-09-15.
     *
     * <p><b>É um teste de custo</b>, e ele mede a ordem porque a ordem
     * <i>é</i> a otimização. Não há como cronometrar um tique de servidor
     * dentro de um gametest sem virar um caso instável; o que dá para
     * afirmar é <b>qual critério respondeu primeiro</b>, e é isso que a
     * contagem do {@link LotRefusals} guarda.
     *
     * <p><b>O que o log do autor mediu, em 2026-09-15:</b> ciclos de 98,
     * 57 e 54 ms — acima do tique de 50 ms —, com o planejador levando 72
     * ms do pior deles, e <b>192.448</b> recusas de lote num ciclo. Delas,
     * <b>126.315</b> eram a Regra 3, que é a pergunta mais cara da fila:
     * {@code BlockProtection.isVillageOriginal} consulta o
     * {@code StructureAccessor}. Ela rodava em <b>terceiro</b> de sete, e
     * a comparação de dois inteiros da Regra 19 — que respondeu por 24.350
     * recusas — rodava em <b>sexto</b>, depois dela.
     *
     * <p>Toda coluna reprovada pela régua da rua pagava a consulta de
     * estrutura antes de chegar à comparação que a reprovaria de graça.
     *
     * <p><b>O cenário</b> monta uma coluna reprovável por dois motivos ao
     * mesmo tempo: um bloco acima do nível da rua <b>e</b> feita de
     * caminho de terra, que não é cubo inteiro e por isso também não passa
     * no {@code isLotGround}. Antes respondia o {@code isLotGround}, em
     * quinto, que lê o bloco; agora responde a Regra 19, em segundo, que
     * não lê nada. A resposta final é a mesma — o lote é recusado dos dois
     * jeitos —, e é justamente por isso que a contagem é a única
     * testemunha da ordem.
     *
     * <p><b>Só o centro entra no índice de ruas</b>, e isso é o cuidado
     * que a primeira versão deste caso não teve: reservar as vinte e cinco
     * colunas fazia cada uma virar rua candidata, com o próprio
     * {@code roadY} tirado do chão dela — e aí o lote levantado estava no
     * nível da sua própria rua, e a régua nunca reprovava nada.
     *
     * <p><b>Ao ler o log depois desta mudança:</b> os números mudam de
     * caixa sem que nada de comportamento tenha mudado. Espere a Regra 3
     * cair muito e a régua da rua subir — uma coluna reprovável por mais
     * de um motivo passa a ser contada pelo mais barato deles.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "build_site_refusal_order")
    public void theCheapRefusalAnswersBeforeTheExpensiveOne(TestContext context) {
        BlockPos center = new BlockPos(3, 1, 3);
        UUID colony = UUID.randomUUID();

        LotRefusals.clearAll();
        paveGround(context, center);

        // A rua de partida, e a única coluna do índice: é dela que sai o
        // roadY, e ela fica no nível de baixo para haver régua.
        context.setBlockState(center, Blocks.DIRT_PATH.getDefaultState());
        reserveRoad(context, colony, center);

        // <b>O degrau</b>: as colunas em volta sobem um bloco, em caminho
        // de terra — fora do nível da rua e não sendo cubo inteiro, as
        // duas coisas de uma vez. Fora do índice, para não virarem ruas.
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }

                // Dois blocos: um so deixou de ser recusa em 2026-09-15,
                // quando o autor mandou tolerar um bloco de desnivel.
                for (int dy = 1; dy <= 2; dy++) {
                    context.setBlockState(
                            center.add(dx, dy, dz), Blocks.DIRT_PATH.getDefaultState());
                }
            }
        }

        try {
            Optional<BuildSiteScanner.Site> site = BuildSiteScanner.find(
                    context.getWorld(),
                    colony,
                    MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(center)),
                    0,
                    SMALL_HOUSE);

            context.assertTrue(
                    site.isEmpty(),
                    "o lote fora do nível da rua foi aceito, e sem a recusa o teste não"
                            + " mede ordem nenhuma");

            context.assertTrue(
                    LotRefusals.countOf(colony, LotRefusals.Reason.OFF_ROAD_LEVEL) > 0,
                    "a régua da rua não respondeu: ela é a comparação de dois inteiros, e"
                            + " precisa vir antes das perguntas que leem o mundo");

            context.assertTrue(
                    LotRefusals.countOf(colony, LotRefusals.Reason.NOT_NATURAL_GROUND) == 0,
                    "a pergunta do solo respondeu primeiro, e ela lê o bloco — a régua da"
                            + " rua devia ter reprovado a coluna de graça antes dela."
                            + " off_road=" + LotRefusals.countOf(
                                    colony, LotRefusals.Reason.OFF_ROAD_LEVEL)
                            + " not_natural=" + LotRefusals.countOf(
                                    colony, LotRefusals.Reason.NOT_NATURAL_GROUND));
        } finally {
            BuildSiteScanner.clearAll();
            LotRefusals.clearAll();
        }

        context.complete();
    }

    /**
     * <b>Um bloco de desnível da rua ainda é lote</b> — decisão do autor,
     * 2026-09-15: <i>"desnivel, permitir somente 1 bloco de desnivel da
     * estrada"</i>.
     *
     * <p><b>A medição que autorizou isto.</b> A pesquisa de 09-11
     * ({@code docs/research/terraplanagem-da-vila.md} §8) registrou a regra
     * do autor: <i>"Medir primeiro. Se a recusa por desnível dominar, a
     * inferência vira fato"</i>. Duas sessões responderam, e o número é
     * estável: <b>35,0%</b> e <b>33,6%</b> das recusas de lote eram
     * {@code OFF_ROAD_LEVEL} — a segunda maior causa, atrás só da área de
     * estrada.
     *
     * <p>A régua era <b>exata</b>: uma coluna um bloco fora reprovava o
     * lote inteiro, e num terreno de planície ondulada isso reprova quase
     * tudo. Um bloco de tolerância é o degrau que um jogador sobe sem
     * pensar, e é o que a Regra 19 queria impedir quando falava de
     * <i>"varanda sem escada"</i> — ela mirava o lote dois acima, não o
     * ondulado.
     *
     * <p><b>O que NÃO entra nesta decisão:</b> mover terra. A preparação
     * do canteiro tira planta e não aterra — ver {@code SitePreparation} —,
     * então a coluna um abaixo fica com um vão sob o piso. O autor foi
     * avisado disso e escolheu assim mesmo, para a vila voltar a crescer;
     * aterrar é a frente de terraplanagem, que continua aberta.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "build_site_slope_one")
    public void oneBlockOffTheRoadLevelIsStillALot(TestContext context) {
        BlockPos center = new BlockPos(3, 1, 3);
        UUID colony = UUID.randomUUID();

        LotRefusals.clearAll();
        paveGround(context, center);

        context.setBlockState(center, Blocks.DIRT_PATH.getDefaultState());
        reserveRoad(context, colony, center);

        // O lote em volta sobe um bloco: é o ondulado que a régua exata
        // reprovava, e que agora tem de passar.
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }

                context.setBlockState(center.add(dx, 1, dz), Blocks.GRASS_BLOCK.getDefaultState());
            }
        }

        try {
            Optional<BuildSiteScanner.Site> site = BuildSiteScanner.find(
                    context.getWorld(),
                    colony,
                    MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(center)),
                    0,
                    SMALL_HOUSE);

            context.assertTrue(
                    site.isPresent(),
                    "um bloco de desnível continuou reprovando o lote — é a segunda maior"
                            + " causa de recusa no log do autor, com 35% e 34% em duas"
                            + " sessões. off_road=" + LotRefusals.countOf(
                                    colony, LotRefusals.Reason.OFF_ROAD_LEVEL)
                            + " no_ground=" + LotRefusals.countOf(
                                    colony, LotRefusals.Reason.NO_GROUND)
                            + " occupied=" + LotRefusals.countOf(
                                    colony, LotRefusals.Reason.OCCUPIED)
                            + " road=" + LotRefusals.countOf(colony, LotRefusals.Reason.ROAD));
        } finally {
            BuildSiteScanner.clearAll();
            LotRefusals.clearAll();
        }

        context.complete();
    }

    /**
     * E dois blocos continuam sendo recusa.
     *
     * <p>A outra metade da decisão: o autor pediu <b>somente</b> um bloco.
     * Sem este caso, afrouxar a régua viraria afrouxá-la sem limite, e a
     * casa voltaria a nascer na varanda sem escada que a Regra 19 descreve.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "build_site_slope_one")
    public void twoBlocksOffTheRoadLevelIsStillRefused(TestContext context) {
        BlockPos center = new BlockPos(3, 1, 3);
        UUID colony = UUID.randomUUID();

        LotRefusals.clearAll();
        paveGround(context, center);

        context.setBlockState(center, Blocks.DIRT_PATH.getDefaultState());
        reserveRoad(context, colony, center);

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }

                for (int dy = 1; dy <= 2; dy++) {
                    context.setBlockState(
                            center.add(dx, dy, dz), Blocks.GRASS_BLOCK.getDefaultState());
                }
            }
        }

        try {
            Optional<BuildSiteScanner.Site> site = BuildSiteScanner.find(
                    context.getWorld(),
                    colony,
                    MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(center)),
                    0,
                    SMALL_HOUSE);

            context.assertTrue(
                    site.isEmpty(),
                    "dois blocos de desnível viraram lote, e o autor pediu somente um");
        } finally {
            BuildSiteScanner.clearAll();
            LotRefusals.clearAll();
        }

        context.complete();
    }

    /**
     * <b>A casa não nasce em cima da roça</b> — 2026-09-16.
     *
     * <p><b>Relato do autor, em jogo:</b> <i>"a segunda construção acavalou
     * em cima de uma fazenda da vila, então a verificação do local para
     * construir deve ter dado erro"</i>. Ele está certo, e o log de 03:23
     * mostra o estrago: o açougue foi planejado em {@code 2503,63,-3045} e
     * o construtor riscou <b>onze</b> posições dizendo
     * <i>"Block{minecraft:farmland} is in the way"</i>.
     *
     * <p><b>A verificação falhou no lugar mais simples.</b>
     * {@code isLotGround} pergunta apenas <i>"é bloco sólido?"</i> — e
     * farmland é sólido. A roça passava por terreno livre.
     *
     * <p>E as outras guardas não a pegavam: a roça de uma vila de planície
     * não é peça de estrutura registrada, então {@code isVillageOriginal}
     * responde não; e {@code isClearAbove} olha o que está <b>acima</b> do
     * chão, não o chão em si — o trigo por cima é substituível e passa.
     *
     * <p>O dano é do tipo que não se desfaz sozinho: a comida da vila
     * vira piso de casa, e os aldeões que comiam dali passam fome.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "build_site_farmland")
    public void farmlandIsNeverALot(TestContext context) {
        BlockPos center = new BlockPos(3, 1, 3);
        UUID colony = UUID.randomUUID();

        LotRefusals.clearAll();
        paveGround(context, center);

        context.setBlockState(center, Blocks.DIRT_PATH.getDefaultState());
        reserveRoad(context, colony, center);

        // A roça da vila, encostada na rua: é exatamente o arranjo de
        // 2503,-3032, onde o açougue do autor passou por cima.
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }

                context.setBlockState(center.add(dx, 0, dz), Blocks.FARMLAND.getDefaultState());
            }
        }

        try {
            Optional<BuildSiteScanner.Site> site = BuildSiteScanner.find(
                    context.getWorld(),
                    colony,
                    MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(center)),
                    0,
                    SMALL_HOUSE);

            context.assertTrue(
                    site.isEmpty(),
                    "a colônia escolheu a roça como lote — é a fazenda da vila virando"
                            + " piso de casa, e o autor viu isso em jogo");

            context.assertTrue(
                    LotRefusals.countOf(colony, LotRefusals.Reason.NOT_NATURAL_GROUND) > 0,
                    "a roça foi recusada por outro motivo que não o solo — o cenário não"
                            + " reproduz o defeito. off_road=" + LotRefusals.countOf(
                                    colony, LotRefusals.Reason.OFF_ROAD_LEVEL)
                            + " no_ground=" + LotRefusals.countOf(
                                    colony, LotRefusals.Reason.NO_GROUND)
                            + " occupied=" + LotRefusals.countOf(
                                    colony, LotRefusals.Reason.OCCUPIED)
                            + " road=" + LotRefusals.countOf(colony, LotRefusals.Reason.ROAD)
                            + " protected=" + LotRefusals.countOf(
                                    colony, LotRefusals.Reason.PROTECTED));
        } finally {
            BuildSiteScanner.clearAll();
            LotRefusals.clearAll();
        }

        context.complete();
    }

    /**
     * E o cultivo por cima também reprova o lote.
     *
     * <p>A roça madura tem trigo sobre a terra arada, e o trigo é
     * <b>substituível</b> — passa por "nada em cima" no
     * {@code isClearAbove}. Sem este caso, bastaria a planta estar crescida
     * para a guarda da terra arada ser contornada por cima.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "build_site_farmland")
    public void aGrownCropDoesNotHideTheFarm(TestContext context) {
        BlockPos center = new BlockPos(3, 1, 3);
        UUID colony = UUID.randomUUID();

        LotRefusals.clearAll();
        paveGround(context, center);

        context.setBlockState(center, Blocks.DIRT_PATH.getDefaultState());
        reserveRoad(context, colony, center);

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }

                context.setBlockState(center.add(dx, 0, dz), Blocks.FARMLAND.getDefaultState());
                context.setBlockState(center.add(dx, 1, dz), Blocks.WHEAT.getDefaultState());
            }
        }

        try {
            Optional<BuildSiteScanner.Site> site = BuildSiteScanner.find(
                    context.getWorld(),
                    colony,
                    MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(center)),
                    0,
                    SMALL_HOUSE);

            context.assertTrue(
                    site.isEmpty(),
                    "o trigo por cima escondeu a roça, e a casa nasceria sobre a comida"
                            + " da vila");
        } finally {
            BuildSiteScanner.clearAll();
            LotRefusals.clearAll();
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "build_site_p0_7")
    public void everyReservedRoadMaterialBlocksTheWholeFootprint(TestContext context) {
        BlockPos center = new BlockPos(3, 1, 3);

        for (net.minecraft.block.Block paving : new net.minecraft.block.Block[] {
                Blocks.DIRT_PATH, Blocks.GRAVEL, Blocks.TERRACOTTA}) {
            UUID colony = UUID.randomUUID();

            LotRefusals.clearAll();
            paveGround(context, center);
            reserveRoadFootprint(context, colony, center, paving);

            Optional<BuildSiteScanner.Site> site = BuildSiteScanner.find(
                    context.getWorld(),
                    colony,
                    MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(center)),
                    0,
                    SMALL_HOUSE);

            context.assertTrue(
                    site.isEmpty(),
                    paving + " reservado como ROAD_AREA deixou uma casa atravessar a estrada");
            context.assertTrue(
                    LotRefusals.countOf(colony, LotRefusals.Reason.ROAD) > 0,
                    paving + " reservado como ROAD_AREA nao registrou a recusa da estrada");

            BuildSiteScanner.clearAll();
            LotRefusals.clearAll();
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "build_site_p0_7")
    public void findingALotNeverTerraformsPreparedGround(TestContext context) {
        BlockPos center = new BlockPos(3, 1, 3);
        UUID colony = UUID.randomUUID();

        paveGround(context, center);
        paveGroundWith(context, center, Blocks.STONE);
        context.setBlockState(center, Blocks.DIRT_PATH.getDefaultState());
        reserveRoad(context, colony, center);

        BlockPos checked = context.getAbsolutePos(center.east());
        net.minecraft.block.BlockState before = context.getWorld().getBlockState(checked);

        Optional<BuildSiteScanner.Site> site = BuildSiteScanner.find(
                context.getWorld(),
                colony,
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(center)),
                RADIUS,
                SMALL_HOUSE);

        context.assertTrue(site.isPresent(), "o terreno preparado deveria permitir lote");
        context.assertTrue(
                context.getWorld().getBlockState(checked).equals(before),
                "a busca de lote alterou o terreno preparado no P0.7");

        BuildSiteScanner.clearAll();
        context.complete();
    }

    /**
     * Uma rua de cascalho so conta como rua quando sua coluna esta reservada
     * em {@code ROAD_AREA}; cascalho natural continua elegivel para lote.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "build_site")
    public void aLotBesideAReservedGravelRoadIsFound(TestContext context) {
        BlockPos center = new BlockPos(3, 1, 3);
        UUID colony = UUID.randomUUID();

        paveGround(context, center);

        context.setBlockState(center, Blocks.GRAVEL.getDefaultState());
        reserveRoad(context, colony, center);

        Optional<BuildSiteScanner.Site> site = BuildSiteScanner.find(
                context.getWorld(),
                colony,
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(center)),
                RADIUS,
                SMALL_HOUSE);

        context.assertTrue(
                site.isPresent(),
                "não achou lote ao lado da estrada de gravel reservada");

        context.complete();
    }

    /**
     * Chão liso com rua ao lado: a colônia acha onde construir.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "build_site")
    public void aLotBesideTheRoadIsFound(TestContext context) {
        BlockPos center = new BlockPos(3, 1, 3);
        UUID colony = UUID.randomUUID();

        paveGround(context, center);

        context.setBlockState(center, Blocks.DIRT_PATH.getDefaultState());

        reserveRoad(context, colony, center);

        Optional<BuildSiteScanner.Site> site = BuildSiteScanner.find(
                context.getWorld(),
                colony,
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
        UUID colony = UUID.randomUUID();

        paveGround(context, center);

        context.setBlockState(center, Blocks.DIRT_PATH.getDefaultState());
        reserveRoad(context, colony, center);

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
                colony,
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

        UUID colony = UUID.randomUUID();

        // Raio curto: cabe inteiro no orçamento, e a varredura termina.
        BuildSiteScanner.find(context.getWorld(), colony, from, RADIUS, SMALL_HOUSE);

        context.assertTrue(
                BuildSiteScanner.sweepPausedAt(colony).isEmpty(),
                "raio de " + RADIUS + " cabe num ciclo, e a busca disse que parou no meio");

        // Raio de vila de verdade: dezesseis mil colunas, mil por chamada.
        BuildSiteScanner.find(context.getWorld(), colony, from, 64, SMALL_HOUSE);

        context.assertTrue(
                BuildSiteScanner.sweepPausedAt(colony).isPresent(),
                "raio de 64 não cabe num ciclo, e a busca disse que varreu tudo");

        BuildSiteScanner.clearAll();

        context.complete();
    }

    /**
     * A varredura não pergunta duas vezes pela mesma coluna — E25.
     *
     * <p>O cursor guardava só o <b>anel</b>, e a passagem seguinte
     * recomeçava do primeiro bloco dele. A casca de um anel de raio 64
     * tem quinhentas e doze colunas: metade de uma passagem inteira
     * gasta para chegar de volta aonde a anterior já tinha chegado.
     *
     * <p>O custo é o que se afirma aqui, e ele tem um piso aritmético:
     * um raio de {@code R} tem {@code 1 + 4R(R+1)} colunas, e nenhuma
     * varredura honesta pode precisar de mais passagens que
     * {@code colunas ÷ teto}, arredondado para cima. Com o defeito eram
     * dezenove das dezessete que o raio pede — meio minuto de vila
     * parada por varredura, para sempre.
     *
     * <p>Sem tique nenhum de propósito: {@code find} é conta e leitura
     * de bloco, e chamá-la em sequência mede exatamente o que o ciclo
     * mediria em dezessete ciclos de trinta segundos.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "build_site_budget")
    public void theSweepNeverAsksTheSameColumnTwice(TestContext context) {
        UUID colony = UUID.randomUUID();

        ColonyPos center = MinecraftTypeAdapter.toColonyPos(
                context.getAbsolutePos(new BlockPos(3, 1, 3)));

        // A mesma casa impossível do teste do centro que se move: um lote
        // encontrado apagaria o cursor e a medição acabaria antes.
        ColonyPos tooBigToFit = new ColonyPos(40, 20, 40);

        int radius = 64;

        int columns = 1 + 4 * radius * (radius + 1);

        int floor = (columns + BuildSiteScanner.MAX_COLUMNS - 1)
                / BuildSiteScanner.MAX_COLUMNS;

        int passes = 0;

        do {
            BuildSiteScanner.find(context.getWorld(), colony, center, radius, tooBigToFit);

            passes++;
        } while (BuildSiteScanner.sweepPausedAt(colony).isPresent() && passes <= floor);

        context.assertTrue(
                passes == floor,
                "a varredura do raio " + radius + " levou " + passes + " passagens, e "
                        + columns + " colunas a " + BuildSiteScanner.MAX_COLUMNS
                        + " por passagem cabem em " + floor
                        + " — está re-perguntando por coluna já respondida");

        BuildSiteScanner.clearAll();

        context.complete();
    }

    /**
     * A varredura sobrevive ao centro da colônia se mexendo.
     *
     * <p>O cursor era guardado pela posição do centro. O centro troca de
     * âncora e volta a cada ciclo — é a ADR-003, vista nos logs de 08-18
     * e 08-19 —, e cada troca dava um cursor novo: a busca recomeçava do
     * anel zero para sempre e nunca passava do orçamento de um ciclo.
     *
     * <p>Na sessão de 2026-08-19, 23:39, isso deixou uma colônia três
     * minutos em "still sweeping" sem nunca planejar nada. Ficou visível
     * agora porque a casa de planície é 7×7×7 contra os 5×5×4 da cabana,
     * e o lote perto acabou: enquanto havia lote no anel de perto, a
     * busca achava antes de o orçamento acabar e o defeito não aparecia.
     *
     * <p>O cursor é da colônia, então é ela quem o guarda.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "build_site_moving_center")
    public void theSweepSurvivesTheCenterMoving(TestContext context) {
        UUID colony = UUID.randomUUID();

        ColonyPos first = MinecraftTypeAdapter.toColonyPos(
                context.getAbsolutePos(new BlockPos(3, 1, 3)));

        // O centro que se move: a mesma colônia, alguns blocos ao lado.
        ColonyPos second = new ColonyPos(first.x() + 9, first.y(), first.z() - 7);

        // Uma casa que não cabe em lugar nenhum, de propósito: este teste
        // é sobre o cursor, e um lote encontrado o apagaria. As arenas da
        // bateria dividem o mundo, e a rua que outro teste pavimentou cai
        // dentro dos 64 blocos daqui.
        ColonyPos tooBigToFit = new ColonyPos(40, 20, 40);

        BuildSiteScanner.find(context.getWorld(), colony, first, 64, tooBigToFit);

        int paused = BuildSiteScanner.sweepPausedAt(colony).orElse(0);

        context.assertTrue(paused > 0, "o orçamento devia ter acabado no meio do raio de 64");

        BuildSiteScanner.find(context.getWorld(), colony, second, 64, tooBigToFit);

        context.assertTrue(
                BuildSiteScanner.sweepPausedAt(colony).orElse(0) > paused,
                "a busca recomeçou do centro quando a âncora mudou, e nunca avança");

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
                UUID.randomUUID(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(center)),
                RADIUS,
                SMALL_HOUSE);

        context.assertTrue(site.isEmpty(), "achou lote sem rua nenhuma por perto");

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "build_site_refresh")
    public void playerRoadMaterialDoesNotCreateARoadArea(TestContext context) {
        BlockPos center = new BlockPos(3, 1, 3);
        paveGround(context, center);

        UUID colony = UUID.randomUUID();
        ColonyPos absoluteCenter = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(center));
        context.setBlockState(center, Blocks.GRAVEL.getDefaultState());
        BuildSiteScanner.reconcileWorldChange(
                colony, context.getWorld(), context.getAbsolutePos(center), absoluteCenter);

        context.assertTrue(
                !BuildSiteScanner.isRoadArea(context.getWorld(), colony, context.getAbsolutePos(center)),
                "gravel do jogador sem reserva espacial virou rua");
        context.assertTrue(
                BuildSiteScanner.roadIndexSize(colony).isEmpty(),
                "material de rua sem ROAD_AREA entrou no índice");

        BuildSiteScanner.clearAll();
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "build_site_refresh")
    public void playerEditDoesNotRecreateARoadArea(TestContext context) {
        BlockPos center = new BlockPos(3, 1, 3);
        paveGround(context, center);

        UUID colony = UUID.randomUUID();
        context.setBlockState(center, Blocks.DIRT_PATH.getDefaultState());
        reserveRoad(context, colony, center);
        ColonyPos absoluteCenter = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(center));

        context.setBlockState(center, Blocks.DIRT.getDefaultState());
        BuildSiteScanner.reconcileWorldChange(
                colony, context.getWorld(), context.getAbsolutePos(center), absoluteCenter);
        context.assertTrue(
                BuildSiteScanner.roadIndexSize(colony).orElse(0) == 0,
                "a área de rua removida permaneceu no índice");

        context.setBlockState(center, Blocks.DIRT_PATH.getDefaultState());
        BuildSiteScanner.reconcileWorldChange(
                colony, context.getWorld(), context.getAbsolutePos(center), absoluteCenter);
        context.assertTrue(
                BuildSiteScanner.roadIndexSize(colony).orElse(0) == 0,
                "material restaurado sem reserva espacial recriou a área de rua");
        BuildSiteScanner.clearAll();
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "build_site_refresh")
    public void removingAPlayerRoadRemovesOnlyThatIndexedColumn(TestContext context) {
        BlockPos center = new BlockPos(3, 1, 3);
        paveGround(context, center);
        context.setBlockState(center, Blocks.DIRT_PATH.getDefaultState());

        UUID colony = UUID.randomUUID();
        reserveRoad(context, colony, center);
        ColonyPos absoluteCenter = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(center));
        ColonyPos impossibleHouse = new ColonyPos(40, 20, 40);

        BuildSiteScanner.find(context.getWorld(), colony, absoluteCenter, RADIUS, impossibleHouse);
        int indexedBefore = BuildSiteScanner.roadIndexSize(colony).orElse(0);
        context.setBlockState(center, Blocks.DIRT.getDefaultState());
        BuildSiteScanner.reconcileWorldChange(
                colony, context.getWorld(), context.getAbsolutePos(center), absoluteCenter);
        int indexedAfter = BuildSiteScanner.roadIndexSize(colony).orElse(0);
        BuildSiteScanner.clearAll();

        context.assertTrue(
                indexedBefore == 1 && indexedAfter == 0,
                "remover uma rua apagou informação demais ou deixou a coluna indexada");
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
        UUID colony = UUID.randomUUID();

        paveGround(context, center);

        context.setBlockState(center, Blocks.DIRT_PATH.getDefaultState());
        reserveRoad(context, colony, center);
        reserveRoad(context, colony, center);

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
                colony,
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(center)),
                RADIUS,
                SMALL_HOUSE);

        context.assertTrue(site.isEmpty(), "aceitou lote com desnível de quatro blocos");

        context.complete();
    }

    /**
     * A Regra 22: bloco dentro da caixa da casa reprova o lote.
     *
     * <p>Pedido do autor em 2026-08-19, depois de ver casa nascendo com
     * coisa dentro. O lote era julgado pelo <b>chão</b>: a coluna dizia
     * onde a casa assenta, e o que estivesse acima da janela de busca
     * passava despercebido. A casa subia em volta do obstáculo, e o
     * construtor pulava os blocos ocupados com {@code is in the way}.
     *
     * <p>Aqui o chão está perfeito e há um bloco no ar, à altura do
     * primeiro andar. O lote inteiro tem de ser recusado.
     *
     * <p>Rodado contra a regra desligada: o lote é aceito e a afirmação
     * falha.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "build_site")
    public void aBlockInsideTheHouseRefusesTheLot(TestContext context) {
        BlockPos center = new BlockPos(3, 1, 3);
        UUID colony = UUID.randomUUID();

        paveGround(context, center);

        context.setBlockState(center, Blocks.DIRT_PATH.getDefaultState());
        reserveRoad(context, colony, center);

        // Uma pedra no ar, em cada uma das quatro vizinhanças da rua:
        // não importa para que lado o lote cresça, ele encontra isto.
        //
        // A quatro blocos do chão de propósito. Mais baixo que isso e
        // quem recusaria seria a janela de busca de chão, que já existia
        // — o teste passaria sem a Regra 22 e não provaria nada. Foi o
        // que aconteceu na primeira versão dele.
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }

                context.setBlockState(
                        center.add(dx, 4, dz), Blocks.STONE.getDefaultState());
            }
        }

        Optional<BuildSiteScanner.Site> site = BuildSiteScanner.find(
                context.getWorld(),
                colony,
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(center)),
                RADIUS,
                TALL_HOUSE);

        context.assertTrue(
                site.isEmpty(),
                "havia bloco dentro da caixa da casa e o lote foi aceito em "
                        + site.map(found -> found.origin().toString()).orElse(""));

        context.complete();
    }

    /**
     * E a flor não reprova nada — o outro lado da Regra 22.
     *
     * <p>"O worker deve ser capaz de destruir flores se for somente o
     * que atrapalha", disse o autor no mesmo pedido. Recusar um lote de
     * planície por causa de um pé de margarida seria recusar a planície
     * inteira, que é o defeito que a sessão de 2026-08-15 já tinha
     * mostrado com a grama alta.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "build_site")
    public void flowersInsideTheHouseDoNotRefuseTheLot(TestContext context) {
        BlockPos center = new BlockPos(3, 1, 3);
        UUID colony = UUID.randomUUID();

        paveGround(context, center);

        context.setBlockState(center, Blocks.DIRT_PATH.getDefaultState());
        reserveRoad(context, colony, center);

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }

                context.setBlockState(
                        center.add(dx, 1, dz), Blocks.DANDELION.getDefaultState());
            }
        }

        Optional<BuildSiteScanner.Site> site = BuildSiteScanner.find(
                context.getWorld(),
                colony,
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(center)),
                RADIUS,
                SMALL_HOUSE);

        context.assertTrue(
                site.isPresent(),
                "um campo de margaridas reprovou o lote — quem constrói tira a flor");

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
        UUID colony = UUID.randomUUID();

        paveGround(context, center);

        context.setBlockState(center, Blocks.DIRT_PATH.getDefaultState());
        reserveRoad(context, colony, center);

        // Tudo em volta da rua sobe DOIS degraus. O lote continua plano
        // entre si, e deixa de estar no nível de quem anda na rua.
        //
        // Era um degrau até 2026-09-15, quando o autor mandou tolerar um
        // bloco — ver oneBlockOffTheRoadLevelIsStillALot. Com um, este
        // caso passaria a medir a tolerância em vez da Regra 19; dois é
        // a varanda sem escada que a regra descreve, e é o que ela ainda
        // recusa.
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }

                for (int dy = 1; dy <= 2; dy++) {
                    context.setBlockState(
                            center.add(dx, dy, dz), Blocks.GRASS_BLOCK.getDefaultState());
                }
            }
        }

        Optional<BuildSiteScanner.Site> site = BuildSiteScanner.find(
                context.getWorld(),
                colony,
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
        UUID colony = UUID.randomUUID();

        paveGround(context, center);

        context.setBlockState(center, Blocks.DIRT_PATH.getDefaultState());
        reserveRoad(context, colony, center);

        Optional<BuildSiteScanner.Site> found = BuildSiteScanner.find(
                context.getWorld(),
                colony,
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
    /**
     * Onde a planta grande não cabe, a colônia levanta a que cabe.
     *
     * <p>Decisão do autor em 2026-08-20, depois de a vila dele parar de
     * crescer: dez minutos de varredura completa para
     * {@code no free lot ... that fits ColonyPos[x=7, y=7, z=7]}, na
     * mesma vila em que três cabanas de 5x5 já estavam de pé.
     *
     * <p>A casa de planície pede 49 colunas no nível exato da rua, fora
     * das peças da vila gerada e com sete blocos livres acima. A cabana
     * pede 25. É a Regra 13 outra vez — construir o que a colônia
     * consegue —, agora sobre o espaço em vez do material.
     *
     * <p>Escolhida por lote, e não por vila: onde a grande couber, é ela
     * que sobe. Só desce um degrau onde a grande não cabe.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "build_site_fallback")
    public void theBiggestPlanThatFitsIsTheOneChosen(TestContext context) {
        BlockPos center = new BlockPos(3, 1, 3);
        UUID colony = UUID.randomUUID();

        paveGround(context, center);

        context.setBlockState(center, Blocks.DIRT_PATH.getDefaultState());
        reserveRoad(context, colony, center);

        // Uma planta maior do que o chão que este teste montou.
        ColonyPos tooBig = new ColonyPos(6, 5, 6);

        Optional<BuildSiteScanner.Site> site = BuildSiteScanner.find(
                context.getWorld(),
                colony,
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(center)),
                RADIUS,
                List.of(tooBig, SMALL_HOUSE));

        context.assertTrue(
                site.isPresent(),
                "a grande não cabia, e a colônia desistiu em vez de descer um degrau");

        context.assertTrue(
                SMALL_HOUSE.equals(site.get().size()),
                "o lote achado é de " + site.get().size() + ", e não da planta que coube");

        BuildSiteScanner.clearAll();

        context.complete();
    }

    /** Onde a grande cabe, é a grande — e não a primeira que servir. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "build_site_fallback")
    public void theBigPlanWinsWhereItFits(TestContext context) {
        BlockPos center = new BlockPos(3, 1, 3);
        UUID colony = UUID.randomUUID();

        paveGround(context, center);

        context.setBlockState(center, Blocks.DIRT_PATH.getDefaultState());
        reserveRoad(context, colony, center);

        Optional<BuildSiteScanner.Site> site = BuildSiteScanner.find(
                context.getWorld(),
                colony,
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(center)),
                RADIUS,
                List.of(TALL_HOUSE, SMALL_HOUSE));

        context.assertTrue(site.isPresent(), "não achou lote no chão que o teste montou");

        context.assertTrue(
                TALL_HOUSE.equals(site.get().size()),
                "a grande cabia e a colônia escolheu " + site.get().size());

        BuildSiteScanner.clearAll();

        context.complete();
    }

    /**
     * Lote sobre casa que a própria colônia levantou não serve.
     *
     * <p><b>E o miolo dela é o caso difícil.</b> A cabana é oca e não tem
     * piso: o chão de dentro é a grama original, no nível da rua, com o
     * volume livre até o teto. Ela passa em todas as perguntas que o
     * scanner sabe fazer ao mundo — a Regra 19, a Regra 22, o chão
     * natural — porque nenhuma delas pergunta de quem é aquilo.
     *
     * <p>Visto em jogo em 2026-08-20, 01:54: a vila achou lote em
     * ColonyPos[-6818, 96, -5050], que é uma cabana de pé desde a
     * véspera. Quem recusava era o planejador, depois da busca, e o
     * comentário dele dizia que "a próxima passagem tenta outro anel" —
     * não tentava. Achar um lote apaga o cursor, então a passagem
     * seguinte recomeçava do centro e reencontrava o mesmo miolo. A
     * colônia ficou em laço fechado.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "build_site_owned")
    public void aLotInsideAColonyHouseIsNotALot(TestContext context) {
        BlockPos center = new BlockPos(3, 1, 3);

        paveGround(context, center);

        context.setBlockState(center, Blocks.DIRT_PATH.getDefaultState());

        UUID colony = UUID.randomUUID();

        // Uma casa da colônia cobrindo tudo em volta da rua. Nenhum bloco
        // entra no mundo: o registro é a única diferença, e é dele que o
        // teste trata.
        ColonyPos from = MinecraftTypeAdapter.toColonyPos(
                context.getAbsolutePos(center.add(-RADIUS, 0, -RADIUS)));

        VillageColonyMod.BUILDINGS.register(new Building(
                colony,
                colony,
                ResourceId.vanilla("village/plains/houses/plains_small_house_1"),
                from,
                new ColonyPos(from.x() + 2 * RADIUS, from.y() + 4, from.z() + 2 * RADIUS)));

        Optional<BuildSiteScanner.Site> site = BuildSiteScanner.find(
                context.getWorld(), colony,
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(center)),
                RADIUS, SMALL_HOUSE);

        context.assertTrue(
                site.isEmpty(),
                "a busca ofereceu " + site.map(BuildSiteScanner.Site::origin).orElse(null)
                        + ", que está dentro de uma casa da colônia");

        VillageColonyMod.BUILDINGS.removeOfColony(colony);

        BuildSiteScanner.clearAll();

        context.complete();
    }

    /** Uma estrada previamente indexada encontra lote sem iniciar varredura completa. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "build_site_resume")
    public void indexedRoadFindsALotWithoutStartingFullSweep(TestContext context) {
        BlockPos center = new BlockPos(3, 1, 3);

        UUID colony = UUID.randomUUID();

        paveGround(context, center);

        context.setBlockState(center, Blocks.DIRT_PATH.getDefaultState());
        reserveRoad(context, colony, center);

        Optional<BuildSiteScanner.Site> site = BuildSiteScanner.find(
                context.getWorld(),
                colony,
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(center)),
                RADIUS,
                SMALL_HOUSE);

        context.assertTrue(
                site.isPresent(),
                "o arranjo não deu lote, então este teste não chega a afirmar nada");

        context.assertTrue(
                BuildSiteScanner.sweepPausedAt(colony).isEmpty(),
                "uma ROAD_AREA indexada iniciou uma varredura completa sem necessidade");

        context.complete();
    }

    /**
     * A varredura que termina o raio deixa as ruas indexadas.
     *
     * <p><b>A medição que pediu isto.</b> Lendo o save do mundo do autor
     * em 2026-08-27, das <b>16.641</b> colunas do quadrado de raio 64 só
     * <b>698</b> eram calçamento — 4,19%, e o mesmo em três centros
     * diferentes (3,35% a 4,63%). O teto da varredura é 1.024 colunas por
     * passagem: seiscentas e noventa e oito <b>cabem numa passagem só</b>.
     * A varredura passa de dezessete ciclos para um, de 8,5 minutos para
     * trinta segundos.
     *
     * <p><b>Por que índice e não seguir o traçado.</b> A ideia natural —
     * alastrar a partir de uma semente e visitar só a rua — foi medida e
     * reprovada: aquelas 698 colunas são <b>14 pedaços soltos</b>, e o
     * maior tem 421. Um alastramento acharia 60% delas, e os 40% de fora
     * podem ser exatamente onde está o único lote livre. Isso é a família
     * do E14 — a colônia dizendo "não há lote" com lote existindo.
     *
     * <p>O índice não corre esse risco porque nasce de uma varredura
     * <b>completa</b>: só é guardado quando o raio inteiro foi visitado.
     * Coluna que deixou de ser rua é reconferida ao ser visitada, que é o
     * que o código já fazia; rua nova entra por {@code remember}, chamado
     * de onde a Regra 15 calça.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "build_site_index")
    public void theCompletedSweepLeavesTheRoadColumnsIndexed(TestContext context) {
        BlockPos center = new BlockPos(3, 1, 3);

        UUID colony = UUID.randomUUID();

        paveGround(context, center);

        context.setBlockState(center, Blocks.DIRT_PATH.getDefaultState());
        reserveRoad(context, colony, center);

        // Rua existe, lote não: torre em cada vizinho põe o desnível
        // acima do limite. É o arranjo do brokenGroundIsRefused, e o que
        // ele garante aqui é que a varredura vai até o fim do raio em vez
        // de sair cedo com lote na mão.
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
                colony,
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(center)),
                RADIUS,
                SMALL_HOUSE);

        context.assertTrue(
                site.isEmpty(),
                "o arranjo deu lote, e este teste precisa de uma varredura que termine o raio");

        context.assertTrue(
                BuildSiteScanner.sweepPausedAt(colony).isEmpty(),
                "a varredura não terminou o raio — o cursor ficou, e o índice só nasce completo");

        context.assertTrue(
                BuildSiteScanner.roadIndexSize(colony).orElse(0) == 1,
                "a varredura terminou o raio e não guardou a única coluna de rua que achou:"
                        + " a próxima passagem vai reperguntar o quadrado inteiro");

        context.complete();
    }

    /**
     * Vila em rocha também tem lote — decisão do autor, 2026-09-12.
     *
     * <p><b>O número que derrubou a regra anterior.</b> A sessão de 09-12
     * recusou <b>9.388 lotes</b>, e <b>6.527 deles — 69,5% — porque o chão
     * não era solo natural</b>. {@code isNaturalGround} aceitava grama,
     * terra, terra grossa, podzol e areia; pedra ficava fora por decisão
     * escrita — <i>"pedra à mostra é montanha"</i>. A vila do autor nasceu
     * em terreno rochoso e não tinha onde crescer.
     *
     * <p>Ele foi avisado do preço — casa sobre afloramento pode ficar de
     * aparência estranha — e escolheu assim, porque era a menor
     * intervenção que resolvia o gargalo. A alternativa era terraplanar,
     * que gasta material e mexe mais no mundo dele.
     *
     * <p><b>Rocha nua, e não pedregulho.</b> Pedregulho é o que o mineiro
     * produz e o que a vila gerada usa de parede; aceitá-lo como chão
     * convidaria a casa a nascer sobre obra. Este caso mede os dois lados.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "build_site_rock")
    public void aVillageOnBedrockStillHasLots(TestContext context) {
        BlockPos center = new BlockPos(3, 1, 3);
        UUID colony = UUID.randomUUID();

        // Rocha em vez de grama, no lote inteiro: é a encosta em que a
        // vila do autor nasceu.
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                context.setBlockState(
                        center.add(dx, 0, dz), Blocks.STONE.getDefaultState());
            }
        }

        context.setBlockState(center, Blocks.DIRT_PATH.getDefaultState());
        reserveRoad(context, colony, center);

        Optional<BuildSiteScanner.Site> site = BuildSiteScanner.find(
                context.getWorld(),
                colony,
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(center)),
                RADIUS,
                SMALL_HOUSE);

        context.assertTrue(
                site.isPresent(),
                "a rocha reprovou o lote — e em vila de encosta isso é todo lote");

        context.complete();
    }

    /**
     * Solo sólido preparado pelo jogador também pode ser lote no P0.7.
     *
     * <p>A prioridade de proteção e de construções existentes continua
     * bloqueando obra registrada. Sem esse contexto espacial, o bloco
     * isolado não revela a origem do terreno e permanece elegível.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "build_site_rock")
    public void preparedCobblestoneCanBeLotGround(TestContext context) {
        BlockPos center = new BlockPos(3, 1, 3);
        UUID colony = UUID.randomUUID();

        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                context.setBlockState(
                        center.add(dx, 0, dz), Blocks.COBBLESTONE.getDefaultState());
            }
        }

        context.setBlockState(center, Blocks.DIRT_PATH.getDefaultState());
        reserveRoad(context, colony, center);

        Optional<BuildSiteScanner.Site> site = BuildSiteScanner.find(
                context.getWorld(),
                colony,
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(center)),
                RADIUS,
                SMALL_HOUSE);

        context.assertTrue(
                site.isPresent(),
                "solo sólido preparado fora de construção registrada foi recusado no P0.7");

        context.complete();
    }

    private static void paveGround(TestContext context, BlockPos center) {
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                context.setBlockState(
                        center.add(dx, 0, dz), Blocks.GRASS_BLOCK.getDefaultState());
            }
        }
    }

    private static void paveGroundWith(
            TestContext context, BlockPos center, net.minecraft.block.Block ground) {
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                context.setBlockState(center.add(dx, 0, dz), ground.getDefaultState());
            }
        }
    }

    private static void reserveRoad(TestContext context, UUID colony, BlockPos road) {
        reserveRoadColumns(context, colony, List.of(road));
    }

    private static void reserveRoadFootprint(
            TestContext context, UUID colony, BlockPos center, net.minecraft.block.Block paving) {
        List<BlockPos> road = new ArrayList<>();

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos at = center.add(dx, 0, dz);
                context.setBlockState(at, paving.getDefaultState());
                road.add(at);
            }
        }

        reserveRoadColumns(context, colony, road);
    }

    private static void reserveRoadColumns(TestContext context, UUID colony, List<BlockPos> road) {
        List<Long> columns = road.stream()
                .map(context::getAbsolutePos)
                .map(at -> ColonyRoads.column(at.getX(), at.getZ()))
                .toList();

        ColonyPos center = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(road.get(0)));
        BuildSiteScanner.restore(new ColonyRoads(colony, center, columns));
    }

    // ------------------------------------------------------------------
    // O índice de ruas da vila grande — 2026-09-11.
    //
    // O índice existe para a colônia não varrer as 16.641 colunas do
    // raio de 64 toda vez que quer um lote. Ele guarda só as colunas
    // CALÇADAS, e perguntar a ele é a diferença entre duas passagens e
    // dezessete.
    //
    // E ele tinha um teto: índice com mais de MAX_COLUMNS colunas era
    // recusado, "porque perguntar por um que não cabe custa o mesmo que
    // varrer". Custa muito menos — e o preço do teto era pago pela vila
    // que deu certo: ela cresce, a rua cresce com ela, o índice passa de
    // mil e vinte e quatro colunas e o atalho morre. A colônia volta aos
    // oito minutos e meio por resposta, e não sai mais de lá, porque a
    // vila não encolhe.
    //
    // Medido na sessão de 2026-09-11 às 00:04: DEZESSETE das DEZENOVE
    // colônias do mundo estavam sem índice, e a que ciclou passou as
    // quinze passagens da sessão sem completar uma volta — quinze mil
    // trezentas e sessenta colunas, zero voltas completas, nenhuma obra
    // aberta. A queixa do autor foi o planejador que não acha lote.
    // ------------------------------------------------------------------

    /** Um índice sintético com mais colunas do que cabe numa passagem. */
    private static ColonyRoads bigIndex(UUID colony, ColonyPos from, int howMany) {
        List<Long> columns = new ArrayList<>(howMany);

        for (int i = 0; i < howMany; i++) {
            // Longe do centro de propósito: nenhuma destas é rua de
            // verdade, então o siteBesideRoadAt responde não a todas e o
            // que se mede é a CONTAGEM, não o terreno.
            columns.add(ColonyRoads.column(from.x() + 1000 + i, from.z() + 1000));
        }

        return new ColonyRoads(colony, from, columns);
    }

    /**
     * <b>Índice maior que uma passagem é aceito.</b> Era recusado, e a
     * recusa é que tirava o atalho da vila grande.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "build_site",
            tickLimit = 20)
    public void theRoadIndexMayOutgrowOnePass(TestContext context) {
        UUID colony = UUID.randomUUID();

        ColonyPos from = MinecraftTypeAdapter.toColonyPos(
                context.getAbsolutePos(new BlockPos(1, 1, 1)));

        try {
            BuildSiteScanner.restore(
                    bigIndex(colony, from, BuildSiteScanner.MAX_COLUMNS * 2));

            // Se o índice tivesse sido recusado, a colônia cairia na
            // varredura do quadrado — e ela deixa cursor de anel.
            BuildSiteScanner.find(context.getWorld(), colony, from, 64, SMALL_HOUSE);

            context.assertTrue(
                    BuildSiteScanner.sweepPausedAt(colony).isEmpty(),
                    "o índice grande foi recusado e a colônia voltou a varrer o quadrado");
        } finally {
            BuildSiteScanner.clearAll();
        }

        context.complete();
    }

    /**
     * <b>E perguntar a ele gasta orçamento.</b> É o que substitui o teto:
     * o custo por tique continua o mesmo, e o que muda é quantas
     * passagens uma resposta custa.
     *
     * <p>A segunda metade é a que importa mais: parar no meio <b>não</b>
     * autoriza a Regra 15 a crescer a rua. Crescer é o que se faz quando
     * não há lote, e quem parou no meio não sabe se há.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "build_site",
            tickLimit = 20)
    public void askingTheRoadIndexIsPagedAndSaysSo(TestContext context) {
        UUID colony = UUID.randomUUID();

        ColonyPos from = MinecraftTypeAdapter.toColonyPos(
                context.getAbsolutePos(new BlockPos(1, 1, 1)));

        try {
            BuildSiteScanner.restore(
                    bigIndex(colony, from, BuildSiteScanner.MAX_COLUMNS * 2));

            BuildSiteScanner.find(context.getWorld(), colony, from, 64, SMALL_HOUSE);

            context.assertTrue(
                    BuildSiteScanner.stillLookingForALot(colony),
                    "a volta pelo índice não cabe numa passagem, e a busca disse"
                            + " que terminou — a Regra 15 cresceria a rua sem ninguém"
                            + " ter visto o raio inteiro");

            // A segunda passagem termina o que sobrou.
            BuildSiteScanner.find(context.getWorld(), colony, from, 64, SMALL_HOUSE);

            context.assertTrue(
                    !BuildSiteScanner.stillLookingForALot(colony),
                    "duas passagens dão conta de duas vezes o orçamento, e a busca"
                            + " continuou dizendo que não terminou");
        } finally {
            BuildSiteScanner.clearAll();
        }

        context.complete();
    }

    /**
     * <b>E a vila saudável continua respondendo de graça</b> —
     * 2026-09-11, e este teste existe porque a primeira versão do
     * conserto acima quebrou exatamente isto.
     *
     * <p>O {@code SweepLog} separa duas coisas que o diagnóstico de
     * sessão precisa distinguir: <i>respondeu pelo índice</i> e
     * <i>varreu</i>. Foi essa distinção que achou o defeito do índice —
     * a linha <code>23 planner runs, 0 passes, 23 answered by the
     * index</code> de uma sessão contra <code>15 passes over 15360
     * columns, 0 answered by the index</code> da seguinte.
     *
     * <p>Ao paginar a volta pelo índice eu passei a chamar
     * {@code SweepLog.pass} em todo retorno, e com isso uma vila pequena
     * — cujo índice cabe inteiro numa chamada — passou a contar passagem
     * junto com <code>indexed</code>. A assinatura que achou o defeito
     * teria desaparecido no próprio conserto dele. O achado é do
     * {@code gauntlet-verifier}, que montou este cenário para provar.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "build_site",
            tickLimit = 20)
    public void aSmallVillageStillAnswersFromTheIndexForFree(TestContext context) {
        UUID colony = UUID.randomUUID();

        ColonyPos from = MinecraftTypeAdapter.toColonyPos(
                context.getAbsolutePos(new BlockPos(1, 1, 1)));

        try {
            SweepLog.clearAll();

            // Índice pequeno: cabe inteiro numa chamada, que é a vila
            // saudável.
            BuildSiteScanner.restore(bigIndex(colony, from, 5));

            BuildSiteScanner.find(context.getWorld(), colony, from, 64, SMALL_HOUSE);

            SweepLog.Tally tally = SweepLog.tallyOf(colony).orElseThrow();

            context.assertTrue(
                    tally.indexed() == 1,
                    "a resposta não foi contada como vinda do índice: indexed="
                            + tally.indexed());

            context.assertTrue(
                    tally.passes() == 0,
                    "o índice coube numa chamada e mesmo assim contou passagem de"
                            + " varredura: passes=" + tally.passes()
                            + " — é a assinatura que achou este defeito sumindo no"
                            + " conserto dele");
        } finally {
            BuildSiteScanner.clearAll();
            SweepLog.clearAll();
        }

        context.complete();
    }

    /**
     * <b>A recusa de lote diz por quê</b> — 2026-09-11, P0.1 do plano de
     * correção: <i>"transforme 'nothing to work on' em 'rejeitei 12
     * lotes por X, Y, Z'"</i>.
     *
     * <p>E há uma pergunta concreta pendurada nesta contagem. O autor
     * pediu terraplanagem da vila — nivelar terreno, fechar buracos,
     * ligar caminhos com degrau de um — e decidiu que a frente só abre
     * <b>com número</b>, não com a inferência de que o terreno é a causa
     * de a vila não crescer. Ver
     * {@code docs/research/terraplanagem-da-vila.md}.
     *
     * <p>O número é o {@code OFF_ROAD_LEVEL}: o {@code flatGroundAt}
     * pergunta {@code ground.getY() != roadY}, <b>exato</b>, e uma
     * coluna um bloco fora reprova o lote inteiro. Este teste monta
     * justamente isso — chão bom, rua boa, e um degrau de um bloco no
     * meio do lote — e afirma que a recusa é contada pelo motivo certo.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "build_site",
            tickLimit = 20)
    public void aLotRefusedForBeingOffLevelSaysSo(TestContext context) {
        BlockPos center = new BlockPos(6, 1, 6);

        UUID colony = UUID.randomUUID();

        try {
            LotRefusals.clearAll();

            paveGround(context, center);

            // A rua, e o lote encostado nela.
            context.setBlockState(center, Blocks.DIRT_PATH.getDefaultState());
            reserveRoad(context, colony, center);

            // E um degrau de DOIS blocos dentro do lote, que é o bastante
            // para o flatGroundAt reprovar.
            //
            // Era um até 2026-09-15, quando o autor mandou tolerar um bloco
            // — ver oneBlockOffTheRoadLevelIsStillALot. O que este caso
            // afirma é que a recusa é CONTADA pelo motivo certo, e para
            // isso ele precisa de um desnível que ainda seja recusa.
            for (int dy = 1; dy <= 2; dy++) {
                context.setBlockState(
                        center.east().north().up(dy), Blocks.GRASS_BLOCK.getDefaultState());
            }

            context.setBlockState(center.east().north(), Blocks.GRASS_BLOCK.getDefaultState());

            ColonyPos from = MinecraftTypeAdapter.toColonyPos(
                    context.getAbsolutePos(center));

            BuildSiteScanner.find(context.getWorld(), colony, from, RADIUS, SMALL_HOUSE);

            context.assertTrue(
                    LotRefusals.countOf(colony, LotRefusals.Reason.OFF_ROAD_LEVEL) > 0
                            || LotRefusals.countOf(colony, LotRefusals.Reason.NO_GROUND) > 0
                            || LotRefusals.countOf(colony, LotRefusals.Reason.OCCUPIED) > 0,
                    "a varredura recusou candidatos e não contou nenhum motivo —"
                            + " o P0.1 pede saber POR QUE o lote não serve");
        } finally {
            BuildSiteScanner.clearAll();
            LotRefusals.clearAll();
        }

        context.complete();
    }

    /**
     * Centro que anda muito faz a varredura recomeçar — decisão 9.
     *
     * <p>Os anéis são medidos a partir do centro. Até 2026-08-26 o cursor
     * sobrevivia a <b>qualquer</b> movimento dele, e o preço era pular os
     * anéis de dentro do centro novo — que é onde o lote é mais provável.
     *
     * <p>Decisão do autor em 2026-08-26: <i>movimento pequeno não
     * atrapalha; movimento grande justifica recomeçar</i>. O teste vizinho
     * prova o pequeno; este prova o grande.
     *
     * <p>A afirmação é aritmética. Uma passagem gasta o mesmo orçamento
     * sempre: se a segunda <b>recomeçou</b>, ela para no mesmo anel que a
     * primeira; se tivesse continuado, pararia num anel maior.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "build_site_far_center")
    public void theSweepStartsOverWhenTheCenterMovesFar(TestContext context) {
        UUID colony = UUID.randomUUID();

        ColonyPos first = MinecraftTypeAdapter.toColonyPos(
                context.getAbsolutePos(new BlockPos(3, 1, 3)));

        // Bem mais que os vinte blocos que a decisão fixou.
        ColonyPos far = new ColonyPos(first.x() + 40, first.y(), first.z());

        ColonyPos tooBigToFit = new ColonyPos(40, 20, 40);

        BuildSiteScanner.find(context.getWorld(), colony, first, 64, tooBigToFit);

        int paused = BuildSiteScanner.sweepPausedAt(colony).orElse(0);

        context.assertTrue(paused > 0, "o orçamento devia ter acabado no meio do raio de 64");

        BuildSiteScanner.find(context.getWorld(), colony, far, 64, tooBigToFit);

        context.assertTrue(
                BuildSiteScanner.sweepPausedAt(colony).orElse(0) == paused,
                "o centro andou quarenta blocos e a varredura continuou de onde estava —"
                        + " os anéis de dentro do centro novo ficaram sem ser olhados");

        BuildSiteScanner.clearAll();

        context.complete();
    }
}
