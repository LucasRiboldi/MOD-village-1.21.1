package com.villagecolony.gametest;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ChestMarker;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A marca que diz de quem é o baú — 2026-08-12.
 *
 * <p>Escreve no mundo, e por isso os testes que importam são os que
 * provam o que ela <b>não</b> faz: não duplica a cada ciclo e não abre
 * espaço na construção do jogador.
 */
public class ChestMarkerGameTest implements FabricGameTest {

    private static final UUID WORKER = UUID.randomUUID();

    /** Um trabalhador com profissão e um baú registrado. */
    private static List<Worker> lumberjack() {
        Worker worker = Worker.register(WORKER, UUID.randomUUID());
        worker.assign(ProfessionType.LUMBERJACK);

        return List.of(worker);
    }

    private static ChestMarker.StorageLookup at(TestContext context, BlockPos chest) {
        ColonyPos position = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(chest));

        return workerId -> Optional.of(position);
    }

    private static int framesAround(TestContext context, BlockPos chest) {
        BlockPos absolute = context.getAbsolutePos(chest);

        return context.getWorld().getEntitiesByClass(
                ItemFrameEntity.class, new Box(absolute).expand(2.0), frame -> true).size();
    }

    /** O baú do lenhador ganha um machado pendurado. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "marker_place")
    public void aChestGetsTheToolOfItsWorker(TestContext context) {
        BlockPos chest = new BlockPos(3, 2, 3);

        context.setBlockState(chest, Blocks.CHEST.getDefaultState());

        ServerWorld world = context.getWorld();

        int marked = ChestMarker.mark(world, lumberjack(), at(context, chest));

        context.assertTrue(marked == 1, "esperava uma marca, foram " + marked);

        List<ItemFrameEntity> frames = world.getEntitiesByClass(
                ItemFrameEntity.class,
                new Box(context.getAbsolutePos(chest)).expand(2.0),
                frame -> true);

        context.assertTrue(frames.size() == 1, "esperava um quadro, foram " + frames.size());

        context.assertTrue(
                frames.get(0).getHeldItemStack().isOf(Items.IRON_AXE),
                "o quadro não trouxe o machado do lenhador");

        context.complete();
    }

    /**
     * Rodar de novo não pendura um segundo quadro.
     *
     * <p>É o teste que importa: a marcação roda a cada ciclo da colônia,
     * e um quadro por ciclo encheria a vila de machados em meia hora.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "marker_idempotent")
    public void markingTwiceLeavesOneFrame(TestContext context) {
        BlockPos chest = new BlockPos(3, 2, 3);

        context.setBlockState(chest, Blocks.CHEST.getDefaultState());

        ServerWorld world = context.getWorld();

        ChestMarker.mark(world, lumberjack(), at(context, chest));

        int again = ChestMarker.mark(world, lumberjack(), at(context, chest));

        context.assertTrue(again == 0, "marcou de novo o que já estava marcado");
        context.assertTrue(
                framesAround(context, chest) == 1,
                "ficaram " + framesAround(context, chest) + " quadros");

        context.complete();
    }

    /**
     * Baú cercado fica sem marca.
     *
     * <p>O mod não abre buraco na construção do jogador para pendurar a
     * sua etiqueta. Sem face livre, sem marca — e isso não é erro.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "marker_boxed_in")
    public void aBoxedInChestIsLeftAlone(TestContext context) {
        BlockPos chest = new BlockPos(3, 2, 3);

        context.setBlockState(chest, Blocks.CHEST.getDefaultState());

        for (net.minecraft.util.math.Direction side
                : net.minecraft.util.math.Direction.Type.HORIZONTAL) {

            context.setBlockState(chest.offset(side), Blocks.STONE.getDefaultState());
        }

        int marked = ChestMarker.mark(context.getWorld(), lumberjack(), at(context, chest));

        context.assertTrue(marked == 0, "pendurou quadro em baú sem face livre");
        context.assertTrue(framesAround(context, chest) == 0, "apareceu quadro do nada");

        context.complete();
    }

    /** Baú liberado perde a marca: dono que não existe não é sinalizado. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "marker_unmark")
    public void aReleasedChestLosesItsMark(TestContext context) {
        BlockPos chest = new BlockPos(3, 2, 3);

        context.setBlockState(chest, Blocks.CHEST.getDefaultState());

        ServerWorld world = context.getWorld();

        ChestMarker.mark(world, lumberjack(), at(context, chest));

        boolean removed = ChestMarker.unmark(
                world, MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(chest)));

        context.assertTrue(removed, "não achou a marca para tirar");
        context.assertTrue(
                framesAround(context, chest) == 0,
                "o quadro continuou pendurado");

        context.complete();
    }

    /**
     * O quadro do jogador não é confundido com o do mod.
     *
     * <p>Sem a distinção, o mod trocaria o item de uma decoração alheia
     * pelo seu ícone — mexer no que o jogador pendurou é exatamente o
     * que este código não pode fazer.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "marker_player_frame")
    public void aPlayerFrameIsNotTouched(TestContext context) {
        BlockPos chest = new BlockPos(3, 2, 3);
        BlockPos wall = new BlockPos(3, 2, 4);

        context.setBlockState(chest, Blocks.CHEST.getDefaultState());
        context.setBlockState(wall, Blocks.STONE.getDefaultState());

        ServerWorld world = context.getWorld();

        ItemFrameEntity mine = new ItemFrameEntity(
                world,
                context.getAbsolutePos(new BlockPos(3, 2, 5)),
                net.minecraft.util.math.Direction.SOUTH);

        mine.setHeldItemStack(Items.DIAMOND.getDefaultStack(), false);
        world.spawnEntity(mine);

        ChestMarker.mark(world, lumberjack(), at(context, chest));

        context.assertTrue(
                mine.getHeldItemStack().isOf(Items.DIAMOND),
                "o mod trocou o item do quadro do jogador");

        context.complete();
    }
}
