package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.integration.VillagerScanner;
import com.villagecolony.core.colony.service.VillageDetector;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.ResourceGroup;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.event.VillageDetectionHandler;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.ChestInventoryReader;
import com.villagecolony.fabric.integration.ColonyChestSurvey;
import com.villagecolony.fabric.integration.ColonyChests;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;

import java.util.UUID;
import java.util.List;
import java.util.Set;
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

        VillagerEntity villager = prepareStorageWorker(context, bed);
        scanStorage(context, villager);

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

        VillagerEntity villager = prepareStorageWorker(context, bed);
        scanStorage(context, villager);

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

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "storage_count")
    public void theColonyCountsTypedAndUncataloguedItemsByExactId(TestContext context) {
        BlockPos chest = new BlockPos(1, 1, 1);
        context.setBlockState(chest, Blocks.CHEST.getDefaultState());
        BlockPos absoluteChest = context.getAbsolutePos(chest);
        if (context.getWorld().getBlockEntity(absoluteChest) instanceof ChestBlockEntity inventory) {
            inventory.setStack(0, new ItemStack(Items.OAK_LOG, 12));
            inventory.setStack(1, new ItemStack(Items.DIRT, 9));
        } else {
            context.throwGameTestException("não há baú em " + chest.toShortString());
        }

        var tally = ChestInventoryReader.read(
                context.getWorld(), absoluteChest);

        context.assertTrue(tally.amountOf(ResourceType.OAK_LOG) == 12,
                "a madeira tipada deve continuar chegando à cadeia existente");
        context.assertTrue(tally.amountOf(ResourceId.vanilla("oak_log")) == 12,
                "o ID exato da madeira deve estar disponível");
        context.assertTrue(tally.amountOf(ResourceId.vanilla("dirt")) == 9,
                "item fora do catálogo deve ser contado pelo ID registrado");
        context.complete();
    }

    /**
     * A fotografia usada pelo ciclo preserva o espaço que o depósito já
     * reconhece para cada grupo.
     *
     * <p>Há madeira, tábuas, item do jogador e slots vazios de propósito:
     * o espaço de um grupo aceita slots vazios e pilhas parciais do próprio
     * grupo, mas nunca usa uma pilha de outra coisa.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "storage_count")
    public void theSurveyKeepsCapacityForWoodAndPlanks(TestContext context) {
        BlockPos chest = new BlockPos(1, 1, 1);
        context.setBlockState(chest, Blocks.CHEST.getDefaultState());

        BlockPos absoluteChest = context.getAbsolutePos(chest);
        if (context.getWorld().getBlockEntity(absoluteChest) instanceof ChestBlockEntity inventory) {
            inventory.setStack(0, new ItemStack(Items.OAK_LOG, 12));
            inventory.setStack(1, new ItemStack(Items.OAK_PLANKS, 20));
            inventory.setStack(2, new ItemStack(Items.DIRT, 9));
        } else {
            context.throwGameTestException("não há baú em " + chest.toShortString());
        }

        ColonyPos position = MinecraftTypeAdapter.toColonyPos(absoluteChest);
        ChestInventoryReader.ChestSurvey survey = ChestInventoryReader.survey(
                context.getWorld(),
                List.of(position),
                ResourceGroup.WOOD,
                ResourceGroup.PLANKS);

        context.assertTrue(
                survey.freeSpaceForGroup(ResourceGroup.WOOD)
                        == ChestDepositor.freeSpaceForGroup(
                                context.getWorld(), position, ResourceGroup.WOOD),
                "a fotografia mudou o espaço disponível para madeira");
        context.assertTrue(
                survey.freeSpaceForGroup(ResourceGroup.PLANKS)
                        == ChestDepositor.freeSpaceForGroup(
                                context.getWorld(), position, ResourceGroup.PLANKS),
                "a fotografia mudou o espaço disponível para tábuas");

        context.complete();
    }

    /** A última fatia vê o item no último baú, sem publicar uma soma parcial antes. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "storage_count")
    public void aSlicedSurveyPublishesTheLastChestOnlyAfterItsRoundCloses(TestContext context) {
        ServerWorld world = context.getWorld();
        UUID colonyId = UUID.randomUUID();
        List<ColonyPos> chests = new java.util.ArrayList<>();

        for (int index = 0; index <= ColonyChestSurvey.CHESTS_PER_CYCLE; index++) {
            BlockPos relative = new BlockPos(1 + index, 1, 1);
            context.setBlockState(relative, Blocks.CHEST.getDefaultState());
            chests.add(MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(relative)));
        }

        BlockPos last = context.getAbsolutePos(new BlockPos(
                1 + ColonyChestSurvey.CHESTS_PER_CYCLE, 1, 1));
        if (world.getBlockEntity(last) instanceof ChestBlockEntity inventory) {
            inventory.setStack(0, new ItemStack(Items.OAK_LOG, 12));
        } else {
            context.throwGameTestException("não há o último baú da fotografia");
        }

        try {
            ChestInventoryReader.ChestSurvey first = ColonyChestSurvey.advance(world, colonyId, chests);
            context.assertTrue(first.isPending(), "a primeira fatia foi publicada como completa");
            context.assertTrue(
                    first.resources().total().amountOf(ResourceId.vanilla("oak_log")) == 0,
                    "o item do último baú apareceu antes da rodada fechar");

            ChestInventoryReader.ChestSurvey completed = ColonyChestSurvey.advance(world, colonyId, chests);
            context.assertTrue(!completed.isPartial(), "a última fatia não fechou a fotografia");
            context.assertTrue(
                    completed.resources().total().amountOf(ResourceId.vanilla("oak_log")) == 12,
                    "a fotografia final não incluiu o item do último baú");
        } finally {
            ColonyChestSurvey.forget(colonyId);
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "storage_count")
    public void pendingSurveyReadsSharedChestBeforeProfessionStorage(TestContext context) {
        ServerWorld world = context.getWorld();
        UUID colonyId = UUID.randomUUID();
        List<ColonyPos> professionChests = new java.util.ArrayList<>();
        for (int index = 0; index < ColonyChestSurvey.CHESTS_PER_CYCLE; index++) {
            BlockPos relative = new BlockPos(1 + index, 1, 1);
            context.setBlockState(relative, Blocks.CHEST);
            professionChests.add(MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(relative)));
        }
        BlockPos shared = new BlockPos(10, 1, 1);
        context.setBlockState(shared, Blocks.CHEST);
        if (world.getBlockEntity(context.getAbsolutePos(shared)) instanceof ChestBlockEntity inventory) {
            inventory.setStack(0, new ItemStack(Items.OAK_LOG, 12));
        } else context.throwGameTestException("não há o baú compartilhado");

        try {
            List<ColonyPos> allChests = new java.util.ArrayList<>(professionChests);
            allChests.add(MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(shared)));
            ChestInventoryReader.ChestSurvey survey = ColonyChestSurvey.advance(
                    world, colonyId, allChests, Set.copyOf(professionChests), ResourceGroup.WOOD);
            context.assertTrue(survey.isPending(), "nove baús devem manter a rodada pendente");
            context.assertTrue(survey.resources().total().amountOf(ResourceId.vanilla("oak_log")) == 12,
                    "a fatia pendente deve observar o baú compartilhado antes dos baús de profissão");
        } finally {
            ColonyChestSurvey.forget(colonyId);
        }
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

    /** Prepara um trabalhador sem acionar a fundação automática da vila. */
    private static VillagerEntity prepareStorageWorker(
            TestContext context, BlockPos bed) {
        VillageColonyMod.COLONIES.clear();
        VillageColonyMod.WORKERS.clear();
        VillageColonyMod.STORAGES.clear();

        ServerWorld world = context.getWorld();
        Colony colony = Colony.create(
                UUID.randomUUID(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(bed)));
        VillageColonyMod.COLONIES.register(colony);

        VillagerEntity villager = context.spawnEntity(
                EntityType.VILLAGER, bed.add(2, 1, 2));
        villager.setBreedingAge(0);
        villager.getBrain().remember(
                MemoryModuleType.HOME,
                GlobalPos.create(world.getRegistryKey(), context.getAbsolutePos(bed)));

        return villager;
    }

    /** Faz as duas passagens do scanner: registro e reivindicação. */
    private static void scanStorage(
            TestContext context, VillagerEntity villager) {
        ServerWorld world = context.getWorld();
        Colony colony = VillageColonyMod.COLONIES.all().stream()
                .findFirst().orElseThrow();

        VillagerScanner.scan(
                world, colony, VillageColonyMod.WORKERS,
                VillageColonyMod.STORAGES);
        VillageColonyMod.WORKERS.find(villager.getUuid()).orElseThrow()
                .assign(ProfessionType.LUMBERJACK);
        VillagerScanner.scan(
                world, colony, VillageColonyMod.WORKERS,
                VillageColonyMod.STORAGES);
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
     * Baú em chunk descarregado torna a contagem parcial e degradada.
     *
     * <p>O ciclo não pode tratar o baú como vazio, carregar o chunk à força
     * nem inventar o conteúdo ausente. Ele continua somente com o estoque
     * físico observado; a retirada confirma o item no ponto de uso e pode
     * seguir para outro baú carregado.
     *
     * <p>O que se fixa aqui é o gatilho: um baú registrado longe conta
     * como inalcançável, e não como vazio. A diferença entre os dois é a
     * diferença entre "a colônia não tem madeira" e "eu não consegui
     * olhar" — o defeito-que-parece-número que o V5 do §7 nomeou.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "storage_partial_count")
    public void aChestInAnUnloadedChunkMakesTheCountPartialAndDegraded(TestContext context) {
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
            // Pela lista da colônia desde 2026-09-11 — P0.3. Ela é quem
            // sabe que baús existem, e é a mesma que a varredura do ciclo
            // usa; montar uma lista à parte aqui faria este teste medir
            // um caminho que a produção não percorre.
            ChestInventoryReader.ChestSurvey survey = ChestInventoryReader.survey(
                    world,
                    ColonyChests.nearestFirst(world, colony.id(), colony.center()));

            context.assertTrue(
                    survey.isPartial(),
                    "o baú fora de alcance passou por lido");

            context.assertTrue(
                    survey.isDegraded(),
                    "o baú fora de alcance não deixou a fotografia degradada");

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
