package com.villagecolony.gametest;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ChestMarker;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
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

    /**
     * A marca diz a profissão de volta, e não só para o olho.
     *
     * <p>Até 2026-08-27 a marca era só decoração: o mod a escrevia e
     * nunca a lia. Ela passou a ser a resposta de "de quem é este baú?"
     * também para o código, e é isso que a torna uma regra em vez de um
     * enfeite.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "marker_read")
    public void theMarkAnswersWhichProfessionOwnsTheChest(TestContext context) {
        BlockPos chest = new BlockPos(3, 2, 3);

        context.setBlockState(chest, Blocks.CHEST.getDefaultState());

        ServerWorld world = context.getWorld();
        BlockPos absolute = context.getAbsolutePos(chest);

        context.assertTrue(
                ChestMarker.professionAt(world, absolute).isEmpty(),
                "um baú sem quadro nenhum disse ter dono");

        ChestMarker.markAt(world, absolute, ProfessionType.FARMER);

        context.assertTrue(
                ChestMarker.professionAt(world, absolute)
                        .filter(found -> found == ProfessionType.FARMER)
                        .isPresent(),
                "a marca do fazendeiro não foi lida de volta");

        context.complete();
    }

    /**
     * O lenhador não leva o baú do fazendeiro — decisão do autor,
     * 2026-08-27.
     *
     * <p>Visto em jogo: <i>"tem material de lenhador indo para o baú do
     * fazendeiro"</i>. O baú era escolhido pelo <b>mais perto da cama</b>,
     * por ordem de chegada, e profissão não entrava na conta — duas camas
     * a dois blocos uma da outra bastavam para quem fosse processado
     * primeiro levar o baú do outro.
     *
     * <p>A marca já existia e já era escrita a cada ciclo. O que faltava
     * era ela <b>valer</b>: baú marcado é de quem a marca diz, e quem não
     * é daquela profissão procura outro.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "marker_read")
    public void aMarkedChestIsOnlyFreeForItsOwnProfession(TestContext context) {
        BlockPos chest = new BlockPos(3, 2, 3);

        context.setBlockState(chest, Blocks.CHEST.getDefaultState());

        ServerWorld world = context.getWorld();
        BlockPos absolute = context.getAbsolutePos(chest);

        ChestMarker.markAt(world, absolute, ProfessionType.FARMER);

        context.assertTrue(
                ChestMarker.allows(world, absolute, Optional.of(ProfessionType.FARMER)),
                "o fazendeiro foi barrado do próprio baú");

        context.assertFalse(
                ChestMarker.allows(world, absolute, Optional.of(ProfessionType.LUMBERJACK)),
                "o lenhador levou o baú do fazendeiro");

        context.assertFalse(
                ChestMarker.allows(world, absolute, Optional.empty()),
                "quem ainda não tem profissão levou um baú que já tem dono");

        context.complete();
    }

    /**
     * Baú sem marca serve a qualquer um, inclusive a quem ainda não tem
     * profissão.
     *
     * <p>É o caso comum da vila nova, e a regra não pode travá-lo: sem
     * isto ninguém reivindicaria o primeiro baú, e a marca — que só
     * nasce depois da reivindicação — nunca chegaria a existir.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "marker_read")
    public void anUnmarkedChestServesAnyone(TestContext context) {
        BlockPos chest = new BlockPos(3, 2, 3);

        context.setBlockState(chest, Blocks.CHEST.getDefaultState());

        ServerWorld world = context.getWorld();
        BlockPos absolute = context.getAbsolutePos(chest);

        context.assertTrue(
                ChestMarker.allows(world, absolute, Optional.of(ProfessionType.LUMBERJACK)),
                "o lenhador foi barrado de um baú sem dono");

        context.assertTrue(
                ChestMarker.allows(world, absolute, Optional.empty()),
                "o aldeão sem profissão foi barrado de um baú sem dono");

        context.complete();
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
     * Dois baús vizinhos não disputam o mesmo quadro.
     *
     * <p>O defeito que este teste guarda, achado no log de 2026-08-12: os
     * baús de {@code 1118,70,727} e {@code 1120,70,727} ficam a dois
     * blocos, e a busca por quadro aceitava qualquer um dentro da caixa.
     * O quadro pendurado no vão entre eles pertencia aos dois: o
     * fabricante punha a mesa, o fazendeiro punha a enxada trinta
     * segundos depois, e a vila passou a sessão inteira com um baú que
     * mentia sobre o próprio dono.
     *
     * <p>Os cinco testes anteriores usam um baú só, e por isso nenhum
     * deles pegava isso.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "marker_neighbours")
    public void twoChestsDoNotShareOneFrame(TestContext context) {
        ChestMarker.clearAll();

        BlockPos ofLumberjack = new BlockPos(3, 2, 3);
        BlockPos ofFarmer = new BlockPos(5, 2, 3);
        BlockPos gap = new BlockPos(4, 2, 3);

        context.setBlockState(ofLumberjack, Blocks.CHEST.getDefaultState());
        context.setBlockState(ofFarmer, Blocks.CHEST.getDefaultState());

        // Só o vão entre eles fica livre. É a única forma de o quadro
        // nascer onde as duas buscas se cruzam — que é o caso do defeito.
        for (BlockPos chest : List.of(ofLumberjack, ofFarmer)) {
            for (net.minecraft.util.math.Direction side
                    : net.minecraft.util.math.Direction.Type.HORIZONTAL) {

                BlockPos neighbour = chest.offset(side);

                if (!neighbour.equals(gap)) {
                    context.setBlockState(neighbour, Blocks.STONE.getDefaultState());
                }
            }
        }

        ServerWorld world = context.getWorld();

        List<Worker> both = List.of(
                worker(ProfessionType.LUMBERJACK), worker(ProfessionType.FARMER));

        ChestMarker.StorageLookup chests = twoChests(
                context, both.get(0), ofLumberjack, both.get(1), ofFarmer);

        int first = ChestMarker.mark(world, both, chests);

        context.assertTrue(first == 2, "esperava duas marcas, foram " + first);

        // O que o defeito fazia: a segunda marcação achava o quadro do
        // vizinho e trocava o ícone dele, e isso nunca parava.
        int again = ChestMarker.mark(world, both, chests);

        context.assertTrue(again == 0, "os dois baús ficaram trocando o mesmo quadro");

        List<ItemFrameEntity> frames = world.getEntitiesByClass(
                ItemFrameEntity.class,
                new Box(context.getAbsolutePos(gap)).expand(2.0),
                frame -> true);

        // Dois: o mesmo bloco de ar comporta um quadro em cada parede, e
        // cada baú fica com o seu.
        context.assertTrue(frames.size() == 2, "esperava dois quadros, foram " + frames.size());

        context.assertTrue(
                heldAt(context, frames, ofLumberjack).isOf(Items.IRON_AXE),
                "o baú do lenhador não ficou com o machado");

        context.assertTrue(
                heldAt(context, frames, ofFarmer).isOf(Items.IRON_HOE),
                "o baú do fazendeiro não ficou com a enxada");

        context.complete();
    }

    /** O item do quadro pregado neste baú. */
    private static ItemStack heldAt(
            TestContext context, List<ItemFrameEntity> frames, BlockPos chest) {

        BlockPos absolute = context.getAbsolutePos(chest);

        for (ItemFrameEntity frame : frames) {
            BlockPos wall = frame.getAttachedBlockPos()
                    .offset(frame.getHorizontalFacing().getOpposite());

            if (wall.equals(absolute)) {
                return frame.getHeldItemStack();
            }
        }

        return ItemStack.EMPTY;
    }

    /** Um trabalhador solto, com função e sem baú ainda. */
    private static Worker worker(ProfessionType profession) {
        Worker worker = Worker.register(UUID.randomUUID(), UUID.randomUUID());
        worker.assign(profession);

        return worker;
    }

    /** Cada um destes dois trabalhadores com o seu baú. */
    private static ChestMarker.StorageLookup twoChests(
            TestContext context,
            Worker first,
            BlockPos firstChest,
            Worker second,
            BlockPos secondChest) {

        ColonyPos one = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(firstChest));
        ColonyPos other = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(secondChest));

        return workerId -> {
            if (workerId.equals(first.villagerId())) {
                return Optional.of(one);
            }

            return workerId.equals(second.villagerId()) ? Optional.of(other) : Optional.empty();
        };
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
