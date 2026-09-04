package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.integration.VillagerScanner;
import com.villagecolony.core.colony.service.VillageDetector;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.event.VillageDetectionHandler;
import com.villagecolony.fabric.integration.ChestInventoryReader;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;

import java.util.UUID;
import java.util.List;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.enums.BedPart;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.GlobalPos;

/**
 * O baú do trabalhador, sem um humano para conferir.
 *
 * <p>É a parte que mais custou ao autor: quatro sessões de jogo entre
 * 2026-08-07 e 2026-08-08, e três regras que só se mostraram erradas
 * lendo log — o baú do outro andar, o baú atrás da parede e a contagem
 * que não distinguia vazio de ilegível.
 *
 * <p>As afirmações são ancoradas na posição do baú que o teste plantou,
 * nunca em contagem global. O mundo do gametest é um só e as estruturas
 * ficam a menos de 64 blocos umas das outras: "ninguém reivindicou baú
 * nenhum" é indemonstrável aqui, mas "este baú foi reivindicado" e
 * "este baú não foi" são locais e valem. Ver a entrada de §15 de
 * 2026-08-08.
 */
public class StorageGameTest implements FabricGameTest {

    /**
     * Uma cama e um baú ao lado, sem nada entre eles.
     *
     * <p>O caminho feliz de Storage-System.md: aldeão, casa, cama, baú
     * da casa.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "storage_claim")
    public void aChestInTheSameRoomIsClaimed(TestContext context) {
        BlockPos bed = new BlockPos(1, 1, 1);
        BlockPos chest = new BlockPos(1, 1, 3);

        buildVillage(context, bed);
        context.setBlockState(chest, Blocks.CHEST.getDefaultState());

        houseVillagerAt(context, bed);
        runCycle(context, bed);

        context.assertTrue(
                isClaimed(context, chest),
                "o baú ao lado da cama deveria ter sido reivindicado");

        context.complete();
    }

    /**
     * Parede desqualifica — a regra do P4.
     *
     * <p>É o baú do vizinho e o baú do jogador ao mesmo tempo: nenhum dos
     * dois tem sinal próprio no Vanilla, os dois têm parede.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "storage_wall")
    public void aChestBehindAWallIsNotClaimed(TestContext context) {
        BlockPos bed = new BlockPos(1, 1, 1);
        BlockPos wall = new BlockPos(1, 1, 3);
        BlockPos chest = new BlockPos(1, 1, 4);

        buildVillage(context, bed);
        context.setBlockState(wall, Blocks.STONE.getDefaultState());
        context.setBlockState(chest, Blocks.CHEST.getDefaultState());

        houseVillagerAt(context, bed);

        // A parede tem de existir antes de a regra ser cobrada por ela.
        context.expectBlock(Blocks.STONE, wall);

        runCycle(context, bed);

        context.assertTrue(
                !isClaimed(context, chest),
                "o baú atrás da parede não deveria ter sido reivindicado — parede em "
                        + context.getAbsolutePos(wall).toShortString()
                        + ", cama em " + context.getAbsolutePos(bed).toShortString()
                        + ", baú em " + context.getAbsolutePos(chest).toShortString()
                        + "; traço: " + probe(context, bed, chest));

        context.complete();
    }

    /**
     * Outro andar desqualifica — a regra de nível.
     *
     * <p>O defeito real: um aldeão de {@code 1068,65,735} reivindicou o
     * baú de {@code 1068,70,735}. Mesma coluna, cinco blocos acima,
     * dentro do raio.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "storage_level")
    public void aChestOnAnotherLevelIsNotClaimed(TestContext context) {
        BlockPos bed = new BlockPos(1, 1, 1);
        BlockPos chest = new BlockPos(1, 4, 1);

        buildVillage(context, bed);
        context.setBlockState(chest, Blocks.CHEST.getDefaultState());

        houseVillagerAt(context, bed);
        runCycle(context, bed);

        context.assertTrue(
                !isClaimed(context, chest),
                "o baú três blocos acima não deveria ter sido reivindicado");

        context.complete();
    }

    /**
     * A colônia conta o que está dentro do baú.
     *
     * <p>O V5 do §7. Em jogo só dava para conferir abrindo o baú e
     * comparando com o log; aqui o número é afirmado.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "storage_count")
    public void theColonyCountsWhatTheChestHolds(TestContext context) {
        BlockPos bed = new BlockPos(1, 1, 1);
        BlockPos chest = new BlockPos(1, 1, 3);

        buildVillage(context, bed);
        context.setBlockState(chest, Blocks.CHEST.getDefaultState());

        fillChest(context, chest, Items.OAK_LOG.getDefaultStack().getItem(), 12);

        houseVillagerAt(context, bed);
        runCycle(context, bed);

        if (!isClaimed(context, chest)) {
            context.throwGameTestException("o baú não foi reivindicado; a contagem nem chega a valer");
        }

        int counted = ChestInventoryReader
                .read(context.getWorld(), context.getAbsolutePos(chest))
                .amountOf(ResourceType.OAK_LOG);

        context.assertTrue(
                counted == 12,
                "esperava 12 toras contadas, achei " + counted);

        context.complete();
    }

    // ----------------------------------------------------------------

    /**
     * O mínimo para existir colônia: camas bastantes e aldeões
     * bastantes.
     *
     * <p>Sem colônia não há trabalhador, e sem trabalhador o baú não é
     * procurado. A cama do trabalhador é a primeira do conjunto.
     */
    private static void buildVillage(TestContext context, BlockPos firstBed) {
        for (int i = 0; i < VillageDetector.MIN_BEDS; i++) {
            BlockPos head = firstBed.add(0, 0, -i * 2);

            context.setBlockState(head, Blocks.WHITE_BED.getDefaultState()
                    .with(BedBlock.PART, BedPart.HEAD)
                    .with(BedBlock.FACING, Direction.NORTH));

            context.setBlockState(head.offset(Direction.SOUTH), Blocks.WHITE_BED.getDefaultState()
                    .with(BedBlock.PART, BedPart.FOOT)
                    .with(BedBlock.FACING, Direction.NORTH));
        }

        for (int i = 0; i < VillageDetector.MIN_VILLAGERS; i++) {
            VillagerEntity villager =
                    context.spawnEntity(EntityType.VILLAGER, firstBed.add(3 + i, 0, 0));

            villager.setBreedingAge(0);
        }
    }

    /**
     * Dá casa ao primeiro aldeão.
     *
     * <p>Em jogo o próprio cérebro do aldeão reivindica a cama e grava
     * {@code HOME}; isso leva tempo de jogo e depende do ciclo dele.
     * Aqui a memória é escrita à mão, porque o que este teste verifica é
     * o que o mod faz **depois** de existir casa, não o Vanilla achando
     * cama.
     */
    private static void houseVillagerAt(TestContext context, BlockPos bed) {
        ServerWorld world = context.getWorld();
        BlockPos absoluteBed = context.getAbsolutePos(bed);

        for (VillagerEntity villager : world.getEntitiesByClass(
                VillagerEntity.class,
                context.getTestBox(),
                villager -> true)) {

            villager.getBrain().remember(
                    MemoryModuleType.HOME,
                    GlobalPos.create(world.getRegistryKey(), absoluteBed));

            return;
        }

        context.throwGameTestException("nenhum aldeão para dar casa");
    }

    private static void fillChest(
            TestContext context, BlockPos chest, net.minecraft.item.Item item, int amount) {

        BlockPos absolute = context.getAbsolutePos(chest);

        if (context.getWorld().getBlockEntity(absolute) instanceof ChestBlockEntity inventory) {
            inventory.setStack(0, new ItemStack(item, amount));

            return;
        }

        context.throwGameTestException("não há baú em " + chest.toShortString());
    }

    /** O que um traço da cama ao baú encontra, para a mensagem de falha. */
    private static String probe(TestContext context, BlockPos bed, BlockPos chest) {
        net.minecraft.util.math.Vec3d from =
                context.getAbsolutePos(bed).toCenterPos();

        net.minecraft.util.math.Vec3d to =
                context.getAbsolutePos(chest).toCenterPos();

        net.minecraft.util.hit.BlockHitResult hit = context.getWorld().raycast(
                new net.minecraft.world.RaycastContext(
                        from,
                        to,
                        net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
                        net.minecraft.world.RaycastContext.FluidHandling.NONE,
                        net.minecraft.block.ShapeContext.absent()));

        return hit.getType() + " em " + hit.getBlockPos().toShortString();
    }

    /** Se este baú, e não outro qualquer, tem dono. */
    private static boolean isClaimed(TestContext context, BlockPos chest) {
        ColonyPos position =
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(chest));

        return VillageColonyMod.STORAGES.isTaken(position);
    }

    /**
     * Dois ciclos, e não um.
     *
     * <p>Desde 2026-08-12 o baú é de quem trabalha, e a função vem de um
     * passo posterior à varredura que procura baú: no primeiro ciclo o
     * aldeão é registrado e recebe a profissão, e no segundo ele
     * reivindica. Um ciclo só deixaria estes testes verificando um estado
     * que o jogo atravessa em trinta segundos.
     */
    private static void runCycle(TestContext context, BlockPos anchor) {
        VillageDetectionHandler.runCycleNow(
                context.getWorld(), context.getAbsolutePos(anchor));

        VillageDetectionHandler.runCycleNow(
                context.getWorld(), context.getAbsolutePos(anchor));
    }

    /**
     * Baú é de quem trabalha.
     *
     * <p>Antes de 2026-08-12 todo aldeão reivindicava um, e a vila do
     * autor acabou com treze baús presos e quatro trabalhadores: o
     * fazendeiro não conseguia nenhum porque os vizinhos desempregados
     * tinham chegado primeiro.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "storage_employed_only")
    public void onlyAWorkerWithAProfessionClaimsAChest(TestContext context) {
        BlockPos bed = new BlockPos(2, 2, 2);
        BlockPos chest = new BlockPos(3, 2, 2);

        VillageColonyMod.COLONIES.clear();
        VillageColonyMod.WORKERS.clear();
        VillageColonyMod.STORAGES.clear();

        context.setBlockState(bed, Blocks.WHITE_BED.getDefaultState()
                .with(BedBlock.PART, BedPart.HEAD)
                .with(BedBlock.FACING, Direction.NORTH));
        context.setBlockState(bed.offset(Direction.SOUTH), Blocks.WHITE_BED.getDefaultState()
                .with(BedBlock.PART, BedPart.FOOT)
                .with(BedBlock.FACING, Direction.NORTH));
        context.setBlockState(chest, Blocks.CHEST.getDefaultState());

        ServerWorld world = context.getWorld();

        Colony colony = Colony.create(
                UUID.randomUUID(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(bed)));

        VillageColonyMod.COLONIES.register(colony);

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, new BlockPos(2, 2, 3));
        villager.setBreedingAge(0);
        villager.getBrain().remember(
                MemoryModuleType.HOME,
                GlobalPos.create(world.getRegistryKey(), context.getAbsolutePos(bed)));

        // Sem profissão: nenhum baú é reivindicado.
        VillagerScanner.scan(world, colony, VillageColonyMod.WORKERS, VillageColonyMod.STORAGES);

        context.assertTrue(
                VillageColonyMod.STORAGES.of(villager.getUuid()).isEmpty(),
                "aldeão sem função reivindicou baú");

        // Com profissão, no ciclo seguinte, ele reivindica.
        VillageColonyMod.WORKERS.find(villager.getUuid()).orElseThrow()
                .assign(ProfessionType.LUMBERJACK);

        VillagerScanner.scan(world, colony, VillageColonyMod.WORKERS, VillageColonyMod.STORAGES);

        context.assertTrue(
                VillageColonyMod.STORAGES.of(villager.getUuid()).isPresent(),
                "o lenhador não conseguiu o baú ao lado da própria cama");

        VillageColonyMod.COLONIES.clear();
        VillageColonyMod.WORKERS.clear();
        VillageColonyMod.STORAGES.clear();

        context.complete();
    }

    /**
     * Baú em chunk descarregado torna a contagem parcial.
     *
     * <p>É a precondição de um congelamento inteiro, e não tinha teste
     * nenhum. O {@code runCycleOf} pula o ciclo da colônia quando a
     * varredura vem parcial — decisão certa desde 2026-08-07, porque
     * decidir sobre meio estoque manda buscar o que já se tem —, e até
     * 2026-09-04 ele pulava <b>calado</b>: uma colônia com um único baú
     * fora de alcance não fazia nada, ciclo após ciclo, sem uma linha no
     * log.
     *
     * <p>O que se fixa aqui é o gatilho: um baú registrado longe conta
     * como inalcançável, e não como vazio. A diferença entre os dois é a
     * diferença entre "a colônia não tem madeira" e "eu não consegui
     * olhar" — o defeito-que-parece-número que o V5 do §7 nomeou.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "storage_partial_count")
    public void aChestInAnUnloadedChunkMakesTheCountPartial(TestContext context) {
        ServerWorld world = context.getWorld();

        BlockPos here = new BlockPos(2, 2, 2);

        context.setBlockState(here, Blocks.CHEST.getDefaultState());

        Colony colony = Colony.create(
                UUID.randomUUID(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(here)));

        VillageColonyMod.COLONIES.register(colony);

        UUID near = UUID.randomUUID();
        UUID far = UUID.randomUUID();

        ColonyFixture owned = ColonyFixture.create()
                .owning(colony)
                .owning(near)
                .owning(far);

        VillageColonyMod.WORKERS.register(near, colony.id()).assign(ProfessionType.LUMBERJACK);

        VillageColonyMod.STORAGES.register(WorkerStorage.of(
                near, MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(here))));

        // Quatro milhões de blocos: dentro da borda do mundo e fora de
        // qualquer chunk que esta bateria carregue.
        VillageColonyMod.WORKERS.register(far, colony.id()).assign(ProfessionType.LUMBERJACK);

        VillageColonyMod.STORAGES.register(
                WorkerStorage.of(far, new ColonyPos(4_000_000, 64, 4_000_000)));

        try {
            ChestInventoryReader.ChestSurvey survey = ChestInventoryReader.survey(
                    world, List.of(near, far), VillageColonyMod.STORAGES);

            context.assertTrue(
                    survey.isPartial(),
                    "o baú fora de alcance passou por lido, e a colônia decidiria sobre"
                            + " meio estoque");

            context.assertTrue(
                    survey.chestsUnreachable() == 1,
                    "esperava 1 baú inalcançável, deu " + survey.chestsUnreachable());

            context.assertTrue(
                    survey.chestsRead() == 1,
                    "esperava 1 baú lido, deu " + survey.chestsRead());
        } finally {
            owned.cleanUp();
        }

        context.complete();
    }
}
