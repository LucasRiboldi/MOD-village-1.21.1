package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.service.VillageDetector;
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
            BlockPos head = anchor.add(i * 3, 0, 0);

            context.setBlockState(head, Blocks.WHITE_BED.getDefaultState()
                    .with(BedBlock.PART, BedPart.HEAD)
                    .with(BedBlock.FACING, Direction.NORTH));

            context.setBlockState(head.offset(Direction.SOUTH), Blocks.WHITE_BED.getDefaultState()
                    .with(BedBlock.PART, BedPart.FOOT)
                    .with(BedBlock.FACING, Direction.NORTH));
        }
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
