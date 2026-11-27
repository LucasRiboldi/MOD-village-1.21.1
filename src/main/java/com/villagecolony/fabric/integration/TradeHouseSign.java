package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

import java.util.Optional;

/**
 * O quadro do ofício em cima da porta da casa de ofício — Regra 392 (H5, item 6, autor 27-11: "só coloque um quadro na frente
 * em cima da porta indicando a profissão como os baús já fazem"). Quando a casa {@code majest_<ofício>} fica pronta, o quadro
 * com o ícone do ofício ({@link ChestMarker#badgeOf}) vai na parede de fora, logo acima da porta do térreo.
 */
public final class TradeHouseSign {

    /** O nome do quadro: separa o da casa do quadro dos baús. */
    static final String TAG = "villagecolony:house";

    /** Até quantos blocos acima da base da casa a porta do térreo é procurada. */
    private static final int GROUND_FLOOR = 3;

    private TradeHouseSign() {
    }

    /** Pendura o quadro do ofício, se a casa é casa de ofício; verdadeiro se pendurou agora. */
    public static boolean mark(ServerWorld world, Building building) {
        Optional<ProfessionType> profession = ColonyModels.professionOf(building.blueprint());

        if (profession.isEmpty()) {
            return false;
        }

        BlockPos min = MinecraftTypeAdapter.toBlockPos(building.min());
        BlockPos max = MinecraftTypeAdapter.toBlockPos(building.max());
        BlockBox box = BlockBox.create(min, max);

        for (BlockPos at : BlockPos.iterate(min, new BlockPos(max.getX(), Math.min(max.getY(), min.getY() + GROUND_FLOOR),
                max.getZ()))) {
            BlockState state = world.getBlockState(at);

            if (!state.isIn(BlockTags.DOORS) || state.get(DoorBlock.HALF) != DoubleBlockHalf.LOWER) {
                continue;
            }

            Direction facing = state.get(DoorBlock.FACING);

            for (Direction out : new Direction[] {facing.getOpposite(), facing}) {
                if (!box.contains(at.offset(out)) && hang(world, at.up(2), out, profession.get())) {
                    VillageColonyMod.LOGGER.info("VC_TRADE_HOUSE_SIGN {} over the door at {} ({})",
                            profession.get(), at.toShortString(), building.blueprint().path());
                    return true;
                }
            }
        }

        return false;
    }

    /** O quadro na face {@code out} do bloco acima da porta, se a parede é sólida e o lugar de fora está livre. */
    private static boolean hang(ServerWorld world, BlockPos wall, Direction out, ProfessionType profession) {
        BlockPos spot = wall.offset(out);

        if (!world.getBlockState(wall).isSideSolidFullSquare(world, wall, out) || !world.getBlockState(spot).isAir()
                || !world.getEntitiesByClass(ItemFrameEntity.class, new Box(spot), frame -> true).isEmpty()) {
            return false;
        }

        ItemFrameEntity frame = new ItemFrameEntity(world, spot, out);

        frame.setHeldItemStack(ChestMarker.badgeOf(profession), false);
        frame.setInvulnerable(true);
        frame.setCustomName(Text.literal(TAG));

        return world.spawnEntity(frame);
    }
}
