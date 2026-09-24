package com.villagecolony.fabric.work;

import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A peça equivalente que o jogo não marca em tag — N3, 2026-09-24.
 *
 * <p><b>Decisão do autor:</b> <i>"trocar por equivalente; não existindo no
 * bioma, daí fazer o item nascer no baú pronto"</i>. A ADR-022 já tentava a
 * família inteira antes de criar a peça — mas a família só existia para
 * madeira, cama e terracota. A auditoria de 24-09 mostrou o resto nascendo
 * no baú só pela cor ou pela pedra: vidraça amarela, terracota vitrificada
 * laranja, escada de granito, muro de diorito, pedregulho musgoso, madeira
 * descascada de abeto, grama alta.
 *
 * <p>As famílias com tag do jogo (lã, tapete, estandarte, muda, flor,
 * portão, botão) ficam em {@link MaterialChoice}. Aqui ficam as que o jogo
 * só agrupa pelo <b>nome</b>, com a mesma convenção que o {@code strip} já
 * usa para as toras descascadas.
 *
 * <p><b>O preferido continua primeiro.</b> A família só é consultada
 * quando o exato não tem rota, e o equivalente com rota no bioma vence a
 * peça criada do nada.
 */
final class EquivalentPieces {

    /**
     * As pedras de construção, da mais comum para a mais rara.
     *
     * <p>Pedregulho primeiro porque é o que o mineiro traz de qualquer
     * bioma; arenito entra porque no deserto é ele a pedra da vila.
     */
    private static final List<String> STONES = List.of(
            "cobblestone", "stone", "stone_brick", "mossy_cobblestone", "mossy_stone_brick",
            "sandstone", "smooth_sandstone", "red_sandstone",
            "andesite", "polished_andesite", "granite", "polished_granite",
            "diorite", "polished_diorite", "smooth_stone");

    private static final List<String> STONE_SHAPES = List.of("_stairs", "_slab", "_wall");

    private static final Pattern STRIPPED_WOOD = Pattern.compile("stripped_[a-z_]+_wood");

    private static final List<List<String>> PLANTS = List.of(
            List.of("short_grass", "fern"),
            List.of("tall_grass", "large_fern"));

    private EquivalentPieces() {
    }

    /** Acrescenta a {@code order} os equivalentes de {@code exact} que ainda não estão lá. */
    static void addFamily(Item exact, List<Item> order) {
        String path = MinecraftTypeAdapter.toResourceId(Block.getBlockFromItem(exact)).path();

        for (String name : familyOf(path)) {
            Item item = Registries.ITEM.get(Identifier.ofVanilla(name));

            if (item != Items.AIR && !order.contains(item)) {
                order.add(item);
            }
        }
    }

    /** Os nomes da família desta peça, sem ela própria garantida na frente. */
    static List<String> familyOf(String path) {
        for (String shape : STONE_SHAPES) {
            for (String stone : STONES) {
                if (path.equals(stone + shape)) {
                    return STONES.stream().map(each -> each + shape).toList();
                }
            }
        }

        if (path.equals("mossy_cobblestone") || path.equals("mossy_stone_bricks")) {
            return List.of("cobblestone", "stone_bricks");
        }

        if (path.equals("granite") || path.equals("diorite") || path.equals("andesite")
                || path.equals("polished_granite") || path.equals("polished_diorite")
                || path.equals("polished_andesite")) {
            return List.of("andesite", "granite", "diorite", "cobblestone");
        }

        if (path.equals("glass_pane") || path.endsWith("_stained_glass_pane")) {
            return colored("stained_glass_pane", "glass_pane");
        }

        if (path.equals("glass") || path.endsWith("_stained_glass")) {
            return colored("stained_glass", "glass");
        }

        if (path.endsWith("_glazed_terracotta")) {
            List<String> family = new ArrayList<>(colored("glazed_terracotta", null));
            family.addAll(colored("terracotta", "terracotta"));

            return family;
        }

        if (STRIPPED_WOOD.matcher(path).matches()) {
            return Registries.ITEM.getIds().stream()
                    .map(Identifier::getPath)
                    .filter(name -> STRIPPED_WOOD.matcher(name).matches())
                    .sorted()
                    .toList();
        }

        for (List<String> plants : PLANTS) {
            if (plants.contains(path)) {
                return plants;
            }
        }

        return List.of();
    }

    /** As dezesseis cores de uma peça, e a versão sem cor por último. */
    private static List<String> colored(String suffix, String plain) {
        List<String> family = new ArrayList<>();

        for (DyeColor color : DyeColor.values()) {
            family.add(color.getName() + "_" + suffix);
        }

        if (plain != null) {
            family.add(plain);
        }

        return family;
    }
}
