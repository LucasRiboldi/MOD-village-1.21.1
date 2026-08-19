package com.villagecolony.gametest;

import com.villagecolony.fabric.brain.WorkHours;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

/**
 * O expediente da colônia — a Regra 18.
 *
 * <p>Até 2026-08-19 quem respondia "é hora de trabalhar?" era a
 * {@code Schedule} do Vanilla, e a resposta dela é curta: o aldeão
 * comum trabalha do tique 2000 ao 9000 e passa o resto do dia claro em
 * MEET e IDLE. São 7.000 tiques de trabalho num dia de 24.000, e
 * 3.000 tiques de sol em que a colônia inteira para — foi o que a
 * sessão de 2026-08-18 mostrou, com as linhas {@code off hours}
 * alternando com {@code work time} enquanto o sol estava alto.
 *
 * <p>O que estes testes prendem é o que depende do aldeão: manhã cedo e
 * meio da tarde são trabalho, e criança não trabalha em hora nenhuma. A
 * janela em si — onde o expediente começa e acaba — é afirmada em
 * {@code WorkClockTest}, fora do jogo.
 *
 * <p><b>Nenhum caso aqui vira a noite</b>, e é de propósito: a hora do
 * mundo é global e a bateria roda testes concorrentes. A primeira
 * versão desta classe punha o relógio às 14.000 para afirmar "à noite
 * ninguém trabalha", e derrubou três testes de lenhador que rodavam
 * junto. As horas usadas aqui estão todas <b>dentro</b> do expediente,
 * e por isso não mexem com ninguém.
 */
public class WorkHoursGameTest implements FabricGameTest {

    /** Onde o aldeão nasce. Qualquer lugar serve: o teste é sobre o relógio. */
    private static final BlockPos STAND = new BlockPos(1, 2, 1);

    /**
     * Início da manhã: o Vanilla ainda chama de IDLE, e a colônia não.
     *
     * <p>Fica antes de {@code Schedule.WORK_TIME}, que é onde a Activity
     * WORK do Vanilla começa.
     */
    private static final int EARLY_MORNING = 1_000;

    /**
     * Meio da tarde: o Vanilla já trocou para MEET, e o sol está alto.
     *
     * <p>É a hora que a sessão de 2026-08-18 flagrou parada, e é o
     * coração desta regra.
     */
    private static final int AFTERNOON = 10_000;

    /**
     * O dia inteiro é expediente, e não só a fatia que o Vanilla chama
     * de WORK.
     *
     * <p>Rodado contra a regra desligada: às 10.000 o Vanilla responde
     * MEET, {@code isWorkTime} devolvia falso, e a asserção da tarde
     * falha.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "work_hours")
    public void theWholeDayIsWorkTime(TestContext context) {
        ServerWorld world = context.getWorld();
        VillagerEntity villager = adultAt(context);

        world.setTimeOfDay(EARLY_MORNING);

        context.assertTrue(
                WorkHours.isWorkTime(world, villager),
                "manhã cedo devia ser expediente, e a colônia disse que não");

        world.setTimeOfDay(AFTERNOON);

        context.assertTrue(
                WorkHours.isWorkTime(world, villager),
                "meio da tarde com sol alto devia ser expediente — é a hora"
                        + " em que a colônia parava, em 2026-08-18");

        context.complete();
    }

    /**
     * Criança não trabalha, nem no meio da tarde.
     *
     * <p>A pergunta antiga acertava isto de graça: a Schedule do bebê
     * não tem WORK em hora nenhuma. Ao deixar de perguntar à Schedule, a
     * colônia passa a precisar dizer isto ela mesma — e é o tipo de
     * coisa que se perde numa troca de regra sem que nada reclame.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "work_hours")
    public void aChildNeverWorks(TestContext context) {
        ServerWorld world = context.getWorld();
        VillagerEntity child = context.spawnEntity(EntityType.VILLAGER, STAND);

        child.setBreedingAge(-24_000);

        world.setTimeOfDay(AFTERNOON);

        context.assertFalse(
                WorkHours.isWorkTime(world, child),
                "criança não trabalha, e a colônia pôs uma para trabalhar");

        context.complete();
    }

    private static VillagerEntity adultAt(TestContext context) {
        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, STAND);

        villager.setBreedingAge(0);

        return villager;
    }
}
