package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.BlueprintKind;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.coordination.SupplyException;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.BlockProtection;
import com.villagecolony.fabric.integration.EditLedger;
import com.villagecolony.fabric.integration.LotGround;
import com.villagecolony.fabric.integration.PlacedLogs;
import com.villagecolony.fabric.integration.SiteSurroundings;
import com.villagecolony.fabric.integration.Supplies;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
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

        return blueprint.isBuried(block) && !world.getBlockState(where).isReplaceable()
                && !mayShapePenFloor(world, blueprint, block, where);
    }

    /**
     * O piso do curral substitui a pedra do terreno por grama (Regra 253). O curral grande também
     * aceita o solo natural de cada bioma, e os médios seguem o preparo da Regra 168.
     * A regra vale para as peças de grama da planta. Canteiros e água seguem as regras próprias.
     */
    public static boolean mayShapePenFloor(
            ServerWorld world, Blueprint blueprint, BlueprintBlock block, BlockPos where) {

        BlockState state = world.getBlockState(where);

        return blueprint.isBuried(block) && blueprint.isPen()
                && !state.isOf(Blocks.GRASS_BLOCK)
                && (state.isIn(BlockTags.BASE_STONE_OVERWORLD)
                    || (BlueprintKind.isMediumPen(blueprint.id()) && SiteSurroundings.isNaturalGround(state))
                    || (PenPlans.isLargePen(blueprint.id()) && LotGround.isBiomeGround(world, where)))
                && world.getBlockEntity(where) == null
                && BlockProtection.mayBuildOver(world, where, state);
    }

    /** Molda o piso no local sem consumir estoque da vila (Regras 168 e 253). */
    static boolean shapePenFloor(ServerWorld world, ConstructionProject project, BlueprintBlock block, BlockPos where) {
        if (!mayShapePenFloor(world, project.blueprint(), block, where)) {
            return false;
        }

        BlockState grass = BlockShaping.withPlanStates(block, Blocks.GRASS_BLOCK.getDefaultState());
        if (!world.setBlockState(where, grass, Block.NOTIFY_ALL)) {
            return false;
        }

        EditLedger.mod(world, where.toImmutable(), grass, "obra"); // Regra 345
        PlacedLogs.note(where, grass, "build:" + project.blueprint().id());
        Supplies.record(SupplyException.SHAPED_GROUND, project.colonyId(), block.block().path(), 1);
        return true;
    }

    /** Preserva a moldagem do curral grande na conclusão de peças adiadas (Regra 253). */
    static boolean shapeLargePenFloor(
            ServerWorld world, ConstructionProject project, BlueprintBlock block, BlockPos where) {
        return PenPlans.isLargePen(project.blueprint().id()) && shapePenFloor(world, project, block, where);
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
     * Se a peça da base pode tomar o lugar do terreno.
     *
     * <p>O construtor pula a posição que não é substituível ("is in the way"),
     * e é o certo para casa de vila e bloco do jogador. Na base — a camada da
     * rua e as de baixo —, a peça que não é chão — pedregulho, tábua, degrau,
     * caminho — vai <b>no</b> terreno. Só terreno natural que a proteção deixa
     * quebrar, e sem bloco com inventário.
     *
     * <p><b>A proteção não conta a própria obra</b> — playtest de 2026-10-02.
     * Perguntar {@link BlockProtection#mayBreak} aqui dizia sempre "não": a
     * posição está dentro da obra aberta, e obra aberta é protegida. O piso
     * da casa 58405bf6 foi pulado 90 vezes ("grass_block is in the way") e a
     * casa ficou com a grama por chão. O teste antigo não registrava a obra.
     */
    public static boolean mayReplaceGround(
            ServerWorld world, Blueprint blueprint, BlueprintBlock block, BlockPos where) {

        if (!blueprint.isBase(block) || blueprint.isBuried(block)) {
            return false;
        }

        BlockState state = world.getBlockState(where);

        return world.getBlockEntity(where) == null
                && WorldTerrain.isNaturalGround(state)
                && BlockProtection.mayBuildOver(world, where, state);
    }
}
