package com.villagecolony.fabric.work;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.Optional;

/**
 * O vaso com planta é montado, e de que ele é feito — 2026-09-19.
 *
 * <p><b>A pergunta do autor:</b> <i>"algum aldeão produz cacto num vaso?
 * cria vaso? colhe cacto e replanta?"</i>. A resposta era <b>não</b> nos
 * três pontos, e a obra pagava por isso: ela parou em
 * {@code waiting for minecraft:potted_cactus} com 148 blocos por pôr.
 *
 * <p><b>E ia esperar para sempre.</b> No Minecraft o vaso com cacto só
 * existe como <b>bloco</b> — o jogador põe o vaso e planta o cacto nele —,
 * e {@code Items.POTTED_CACTUS} <b>não existe</b>. A colônia esperava um
 * item que não pode existir, e nenhum trabalhador do mundo o traria.
 *
 * <p><b>As duas metades do conserto, e elas são independentes:</b>
 *
 * <pre>
 * BuilderWork    bloco sem item é montado no lugar, e não esperado.
 *                Isso destrava a obra sozinho, e vale para os oito
 *                blocos assim do catálogo — cinco vasos, água, lava e
 *                o caldeirão.
 * esta classe    e ele é montado do que a colônia TEM: o vaso e a
 *                planta saem do baú, e não do nada.
 * </pre>
 *
 * <p><b>Por que a segunda metade importa.</b> Montar sem custo seria a
 * colônia <b>criando recurso</b>, que o Construction-System proíbe na
 * primeira regra de arquitetura. O vaso é três tijolos, o tijolo é argila
 * assada, e o cacto é colhido — a cadeia inteira é do jogo, e cada degrau
 * tem dono entre as profissões.
 *
 * <p>A que planta vai em cada vaso sai do <b>nome do bloco</b>, pela
 * convenção do próprio registro: {@code potted_cactus} é vaso mais
 * {@code cactus}, {@code potted_dandelion} é vaso mais
 * {@code dandelion}. É a ADR-009 — quem sabe é o jogo, e escrever a
 * tabela à mão seria refazer, com risco de errar, o que o registro já
 * mantém.
 */
public final class PottedPlant {

    /** O prefixo que o jogo dá a todo vaso com planta. */
    private static final String POTTED = "potted_";

    private PottedPlant() {
    }

    /** Se este bloco é um vaso com planta. */
    public static boolean isPotted(Block block) {
        return Registries.BLOCK.getId(block).getPath().startsWith(POTTED);
    }

    /**
     * A planta que vai neste vaso, se o jogo tiver o item dela.
     *
     * <p>Vazio quando a planta não tem item — e isso acontece: o
     * {@code potted_dead_bush} pede {@code dead_bush}, que existe, mas um
     * datapack pode registrar vaso de coisa que não se carrega. Nesse
     * caso o vaso continua sendo montado, só que sem custo de planta.
     */
    public static Optional<Item> plantOf(Block potted) {
        String path = Registries.BLOCK.getId(potted).getPath();

        if (!path.startsWith(POTTED)) {
            return Optional.empty();
        }

        Identifier plant = Identifier.of("minecraft", path.substring(POTTED.length()));

        if (!Registries.ITEM.containsId(plant)) {
            return Optional.empty();
        }

        Item item = Registries.ITEM.get(plant);

        return item == Items.AIR ? Optional.empty() : Optional.of(item);
    }

    /** O vaso vazio, que é o outro ingrediente de todo vaso com planta. */
    public static Item pot() {
        return Items.FLOWER_POT;
    }

    /** Se este estado é um vaso com planta. */
    public static boolean isPotted(BlockState state) {
        return isPotted(state.getBlock());
    }
}
