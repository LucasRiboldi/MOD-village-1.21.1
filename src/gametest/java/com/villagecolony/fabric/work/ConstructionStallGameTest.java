package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ColonyChests;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * A peça que trava a obra aparece inteira no baú do construtor, e o caso fica registrado — G2,
 * pedido do autor, 2026-10-20.
 */
public class ConstructionStallGameTest implements FabricGameTest {

    /** Duas peças em falta não zeram o contador uma da outra (era um contador por obra). */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "construction_stall")
    public void twoMissingPiecesDoNotResetEachOther(TestContext context) {
        UUID project = UUID.randomUUID();

        try {
            ConstructionSupplyDispatches.recordCycleDispatch(project, Items.WHITE_WOOL, 1);
            ConstructionSupplyDispatches.recordCycleDispatch(project, Items.LANTERN, 1);
            ConstructionSupplyDispatches.recordCycleDispatch(project, Items.WHITE_WOOL, 2);

            context.assertTrue(ConstructionSupplyDispatches.fallbackDue(project, Items.WHITE_WOOL),
                    "a lanterna zerou a contagem da lã");
            context.assertFalse(ConstructionSupplyDispatches.fallbackDue(project, Items.LANTERN),
                    "a lanterna contou dois ciclos com um só");
        } finally {
            ConstructionSupplyDispatches.delivered(project, Items.WHITE_WOOL);
            ConstructionSupplyDispatches.delivered(project, Items.LANTERN);
        }

        context.complete();
    }

    /**
     * Peça posta de lado por falta de material, com a obra ainda construindo outra coisa: travou em
     * dois ciclos, aparece tudo o que a obra pede dela, e o registro ganha uma linha.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "construction_stall", tickLimit = 40)
    public void aPieceSetAsideForMaterialAppearsWholeInTheBuildersChest(TestContext context) {
        BlockPos chestAt = context.getAbsolutePos(new BlockPos(2, 2, 2));
        context.getWorld().setBlockState(chestAt, Blocks.CHEST.getDefaultState());
        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(chestAt);
        Colony colony = Colony.create(UUID.randomUUID(), chest);
        UUID builder = UUID.randomUUID();

        VillageColonyMod.COLONIES.register(colony);
        VillageColonyMod.WORKERS.register(builder, colony.id()).assign(ProfessionType.BUILDER);
        VillageColonyMod.STORAGES.register(WorkerStorage.of(builder, chest));

        // Bancada de flecheiro: sem fonte e sem receita que a colônia feche (lã e lanterna do mar não
        // servem — linha e prismarinho são drops, ingredientes automáticos).
        ResourceId table = MinecraftTypeAdapter.toResourceId(Blocks.FLETCHING_TABLE);
        BlueprintBlock first = new BlueprintBlock(new ColonyPos(0, 0, 0), table);
        BlueprintBlock second = new BlueprintBlock(new ColonyPos(1, 0, 0), table);
        BlueprintBlock stone = new BlueprintBlock(new ColonyPos(2, 0, 0), ResourceId.vanilla("cobblestone"));
        ConstructionProject project = ConstructionProject.plan(colony.id(),
                Blueprint.of(ResourceId.vanilla("village/plains/houses/stall"), List.of(first, second, stone)), chest);

        project.moveTo(ConstructionState.PREPARING);
        project.moveTo(ConstructionState.BUILDING);
        project.deferForMaterial(first);
        project.deferForMaterial(second);
        VillageColonyMod.CONSTRUCTIONS.register(project);

        Path ledger = context.getWorld().getServer().getRunDirectory()
                .resolve("villagecolony").resolve("supply-gaps.jsonl");
        long before = lines(ledger);

        ConstructionSupplyDispatches.renewForCycle(context.getWorld(), colony);

        context.waitAndRun(1, () -> {
            try {
                ConstructionSupplyDispatches.renewForCycle(context.getWorld(), colony);

                int stocked = ColonyChests.countIn(context.getWorld(), List.of(chest), Items.FLETCHING_TABLE);
                context.assertTrue(stocked == 2,
                        "a peça posta de lado não apareceu inteira no baú do construtor: " + stocked + " de 2"
                                + " (disponível " + com.villagecolony.fabric.integration.ColonySupply.canProvide(
                                        context.getWorld(), colony.id(), chest, Items.FLETCHING_TABLE)
                                + ", peças de lado " + project.deferredPieces().size() + ")");
                context.assertTrue(lines(ledger) > before, "o travamento não foi registrado em " + ledger);
            } finally {
                VillageColonyMod.CONSTRUCTIONS.removeOfColony(colony.id());
                VillageColonyMod.STORAGES.remove(builder);
                VillageColonyMod.WORKERS.remove(builder);
                VillageColonyMod.COLONIES.remove(colony.id());
                context.getWorld().setBlockState(chestAt, Blocks.AIR.getDefaultState());
            }

            context.complete();
        });
    }

    private static long lines(Path file) {
        try {
            return Files.exists(file) ? Files.readAllLines(file, StandardCharsets.UTF_8).size() : 0;
        } catch (IOException failure) {
            throw new AssertionError("não deu para ler " + file, failure);
        }
    }
}
