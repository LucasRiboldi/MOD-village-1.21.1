package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.Production;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ColonyChests;
import com.villagecolony.fabric.integration.CraftingLookup;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * A cadeia que traz uma peça até a obra, elo por elo — E5, decisão do autor de
 * 2026-10-08. O {@code /vc log} dizia "uma profissão consegue no bioma; falta
 * entregar" para o funil quando <b>nenhuma</b> profissão tinha o pedido; a frase
 * certa nomeia o elo que falta: <i>hopper (bancada) ← iron_ingot (fundidor: sem
 * tarefa) ← raw_iron (mineiro: tarefa aberta)</i>.
 *
 * <p>Desce pela fornalha quando o item é fundido e pela bancada quando é
 * fabricado, sem passar pela forma guardada (E6); para no primeiro elo que está
 * no baú ou em {@link #DEPTH} degraus. Só lê: não abre tarefa nem mexe em baú.
 */
public final class ProductionChain {

    /** Em que pé está um elo. */
    public enum Link {
        /** Já há no baú da colônia. */
        IN_CHEST,
        /** Uma profissão tem tarefa aberta para ele. */
        TASK_OPEN,
        /** Uma profissão o obtém, e nenhuma tem tarefa para ele agora. */
        NO_TASK,
        /** Sai da bancada da colônia quando os ingredientes chegam; não pede tarefa. */
        AT_THE_BENCH,
        /** Ninguém da colônia o obtém. */
        NO_SOURCE
    }

    /** Um elo: o item, quem o obtém e em que pé está. */
    public record Step(Item item, @Nullable String profession, Link link) {

        String say() {
            String name = Registries.ITEM.getId(item).getPath();

            return switch (link) {
                case IN_CHEST -> name + " (no baú)";
                case AT_THE_BENCH -> name + " (bancada)";
                case NO_SOURCE -> name + " (ninguém obtém)";
                case TASK_OPEN -> name + " (" + profession + ": tarefa aberta)";
                case NO_TASK -> name + " (" + profession + ": sem tarefa)";
            };
        }
    }

    /** Quantos degraus a descida olha. */
    static final int DEPTH = 4;

    private ProductionChain() {
    }

    /** Os elos, da peça para baixo. */
    public static List<Step> of(ServerWorld world, UUID colonyId, Item piece, List<ColonyPos> chests) {
        List<Step> steps = new ArrayList<>();
        Set<Item> seen = new HashSet<>();
        Item current = piece;

        for (int depth = 0; depth < DEPTH && current != null && seen.add(current); depth++) {
            Optional<ResourceType> resource = MinecraftTypeAdapter.toResourceType(current);
            Link link;
            String who = resource.map(found -> professionOf(found.production())).orElse(null);

            if (ColonyChests.countIn(world, chests, current) > 0) {
                link = Link.IN_CHEST;
            } else if (resource.isPresent() && hasOpenTask(colonyId, resource.get())) {
                link = Link.TASK_OPEN;
            } else if (resource.isPresent()) {
                link = Link.NO_TASK;
            } else if (CraftingLookup.billFor(world, current, CraftingLookup.producing(world, current)).isPresent()) {
                link = Link.AT_THE_BENCH;
            } else {
                link = Link.NO_SOURCE;
            }

            steps.add(new Step(current, who, link));

            if (link == Link.IN_CHEST || link == Link.NO_SOURCE) {
                break;
            }

            current = nextDown(world, current, chests);
        }

        return steps;
    }

    /** A frase: os elos em ordem e o primeiro que falta. */
    public static String describe(List<Step> steps) {
        StringBuilder text = new StringBuilder();

        for (Step step : steps) {
            text.append(text.length() == 0 ? "" : " ← ").append(step.say());
        }

        missing(steps).ifPresent(step -> text.append(" — falta: ").append(step.say()));

        return text.toString();
    }

    /** O primeiro elo sem ninguém trabalhando nele. */
    public static Optional<Step> missing(List<Step> steps) {
        return steps.stream()
                .filter(step -> step.link() == Link.NO_TASK || step.link() == Link.NO_SOURCE)
                .findFirst();
    }

    private static boolean hasOpenTask(UUID colonyId, ResourceType resource) {
        return VillageColonyMod.TASKS.ofColony(colonyId).stream()
                .anyMatch(task -> task.isOpen() && task.targetResource() == resource);
    }

    /**
     * O degrau de baixo: a entrada da fornalha, se o item é fundido; senão o
     * ingrediente de bancada que falta no baú, preferindo o que é recurso da colônia.
     */
    private static @Nullable Item nextDown(ServerWorld world, Item item, List<ColonyPos> chests) {
        boolean smelted = MinecraftTypeAdapter.toResourceType(item)
                .map(found -> found.production() == Production.SMELTED)
                .orElse(false);

        if (smelted) {
            List<Item> inputs = CraftingLookup.smeltingInputsFor(world, item);

            return inputs.stream()
                    .filter(input -> MinecraftTypeAdapter.toResourceType(input).isPresent())
                    .findFirst()
                    .orElse(inputs.isEmpty() ? null : inputs.getFirst());
        }

        Optional<CraftingLookup.Bill> bill =
                CraftingLookup.billFor(world, item, CraftingLookup.producing(world, item));

        if (bill.isEmpty()) {
            return null;
        }

        Item fallback = null;

        for (Map.Entry<Item, Integer> part : bill.get().ingredients().entrySet()) {
            if (ColonyChests.countIn(world, chests, part.getKey()) >= part.getValue()) {
                continue;
            }

            if (MinecraftTypeAdapter.toResourceType(part.getKey()).isPresent()) {
                return part.getKey();
            }

            if (fallback == null) {
                fallback = part.getKey();
            }
        }

        return fallback;
    }

    private static String professionOf(Production production) {
        return switch (production) {
            case HARVESTED -> "lenhador";
            case MINED -> "mineiro";
            case SMELTED -> "fundidor";
            case CRAFTED_WOOD -> "carpinteiro";
            case CRAFTED_STONE -> "pedreiro";
            case FARMED -> "fazendeiro";
            case SHEARED -> "pastor";
            case SURFACE_GATHERED, SOIL_GATHERED -> "coleta de superfície";
        };
    }
}
