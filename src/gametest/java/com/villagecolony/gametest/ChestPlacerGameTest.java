package com.villagecolony.gametest;

import com.villagecolony.fabric.integration.ChestPlacer;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.BedPart;
import net.minecraft.state.property.Properties;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Optional;

/**
 * O baú que nasce ao lado da cama — Regra 8, de 2026-08-15.
 *
 * <p>Escreve no mundo do jogador, que é a coisa mais perigosa que o mod
 * faz. Por isso a maior parte destes testes prova o que ele <b>não</b>
 * faz: não troca bloco de ninguém, não come meia cama, não deixa baú
 * flutuando, não põe baú que não abre, e não cola dois baús num baú
 * duplo de dono ambíguo.
 */
public class ChestPlacerGameTest implements FabricGameTest {

    private static final BlockPos BED = new BlockPos(3, 2, 3);

    /**
     * Uma cama de verdade, com as duas metades, sobre chão de pedra.
     *
     * <p>Cama inteira e não meia: os vizinhos de uma metade incluem a
     * outra, e é justamente esse o vizinho que o placer tem de recusar.
     */
    private static void layBed(TestContext context) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                context.setBlockState(BED.add(dx, -1, dz), Blocks.STONE.getDefaultState());
            }
        }

        context.setBlockState(BED, Blocks.RED_BED.getDefaultState()
                .with(Properties.BED_PART, BedPart.FOOT)
                .with(Properties.HORIZONTAL_FACING, Direction.NORTH));

        context.setBlockState(BED.north(), Blocks.RED_BED.getDefaultState()
                .with(Properties.BED_PART, BedPart.HEAD)
                .with(Properties.HORIZONTAL_FACING, Direction.NORTH));
    }

    private static Optional<BlockPos> place(TestContext context) {
        return ChestPlacer.placeBeside(context.getWorld(), context.getAbsolutePos(BED));
    }

    /** O caso simples: cama num quarto vazio ganha baú ao lado. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "placer_puts")
    public void aBedWithRoomBesideItGetsAChest(TestContext context) {
        layBed(context);

        Optional<BlockPos> spot = place(context);

        context.assertTrue(spot.isPresent(), "havia lugar de sobra e nenhum baú foi posto");
        context.assertTrue(
                context.getWorld().getBlockState(spot.get()).isOf(Blocks.CHEST),
                "a posição voltou mas não há baú nela");

        context.complete();
    }

    /**
     * A outra metade da cama não vira baú.
     *
     * <p>É o vizinho mais próximo e o mais perigoso: trocá-lo por um baú
     * quebraria a cama e tiraria o aldeão de casa.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "placer_bed")
    public void theOtherHalfOfTheBedIsNeverTaken(TestContext context) {
        layBed(context);

        place(context);

        context.expectBlock(Blocks.RED_BED, BED.north());

        context.complete();
    }

    /**
     * Bloco do jogador fica onde está.
     *
     * <p>Cama cercada de pedra por todos os lados, nos dois níveis: não
     * há lugar bom, e a resposta certa é não pôr nada.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "placer_walls")
    public void nothingOfThePlayersIsReplaced(TestContext context) {
        layBed(context);

        for (Direction side : Direction.Type.HORIZONTAL) {
            context.setBlockState(BED.offset(side), Blocks.STONE.getDefaultState());
            context.setBlockState(BED.offset(side).down(), Blocks.STONE.getDefaultState());
        }

        // A cabeceira é cama, e o resto é parede.
        context.setBlockState(BED.north(), Blocks.RED_BED.getDefaultState()
                .with(Properties.BED_PART, BedPart.HEAD)
                .with(Properties.HORIZONTAL_FACING, Direction.NORTH));

        Optional<BlockPos> spot = place(context);

        context.assertTrue(spot.isEmpty(), "não havia lugar livre e um baú foi posto assim mesmo");

        for (Direction side : Direction.Type.HORIZONTAL) {
            if (side == Direction.NORTH) {
                continue;
            }

            context.expectBlock(Blocks.STONE, BED.offset(side));
        }

        context.complete();
    }

    /**
     * Baú com bloco opaco em cima não abre — então não se põe ali.
     *
     * <p>Serve ao aldeão e ao jogador pelo mesmo motivo: um baú que não
     * abre é um baú que não existe.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "placer_lid")
    public void noChestUnderACeiling(TestContext context) {
        layBed(context);

        for (Direction side : Direction.Type.HORIZONTAL) {
            context.setBlockState(BED.offset(side).up(), Blocks.STONE.getDefaultState());
            context.setBlockState(BED.offset(side), Blocks.STONE.getDefaultState());
        }

        // Um só vizinho livre, e com teto: tem de ser recusado.
        context.setBlockState(BED.east(), Blocks.AIR.getDefaultState());

        Optional<BlockPos> spot = place(context);

        context.assertTrue(
                spot.isEmpty() || !spot.get().equals(context.getAbsolutePos(BED.east())),
                "pôs um baú debaixo de um bloco opaco, e ele não abriria");

        context.complete();
    }

    /**
     * Dois baús encostados viram um baú duplo, com dois donos.
     *
     * <p>O inventário passaria a ser o mesmo para dois trabalhadores, e
     * a conta de espaço livre da Regra 1 contaria o mesmo baú duas vezes.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "placer_double")
    public void noChestEndsUpBesideAnotherChest(TestContext context) {
        layBed(context);

        for (Direction side : Direction.Type.HORIZONTAL) {
            context.setBlockState(BED.offset(side), Blocks.STONE.getDefaultState());
        }

        // Só o leste está livre — e tem um baú colado nele, ao norte.
        context.setBlockState(BED.east(), Blocks.AIR.getDefaultState());
        context.setBlockState(BED.east().north(), Blocks.CHEST.getDefaultState());

        Optional<BlockPos> spot = place(context);

        context.assertTrue(
                spot.isEmpty() || !spot.get().equals(context.getAbsolutePos(BED.east())),
                "pôs um baú colado noutro, e os dois viraram um baú duplo");

        context.complete();
    }

    /**
     * Sem chão firme não se põe: o baú ficaria flutuando.
     *
     * <p>Cama sobre plataforma, com o vizinho de leste sobre o vazio.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "placer_floor")
    public void noChestOverThinAir(TestContext context) {
        layBed(context);

        for (Direction side : Direction.Type.HORIZONTAL) {
            context.setBlockState(BED.offset(side), Blocks.STONE.getDefaultState());
        }

        // Leste livre, mas sem chão nem no nível nem um abaixo.
        context.setBlockState(BED.east(), Blocks.AIR.getDefaultState());
        context.setBlockState(BED.east().down(), Blocks.AIR.getDefaultState());
        context.setBlockState(BED.east().down(2), Blocks.AIR.getDefaultState());

        Optional<BlockPos> spot = place(context);

        context.assertTrue(
                spot.isEmpty() || !spot.get().equals(context.getAbsolutePos(BED.east())),
                "pôs um baú sobre o vazio");

        context.complete();
    }
}
