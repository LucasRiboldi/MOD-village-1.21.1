package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.work.ShepherdFlock;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Os três "não" do rebanho — revisão de 2026-10-02: o pastor tinha 2 GameTests
 * contra dezenas do mineiro, e o {@code ShepherdFlock} só tinha o caminho feliz
 * coberto ({@code ProfessionReviewGameTest.theShepherdFeedsTwoSheepToBreed}).
 * Cada um em lote próprio: {@code ShepherdFlock.clearAll} é global.
 */
public class ShepherdFlockGameTest implements FabricGameTest {

    private static final BlockPos CHEST = new BlockPos(2, 1, 2);

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "flock_full")
    public void aFullFlockEatsNoWheat(TestContext context) {
        Scene scene = new Scene(context, 4, true);
        List<SheepEntity> flock = new ArrayList<>();

        for (int i = 0; i < ShepherdFlock.FLOCK_TARGET; i++) {
            flock.add(context.spawnEntity(EntityType.SHEEP, new BlockPos(3 + i % 4, 1, 4 + i / 4)));
        }

        try {
            context.assertFalse(ShepherdFlock.tend(context.getWorld(), scene.colony),
                    "rebanho de " + ShepherdFlock.FLOCK_TARGET + " adultas não procria mais");
            context.assertTrue(scene.wheat() == 4, "o rebanho cheio comeu trigo: sobraram " + scene.wheat());
            context.assertTrue(flock.stream().noneMatch(SheepEntity::isInLove), "uma ovelha do rebanho cheio entrou no cio");
        } finally {
            scene.cleanUp();
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "flock_one_wheat")
    public void oneWheatGoesBackToTheChest(TestContext context) {
        Scene scene = new Scene(context, 1, true);
        SheepEntity first = context.spawnEntity(EntityType.SHEEP, new BlockPos(4, 1, 4));
        SheepEntity second = context.spawnEntity(EntityType.SHEEP, new BlockPos(5, 1, 4));

        try {
            context.assertFalse(ShepherdFlock.tend(context.getWorld(), scene.colony),
                    "um trigo não alimenta um par");
            context.assertTrue(scene.wheat() == 1,
                    "o trigo que não bastava devia voltar ao baú, e não sumir: sobraram " + scene.wheat());
            context.assertFalse(first.isInLove() || second.isInLove(), "uma ovelha entrou no cio sem trigo");
        } finally {
            scene.cleanUp();
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "flock_no_shepherd")
    public void noShepherdNoBreeding(TestContext context) {
        Scene scene = new Scene(context, 4, false);
        SheepEntity first = context.spawnEntity(EntityType.SHEEP, new BlockPos(4, 1, 4));
        SheepEntity second = context.spawnEntity(EntityType.SHEEP, new BlockPos(5, 1, 4));

        try {
            context.assertFalse(ShepherdFlock.tend(context.getWorld(), scene.colony),
                    "sem pastor ninguém cuida do rebanho");
            context.assertTrue(scene.wheat() == 4, "sem pastor o trigo foi gasto: sobraram " + scene.wheat());
            context.assertFalse(first.isInLove() || second.isInLove(), "uma ovelha entrou no cio sem pastor");
        } finally {
            scene.cleanUp();
        }

        context.complete();
    }

    /** O baú com trigo, a colônia e, se pedido, o pastor. */
    private static final class Scene {

        final Colony colony;

        private final ChestBlockEntity chest;

        private final ColonyFixture fixture;

        Scene(TestContext context, int wheat, boolean shepherd) {
            context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());
            chest = (ChestBlockEntity) context.getBlockEntity(CHEST);
            chest.setStack(0, new ItemStack(Items.WHEAT, wheat));

            ColonyPos center = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));
            colony = Colony.create(UUID.randomUUID(), center);
            VillageColonyMod.COLONIES.register(colony);
            fixture = ColonyFixture.create().owning(colony);

            // O baú é da colônia pelo vínculo de um trabalhador: com pastor é
            // o dele; sem pastor, de um aldeão sem ofício.
            UUID workerId = UUID.randomUUID();
            var worker = VillageColonyMod.WORKERS.register(workerId, colony.id());

            if (shepherd) {
                worker.assign(ProfessionType.SHEPHERD);
            }

            VillageColonyMod.STORAGES.register(WorkerStorage.of(workerId, center));
            fixture.owning(workerId);
            ShepherdFlock.clearAll();
        }

        int wheat() {
            int total = 0;

            for (int slot = 0; slot < chest.size(); slot++) {
                if (chest.getStack(slot).isOf(Items.WHEAT)) {
                    total += chest.getStack(slot).getCount();
                }
            }

            return total;
        }

        void cleanUp() {
            fixture.cleanUp();
            ShepherdFlock.clearAll();
        }
    }
}
