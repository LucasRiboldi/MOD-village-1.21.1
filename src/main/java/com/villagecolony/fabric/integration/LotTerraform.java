package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.ConstructionProject;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

/**
 * A terraplanagem do lote — fase 1 do plano do autor (15-10), aplicada em 2026-10-20.
 *
 * <p>O plano manda <b>não afrouxar</b> o critério da obra e <b>preparar o terreno</b>. A casa pronta
 * continua com a base exatamente no nível da rua (Regra 19); o que muda é que um lote de chão
 * irregular, com cada coluna a até {@link #MAX_STEP} blocos da rua, é aceito como aplainável
 * ({@link LotLevel}) e aplainado aqui, na abertura da obra, antes de a preparação de fundação
 * conferir a base: o que sobra acima da base é cortado, e o que falta abaixo é aterrado com o chão
 * do bioma. A fase 2 é a vila inteira (a montanha em subida de 1 em 1).
 *
 * <p>Só se corta o chão do bioma — nunca peça da vila gerada, obra da colônia ou fluido — e só se
 * aterra sobre ar ou planta. Na bancada da floresta, 81% das recusas de lote eram relevo (52% fora
 * da janela vertical, 29% fora do nível da rua).
 */
public final class LotTerraform {

    /** Quanto uma coluna pode estar acima ou abaixo da rua e ainda ser aplainada. */
    static final int MAX_STEP = 2;

    private LotTerraform() {
    }

    /**
     * Se a coluna com chão em {@code groundY} pode ser levada a {@code baseY}: o que se corta é chão
     * do bioma, o que se aterra é vazio.
     */
    static boolean columnLevels(ServerWorld world, int x, int z, int groundY, int baseY) {
        if (Math.abs(groundY - baseY) > MAX_STEP) {
            return false;
        }

        for (int y = baseY + 1; y <= groundY; y++) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = world.getBlockState(pos);

            if (!LotGround.isBiomeGround(world, pos) || !state.getFluidState().isEmpty()
                    || BlockProtection.isColonyBuilt(pos) || besideFluid(world, pos)) {
                return false;
            }
        }

        for (int y = groundY + 1; y <= baseY; y++) {
            BlockState state = world.getBlockState(new BlockPos(x, y, z));

            if (!LotGround.isNothing(state) || !state.getFluidState().isEmpty()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Se o bloco encosta em água ou lava: cortá-lo abriria o lote para o fluido — na bancada da taiga,
     * o lote aplainado à beira do oceano frio deixou o construtor 76% bloqueado.
     */
    private static boolean besideFluid(ServerWorld world, BlockPos pos) {
        for (net.minecraft.util.math.Direction side : net.minecraft.util.math.Direction.values()) {
            if (side != net.minecraft.util.math.Direction.DOWN && !world.getFluidState(pos.offset(side)).isEmpty()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Aplaina a pegada da obra até a base — a cota da rua da planta, a mesma que
     * {@link FoundationPreparation} confere logo depois.
     *
     * @return quantos blocos foram cortados ou aterrados
     */
    public static int level(ServerWorld world, ConstructionProject project) {
        Blueprint blueprint = project.blueprint();

        if (!blueprint.hasStreetLayer()) {
            return 0;
        }

        int baseY = project.origin().y() + blueprint.streetLayer();
        int moved = levelFootprint(world, project.origin().x(), project.origin().z(),
                blueprint.size().x(), blueprint.size().z(), baseY);

        if (moved > 0) {
            VillageColonyMod.LOGGER.info("Colony {} levelled the lot of {} at {} — {} blocks cut or filled",
                    project.colonyId(), blueprint.id(), project.origin(), moved);
        }

        return moved;
    }

    /** Aplaina cada coluna da pegada que pode ser levada à base; a que não pode fica como está. */
    static int levelFootprint(ServerWorld world, int originX, int originZ, int sizeX, int sizeZ, int baseY) {
        int moved = 0;

        for (int dx = 0; dx < sizeX; dx++) {
            for (int dz = 0; dz < sizeZ; dz++) {
                int x = originX + dx;
                int z = originZ + dz;
                Optional<BlockPos> ground = LotGround.groundInColumn(world, x, z, baseY);

                if (ground.isEmpty() || ground.get().getY() == baseY
                        || !columnLevels(world, x, z, ground.get().getY(), baseY)) {
                    continue;
                }

                moved += levelColumn(world, x, z, ground.get().getY(), baseY);
            }
        }

        return moved;
    }

    private static int levelColumn(ServerWorld world, int x, int z, int groundY, int baseY) {
        int moved = 0;

        // A planta de cima da coluna cortada sairia flutuando: vai junto.
        BlockPos plant = new BlockPos(x, groundY + 1, z);

        if (groundY > baseY && LotGround.isNothing(world.getBlockState(plant)) && !world.getBlockState(plant).isAir()) {
            world.setBlockState(plant, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
        }

        for (int y = groundY; y > baseY; y--) {
            world.setBlockState(new BlockPos(x, y, z), Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
            moved++;
        }

        for (int y = groundY + 1; y <= baseY; y++) {
            BlockPos pos = new BlockPos(x, y, z);
            world.setBlockState(pos, VillageBiomes.foundationGroundAt(world, pos).getDefaultState(), Block.NOTIFY_ALL);
            moved++;
        }

        return moved;
    }
}
