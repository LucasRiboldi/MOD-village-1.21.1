package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.BlueprintKind;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.type.ColonyPos;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * O canteiro limpo antes de a casa subir — o estado PREPARING.
 *
 * <p>Previsto em {@code Construction-System.md} desde o começo e nunca
 * escrito. O código dizia, com franqueza, que não havia o que limpar:
 * o lote só era aceito sem nada em cima. Só que "nada em cima" tratava
 * grama alta e flor como nada — e elas continuavam lá, dentro da casa,
 * porque o miolo da planta não põe bloco nenhum e portanto nunca as
 * cobria.
 *
 * <p>O autor pediu a limpeza em 2026-08-19: <i>o construtor deve ser
 * capaz de retirar plantas do caminho da construção</i>. É o par da
 * Regra 22 — a planta não reprova o lote, e por não reprovar precisa de
 * alguém que a tire.
 *
 * <p>A preparação tira plantas substituíveis e árvores naturais inteiras do lote
 * (Regra 22). Tronco da vila, da colônia e do jogador permanece protegido
 * (Regra 3). A madeira da árvore vai aos baús públicos; excedente cai no mundo.
 *
 * <p><b>Plantas pequenas sem drop.</b> A flor some, não vira item no chão. É o mesmo que
 * acontece quando um jogador põe um bloco sobre grama alta: o mundo a
 * substitui e nada cai. Fazer cair encheria o canteiro de itens que
 * precisa ser recolhido dentro da obra.
 */
public final class SitePreparation {

    /** Margem externa limpa junto do lote para não deixar vegetação entrando na construção. */
    private static final int OUTER_MARGIN = 1;

    private SitePreparation() {
    }

    /**
     * Limpa a planta que estiver dentro da caixa da obra e na margem externa de um bloco.
     *
     * <p>Roda uma vez, quando o projeto nasce e quando ele volta do
     * save — o jogador pode ter plantado alguma coisa entre uma sessão e
     * outra, e a Regra 23 diz que o que já foi olhado se olha de novo.
     *
     * @return quantos blocos foram limpos ou convertidos
     */
    public static int clear(ServerWorld world, ConstructionProject project) {
        ColonyPos origin = project.origin();
        ColonyPos size = project.blueprint().size();

        int cleared = 0;

        for (int dx = -OUTER_MARGIN; dx < size.x() + OUTER_MARGIN; dx++) {
            for (int dy = 0; dy < size.y(); dy++) {
                for (int dz = -OUTER_MARGIN; dz < size.z() + OUTER_MARGIN; dz++) {
                    BlockPos pos = new BlockPos(
                            origin.x() + dx, origin.y() + dy, origin.z() + dz);

                    if (isPlant(world.getBlockState(pos))
                            && BlockProtection.mayBreak(world, pos, world.getBlockState(pos))) {
                        world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);

                        cleared++;
                    }
                }
            }
        }

        cleared += clearTrees(world, project);
        cleared += TreeClearing.sweep(world, origin.x() - OUTER_MARGIN, origin.z() - OUTER_MARGIN,
                origin.x() + size.x() + OUTER_MARGIN - 1, origin.z() + size.z() + OUTER_MARGIN - 1, origin.y(),
                ColonyChests.nearestFirst(world, project.colonyId(), origin));
        cleared += removeGrassFromHouseBase(world, project);

        if (cleared > 0) {
            VillageColonyMod.LOGGER.info(
                    "Project {} cleared {} blocks off the site at {}",
                    project.id(),
                    cleared,
                    origin);
        }

        return cleared;
    }

    /**
     * Derruba a árvore natural inteira, em qualquer bioma, quando o tronco ocupa o lote ou sua margem
     * (Regra 22). A copa conectada também sai; proteção e drops vêm de {@link TreeHarvester}.
     */
    private static int clearTrees(ServerWorld world, ConstructionProject project) {
        ColonyPos origin = project.origin();
        ColonyPos size = project.blueprint().size();
        int through = origin.y() + Math.max(size.y(), BuildSiteScanner.VERTICAL_CLEARANCE);
        int cleared = 0;
        java.util.Set<BlockPos> visited = new java.util.HashSet<>();
        java.util.List<ColonyPos> chests = ColonyChests.nearestFirst(world, project.colonyId(), origin);

        for (int dx = -OUTER_MARGIN; dx < size.x() + OUTER_MARGIN; dx++) {
            for (int dz = -OUTER_MARGIN; dz < size.z() + OUTER_MARGIN; dz++) {
                int x = origin.x() + dx;
                int z = origin.z() + dz;
                for (int y = origin.y(); y <= through + 40; y++) {
                    BlockPos at = new BlockPos(x, y, z);
                    if (visited.contains(at) || !world.getBlockState(at).isIn(BlockTags.LOGS)) {
                        continue;
                    }
                    TreeHarvester.Plan tree = TreeHarvester.plan(world, at);
                    if (tree.isEmpty()) {
                        continue;
                    }
                    visited.addAll(tree.blocks());
                    java.util.List<BlockPos> canopy = TreeShape.connectedLeaves(world, tree.species(), tree.blocks());
                    for (BlockPos block : java.util.stream.Stream.concat(tree.blocks().stream(), canopy.stream()).toList()) {
                        for (ItemStack drop : TreeHarvester.breakOne(world, tree, block)) {
                            int left = drop.getCount();
                            for (ColonyPos chest : chests) {
                                left = ChestDepositor.deposit(world, chest, drop.getItem(), left);
                                if (left == 0) {
                                    break;
                                }
                            }
                            if (left > 0) {
                                ItemStack remaining = drop.copyWithCount(left);
                                world.spawnEntity(new ItemEntity(world, block.getX() + 0.5, block.getY() + 0.5,
                                        block.getZ() + 0.5, remaining));
                            }
                        }
                        cleared++;
                    }
                }
            }
        }

        for (int dx = 0; dx < size.x(); dx++) {
            for (int dz = 0; dz < size.z(); dz++) {
                int x = origin.x() + dx;
                int z = origin.z() + dz;

                if (LotGrowth.clearsTreesAt(world, new BlockPos(x, origin.y(), z))) {
                    cleared += LotGrowth.clearColumn(world, x, origin.y(), through, z);
                }
            }
        }

        return cleared;
    }

    /**
     * Tira somente a relva que a base de uma construção comum substituirá.
     *
     * <p>Ela vira terra em vez de ar: a fundação continua apoiada, mas a
     * camada verde não fica enterrada sob a obra. Areia, pedra, terra,
     * líquidos e qualquer posição fora da base planejada permanecem como
     * estavam. Plantações são excluídas porque o relevo e a água fazem parte
     * do desenho funcional delas.
     */
    private static int removeGrassFromHouseBase(ServerWorld world, ConstructionProject project) {
        Blueprint blueprint = project.blueprint();

        if (BlueprintKind.isFarm(blueprint.id())) {
            return 0;
        }

        int converted = 0;

        for (BlueprintBlock block : blueprint.blocks()) {
            if (!blueprint.isBase(block)) {
                continue;
            }

            ColonyPos offset = block.offset();
            ColonyPos origin = project.origin();
            BlockPos at = new BlockPos(
                    origin.x() + offset.x(),
                    origin.y() + offset.y(),
                    origin.z() + offset.z());

            if (world.getBlockState(at).isOf(Blocks.GRASS_BLOCK)) {
                world.setBlockState(at, Blocks.DIRT.getDefaultState(), Block.NOTIFY_ALL);
                converted++;
            }
        }

        return converted;
    }

    /**
     * Planta que sai do caminho, e não bloco que fica.
     *
     * <p>A mesma pergunta que {@code LotGround.isNothing} faz
     * para não reprovar o lote, e é de propósito que sejam a mesma: o
     * que não reprova é exatamente o que alguém precisa tirar. Se as
     * duas divergissem, ou o lote seria recusado por uma flor, ou a flor
     * ficaria dentro da casa.
     *
     * <p>Ar fica de fora porque não há o que limpar nele.
     */
    private static boolean isPlant(BlockState state) {
        if (state.isAir()) {
            return false;
        }

        return state.isIn(BlockTags.REPLACEABLE) || state.isIn(BlockTags.SMALL_FLOWERS);
    }
}
