package com.villagecolony.gametest;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.storage.service.StorageRegistry;
import com.villagecolony.core.worker.service.WorkerService;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ChestScanner;
import com.villagecolony.fabric.integration.VillagerScanner;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Contar candidatos não é contar baús — o E11 do §17.
 *
 * <p>A colônia dispensa um trabalhador sem baú para cada aldeão que
 * <em>conseguiria</em> um. Até 2026-08-15 ela contava <b>candidatos</b>,
 * e dois aldeões do mesmo cômodo respondem sim olhando para o
 * <b>mesmo</b> baú.
 *
 * <p>O custo apareceu na sessão de cinco horas e quarenta de 2026-08-15:
 * 689 dispensas e reatribuições, uma por ciclo, sem que nada no mundo
 * tivesse mudado. Três candidatos enxergando um baú rendiam três
 * dispensas, uma reivindicação e dois trabalhadores novos sem baú — e no
 * ciclo seguinte, a mesma troca.
 *
 * <p>Decisão do autor em 2026-08-15: só se dispensa quem não tem baú
 * quando há baú livre <b>de verdade</b> para o substituto.
 *
 * <p>Gametest e não teste de unidade porque a pergunta é da fronteira:
 * só um mundo de verdade tem cama, baú e a memória {@code HOME} que liga
 * as duas coisas.
 */
public class FreeChestCountGameTest implements FabricGameTest {

    /** A cama que os dois aldeões vão dividir. */
    private static final BlockPos BED = new BlockPos(1, 2, 1);

    /** O único baú do cômodo. */
    private static final BlockPos CHEST = new BlockPos(3, 2, 1);

    /**
     * Dois aldeões, uma cama, um baú: um baú livre, e não dois.
     *
     * <p>É a afirmação inteira do E11. Se ela falhar, a colônia volta a
     * dispensar dois trabalhadores para uma vaga que só um pode ocupar.
     *
     * <p>Rodado contra a regra desligada em 2026-08-15 — contando
     * candidatos em vez de baús — e falha.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "free_chest_count")
    public void twoVillagersLookingAtOneChestCountAsOneFreeChest(TestContext context) {
        ServerWorld world = context.getWorld();

        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());

        StorageRegistry storages = new StorageRegistry();

        VillagerEntity first = housed(context, new BlockPos(1, 2, 2));
        VillagerEntity second = housed(context, new BlockPos(2, 2, 2));

        // Sem profissao: os dois sao candidatos, e o bau do comodo esta
        // sem marca — a regra de 2026-08-27 nao muda nada aqui.
        Optional<ColonyPos> forFirst =
                ChestScanner.freeChestFor(world, first, storages, Optional.empty());
        Optional<ColonyPos> forSecond =
                ChestScanner.freeChestFor(world, second, storages, Optional.empty());

        context.assertTrue(
                forFirst.isPresent() && forSecond.isPresent(),
                "os dois aldeões deviam enxergar o baú do cômodo: "
                        + forFirst + " e " + forSecond);

        Set<ColonyPos> distinct = new HashSet<>();
        forFirst.ifPresent(distinct::add);
        forSecond.ifPresent(distinct::add);

        context.assertTrue(
                distinct.size() == 1,
                "dois aldeões e um baú deram " + distinct.size()
                        + " baús livres — a colônia vai dispensar dois"
                        + " trabalhadores para uma vaga só");

        context.complete();
    }

    /**
     * Um aldeão adulto com a cama de {@link #BED} na memória.
     *
     * <p>A cama é a mesma para os dois de propósito: é o cômodo
     * compartilhado que produz o defeito, e é o caso comum numa casa de
     * vila com dois moradores.
     */
    private static VillagerEntity housed(TestContext context, BlockPos where) {
        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, where);
        villager.setBreedingAge(0);

        villager.getBrain().remember(
                MemoryModuleType.HOME,
                GlobalPos.create(
                        context.getWorld().getRegistryKey(), context.getAbsolutePos(BED)));

        return villager;
    }

    /**
     * A ligação, e não só a premissa.
     *
     * <p>O teste acima prova que dois aldeões do mesmo cômodo enxergam o
     * mesmo baú. Este prova que a <b>varredura</b> conta isso como um, e
     * é o número que ela entrega — {@code freeChests} — que decide
     * quantas dispensas cabem em
     * {@code VillageDetectionHandler.dismissExtraWorkers}.
     *
     * <p>Os dois números aparecem juntos de propósito: {@code equippable}
     * continua sendo dois, porque são dois candidatos de verdade. O que
     * mudou é qual dos dois a colônia usa para dispensar.
     *
     * <p>Rodado contra a regra desligada em 2026-08-15 — {@code freeChests}
     * recebendo um valor por candidato — e falha.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "free_chest_count")
    public void theScanCountsOneFreeChestForTwoCandidates(TestContext context) {
        ServerWorld world = context.getWorld();

        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());

        housed(context, new BlockPos(1, 2, 2));
        housed(context, new BlockPos(2, 2, 2));

        WorkerService workers = new WorkerService();
        StorageRegistry storages = new StorageRegistry();

        Colony colony = Colony.create(
                UUID.randomUUID(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(BED)));

        // Duas passadas: a primeira registra os aldeões, e só a partir
        // dela a varredura sabe que há vaga aberta — que é a condição
        // para ela perguntar quem consegue baú.
        VillagerScanner.scan(world, colony, workers, storages);

        VillagerScanner.ScanResult result =
                VillagerScanner.scan(world, colony, workers, storages);

        context.assertTrue(
                result.equippable().size() == 2,
                "os dois aldeões deviam ser candidatos, e vieram "
                        + result.equippable().size());

        context.assertTrue(
                result.freeChests().size() == 1,
                "a varredura contou " + result.freeChests().size()
                        + " baús livres para um baú só — a colônia vai"
                        + " dispensar dois trabalhadores para uma vaga");

        context.complete();
    }
}
