package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.fabric.integration.TreeScanner;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.UUID;

/**
 * O lenhador derruba a espécie que a obra pede — T1 (autor, 2026-10-29): a casa de acácia esperava acácia enquanto o
 * lenhador cortava o carvalho mais perto. A busca prefere a espécie da tarefa; sem ela no alcance, a mais perto.
 */
public class TreeChoiceSpeciesGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "tree_species", tickLimit = 20)
    public void theLumberjackLooksForTheSpeciesTheTaskAsks(TestContext context) {
        UUID colony = UUID.randomUUID();
        BlockPos center = context.getAbsolutePos(new BlockPos(1, 2, 1));
        BlockPos oak = context.getAbsolutePos(new BlockPos(3, 2, 3));
        BlockPos acacia = context.getAbsolutePos(new BlockPos(9, 2, 9));

        tree(context, new BlockPos(3, 2, 3), Blocks.OAK_LOG, Blocks.OAK_LEAVES);
        tree(context, new BlockPos(9, 2, 9), Blocks.ACACIA_LOG, Blocks.ACACIA_LEAVES);

        Task wantsAcacia = VillageColonyMod.TASKS.create(
                colony, TaskType.COLLECT_WOOD, TaskPriority.CONSTRUCTION_MATERIAL, ResourceType.ACACIA_LOG, 8);
        Task wantsAnyOak = VillageColonyMod.TASKS.create(
                colony, TaskType.COLLECT_WOOD, TaskPriority.CONSTRUCTION_MATERIAL, ResourceType.DARK_OAK_LOG, 8);

        try {
            Optional<BlockPos> found = TreeScanner.findNearestLog(context.getWorld(), center, 16,
                    WantedTree.species(context.getWorld(), wantsAcacia));

            context.assertTrue(found.isPresent() && context.getWorld().getBlockState(found.get()).isOf(Blocks.ACACIA_LOG),
                    "a busca devia achar a acácia que a tarefa pede, achou " + found.map(at ->
                            context.getWorld().getBlockState(at).getBlock()) + "; carvalho em " + oak.toShortString()
                            + ", acácia em " + acacia.toShortString());

            context.assertTrue(TreeScanner.findNearestLog(context.getWorld(), center, 16,
                            WantedTree.species(context.getWorld(), wantsAnyOak)).isEmpty(),
                    "controle: sem a espécie pedida no alcance, o filtro não aceita outra");
        } finally {
            VillageColonyMod.TASKS.ofColony(colony).forEach(task -> VillageColonyMod.TASKS.remove(task.id()));
            TreeScanner.clearAll();
        }

        context.complete();
    }

    /** Tronco de 4 com copa em cima, num chão de terra. */
    private static void tree(TestContext context, BlockPos base, Block log, Block leaves) {
        context.setBlockState(base.down(), Blocks.DIRT);

        for (int y = 0; y < 4; y++) {
            context.setBlockState(base.up(y), log);
        }

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                context.setBlockState(base.add(dx, 4, dz), leaves);
            }
        }
    }
}
