package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.StructureBlueprintReader;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FallingBlock;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.EmptyBlockView;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * O encalhado fechado por blocos protegidos atravessa só a peça da planta da
 * colônia — estudo de 01-10, §9b-A. O bloco do jogador no mesmo lugar não é
 * peça, e a Regra 3 continua.
 */
public final class ClimbOutPiecesGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "climb_pieces")
    public void aBoxedInWorkerMayCutOnlyThroughTheColonysOwnPiece(TestContext context) {
        ColonyPos origin = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(1, 4, 1)));
        Colony colony = Colony.create(UUID.randomUUID(), origin);
        Blueprint plan = PlanPlacement.blueprintOf(
                context.getWorld(), colony.id(), StructureBlueprintReader.BIG_HOUSE_MOD, origin)
                .orElseThrow(() -> new AssertionError("planta da BigHouseMOD ausente"));

        BlueprintBlock pieceBlock = plan.blocks().stream()
                .filter(block -> block.offset().y() >= 1)
                .filter(block -> MinecraftTypeAdapter.toBlock(block.block()).map(ClimbOutPiecesGameTest::isPlainCube)
                        .orElse(false))
                .findFirst()
                .orElseThrow(() -> new AssertionError("planta sem bloco cheio para a parede"));
        Block expected = MinecraftTypeAdapter.toBlock(pieceBlock.block()).orElseThrow();

        BlockPos piece = new BlockPos(origin.x() + pieceBlock.offset().x(), origin.y() + pieceBlock.offset().y(),
                origin.z() + pieceBlock.offset().z());
        Direction way = Direction.EAST;
        BlockPos feet = piece.offset(way.getOpposite());

        Map<BlockPos, BlockState> before = new HashMap<>();
        Runnable restore = () -> before.forEach((pos, state) -> context.getWorld().setBlockState(pos, state));

        // A caixa da planta já girada para este lugar, como a construção real
        // é registrada: o tamanho cru do arquivo trocaria largura e fundo.
        ColonyPos size = plan.size();

        try {
            // Um poço de bedrock: pés e cabeça fechados nos quatro lados, menos
            // a leste, onde está a peça da casa e o vão acima dela.
            for (Direction side : Direction.Type.HORIZONTAL) {
                for (BlockPos level : new BlockPos[] {feet, feet.up()}) {
                    BlockPos cell = level.offset(side);

                    if (cell.equals(piece) || cell.equals(piece.up())) {
                        continue;
                    }

                    set(context, before, cell, Blocks.BEDROCK.getDefaultState());
                }
            }

            set(context, before, feet, Blocks.AIR.getDefaultState());
            set(context, before, feet.up(), Blocks.AIR.getDefaultState());
            set(context, before, feet.down(), Blocks.BEDROCK.getDefaultState());
            set(context, before, piece, expected.getDefaultState());
            set(context, before, piece.up(), Blocks.AIR.getDefaultState());
            set(context, before, piece.down(), Blocks.BEDROCK.getDefaultState());

            VillageColonyMod.BUILDINGS.register(new Building(UUID.randomUUID(), colony.id(),
                    StructureBlueprintReader.BIG_HOUSE_MOD, origin,
                    new ColonyPos(origin.x() + size.x() - 1, origin.y() + size.y() - 1, origin.z() + size.z() - 1)));

            BlockPos home = feet.offset(way, 30);

            context.assertTrue(ColonyPieces.isPlannedPiece(context.getWorld(), piece),
                    "o " + expected + " no lugar que a planta põe devia ser peça da colônia; offset "
                            + pieceBlock.offset() + ", caixa " + size);
            context.assertTrue(ClimbTerrain.tunnelWay(context.getWorld(), feet, home).isEmpty(),
                    "sem a regra da peça, a casa fecha o caminho — o caso do §9b");
            context.assertTrue(ClimbTerrain.tunnelWay(context.getWorld(), feet, home, true).equals(Optional.of(way)),
                    "fechado, ele devia atravessar a peça da colônia a leste");

            // O mesmo lugar com um bloco que a planta não põe ali: é do jogador.
            context.getWorld().setBlockState(piece, Blocks.GOLD_BLOCK.getDefaultState());

            context.assertFalse(ColonyPieces.isPlannedPiece(context.getWorld(), piece),
                    "bloco que a planta não põe ali não é peça da colônia (Regra 3)");
            context.assertTrue(ClimbTerrain.tunnelWay(context.getWorld(), feet, home, true).isEmpty(),
                    "o bloco do jogador não se atravessa nem fechado");
        } finally {
            VillageColonyMod.BUILDINGS.removeOfColony(colony.id());
            restore.run();
        }

        context.complete();
    }

    private static void set(TestContext context, Map<BlockPos, BlockState> before, BlockPos pos, BlockState state) {
        before.putIfAbsent(pos.toImmutable(), context.getWorld().getBlockState(pos));
        context.getWorld().setBlockState(pos, state);
    }

    private static boolean isPlainCube(Block block) {
        BlockState state = block.getDefaultState();

        return !(block instanceof FallingBlock) && !state.hasBlockEntity() && state.getFluidState().isEmpty()
                && state.isFullCube(EmptyBlockView.INSTANCE, BlockPos.ORIGIN);
    }
}
