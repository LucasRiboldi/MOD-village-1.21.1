package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.BlockProtection;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * O chão da obra na altura da rua — sessão de jogo de 2026-09-26, pedido do
 * autor: "não deve ser construído as camadas de terra na base das construções;
 * o chão da construção e a porta devem estar na altura da rua".
 *
 * <p>A planta sabe qual camada fica na rua ({@link Blueprint#streetLayer}) e o
 * que dela para baixo é chão ({@link Blueprint#isBuried}). Esta classe é a
 * pergunta ao mundo, feita num lugar só pelos quatro que precisam concordar:
 * quem abre a obra, quem a retoma depois de carregar o mundo, quem a repara e
 * quem assenta o bloco. Se um deles discordasse, a terra enterrada voltaria
 * como falta — pedido de material, reparo sem fim.
 *
 * <p><b>"O chão já está lá" é lido do mundo, não suposto.</b> Uma peça enterrada
 * conta como assentada só quando a posição dela está ocupada por bloco que não
 * se substitui. Obra aberta antes desta regra tem a origem um acima do chão:
 * ali a posição é ar, e a peça é construída como sempre foi — as casas e obras
 * que o save já tem não mudam.
 */
public final class BuriedPieces {

    private BuriedPieces() {
    }

    /** Se esta peça é chão e o chão já está no lugar dela. */
    public static boolean heldByTheGround(
            ServerWorld world, Blueprint blueprint, BlueprintBlock block, BlockPos where) {

        return blueprint.isBuried(block) && !world.getBlockState(where).isReplaceable();
    }

    /**
     * Dá por assentado o que o chão já segura, logo ao abrir a obra.
     *
     * @return quantas peças eram chão
     */
    public static int markHeldByTheGround(ServerWorld world, ConstructionProject project) {
        int held = 0;

        for (BlueprintBlock block : project.remaining()) {
            BlockPos where = MinecraftTypeAdapter.toBlockPos(project.worldPositionOf(block));

            if (heldByTheGround(world, project.blueprint(), block, where)) {
                project.markPlaced(block);
                held++;
            }
        }

        return held;
    }

    /**
     * Se o piso da camada da rua pode tomar o lugar do terreno.
     *
     * <p>O construtor pula a posição que não é substituível ("is in the way"),
     * e é o certo para casa de vila e bloco do jogador. Na camada da rua, a
     * peça que não é chão — pedregulho, tábua, degrau, caminho — vai
     * <b>no</b> terreno: é o piso na altura da rua. Só terreno natural que a
     * proteção deixa quebrar, e sem bloco com inventário.
     */
    public static boolean mayReplaceGround(
            ServerWorld world, Blueprint blueprint, BlueprintBlock block, BlockPos where) {

        if (!blueprint.hasStreetLayer()
                || block.offset().y() != blueprint.streetLayer()
                || blueprint.isBuried(block)) {
            return false;
        }

        BlockState state = world.getBlockState(where);

        return world.getBlockEntity(where) == null
                && WorldTerrain.isNaturalGround(state)
                && BlockProtection.mayBreak(world, where, state);
    }
}
