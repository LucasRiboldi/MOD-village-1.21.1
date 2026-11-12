package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ResourceId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * A troca de blocos que veste uma estrutura do mod com o bioma da vila — Regra 262 (autor, 2026-11-12): um arquivo serve
 * todos os biomas. Toda madeira do arquivo vira a madeira da vila ({@code VillageBiomes.woodAt}) na mesma forma (tora,
 * tábua, laje, cerca, porta...), e o pedregulho ou o arenito vira a pedra da paleta das casas ({@link VillagePalette#stone}),
 * também na mesma forma (bloco, laje, degrau, muro). O resto do arquivo fica como está.
 *
 * <p>O <b>papel</b> de um bloco ({@link #role}) é o nome dele com a madeira e a pedra apagadas: {@code spruce_fence} e
 * {@code oak_fence} têm o mesmo papel. É por ele que se confere o que já está de pé, para que a estrutura erguida antes
 * desta regra, ou noutro bioma, continue reconhecida sem ser trocada.
 *
 * @param wood a espécie da madeira da vila ({@code oak}, {@code spruce}, {@code acacia})
 * @param stone a pedra da paleta das casas: pedregulho ou arenito
 */
public record BiomeBlockSwap(String wood, ResourceId stone) {

    /** As espécies que a troca reconhece; carvalho-escuro antes de carvalho, que é sufixo dele. */
    static final List<String> WOODS = List.of(
            "dark_oak", "oak", "spruce", "birch", "jungle", "acacia", "mangrove", "cherry");

    /** As pedras que trocam entre si, nas formas que as duas têm. */
    static final List<String> STONES = List.of("cobblestone", "sandstone");

    static final List<String> STONE_SHAPES = List.of("", "_slab", "_stairs", "_wall");

    /** A troca neutra: o papel de um bloco é o nome dele por esta troca. */
    private static final BiomeBlockSwap ROLE =
            new BiomeBlockSwap("oak", VillagePalette.COBBLESTONE);

    public BiomeBlockSwap {
        Objects.requireNonNull(wood, "wood");
        Objects.requireNonNull(stone, "stone");
    }

    /** A troca da vila desta paleta, com a madeira dada pelo nome da tábua ({@code minecraft:spruce_planks}). */
    public static BiomeBlockSwap of(VillagePalette palette, ResourceId planks) {
        String path = planks.path();

        return new BiomeBlockSwap(path.endsWith("_planks") ? path.substring(0, path.length() - "_planks".length()) : path,
                palette.stone());
    }

    /** O bloco que entra no lugar deste; o mesmo, quando ele não é madeira nem pedra trocável. */
    public ResourceId apply(ResourceId block) {
        return swapped(block).orElse(block);
    }

    /** A planta com cada bloco trocado, na mesma ordem e camada da rua; {@code exists} recusa nome que o jogo não tem. */
    public Blueprint apply(Blueprint plan, Predicate<ResourceId> exists) {
        List<BlueprintBlock> blocks = new ArrayList<>(plan.blocks().size());
        boolean changed = false;

        for (BlueprintBlock block : plan.blocks()) {
            Optional<ResourceId> other = swapped(block.block()).filter(exists);

            if (other.isPresent()) {
                blocks.add(new BlueprintBlock(block.offset(), other.get(), block.furniture(), block.facing(), block.states()));
                changed = true;
            } else {
                blocks.add(block);
            }
        }

        if (!changed) {
            return plan;
        }

        Blueprint swapped = Blueprint.of(plan.id(), blocks);

        return plan.hasStreetLayer() ? swapped.withStreetLayer(plan.streetLayer()) : swapped;
    }

    /** O papel do bloco: o nome com a madeira e a pedra apagadas. */
    public static ResourceId role(ResourceId block) {
        return ROLE.apply(block);
    }

    /** Se os dois blocos fazem o mesmo papel numa estrutura do mod. */
    public static boolean sameRole(ResourceId one, ResourceId other) {
        return one.equals(other) || role(one).equals(role(other));
    }

    private Optional<ResourceId> swapped(ResourceId block) {
        if (!ResourceId.VANILLA.equals(block.namespace())) {
            return Optional.empty();
        }

        String path = block.path();
        Optional<String> stoned = stoneOf(path);

        if (stoned.isPresent()) {
            return stoned.map(name -> new ResourceId(ResourceId.VANILLA, name)).filter(other -> !other.equals(block));
        }

        return woodOf(path).map(name -> new ResourceId(ResourceId.VANILLA, name)).filter(other -> !other.equals(block));
    }

    private Optional<String> stoneOf(String path) {
        String target = stone.path();

        if (!STONES.contains(target)) {
            return Optional.empty();
        }

        for (String from : STONES) {
            for (String shape : STONE_SHAPES) {
                if (path.equals(from + shape)) {
                    return Optional.of(target + shape);
                }
            }
        }

        return Optional.empty();
    }

    private Optional<String> woodOf(String path) {
        String prefix = path.startsWith("stripped_") ? "stripped_" : "";
        String rest = path.substring(prefix.length());

        for (String species : WOODS) {
            if (rest.startsWith(species + "_")) {
                return Optional.of(prefix + wood + rest.substring(species.length()));
            }
        }

        return Optional.empty();
    }
}
