package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.brain.WorkTargets;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.Schedule;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * O construtor larga o lugar de pé que a navegação não alcança — playtest de
 * 2026-10-03, casa do pastor de {@code -292, 65, 386}: o lugar mais perto
 * dele ficava dentro da casa fechada, e ele esperava o guarda de trezentos
 * tiques do lado de fora.
 */
public final class BuilderRefusedSpotGameTest implements FabricGameTest {

    /** O bolsão selado: chão de pedra, vidro dos quatro lados e por cima. */
    private static final BlockPos POCKET = new BlockPos(6, 2, 6);

    private static final BlockPos REACHABLE = new BlockPos(9, 2, 9);

    private static final BlockPos SPAWN = new BlockPos(2, 2, 2);

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "builder_refused_spot")
    public void aRefusedSpotSendsTheBuilderToAnotherOne(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos target = context.getAbsolutePos(new BlockPos(8, 10, 8));
        ConstructionProject project = ConstructionProject.plan(
                UUID.randomUUID(),
                Blueprint.of(
                        ResourceId.vanilla("village/plains/houses/refused_spot"),
                        List.of(new BlueprintBlock(new ColonyPos(0, 0, 0), ResourceId.vanilla("oak_planks")))),
                MinecraftTypeAdapter.toColonyPos(target));

        // Pedra em toda a faixa que a busca lê, e dois lugares de pé abertos.
        for (int x = 3; x <= 13; x++) {
            for (int z = 3; z <= 13; z++) {
                for (int y = 4; y <= 16; y++) {
                    context.setBlockState(new BlockPos(x, y, z), Blocks.STONE.getDefaultState());
                }
            }
        }

        for (BlockPos open : new BlockPos[] {new BlockPos(9, 10, 8), new BlockPos(7, 10, 8)}) {
            context.setBlockState(open, Blocks.AIR.getDefaultState());
            context.setBlockState(open.up(), Blocks.AIR.getDefaultState());
        }

        BlockPos east = context.getAbsolutePos(new BlockPos(9, 10, 8));
        BlockPos west = context.getAbsolutePos(new BlockPos(7, 10, 8));
        BlockPos worker = target.east(12);

        context.assertTrue(
                BuilderApproach.footOf(world, project, target, worker, Set.of()).equals(Optional.of(east)),
                "sem recusa, o lugar devia ser o do lado do construtor");
        context.assertTrue(
                BuilderApproach.footOf(world, project, target, worker, Set.of(east)).equals(Optional.of(west)),
                "recusado o lado leste, o construtor devia ir ao oeste");
        context.assertTrue(
                BuilderApproach.footOf(world, project, target, worker, Set.of(east, west)).isEmpty(),
                "com todo lugar recusado a peça não se alcança daqui, e a resposta é vazia");
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "builder_refused_spot", tickLimit = 200)
    public void theNavigationsRefusalStrikesASealedSpot(TestContext context) {
        walkTo(context, POCKET, true);
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "builder_refused_spot", tickLimit = 200)
    public void aSpotTheBuilderCanWalkToIsKept(TestContext context) {
        walkTo(context, REACHABLE, false);
    }

    /**
     * Um aldeão de verdade andando até {@code spot} pela task do mod, e a
     * pergunta do construtor feita a cada tique, como no {@code step}.
     */
    private static void walkTo(TestContext context, BlockPos spot, boolean sealed) {
        ServerWorld world = context.getWorld();

        world.setTimeOfDay(Schedule.WORK_TIME);

        for (int x = 0; x <= 12; x++) {
            for (int z = 0; z <= 12; z++) {
                context.setBlockState(new BlockPos(x, 1, z), Blocks.STONE.getDefaultState());

                for (int y = 2; y <= 4; y++) {
                    context.setBlockState(new BlockPos(x, y, z), Blocks.AIR.getDefaultState());
                }
            }
        }

        // Vidro, e não pedra: bloco sólido no chão vira base de aterro.
        for (BlockPos side : new BlockPos[] {POCKET.east(), POCKET.west(), POCKET.north(), POCKET.south()}) {
            context.setBlockState(side, Blocks.GLASS.getDefaultState());
            context.setBlockState(side.up(), Blocks.GLASS.getDefaultState());
        }

        context.setBlockState(POCKET.up(2), Blocks.GLASS.getDefaultState());

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, SPAWN);
        villager.setBreedingAge(0);

        UUID id = villager.getUuid();
        BlockPos destination = context.getAbsolutePos(spot);
        UnreachableSpots spots = new UnreachableSpots();
        int[] tick = {0};
        boolean[] done = {false};

        WorkTargets.set(id, destination, BuilderApproach.ARRIVAL);

        context.runAtEveryTick(() -> {
            if (done[0]) {
                return;
            }

            tick[0]++;

            boolean refused = spots.gaveUp(world, villager, destination);

            if (sealed && refused) {
                done[0] = true;
                forget(villager);
                context.assertTrue(spots.spots().contains(destination), "o lugar recusado não ficou riscado");
                context.complete();

                return;
            }

            if (!sealed && refused) {
                done[0] = true;
                forget(villager);
                context.throwPositionedException("um lugar alcançável foi recusado, aldeão em "
                        + villager.getBlockPos().toShortString(), destination);
            }

            if (tick[0] == 150) {
                done[0] = true;
                forget(villager);

                if (sealed) {
                    context.throwPositionedException("o bolsão selado não foi recusado em 150 tiques; aldeão em "
                            + villager.getBlockPos().toShortString(), destination);
                }

                context.complete();
            }
        });
    }

    private static void forget(VillagerEntity villager) {
        WorkTargets.clear(villager.getUuid());
        villager.discard();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "builder_refused_spot")
    public void aPaneWithNoSandInTheWorldIsNotWaitedFor(TestContext context) {
        ServerWorld world = context.getWorld();
        UUID colony = UUID.randomUUID();
        ConstructionProject project = ConstructionProject.plan(
                colony,
                Blueprint.of(
                        ResourceId.vanilla("village/plains/houses/no_sand"),
                        List.of(new BlueprintBlock(new ColonyPos(0, 0, 0), ResourceId.vanilla("glass_pane")))),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(4, 2, 4))));
        ResourceId pane = ResourceId.vanilla("glass_pane");

        try {
            context.assertFalse(TestBarrier.nothingToWaitFor(world, project, pane),
                    "sem a varredura vazia a vidraça ainda tem a carência");

            EmptySweeps.foundNothing(colony, ResourceType.SAND, world.getTime());

            context.assertTrue(TestBarrier.nothingToWaitFor(world, project, pane),
                    "o raio não tem areia e o baú não tem vidro: esperar não traz a vidraça");
            context.assertFalse(TestBarrier.nothingToWaitFor(world, project, ResourceId.vanilla("oak_door")),
                    "a falta de areia não diz nada da porta");
        } finally {
            EmptySweeps.found(colony, ResourceType.SAND);
        }

        context.complete();
    }
}
