package com.villagecolony.gametest;

import com.villagecolony.fabric.integration.ChestPlacer;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.BedPart;
import net.minecraft.state.property.Properties;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockBox;
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

    private static BlockBox room(TestContext context) {
        BlockPos origin = context.getAbsolutePos(BED);
        BlockBox box = new BlockBox(
                origin.getX() - 2, origin.getY() - 1, origin.getZ() - 2,
                origin.getX() + 2, origin.getY() + 2, origin.getZ() + 2);
        for (int x = box.getMinX(); x <= box.getMaxX(); x++) {
            for (int z = box.getMinZ(); z <= box.getMaxZ(); z++) {
                context.getWorld().setBlockState(new BlockPos(x, origin.getY() - 1, z),
                        Blocks.STONE.getDefaultState());
            }
        }
        for (int y = origin.getY(); y <= origin.getY() + 2; y++) {
            for (int x = box.getMinX(); x <= box.getMaxX(); x++) {
                context.getWorld().setBlockState(new BlockPos(x, y, box.getMinZ()), Blocks.STONE.getDefaultState());
                context.getWorld().setBlockState(new BlockPos(x, y, box.getMaxZ()), Blocks.STONE.getDefaultState());
            }
            for (int z = box.getMinZ(); z <= box.getMaxZ(); z++) {
                context.getWorld().setBlockState(new BlockPos(box.getMinX(), y, z), Blocks.STONE.getDefaultState());
                context.getWorld().setBlockState(new BlockPos(box.getMaxX(), y, z), Blocks.STONE.getDefaultState());
            }
        }
        BlockPos door = origin.west(2);
        context.getWorld().setBlockState(door, Blocks.OAK_DOOR.getDefaultState()
                .with(Properties.DOUBLE_BLOCK_HALF, net.minecraft.block.enums.DoubleBlockHalf.LOWER));
        context.getWorld().setBlockState(door.up(), Blocks.OAK_DOOR.getDefaultState()
                .with(Properties.DOUBLE_BLOCK_HALF, net.minecraft.block.enums.DoubleBlockHalf.UPPER));
        return box;
    }

    private static ChestPlacer.Result placeInRoom(TestContext context) {
        return ChestPlacer.placeForOriginalVillageBed(
                context.getWorld(), context.getAbsolutePos(BED), room(context));
    }

    /**
     * Uma cama solta nao prova que existe uma casa vanilla, muito menos
     * qual lado da porta e o interior. Nessa duvida o mundo do jogador
     * fica intacto.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "placer_structure")
    public void aBedWithoutAnOriginalVillageRoomDoesNotCreateAChest(TestContext context) {
        layBed(context);

        context.assertTrue(
                place(context).isEmpty(),
                "uma cama sem estrutura/porta ganhou um bau por tentativa generica");

        context.complete();
    }

    /** O caso positivo: a peça tem uma porta inequívoca e parede traseira. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "placer_puts")
    public void aBedWithRoomBesideItGetsAChest(TestContext context) {
        layBed(context);

        ChestPlacer.Result result = placeInRoom(context);

        context.assertTrue(result.chest().isPresent(), "havia quarto valido e nenhum baú foi posto");
        context.assertTrue(
                context.getWorld().getBlockState(result.chest().get()).isOf(Blocks.CHEST),
                "a posição voltou mas não há baú nela");
        context.assertTrue(
                context.getWorld().getBlockState(result.chest().get())
                        .get(Properties.HORIZONTAL_FACING) == Direction.WEST,
                "a abertura do bau nao ficou voltada para a celula interna da porta");

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

    /**
     * A cama sem vão não é reperguntada a cada ciclo — E24, 2026-08-25.
     *
     * <p>Na sessão daquele dia a linha
     * {@code No room for a chest beside the bed at -7580, 64, -5129}
     * saiu a cada trinta segundos até o servidor parar: três camas
     * coladas, oito posições relidas por ciclo, e um log que só ensinava
     * que o mod não muda de ideia.
     *
     * <p>A prova é o segundo pedido com o vão <b>já aberto</b>: se a
     * recusa vale, ele continua vazio. É a mesma marca que envelhece do
     * {@code TreeMarks}, então o vão aberto passa a valer dez ciclos
     * depois — e isso é o preço declarado de não repetir a busca.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "placer_walls")
    public void aBedWithoutRoomIsNotAskedAgainEveryCycle(TestContext context) {
        layBed(context);

        for (Direction side : Direction.Type.HORIZONTAL) {
            context.setBlockState(BED.offset(side), Blocks.STONE.getDefaultState());
            context.setBlockState(BED.offset(side).down(), Blocks.STONE.getDefaultState());
        }

        context.setBlockState(BED.north(), Blocks.RED_BED.getDefaultState()
                .with(Properties.BED_PART, BedPart.HEAD)
                .with(Properties.HORIZONTAL_FACING, Direction.NORTH));

        context.assertTrue(
                place(context).isEmpty(),
                "a montagem falhou: era para não haver vão nenhum");

        // O vão abre — e mesmo assim a colônia não repergunta agora.
        context.setBlockState(BED.east(), Blocks.AIR.getDefaultState());

        context.assertTrue(
                place(context).isEmpty(),
                "a cama foi reperguntada no mesmo instante, e é isso que enche o log");

        context.assertTrue(
                !context.getBlockState(BED.east()).isOf(Blocks.CHEST),
                "pôs baú numa cama que estava de castigo");

        context.complete();
    }

    // --- regra (b), decisão do autor em 2026-09-26: "ao lado da cama e
    // encostado em uma parede, nunca na frente da porta" ---

    /**
     * A caixa da casa vanilla inclui o degrau de fora, então a porta tem dois
     * lados "dentro". A regra de 23-09 desistia disso (3 de 3 camas na vila de
     * 26-09); a de agora não pergunta pela porta para decidir, só para evitar.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "placer_rule_b")
    public void aDoorWithTwoOpenSidesNoLongerBlocksTheChest(TestContext context) {
        layBed(context);

        BlockBox room = room(context);
        BlockPos step = context.getAbsolutePos(BED).west(3);
        context.getWorld().setBlockState(step.down(), Blocks.STONE.getDefaultState());
        context.getWorld().setBlockState(step, Blocks.AIR.getDefaultState());
        BlockBox withStep = new BlockBox(room.getMinX() - 1, room.getMinY(), room.getMinZ(),
                room.getMaxX(), room.getMaxY(), room.getMaxZ());

        ChestPlacer.Result result = ChestPlacer.placeForOriginalVillageBed(
                context.getWorld(), context.getAbsolutePos(BED), withStep);

        context.assertTrue(result.chest().isPresent(),
                "a porta com degrau fora ainda impediu o baú: " + result.outcome());
        context.complete();
    }

    /** A célula logo diante da porta é caminho, e o baú nunca vai para lá. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "placer_rule_b")
    public void theChestIsNeverInFrontOfTheDoor(TestContext context) {
        layBed(context);

        BlockBox room = room(context);
        BlockPos bed = context.getAbsolutePos(BED);

        // Só sobra o lado oeste do pé da cama, que é a frente da porta (x-2).
        for (BlockPos taken : new BlockPos[] {bed.east(), bed.north().east(), bed.north().west()}) {
            context.getWorld().setBlockState(taken, Blocks.STONE.getDefaultState());
        }

        ChestPlacer.Result result = ChestPlacer.placeForOriginalVillageBed(context.getWorld(), bed, room);

        context.assertTrue(result.chest().isEmpty(),
                "o baú foi posto diante da porta: " + result.chest().map(BlockPos::toShortString));
        context.assertFalse(context.getWorld().getBlockState(bed.west()).isOf(Blocks.CHEST),
                "há um baú bloqueando a entrada");
        context.complete();
    }

    /** Sem parede ao lado, não há baú: ele fica encostado, nunca solto no meio do quarto. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "placer_rule_b")
    public void theChestLeansOnAWall(TestContext context) {
        layBed(context);

        BlockPos bed = context.getAbsolutePos(BED);

        // Quarto grande, sem parede a menos de dois blocos da cama, com uma porta longe.
        BlockBox wide = new BlockBox(bed.getX() - 5, bed.getY() - 1, bed.getZ() - 5,
                bed.getX() + 5, bed.getY() + 2, bed.getZ() + 5);
        for (int x = wide.getMinX(); x <= wide.getMaxX(); x++) {
            for (int z = wide.getMinZ(); z <= wide.getMaxZ(); z++) {
                context.getWorld().setBlockState(new BlockPos(x, bed.getY() - 1, z), Blocks.STONE.getDefaultState());
            }
        }

        ChestPlacer.Result loose = ChestPlacer.placeForOriginalVillageBed(context.getWorld(), bed, wide);

        context.assertTrue(loose.chest().isEmpty(),
                "pôs baú solto no meio do quarto: " + loose.chest().map(BlockPos::toShortString));

        // Com uma parede colada ao leste do pé da cama, o baú encosta nela.
        context.getWorld().setBlockState(bed.east(2), Blocks.STONE.getDefaultState());

        ChestPlacer.Result leaning = ChestPlacer.placeForOriginalVillageBed(context.getWorld(), bed, wide);

        context.assertTrue(leaning.chest().isPresent() && leaning.chest().get().equals(bed.east()),
                "com parede ao lado, o baú devia ir para " + bed.east().toShortString()
                        + ": " + leaning.chest().map(BlockPos::toShortString));
        context.assertTrue(context.getWorld().getBlockState(bed.east())
                        .get(Properties.HORIZONTAL_FACING) == Direction.WEST,
                "o baú encostado devia abrir para longe da parede");
        context.complete();
    }
}
