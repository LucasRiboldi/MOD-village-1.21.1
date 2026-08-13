package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.service.VillageDetector;
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
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.poi.PointOfInterestTypes;

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
        clearColonyState();

        BlockPos anchor = new BlockPos(1, 1, 1);

        placeBeds(context, anchor, BEDS);
        spawnVillagers(context, anchor, VillageDetector.MIN_VILLAGERS);

        runCycle(context, anchor);

        context.assertTrue(
                VillageColonyMod.COLONIES.count() == 1,
                "esperava uma colônia, achei " + VillageColonyMod.COLONIES.count()
                        + " — " + diagnose(context, anchor));

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

    /**
     * Os aldeões da colônia viram trabalhadores com profissão.
     *
     * <p>V1 e V2 do §7 juntos: registro e atribuição.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "colony_workers")
    public void villagersBecomeWorkersWithAProfession(TestContext context) {
        clearColonyState();

        BlockPos anchor = new BlockPos(1, 1, 1);

        placeBeds(context, anchor, BEDS);
        spawnVillagers(context, anchor, VillageDetector.MIN_VILLAGERS);

        runCycle(context, anchor);

        if (VillageColonyMod.COLONIES.count() != 1) {
            context.throwGameTestException("nenhuma colônia foi detectada");
        }

        Colony colony = VillageColonyMod.COLONIES.all().iterator().next();

        int workers = VillageColonyMod.WORKERS.countOfColony(colony.id());

        context.assertTrue(
                workers >= VillageDetector.MIN_VILLAGERS,
                "esperava ao menos " + VillageDetector.MIN_VILLAGERS
                        + " trabalhadores, achei " + workers);

        long withProfession = VillageColonyMod.WORKERS.ofColony(colony.id()).stream()
                .filter(worker -> worker.hasProfession())
                .count();

        context.assertTrue(
                withProfession == workers,
                "trabalhador sem profissão: " + (workers - withProfession) + " de " + workers);

        context.complete();
    }

    /**
     * A colônia encolhe quando a sonda dela confirma a leitura menor.
     *
     * <p>É o item A do §8 e o caminho inteiro do E2: a vila perde casas,
     * a sonda ancorada no centro lê menos, e a colônia só baixa a
     * contagem quando a leitura menor se repete. Uma leitura menor
     * sozinha não vale — visão parcial é o caso comum, e foi ela que fez
     * o centro derivar em 2026-08-07.
     *
     * <p>O teste exige as duas metades: **não** encolher na primeira
     * leitura menor, e encolher na segunda. Só a segunda passaria com a
     * regra apagada.
     *
     * <p>A contagem absoluta não é afirmada. As estruturas dos outros
     * testes ficam a menos de {@code CLUSTER_DISTANCE} e suas camas
     * entram no aglomerado — o que este teste pode afirmar é a diferença
     * entre antes e depois, que é o que a regra decide.
     *
     * <p><b>Por que este teste atravessa ticks.</b> A sonda só roda para
     * colônia ACTIVE, e ACTIVE quer dizer chunk ticando. Em jogo quem
     * mantém o chunk assim é o jogador por perto; aqui não há jogador, e
     * o centro da colônia cai onde o aglomerado o puser — inclusive fora
     * da estrutura do teste, porque as camas das estruturas vizinhas
     * entram na conta. O teste força o chunk do centro, e o pedido de
     * carga só vale no tick seguinte: por isso os ciclos são agendados
     * em vez de rodarem em sequência.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "colony_shrink",
            tickLimit = 200)
    public void aColonyShrinksOnlyWhenItsOwnProbeConfirms(TestContext context) {
        clearColonyState();

        BlockPos anchor = new BlockPos(1, 1, 1);
        int planted = BEDS + 4;

        placeBeds(context, anchor, planted);
        spawnVillagers(context, anchor, VillageDetector.MIN_VILLAGERS);

        runCycle(context, anchor);

        if (VillageColonyMod.COLONIES.count() != 1) {
            context.throwGameTestException(
                    "nenhuma colônia foi detectada — " + diagnose(context, anchor));
        }

        Colony colony = VillageColonyMod.COLONIES.all().iterator().next();
        ChunkPos center = new ChunkPos(MinecraftTypeAdapter.toBlockPos(colony.center()));

        context.getWorld().setChunkForced(center.x, center.z, true);

        int before = colony.observedBeds();

        removeLastBeds(context, anchor, planted, 4);

        context.runAtTick(20, () -> {
            runCycle(context, anchor);

            context.assertTrue(
                    colony.isActive(),
                    "a colônia continua dormente e a sonda nunca roda — "
                            + "centro " + colony.center() + ", chunk " + center);

            context.assertTrue(
                    colony.observedBeds() == before,
                    "encolheu já na primeira leitura menor: " + before
                            + " → " + colony.observedBeds());
        });

        context.runAtTick(30, () -> {
            runCycle(context, anchor);

            context.assertTrue(
                    colony.observedBeds() < before,
                    "a sonda confirmou a leitura menor e a colônia não encolheu: "
                            + before + " → " + colony.observedBeds()
                            + " — âncora da sonda " + colony.probeAnchor());

            context.getWorld().setChunkForced(center.x, center.z, false);

            clearColonyState();

            context.complete();
        });
    }

    // ----------------------------------------------------------------

    /**
     * Zera o estado do mod entre testes.
     *
     * <p>Os registros são estáticos e vivem no servidor, que é um só para
     * a bateria inteira. Sem isto, a colônia de um teste apareceria na
     * contagem do seguinte, e a ordem de execução — que não é garantida —
     * mudaria o resultado.
     */
    private static void clearColonyState() {
        VillageColonyMod.COLONIES.clear();
        VillageColonyMod.WORKERS.clear();
        VillageColonyMod.STORAGES.clear();
        VillageColonyMod.TASKS.clear();
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

    /** Tira as últimas camas plantadas, como uma vila que perde casas. */
    private static void removeLastBeds(
            TestContext context, BlockPos anchor, int planted, int count) {

        for (int i = planted - count; i < planted; i++) {
            BlockPos head = bedHead(anchor, i);

            // As duas metades: meia cama continuaria de pé como bloco
            // solto, e o POI nasce da cabeceira.
            context.setBlockState(head, Blocks.AIR.getDefaultState());
            context.setBlockState(head.offset(Direction.SOUTH), Blocks.AIR.getDefaultState());
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

    private static void spawnVillagers(TestContext context, BlockPos anchor, int count) {
        for (int i = 0; i < count; i++) {
            VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, anchor.add(i, 0, 2));

            // Adulto: bebê não recebe profissão, e a atribuição é o que
            // este teste verifica. Ver a correção de 2026-08-07.
            villager.setBreedingAge(0);
        }
    }

    private static void runCycle(TestContext context, BlockPos anchor) {
        ServerWorld world = context.getWorld();

        VillageDetectionHandler.runCycleNow(world, context.getAbsolutePos(anchor));
    }
}
