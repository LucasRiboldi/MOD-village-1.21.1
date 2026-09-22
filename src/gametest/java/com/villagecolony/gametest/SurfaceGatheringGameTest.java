package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskState;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.BlockProtection;
import com.villagecolony.fabric.integration.DirtPatch;
import com.villagecolony.fabric.integration.ChestInventoryReader;
import com.villagecolony.fabric.integration.ClayPatch;
import com.villagecolony.fabric.integration.FarthestVillageSector;
import com.villagecolony.fabric.integration.GrassPatch;
import com.villagecolony.fabric.integration.RingSweep;
import com.villagecolony.fabric.integration.SandPatch;
import com.villagecolony.fabric.integration.WorkerEquipment;
import com.villagecolony.fabric.work.SurfaceGatheringWork;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.List;
import java.util.UUID;

/** Prova a coleta de grass_block fora da vila com a ferramenta do fundidor. */
public class SurfaceGatheringGameTest implements FabricGameTest {

    private static final BlockPos CHEST = new BlockPos(2, 2, 2);

    /**
     * De que a recusa do cenário é feita — 2026-09-22.
     *
     * <p>Estas quatro provas caem sozinhas de vez em quando: em 22-09, três
     * rodadas seguidas sem tocar em código deram nenhuma falha, depois a da
     * relva e a da terra <b>juntas</b>, depois nenhuma outra vez. A mensagem
     * antiga — <i>"o cenário precisa expor X elegível"</i> — diz que o
     * {@code Patch} recusou, e não <b>qual</b> das saídas dele fechou.
     *
     * <p>São cinco portas, e elas pedem consertos diferentes: a coluna fora
     * do setor é conta de geometria; o chunk nulo é carregamento; o bloco
     * errado é a escrita que não pegou; o teto não vazio é vizinhança; e a
     * proteção negada é estado global de outra prova da bateria. Sem separar,
     * o próximo a investigar recomeça do zero — e o setor é sorteado do hash
     * de um {@code UUID.randomUUID()}, então ele nem sabe para que lado o
     * cenário apontou na rodada que caiu.
     */
    private static String refusal(
            ServerWorld world, BlockPos target, BlockPos center, Direction sector) {

        BlockState state = world.getBlockState(target);

        return " — setor " + sector
                + ", alvo " + target
                + ", centro " + center
                + ", no setor: " + FarthestVillageSector.isInSector(center, target, sector)
                + ", chunk carregado: "
                + (world.getChunkManager().getWorldChunk(target.getX() >> 4, target.getZ() >> 4)
                        != null)
                + ", bloco " + state
                + ", acima " + world.getBlockState(target.up())
                + ", pode quebrar: " + BlockProtection.mayBreak(world, target, state)
                // As tres portas separadas: uma acusa folha do jogador, outra
                // caixa registrada por outra prova da bateria, e a terceira
                // vila que o proprio jogo gerou naquele pedaco de terreno. Sao
                // consertos opostos, e "pode quebrar: false" sozinho nao diz
                // qual delas fechou.
                + " [jogador: " + BlockProtection.isPlayerPlaced(state)
                + ", colonia: " + BlockProtection.isColonyBuilt(target)
                + ", vila: " + BlockProtection.isVillageOriginal(world, target) + "]";
    }

    /**
     * Por que o trabalhador do cenário não chegou ao {@code ServerWorld}.
     *
     * <p>Mesmo motivo do {@link #refusal}: o aldeão nasce a 65 ou 97 blocos
     * da arena, na direção sorteada, e a prova só dizia que ele não estava
     * registrado. Separar <b>removido</b> de <b>chunk ausente</b> decide
     * entre uma limpeza de outra prova da bateria e o carregamento do setor.
     *
     * <p><b>Três hipóteses já caíram aqui — 2026-09-22.</b> Quem retomar
     * isto não precisa repeti-las:
     *
     * <ol>
     *   <li><i>o chunk está descarregado</i> — não está. A medida diz
     *       {@code chunk carregado: true}, e {@code setBlockState} carrega o
     *       chunk sozinho, então a suspeita já nasce refutada em qualquer
     *       posição onde a prova escreveu bloco;
     *   <li><i>falta ticket de tique de entidade</i> — {@code setChunkForced}
     *       foi medido com {@code forçado: true} e não mudou nada;
     *   <li><i>o ticket só vale no tique seguinte</i> — nascer o trabalhador
     *       no tique 2 e medir no 5 também não mudou nada.
     * </ol>
     *
     * <p>O que sobra medido é isto: {@code spawnEntity} devolve
     * <b>verdadeiro</b>, a entidade <b>não</b> está removida e está na
     * posição exata, e mesmo assim o mundo não a devolve nem pelo índice de
     * uuid nem por varredura. A premissa de que o trabalhador consegue
     * existir tão longe da arena é o que precisa ser questionado, e não o
     * carregamento.
     */
    private static String stranding(
            ServerWorld world, BlockPos stand, Direction sector, VillagerEntity villager) {

        // "no mundo" pergunta por varredura, e nao pelo indice de uuid:
        // as duas respostas juntas separam entidade ausente de entidade
        // presente com indice furado.
        boolean seen = false;

        for (net.minecraft.entity.Entity each
                : world.getEntitiesByType(EntityType.VILLAGER, entity -> true)) {

            if (each == villager) {
                seen = true;
                break;
            }
        }

        return " — setor " + sector
                + ", pé em " + stand
                + ", chunk carregado: "
                + (world.getChunkManager().getWorldChunk(stand.getX() >> 4, stand.getZ() >> 4)
                        != null)
                + ", no mundo: " + seen
                + ", removido: " + villager.isRemoved()
                + ", em " + villager.getBlockPos();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "surface_resource_catalogue",
            tickLimit = 20)
    public void dirtIsARecognizedSurfaceResource(TestContext context) {
        context.assertTrue(
                MinecraftTypeAdapter.toResourceType(Items.DIRT).isPresent(),
                "dirt precisa entrar no catálogo para a obra gerar uma tarefa de coleta ao fazendeiro");
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "surface_grass_gathering", tickLimit = 100)
    public void smelterGathersGrassOutsideTheProtectedVillageRadius(TestContext context) {
        var world = context.getWorld();
        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());

        BlockPos center = context.getAbsolutePos(CHEST);
        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(center);
        Colony colony = Colony.create(UUID.randomUUID(), chest);
        VillageColonyMod.COLONIES.register(colony);
        ColonyFixture owned = ColonyFixture.create().owning(colony);

        var sector = FarthestVillageSector.farthestLoadedSector(world, center, colony.id());
        BlockPos grass = center.offset(sector, FarthestVillageSector.PROTECTED_RADIUS + 1);
        BlockPos stand = grass.offset(sector.rotateYClockwise(), 2);

        world.setBlockState(grass.down(), Blocks.DIRT.getDefaultState());
        world.setBlockState(grass, Blocks.GRASS_BLOCK.getDefaultState());
        world.setBlockState(grass.up(), Blocks.AIR.getDefaultState());
        context.assertTrue(
                GrassPatch.in(world, grass, center.getY(), center, sector).filter(grass::equals).isPresent(),
                "o cenário precisa expor grass_block elegível no setor carregado"
                        + refusal(world, grass, center, sector));
        world.setBlockState(stand.down(), Blocks.DIRT.getDefaultState());
        VillagerEntity villager = EntityType.VILLAGER.create(world);
        villager.setBreedingAge(0);
        villager.refreshPositionAndAngles(
                stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, 0.0f, 0.0f);
        context.assertTrue(world.spawnEntity(villager), "não foi possível criar o fundidor no setor de coleta");
        context.runAtTick(1, () -> {
            try {
                context.assertTrue(world.getEntity(villager.getUuid()) == villager,
                        "fundidor criado no setor não está registrado no ServerWorld"
                                + stranding(world, stand, sector, villager));
                Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
                worker.assign(ProfessionType.SMELTER);
                VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chest));
                owned.owning(villager.getUuid());
                WorkerEquipment.equip(world, List.of(worker));
                context.assertTrue(
                        RingSweep.around(villager.getUuid(), grass, 48,
                                column -> GrassPatch.in(world, column, center.getY(), center, sector))
                                .filter(grass::equals).isPresent(),
                        "o RingSweep não encontrou a coluna que GrassPatch aceitou diretamente");

                Task task = VillageColonyMod.TASKS.create(
                        colony.id(), TaskType.COLLECT_SURFACE_RESOURCE,
                        TaskPriority.PRODUCTION, ResourceType.GRASS_BLOCK, 1);
                task.reserveFor(villager.getUuid());
                int opened = SurfaceGatheringWork.run(world, colony);
                context.assertTrue(opened == 1, "o coletor não abriu a tarefa reservada: " + task.state());

                for (int tick = 0; tick < 80 && !world.getBlockState(grass).isAir(); tick++) {
                    SurfaceGatheringWork.tick(world);
                }

                context.assertTrue(
                        world.getBlockState(grass).isAir(),
                        "o fundidor não removeu o grass_block do setor externo escolhido");
                context.assertTrue(
                        villager.getEquippedStack(EquipmentSlot.MAINHAND).isOf(Items.IRON_SHOVEL),
                        "o fundidor não estava com a pá de ferro do mod");
                context.assertTrue(
                        ChestInventoryReader.read(world, center).amountOf(ResourceType.GRASS_BLOCK) == 1,
                        "o grass_block não chegou ao baú pessoal do fundidor");
                context.assertTrue(
                        task.state() == TaskState.COMPLETED,
                        "a tarefa não foi concluída após recolher o bloco: " + task.state());
                context.complete();
            } finally {
                owned.cleanUp();
            }
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "surface_sand_gathering", tickLimit = 100)
    public void smelterGathersSandOnlyOutsideTheProtectedVillageRadius(TestContext context) {
        var world = context.getWorld();
        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());

        BlockPos center = context.getAbsolutePos(CHEST);
        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(center);
        Colony colony = Colony.create(UUID.randomUUID(), chest);
        VillageColonyMod.COLONIES.register(colony);
        ColonyFixture owned = ColonyFixture.create().owning(colony);

        var sector = FarthestVillageSector.farthestLoadedSector(world, center, colony.id());
        BlockPos near = center.offset(sector, FarthestVillageSector.PROTECTED_RADIUS - 1);
        BlockPos sand = center.offset(sector, FarthestVillageSector.PROTECTED_RADIUS + 1);
        BlockPos stand = sand.offset(sector.rotateYClockwise(), 2);

        world.setBlockState(near.down(), Blocks.SANDSTONE.getDefaultState());
        world.setBlockState(near, Blocks.SAND.getDefaultState());
        world.setBlockState(near.up(), Blocks.AIR.getDefaultState());

        world.setBlockState(sand.down(), Blocks.SANDSTONE.getDefaultState());
        world.setBlockState(sand, Blocks.SAND.getDefaultState());
        world.setBlockState(sand.up(), Blocks.AIR.getDefaultState());
        context.assertTrue(
                SandPatch.in(world, sand, center.getY()).filter(sand::equals).isPresent(),
                "o cenário precisa expor areia elegível no setor externo"
                        + refusal(world, sand, center, sector));
        world.setBlockState(stand.down(), Blocks.SANDSTONE.getDefaultState());
        VillagerEntity villager = EntityType.VILLAGER.create(world);
        villager.setBreedingAge(0);
        villager.refreshPositionAndAngles(
                stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, 0.0f, 0.0f);
        context.assertTrue(world.spawnEntity(villager), "não foi possível criar o fundidor no setor de coleta");
        context.runAtTick(1, () -> {
            try {
                Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
                worker.assign(ProfessionType.SMELTER);
                VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chest));
                owned.owning(villager.getUuid());
                WorkerEquipment.equip(world, List.of(worker));

                Task task = VillageColonyMod.TASKS.create(
                        colony.id(), TaskType.COLLECT_SURFACE_RESOURCE,
                        TaskPriority.PRODUCTION, ResourceType.SAND, 1);
                task.reserveFor(villager.getUuid());
                int opened = SurfaceGatheringWork.run(world, colony);
                context.assertTrue(opened == 1, "o coletor não abriu a tarefa de areia reservada: " + task.state());

                for (int tick = 0; tick < 80 && !world.getBlockState(sand).isAir(); tick++) {
                    SurfaceGatheringWork.tick(world);
                }

                context.assertTrue(world.getBlockState(near).isOf(Blocks.SAND),
                        "a coleta de areia invadiu o raio protegido da vila");
                context.assertTrue(world.getBlockState(sand).isAir(),
                        "o fundidor não removeu a areia do setor externo escolhido");
                context.assertTrue(
                        villager.getEquippedStack(EquipmentSlot.MAINHAND).isOf(Items.IRON_SHOVEL),
                        "o fundidor não estava com a pá de ferro do mod");
                context.assertTrue(
                        ChestInventoryReader.read(world, center).amountOf(ResourceType.SAND) == 1,
                        "a areia não chegou ao baú pessoal do fundidor");
                context.assertTrue(
                        task.state() == TaskState.COMPLETED,
                        "a tarefa de areia não foi concluída: " + task.state());
                context.complete();
            } finally {
                owned.cleanUp();
            }
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "surface_clay_gathering", tickLimit = 100)
    public void smelterGathersClayBallsWithItsSilkTouchShovel(TestContext context) {
        var world = context.getWorld();
        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());

        BlockPos center = context.getAbsolutePos(CHEST);
        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(center);
        Colony colony = Colony.create(UUID.randomUUID(), chest);
        VillageColonyMod.COLONIES.register(colony);
        ColonyFixture owned = ColonyFixture.create().owning(colony);

        var sector = FarthestVillageSector.farthestLoadedSector(world, center, colony.id());
        BlockPos clay = center.offset(sector, FarthestVillageSector.PROTECTED_RADIUS + 1);
        BlockPos stand = clay.offset(sector.rotateYClockwise(), 2);

        world.setBlockState(clay.down(), Blocks.DIRT.getDefaultState());
        world.setBlockState(clay, Blocks.CLAY.getDefaultState());
        world.setBlockState(clay.up(), Blocks.AIR.getDefaultState());
        context.assertTrue(
                ClayPatch.in(world, clay, center.getY()).filter(clay::equals).isPresent(),
                "o cenário precisa expor argila elegível no setor externo"
                        + refusal(world, clay, center, sector));
        world.setBlockState(stand.down(), Blocks.DIRT.getDefaultState());
        VillagerEntity villager = EntityType.VILLAGER.create(world);
        villager.setBreedingAge(0);
        villager.refreshPositionAndAngles(
                stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, 0.0f, 0.0f);
        context.assertTrue(world.spawnEntity(villager), "não foi possível criar o fundidor no setor de coleta");
        context.runAtTick(1, () -> {
            try {
                Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
                worker.assign(ProfessionType.SMELTER);
                VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chest));
                owned.owning(villager.getUuid());
                WorkerEquipment.equip(world, List.of(worker));

                Task task = VillageColonyMod.TASKS.create(
                        colony.id(), TaskType.COLLECT_SURFACE_RESOURCE,
                        TaskPriority.PRODUCTION, ResourceType.CLAY_BALL, 4);
                task.reserveFor(villager.getUuid());
                context.assertTrue(SurfaceGatheringWork.run(world, colony) == 1,
                        "o coletor não abriu a tarefa de bolas de argila reservada: " + task.state());

                for (int tick = 0; tick < 80 && !world.getBlockState(clay).isAir(); tick++) {
                    SurfaceGatheringWork.tick(world);
                }

                context.assertTrue(world.getBlockState(clay).isAir(),
                        "o fundidor não removeu a argila do setor externo escolhido");
                context.assertTrue(
                        ChestInventoryReader.read(world, center).amountOf(ResourceType.CLAY_BALL) == 4,
                        "a pá com Toque Suave não pode impedir a entrega de bolas de argila");
                context.assertTrue(task.state() == TaskState.COMPLETED,
                        "a tarefa de bolas de argila não foi concluída: " + task.state());
                context.complete();
            } finally {
                owned.cleanUp();
            }
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "surface_dirt_gathering", tickLimit = 160)
    public void farmerGathersDirtOutsideTheSoilProtectedRadius(TestContext context) {
        var world = context.getWorld();
        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());

        BlockPos center = context.getAbsolutePos(CHEST);
        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(center);
        Colony colony = Colony.create(UUID.randomUUID(), chest);
        VillageColonyMod.COLONIES.register(colony);
        ColonyFixture owned = ColonyFixture.create().owning(colony);

        var sector = FarthestVillageSector.farthestLoadedSector(world, center, colony.id());
        BlockPos near = center.offset(sector, FarthestVillageSector.SOIL_PROTECTED_RADIUS - 1);
        BlockPos dirt = center.offset(sector, FarthestVillageSector.SOIL_PROTECTED_RADIUS + 1);
        BlockPos stand = dirt.offset(sector.rotateYClockwise(), 2);

        world.setBlockState(near.down(), Blocks.DIRT.getDefaultState());
        world.setBlockState(near, Blocks.DIRT.getDefaultState());
        world.setBlockState(near.up(), Blocks.AIR.getDefaultState());

        world.setBlockState(dirt.down(), Blocks.DIRT.getDefaultState());
        world.setBlockState(dirt, Blocks.DIRT.getDefaultState());
        world.setBlockState(dirt.up(), Blocks.AIR.getDefaultState());
        context.assertTrue(
                DirtPatch.in(world, dirt, center.getY(), center, sector).filter(dirt::equals).isPresent(),
                "o cenário precisa expor terra elegível no setor carregado"
                        + refusal(world, dirt, center, sector));
        world.setBlockState(stand.down(), Blocks.DIRT.getDefaultState());
        VillagerEntity villager = EntityType.VILLAGER.create(world);
        villager.setBreedingAge(0);
        villager.refreshPositionAndAngles(
                stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, 0.0f, 0.0f);
        context.assertTrue(world.spawnEntity(villager), "não foi possível criar o fazendeiro no setor de coleta");
        context.runAtTick(1, () -> {
            try {
                context.assertTrue(world.getEntity(villager.getUuid()) == villager,
                        "fazendeiro criado no setor não está registrado no ServerWorld"
                                + stranding(world, stand, sector, villager));
                Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
                worker.assign(ProfessionType.FARMER);
                VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chest));
                owned.owning(villager.getUuid());
                WorkerEquipment.equip(world, List.of(worker));
                context.assertTrue(
                        RingSweep.around(villager.getUuid(), dirt, 48,
                                column -> DirtPatch.in(world, column, center.getY(), center, sector))
                                .filter(dirt::equals).isPresent(),
                        "o RingSweep não encontrou a coluna que DirtPatch aceitou diretamente");

                Task task = VillageColonyMod.TASKS.create(
                        colony.id(), TaskType.COLLECT_SOIL,
                        TaskPriority.PRODUCTION, ResourceType.DIRT, 1);
                task.reserveFor(villager.getUuid());
                int opened = SurfaceGatheringWork.run(world, colony);
                context.assertTrue(opened == 1, "o coletor não abriu a tarefa reservada: " + task.state());

                for (int tick = 0; tick < 120 && !world.getBlockState(dirt).isAir(); tick++) {
                    SurfaceGatheringWork.tick(world);
                }

                context.assertTrue(world.getBlockState(near).isOf(Blocks.DIRT),
                        "a coleta de terra invadiu o raio protegido ampliado da vila");
                context.assertTrue(
                        world.getBlockState(dirt).isAir(),
                        "o fazendeiro não removeu a terra do setor externo escolhido");
                context.assertTrue(
                        villager.getEquippedStack(EquipmentSlot.MAINHAND).isOf(Items.IRON_HOE),
                        "o fazendeiro não estava com a enxada de ferro do mod");
                context.assertTrue(
                        ChestInventoryReader.read(world, center).amountOf(ResourceType.DIRT) == 1,
                        "a terra não chegou ao baú pessoal do fazendeiro");
                context.assertTrue(
                        task.state() == TaskState.COMPLETED,
                        "a tarefa não foi concluída após recolher o bloco: " + task.state());
                context.complete();
            } finally {
                owned.cleanUp();
            }
        });
    }
}
