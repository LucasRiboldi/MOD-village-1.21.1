package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.service.VillageDetector;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.core.worker.service.ProfessionAssigner;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.event.VillageDetectionHandler;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.BedPart;
import net.minecraft.util.math.Direction;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.poi.PointOfInterestTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Os primeiros testes que não precisam de um humano.
 *
 * <p>Item A do §8. Toda verificação da camada fabric até 2026-08-08
 * custou uma sessão de jogo do autor, e os quatro defeitos do §11 só
 * apareceram assim. Isto sobe um servidor sem cliente pelo Gradle,
 * monta a vila, afirma, e falha o build.
 *
 * <p>Não substitui o jogo inteiro: persistência exige fechar e reabrir o
 * mundo, e o gametest roda um servidor só. O V3 do §7 continua sendo
 * verificação humana.
 *
 * <p>As posições são relativas à estrutura do teste. {@code TestContext}
 * traduz para absolutas — usar {@code BlockPos} absoluto aqui faria o
 * teste depender de onde o servidor decidiu montar a estrutura.
 */
public class ColonyDetectionGameTest implements FabricGameTest {

    /** Camas suficientes para a vila valer, conforme ADR-003 §3. */
    private static final int BEDS = VillageDetector.MIN_BEDS;

    /**
     * Uma vila mínima vira colônia.
     *
     * <p>É o V1 do §7, que até aqui só podia ser conferido lendo log
     * depois de andar pela vila.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "colony_detection")
    public void aVillageBecomesAColony(TestContext context) {
        BlockPos anchor = new BlockPos(1, 1, 1);

        placeBeds(context, anchor, BEDS);
        spawnVillagers(context, anchor, VillageDetector.MIN_VILLAGERS);

        runCycle(context, anchor);

        try {
            context.assertTrue(
                    colonyOf(context, anchor).isPresent(),
                    "nenhuma colônia nasceu destas camas — " + diagnose(context, anchor));
        } finally {
            forget(context, anchor);
        }

        context.complete();
    }

    /**
     * O que o mundo do teste tinha no momento da falha.
     *
     * <p>Existe pelo mesmo motivo das linhas de log do §11: uma
     * afirmação que só diz "esperava 1, achei 0" manda alguém adivinhar
     * entre cama que não virou POI, bioma recusado e detecção que não
     * rodou.
     */
    private static String diagnose(TestContext context, BlockPos anchor) {
        ServerWorld world = context.getWorld();
        BlockPos absolute = context.getAbsolutePos(anchor);

        long beds = world.getPointOfInterestStorage()
                .getInCircle(
                        poi -> poi.matchesKey(PointOfInterestTypes.HOME),
                        absolute,
                        VillageDetector.SEARCH_RADIUS,
                        PointOfInterestStorage.OccupationStatus.ANY)
                .count();

        String biome = world.getBiome(absolute).getKey()
                .map(key -> key.getValue().toString())
                .orElse("desconhecido");

        return "POIs de cama: " + beds + ", bioma: " + biome;
    }

    // O caso negativo — "camas de menos não são vila" — não mora aqui.
    //
    // Tentado e descartado em 2026-08-08 com prova: o teste plantou duas
    // camas e a diagnose encontrou cinco POIs no raio de busca. As
    // estruturas dos outros testes ficam a menos de 64 blocos e suas
    // camas contam. Separar em batches não resolveu: os blocos
    // permanecem no mundo entre batches.
    //
    // "Não existe colônia" é propriedade global, e nenhum teste pode
    // afirmá-la num mundo partilhado. O caso está coberto onde cabe, em
    // VillageDetectorTest#tooFewBedsIsNotAVillage, que é onde a regra
    // vive.
    //
    // O que sobra aqui é o que só o jogo prova: POI vira detecção, que
    // vira colônia, que vira trabalhador com profissão.

    /** Trinta adultos abrem duas vagas para cada profissão produtora. */
    private static final int CROWD = 30;

    /**
     * Os aldeões da colônia viram trabalhadores com profissão.
     *
     * <p>V1 e V2 do §7 juntos: registro e atribuição.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "colony_workers")
    public void villagersBecomeWorkersWithAProfession(TestContext context) {
        BlockPos anchor = new BlockPos(1, 1, 1);

        // A vizinha que já estiver aqui sai antes — 2026-08-21. A
        // bateria roda batches concorrentes, e uma colônia largada por
        // outro teste a menos de DUPLICATE_DISTANCE adota estas camas em
        // vez de deixar nascer colônia nova. Ela não se move para cá
        // desde a Emenda 4 da ADR-003: o centro só anda numa leitura da
        // sonda. Então os aldeões daqui ficam fora do alcance dela, e
        // nenhum vira trabalhador.
        forget(context, anchor);

        placeBeds(context, anchor, BEDS);

        List<VillagerEntity> crowd = spawnVillagers(context, anchor, CROWD);

        // Três passagens, e não uma. O POI da cama não nasce no tick em
        // que o bloco entra, e a bateria roda os batches concorrentes —
        // uma passagem só às vezes chega antes de o mundo ter registrado
        // as camas, e aí não há vila para detectar. Passar de novo custa
        // nada e tira a afirmação do relógio da bateria.
        Colony colony = null;

        for (int attempt = 0; attempt < 3 && colony == null; attempt++) {
            runCycle(context, anchor);

            colony = colonyOwning(crowd).orElse(null);
        }

        if (colony == null) {
            long registered = crowd.stream()
                    .filter(villager -> VillageColonyMod.WORKERS.find(villager.getUuid())
                            .isPresent())
                    .count();

            throw new AssertionError("nenhuma colônia ficou com estes aldeões — "
                    + registered + " de " + crowd.size() + " viraram trabalhador");
        }

        List<Worker> crew = VillageColonyMod.WORKERS.ofColony(colony.id());

        try {
            context.assertTrue(
                    crew.size() >= CROWD,
                    "esperava ao menos " + CROWD + " trabalhadores, achei " + crew.size());

            assertTwoWorkersPerProducer(context, crew);
        } finally {
            forgetColony(colony);
        }

        context.complete();
    }

    /** Trinta adultos precisam preencher as duas vagas de cada produtor. */
    private static void assertTwoWorkersPerProducer(TestContext context, List<Worker> crew) {
        for (var type : ProfessionAssigner.PRODUCER_ORDER) {
            long employed = crew.stream()
                    .filter(worker -> worker.profession().filter(type::equals).isPresent())
                    .count();

            context.assertTrue(
                    employed >= 2,
                    type + ": esperava ao menos 2 trabalhadores, achei " + employed);
        }
    }

    // O encolhimento da colônia também não mora aqui, e a razão é a
    // mesma do caso acima — descoberto em 2026-08-13, depois de o teste
    // existir por um dia e falhar de forma intermitente.
    //
    // A regra precisa de duas leituras da sonda sobre o mesmo aglomerado
    // de camas. O aglomerado inclui as camas das estruturas vizinhas, e
    // os testes rodam concorrentes: enquanto este media, outro plantava
    // as suas. A colônia crescia de 11 para 13 camas entre um ciclo e o
    // seguinte, sem que nada do mod estivesse errado.
    //
    // Tentado e descartado: forçar o chunk do centro (o centro se move),
    // forçar a vizinhança inteira (a população de camas continua
    // mudando), e afirmar só a diferença antes e depois (contaminada
    // pelo mesmo motivo).
    //
    // A regra está coberta onde ela cabe:
    // PartialObservationTest#aRepeatedProbeReadingShrinksTheColony e
    // #aSingleProbeReadingProvesNothing. E o caminho inteiro — cama
    // destruída no mundo virando contagem menor — foi verificado em jogo
    // em 2026-08-07; ver o Development Log.

    // ----------------------------------------------------------------

    /**
     * A colônia desta estrutura de teste, se nasceu alguma.
     *
     * <p>Por posição, e não por contagem global. A bateria roda testes
     * concorrentes, e um deles pode ter colônia própria no mesmo
     * instante: afirmar "existe uma colônia no mundo" seria afirmar
     * sobre o vizinho. Ver {@link ColonyFixture}.
     */
    private static Optional<Colony> colonyOf(TestContext context, BlockPos anchor) {
        return VillageColonyMod.COLONIES.findNearest(
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(anchor)),
                VillageDetector.DUPLICATE_DISTANCE);
    }

    /**
     * Tira do registro a colônia desta estrutura, e os trabalhadores
     * dela.
     *
     * <p>Só o que este teste fez nascer. Limpar tudo apagaria a colônia
     * de um teste que ainda está rodando.
     */
    private static void forget(TestContext context, BlockPos anchor) {
        colonyOf(context, anchor).ifPresent(ColonyDetectionGameTest::forgetColony);
    }

    /** O mesmo, quando a colônia já está em mãos. */
    private static void forgetColony(Colony colony) {
        ColonyFixture owned = ColonyFixture.create().owning(colony);

        for (Worker worker : VillageColonyMod.WORKERS.ofColony(colony.id())) {
            owned.owning(worker.villagerId());
        }

        owned.cleanUp();
    }

    /**
     * Camas inteiras, cabeceira e pé.
     *
     * <p>O POI HOME nasce da cabeceira. Meia cama é bloco solto e não
     * vira POI — e sem POI a detecção não enxerga vila alguma.
     */
    private static void placeBeds(TestContext context, BlockPos anchor, int count) {
        for (int i = 0; i < count; i++) {
            BlockPos head = bedHead(anchor, i);

            context.setBlockState(head, Blocks.WHITE_BED.getDefaultState()
                    .with(BedBlock.PART, BedPart.HEAD)
                    .with(BedBlock.FACING, Direction.NORTH));

            context.setBlockState(head.offset(Direction.SOUTH), Blocks.WHITE_BED.getDefaultState()
                    .with(BedBlock.PART, BedPart.FOOT)
                    .with(BedBlock.FACING, Direction.NORTH));
        }
    }

    /**
     * Onde vai a cama de índice {@code i}.
     *
     * <p>Em grade de quatro por fila, e não em linha: uma fila de sete
     * camas espaçadas passaria da borda da estrutura de teste. A cama
     * ocupa dois blocos em z, por isso as filas ficam a três.
     */
    private static BlockPos bedHead(BlockPos anchor, int i) {
        return anchor.add((i % 4) * 2, 0, (i / 4) * 3);
    }

    /**
     * Aldeões atrás das camas, em grade de quatro por fila.
     *
     * <p>Em grade pelo mesmo motivo que as camas: uma fila de doze passaria
     * da borda da estrutura de teste, e {@code spawnEntity} fora dos
     * limites não é aldeão que não conta — é o teste que não roda.
     *
     * <p>A partir de z+4 para não disputar espaço com as camas, que ocupam
     * z+0 e z+1 na primeira fila.
     */
    private static List<VillagerEntity> spawnVillagers(
            TestContext context, BlockPos anchor, int count) {

        List<VillagerEntity> born = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            VillagerEntity villager = context.spawnEntity(
                    EntityType.VILLAGER, anchor.add(i % 4, 0, 4 + (i / 4)));

            // Adulto: bebê não recebe profissão, e a atribuição é o que
            // este teste verifica. Ver a correção de 2026-08-07.
            villager.setBreedingAge(0);

            born.add(villager);
        }

        return born;
    }

    /**
     * A colônia que ficou com <b>estes</b> aldeões.
     *
     * <p>Por dono, e não por posição — 2026-08-21. {@link #colonyOf}
     * procura no raio de {@code DUPLICATE_DISTANCE} da âncora, e isso
     * pressupunha que a colônia destas camas ficasse com o centro por
     * perto. Deixou de ser verdade com a Emenda 4 da ADR-003: o centro
     * só se move numa leitura da sonda, então uma colônia vizinha que
     * adote estas camas as adota <b>sem sair do lugar</b> — e ela pode
     * estar a mais de 32 blocos daqui.
     *
     * <p>É a contaminação que este arquivo já documenta em três lugares,
     * aparecendo por uma porta nova. Perguntar "de quem é este aldeão"
     * não depende de distância nenhuma.
     */
    private static Optional<Colony> colonyOwning(List<VillagerEntity> villagers) {
        for (VillagerEntity villager : villagers) {
            Optional<Colony> owner = VillageColonyMod.WORKERS.find(villager.getUuid())
                    .flatMap(worker -> VillageColonyMod.COLONIES.find(worker.colonyId()));

            if (owner.isPresent()) {
                return owner;
            }
        }

        return Optional.empty();
    }

    private static void runCycle(TestContext context, BlockPos anchor) {
        ServerWorld world = context.getWorld();

        VillageDetectionHandler.runCycleNow(world, context.getAbsolutePos(anchor));
    }
}
