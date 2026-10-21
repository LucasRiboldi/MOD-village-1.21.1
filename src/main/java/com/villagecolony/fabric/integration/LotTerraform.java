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
 * irregular, com cada coluna a até {@link #MAX_CUT} blocos acima ou {@link #MAX_FILL} abaixo da rua, é aceito como aplainável
 * ({@link LotLevel}) e aplainado aqui, na abertura da obra, antes de a preparação de fundação
 * conferir a base: o que sobra acima da base é cortado, e o que falta abaixo é aterrado com o chão
 * do bioma. A fase 2 é a vila inteira (a montanha em subida de 1 em 1).
 *
 * <p>Só se corta o chão do bioma — nunca peça da vila gerada, obra da colônia ou fluido — e só se
 * aterra sobre ar ou planta. Na bancada da floresta, 81% das recusas de lote eram relevo (52% fora
 * da janela vertical, 29% fora do nível da rua).
 */
public final class LotTerraform {

    /**
     * Quanto chão acima da rua uma coluna pode ter e ainda ser cortada — autor, 2026-10-20, depois do
     * deserto jogado: a vila na encosta da duna teve 0 lotes aprovados em 2,8 dias (31.478 recusas
     * "sem chão na janela"), com o relevo de +3 a +11 acima da rua. Corta-se só o chão do bioma.
     */
    static final int MAX_CUT = 5;

    /** Quanto abaixo da rua uma coluna pode estar e ainda ser aterrada (fase 1). */
    static final int MAX_FILL = 2;

    /**
     * A largura do corredor cortado em volta da casa no lote aplainado — autor, 2026-10-21 (opção 1).
     * Na bancada do deserto o lote cortado na duna deixava a casa num buraco: o construtor não achava
     * lugar de pé para o telhado (570 desistências de lado) e a vila parava. O corredor, na altura da
     * base, dá a volta na casa.
     */
    static final int RING = 2;

    private LotTerraform() {
    }

    /**
     * Se a coluna com chão em {@code groundY} pode ser levada a {@code baseY}: o que se corta é chão
     * do bioma, o que se aterra é vazio.
     */
    static boolean columnLevels(ServerWorld world, int x, int z, int groundY, int baseY) {
        if (groundY - baseY > MAX_CUT || baseY - groundY > MAX_FILL) {
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
            moved += cutRing(world, project.origin().x(), project.origin().z(),
                    blueprint.size().x(), blueprint.size().z(), baseY);
        }

        if (moved > 0) {
            VillageColonyMod.LOGGER.info("Colony {} levelled the lot of {} at {} — {} blocks cut or filled",
                    project.colonyId(), blueprint.id(), project.origin(), moved);
        }

        return moved;
    }

    /**
     * O corredor de {@link #RING} blocos em volta da pegada: só corta, até a base, o chão do bioma que
     * sobra acima dela (o que está abaixo fica — o corredor não aterra); rua e o que a coluna não deixa
     * cortar ({@link #columnLevels}) ficam.
     *
     * @return quantos blocos saíram
     */
    static int cutRing(ServerWorld world, int originX, int originZ, int sizeX, int sizeZ, int baseY) {
        int moved = 0;

        for (int dx = -RING; dx < sizeX + RING; dx++) {
            for (int dz = -RING; dz < sizeZ + RING; dz++) {
                if (dx >= 0 && dx < sizeX && dz >= 0 && dz < sizeZ) {
                    continue;
                }

                int x = originX + dx;
                int z = originZ + dz;
                Optional<BlockPos> ground = LotGround.groundInColumn(world, x, z, baseY, MAX_CUT);

                if (ground.isEmpty() || ground.get().getY() <= baseY
                        || VillageRoad.isPaving(world, ground.get())
                        || !columnLevels(world, x, z, ground.get().getY(), baseY)
                        || touchesTheVillage(world, x, z, ground.get().getY(), baseY)) {
                    continue;
                }

                moved += levelColumn(world, x, z, ground.get().getY(), baseY);
            }
        }

        return moved;
    }

    /**
     * Se o corte desta coluna levaria peça da vila gerada que não é solo natural — a parede de arenito
     * liso das casas do deserto conta como chão do bioma, e o corredor não foi validado com o lote.
     */
    private static boolean touchesTheVillage(ServerWorld world, int x, int z, int groundY, int baseY) {
        for (int y = baseY + 1; y <= groundY; y++) {
            BlockPos pos = new BlockPos(x, y, z);

            if (!LotGround.isNaturalGround(world.getBlockState(pos)) && BlockProtection.isVillageOriginal(world, pos)) {
                return true;
            }
        }

        return false;
    }

    /** Aplaina cada coluna da pegada que pode ser levada à base; a que não pode fica como está. */
    static int levelFootprint(ServerWorld world, int originX, int originZ, int sizeX, int sizeZ, int baseY) {
        int moved = 0;

        for (int dx = 0; dx < sizeX; dx++) {
            for (int dz = 0; dz < sizeZ; dz++) {
                int x = originX + dx;
                int z = originZ + dz;
                Optional<BlockPos> ground = LotGround.groundInColumn(world, x, z, baseY, MAX_CUT);

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
