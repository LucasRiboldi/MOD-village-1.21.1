package com.villagecolony.gametest;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.fabric.work.DetourWalker;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * O desvio que cava e põe bloco, andando de verdade no mundo — ADR-025, fase 2.
 *
 * <p>O aldeão vai de passo em passo por teleporte, sem IA: a arena da bateria
 * não garante o cérebro andando (a mesma ressalva do
 * {@code StrandedEscapeGameTest}). O que se mede é o que o desvio faz no mundo —
 * o que abre, o que põe, de onde tira o pedregulho e quando se recusa.
 */
public class DetourWalkerGameTest implements FabricGameTest {

    private static void stoneBox(TestContext context) {
        for (int x = 0; x <= 10; x++) {
            for (int y = 1; y <= 6; y++) {
                for (int z = 0; z <= 4; z++) {
                    context.setBlockState(new BlockPos(x, y, z), Blocks.STONE.getDefaultState());
                }
            }
        }
    }

    private static VillagerEntity minerAt(TestContext context, BlockPos feet) {
        context.setBlockState(feet, Blocks.AIR.getDefaultState());
        context.setBlockState(feet.up(), Blocks.AIR.getDefaultState());

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, feet);
        villager.setAiDisabled(true);
        villager.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));

        return villager;
    }

    /** Leva o aldeão para onde o desvio o mandou andar, e devolve o estado do tique. */
    private static DetourWalker.Status step(
            TestContext context, VillagerEntity villager, DetourWalker walker, ColonyPos chest) {

        DetourWalker.Status status = walker.tick(context.getWorld(), villager, chest);

        Optional<BlockPos> walkTo = WorkTargets.of(villager.getUuid());

        if (walkTo.isPresent() && !walkTo.get().equals(villager.getBlockPos())) {
            villager.refreshPositionAndAngles(
                    walkTo.get().getX() + 0.5, walkTo.get().getY(), walkTo.get().getZ() + 0.5, 0, 0);
        }

        return status;
    }

    private static void runToTheEnd(
            TestContext context, VillagerEntity villager, DetourWalker walker, ColonyPos chest,
            java.util.function.Consumer<DetourWalker.Status> check) {

        DetourWalker.Status[] last = {DetourWalker.Status.WALKING};

        context.runAtEveryTick(() -> {
            if (last[0] == DetourWalker.Status.WALKING) {
                last[0] = step(context, villager, walker, chest);

                if (last[0] != DetourWalker.Status.WALKING) {
                    WorkTargets.clear(villager.getUuid());
                    check.accept(last[0]);
                    context.complete();
                }
            }
        });
    }

    /** Emparedado a cinco blocos da pedra: ele abre o túnel até ter a pedra ao alcance. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "detour_walker", tickLimit = 400)
    public void aWalledInMinerDigsUntilTheStoneIsInReach(TestContext context) {
        stoneBox(context);

        VillagerEntity villager = minerAt(context, new BlockPos(2, 2, 2));
        BlockPos stone = context.getAbsolutePos(new BlockPos(7, 3, 2));
        Predicate<BlockPos> inReach = feet -> !feet.equals(stone)
                && feet.getSquaredDistance(stone.getX(), stone.getY() + 0.5, stone.getZ()) <= 16;

        DetourWalker walker = DetourWalker.plan(context.getWorld(), villager.getUuid(),
                villager.getBlockPos(), stone, inReach, Set.of(stone), false).orElseThrow();

        runToTheEnd(context, villager, walker, null, status -> {
            context.assertTrue(status == DetourWalker.Status.DONE, "o desvio parou: " + walker.why());
            context.assertTrue(walker.dug() > 0, "não cavou nada");
            context.assertTrue(inReach.test(villager.getBlockPos()),
                    "terminou fora do alcance da pedra: " + villager.getBlockPos().toShortString());
            context.assertTrue(context.getWorld().getBlockState(stone).isOf(Blocks.STONE),
                    "o desvio quebrou a pedra que era trabalho da picareta");
        });
    }

    /** Um vão de dois no corredor: a ponte sai do pedregulho do baú dele. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "detour_walker", tickLimit = 400)
    public void aGapIsBridgedWithCobblestoneFromTheChest(TestContext context) {
        ColonyPos chest = corridorWithAGap(context);
        ((ChestBlockEntity) context.getWorld().getBlockEntity(MinecraftTypeAdapter.toBlockPos(chest)))
                .setStack(0, new ItemStack(Items.COBBLESTONE, 5));

        VillagerEntity villager = minerAt(context, new BlockPos(1, 3, 2));
        BlockPos end = context.getAbsolutePos(new BlockPos(7, 3, 2));

        DetourWalker walker = DetourWalker.plan(context.getWorld(), villager.getUuid(),
                villager.getBlockPos(), end, end::equals, Set.of(), false).orElseThrow();

        runToTheEnd(context, villager, walker, chest, status -> {
            context.assertTrue(status == DetourWalker.Status.DONE, "o desvio parou: " + walker.why());
            context.assertTrue(walker.placed() == 2, "pôs " + walker.placed() + " blocos, o vão tinha 2;"
                    + " caminho " + walker.path() + "; terminou em " + villager.getBlockPos().toShortString());
            context.expectBlock(Blocks.COBBLESTONE, new BlockPos(4, 2, 2));
            context.expectBlock(Blocks.COBBLESTONE, new BlockPos(5, 2, 2));

            ChestBlockEntity box = (ChestBlockEntity) context.getWorld()
                    .getBlockEntity(MinecraftTypeAdapter.toBlockPos(chest));
            context.assertTrue(box.getStack(0).getCount() == 3,
                    "o baú devia ter dado dois pedregulhos: sobrou " + box.getStack(0).getCount());
        });
    }

    /** Sem pedregulho no baú não há ponte, e o desvio diz por quê. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "detour_walker", tickLimit = 400)
    public void withoutCobblestoneTheBridgeIsNotBuilt(TestContext context) {
        ColonyPos chest = corridorWithAGap(context);

        VillagerEntity villager = minerAt(context, new BlockPos(1, 3, 2));
        BlockPos end = context.getAbsolutePos(new BlockPos(7, 3, 2));

        DetourWalker walker = DetourWalker.plan(context.getWorld(), villager.getUuid(),
                villager.getBlockPos(), end, end::equals, Set.of(), false).orElseThrow();

        runToTheEnd(context, villager, walker, chest, status -> {
            context.assertTrue(status == DetourWalker.Status.FAILED, "atravessou sem material; caminho "
                    + walker.path() + "; terminou em " + villager.getBlockPos().toShortString());
            context.assertTrue(walker.why().contains("no cobblestone"), "motivo: " + walker.why());
            context.expectBlock(Blocks.AIR, new BlockPos(4, 2, 2));
        });
    }

    /**
     * Água que chega depois do plano: o passo é perguntado de novo, a pedra
     * encostada nela fica, e o desvio para em vez de abrir a nascente.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "detour_walker", tickLimit = 400)
    public void waterThatArrivesAfterThePlanStopsTheDetour(TestContext context) {
        stoneBox(context);

        VillagerEntity villager = minerAt(context, new BlockPos(2, 2, 2));
        BlockPos end = context.getAbsolutePos(new BlockPos(5, 2, 2));

        DetourWalker walker = DetourWalker.plan(context.getWorld(), villager.getUuid(),
                villager.getBlockPos(), end, end::equals, Set.of(), false).orElseThrow();

        // O plano foi feito em rocha seca; agora há água encostada no primeiro passo.
        context.setBlockState(new BlockPos(3, 4, 2), Blocks.WATER.getDefaultState());

        runToTheEnd(context, villager, walker, null, status -> {
            context.assertTrue(status == DetourWalker.Status.FAILED, "seguiu cavando junto da água");
            context.expectBlock(Blocks.STONE, new BlockPos(3, 3, 2));
        });
    }

    /**
     * Corredor em x 1..7 a y = 3, tudo em bedrock: chão, paredes e teto. Em x 4
     * e 5 o chão some num poço de dois blocos — fundo demais para descer e
     * subir de novo, e a bedrock não deixa cavar em volta. O baú fica fora.
     *
     * <p>A primeira versão tinha chão de pedra e poço raso, e o planejador
     * achou o caminho mais barato que ela não previa: desceu no poço e andou
     * pelo chão do mundo, logo abaixo da arena.
     */
    private static ColonyPos corridorWithAGap(TestContext context) {
        for (int x = 0; x <= 8; x++) {
            for (int y = 0; y <= 5; y++) {
                for (int z = 1; z <= 3; z++) {
                    context.setBlockState(new BlockPos(x, y, z), Blocks.BEDROCK.getDefaultState());
                }
            }
        }

        for (int x = 1; x <= 7; x++) {
            context.setBlockState(new BlockPos(x, 3, 2), Blocks.AIR.getDefaultState());
            context.setBlockState(new BlockPos(x, 4, 2), Blocks.AIR.getDefaultState());
        }

        for (int x = 4; x <= 5; x++) {
            context.setBlockState(new BlockPos(x, 2, 2), Blocks.AIR.getDefaultState());
            context.setBlockState(new BlockPos(x, 1, 2), Blocks.AIR.getDefaultState());
        }

        BlockPos chest = new BlockPos(1, 2, 5);
        context.setBlockState(chest, Blocks.CHEST.getDefaultState());

        return MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(chest));
    }
}
