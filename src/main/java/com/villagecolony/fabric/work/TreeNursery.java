package com.villagecolony.fabric.work;

import com.villagecolony.fabric.integration.LotGround;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.VillageBiomes;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

/**
 * O viveiro do fazendeiro, na borda da vila — 2026-09-19.
 *
 * <p><b>Decisão do autor:</b> <i>"adicionar ao fazendeiro uma habilidade
 * nova: no limite da vila, o fazendeiro deve adicionar um bloco de terra
 * enraizada e plantar um rebento (cada bioma planta o tipo de rebento que
 * precisa para criar seus itens, portas e afins)"</i>.
 *
 * <p><b>O que isto conserta, medido na sessão de 13:04→13:41.</b> A obra
 * parou esperando {@code jungle_door} numa vila de <b>deserto</b>. A
 * colônia tinha dez toras e nove tábuas de selva ao todo, e o lenhador
 * passou a sessão inteira dizendo <i>"looking for a tree"</i> — 37 vezes.
 * Não é substituição que falta: a regra da madeira já existe desde 09-05
 * e aceita qualquer espécie. <b>Falta árvore.</b>
 *
 * <p>No deserto isso é estrutural: a vila nasce onde não há floresta, e
 * as poucas árvores ao alcance acabam. Sem quem plante, a colônia fica
 * sem madeira para sempre — e porta, cama e móvel são de madeira.
 *
 * <p><b>Por que terra enraizada.</b> É a decisão do autor, e ela tem uma
 * propriedade útil: {@code rooted_dirt} é terra de verdade — o rebento
 * pega nela — e <b>se distingue à vista</b> do chão do bioma. Quem olha a
 * vila sabe que aquilo é plantio da colônia, e não duna que sobrou.
 *
 * <p><b>A espécie sai do bioma</b>, pela mesma tabela que já decide a
 * tábua da obra ({@code VillageBiomes.woodAt}) — ADR-009, quem sabe é o
 * jogo. Plantar carvalho num deserto que constrói com selva daria madeira
 * que a planta não pede.
 */
public final class TreeNursery {

    /** O chão que o viveiro assenta antes do rebento. */
    public static final Block BED = Blocks.ROOTED_DIRT;

    private TreeNursery() {
    }

    /**
     * O rebento da madeira desta vila, se o jogo tiver um.
     *
     * <p>Pela convenção de nome — {@code <espécie>_planks} vira
     * {@code <espécie>_sapling} —, e conferida contra a tag
     * {@link BlockTags#SAPLINGS} do próprio jogo. A convenção sozinha
     * inventaria {@code bamboo_sapling}, que não existe; a tag sozinha
     * não diria <b>qual</b> das onze serve a esta vila.
     */
    public static Optional<Block> saplingFor(ServerWorld world, ColonyPos where) {
        return VillageBiomes.woodAt(world, where).flatMap(TreeNursery::saplingOf);
    }

    /** O rebento da espécie desta tábua, se existir e for mesmo um rebento. */
    public static Optional<Block> saplingOf(ResourceId planks) {
        String path = planks.path();

        if (!path.endsWith("_planks")) {
            return Optional.empty();
        }

        String species = path.substring(0, path.length() - "_planks".length());

        Identifier id = Identifier.of(ResourceId.VANILLA, species + "_sapling");

        if (!Registries.BLOCK.containsId(id)) {
            return Optional.empty();
        }

        Block sapling = Registries.BLOCK.get(id);

        // <b>E o jogo confirma que é rebento</b> — ADR-009. Sem esta
        // pergunta a convenção de nome bastaria, e ela erra: há madeira
        // no jogo cujo "rebento" é outra coisa (o bambu é broto, o
        // carmesim é fungo), e plantar o bloco errado sairia como um
        // canteiro que nunca vira árvore.
        return sapling.getDefaultState().isIn(BlockTags.SAPLINGS)
                ? Optional.of(sapling)
                : Optional.empty();
    }

    /**
     * Se este ponto serve de viveiro.
     *
     * <p>Três condições, e as três juntas — a mesma forma do
     * {@code SandPatch}:
     *
     * <pre>
     * o chão é do bioma      nada de peça de vila nem obra da colônia
     * o espaço está livre    ar acima, onde a árvore vai crescer
     * não há rebento já      dois viveiros no mesmo lugar é um só
     * </pre>
     */
    public static boolean isSpotForANursery(ServerWorld world, BlockPos ground) {
        BlockState above = world.getBlockState(ground.up());

        if (!above.isAir()) {
            return false;
        }

        BlockState state = world.getBlockState(ground);

        if (state.isOf(BED)) {
            // Já é viveiro: o rebento dele é assunto do plantio, não de
            // procurar lugar novo.
            return false;
        }

        return com.villagecolony.fabric.integration.LotGround.isBiomeGround(world, ground)
                && !com.villagecolony.fabric.integration.BlockProtection
                        .isVillageOriginal(world, ground);
    }

    /**
     * Assenta o viveiro: terra enraizada embaixo, rebento em cima.
     *
     * @return {@code true} se o viveiro nasceu agora
     */
    public static boolean plant(ServerWorld world, BlockPos ground, Block sapling) {
        if (!isSpotForANursery(world, ground)) {
            return false;
        }

        BlockState seedling = sapling.getDefaultState();

        BlockState had = world.getBlockState(ground);

        world.setBlockState(ground, BED.getDefaultState(), Block.NOTIFY_ALL);

        if (!seedling.canPlaceAt(world, ground.up())) {
            // <b>Devolve o chão que estava ali</b>, e não um chão
            // escolhido por mim: repor areia numa vila de planície
            // trocaria grama por deserto, que é estrago onde havia só
            // uma tentativa falha. Guardar o estado antes é a única
            // forma de desfazer sem inventar.
            world.setBlockState(ground, had, Block.NOTIFY_ALL);

            return false;
        }

        world.setBlockState(ground.up(), seedling, Block.NOTIFY_ALL);

        return true;
    }

    /** O bloco que o rebento vira, para a bateria e o log. */
    public static ResourceId idOf(Block block) {
        return MinecraftTypeAdapter.toResourceId(block);
    }
}
