package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.worker.model.ProfessionType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A ordem em que a vila levanta as coisas — N9, 2026-09-24.
 *
 * <p><b>Decisão do autor:</b> <i>"priorizar casa, depois estrutura vinculada
 * à profissão; depois de ter uma de cada profissão, intercalar as demais
 * que sobraram"</i>. O rodízio casa/outra continua (e o E48 continua
 * forçando casa quando falta cama); o que esta classe decide é <b>qual
 * outra</b>: primeiro a oficina de cada ofício que ainda não tem a sua, e
 * só depois o resto do catálogo — templo, cercado, poste, as oficinas
 * que não são de ofício da colônia — começando pelo que a vila ainda não
 * tem.
 *
 * <p><b>Casa é casa.</b> As oficinas do jogo têm cama e por isso contavam
 * como moradia: a vez da casa podia sair açougue. Agora a vez da casa só
 * oferece casa de morar ({@link #isShop} separa as oficinas pelo nome).
 *
 * <p><b>Por substring</b>, como o resto do catálogo, porque os nomes do
 * jogo mudam entre estilos: {@code butcher_shop} e {@code butchers_shop},
 * {@code mason_1} e {@code masons_house_1}.
 */
final class ConstructionOrder {

    /**
     * A oficina de cada ofício, na ordem em que a vila as levanta.
     *
     * <p>Só os ofícios que têm oficina no jogo. Lenhador, mineiro e
     * construtor não têm estrutura de profissão no Vanilla, e inventar uma
     * seria sair do catálogo.
     */
    static final Map<ProfessionType, List<String>> WORKSHOPS;

    static {
        Map<ProfessionType, List<String>> order = new LinkedHashMap<>();
        order.put(ProfessionType.FARMER, List.of("farm"));
        order.put(ProfessionType.SHEPHERD, List.of("shepherd", "animal_pen"));
        order.put(ProfessionType.MASON, List.of("mason"));
        order.put(ProfessionType.SMELTER, List.of("armorer", "tool_smith", "weaponsmith", "weapon_smith"));
        order.put(ProfessionType.CARPENTER, List.of("fletcher"));
        WORKSHOPS = Collections.unmodifiableMap(order);
    }

    /** As oficinas do jogo — com cama, mas lugar de trabalho, e não casa. */
    private static final List<String> SHOPS = List.of(
            "armorer", "butcher", "cartographer", "fisher", "fletcher", "library",
            "mason", "shepherd", "tannery", "tool_smith", "weaponsmith", "weapon_smith");

    private ConstructionOrder() {
    }

    /** O ofício da colônia a que esta planta serve, se servir a algum. */
    static Optional<ProfessionType> professionOf(ResourceId id) {
        for (Map.Entry<ProfessionType, List<String>> entry : WORKSHOPS.entrySet()) {
            for (String name : entry.getValue()) {
                if (id.path().contains(name)) {
                    return Optional.of(entry.getKey());
                }
            }
        }

        return Optional.empty();
    }

    /** O nome da oficina do jogo nesta planta, se ela for uma. */
    static Optional<String> shopType(ResourceId id) {
        return SHOPS.stream().filter(name -> id.path().contains(name)).findFirst();
    }

    /** Se esta planta é oficina do jogo, e não casa de morar. */
    static boolean isShop(ResourceId id) {
        return shopType(id).isPresent();
    }

    /**
     * Os ofícios ainda sem oficina de pé, na ordem de {@link #WORKSHOPS},
     * entre os que o catálogo desta vila oferece.
     */
    static List<ProfessionType> missingWorkshops(
            List<Building> buildings, Set<ProfessionType> offered) {

        List<ProfessionType> missing = new ArrayList<>();

        for (ProfessionType profession : WORKSHOPS.keySet()) {
            if (!offered.contains(profession)) {
                continue;
            }

            boolean standing = buildings.stream()
                    .filter(Building::finished)
                    .anyMatch(building -> professionOf(building.blueprint())
                            .filter(profession::equals)
                            .isPresent());

            if (!standing) {
                missing.add(profession);
            }
        }

        return List.copyOf(missing);
    }

    /**
     * O tipo a levantar na vez de "outra", entre os que o catálogo oferece.
     *
     * <p>Primeiro a oficina do primeiro ofício sem oficina; depois o tipo
     * que a vila ainda não tem; e, tendo de tudo, o primeiro da lista. Quem
     * chama já tirou o tipo da obra anterior, que é a regra da alternância.
     *
     * @param types os tipos oferecidos, na ordem do catálogo
     * @param typeOf o tipo de uma planta, para comparar com o que existe
     */
    static Optional<String> nextType(
            List<Building> buildings,
            Collection<String> types,
            Map<String, Optional<ProfessionType>> professionOfType,
            java.util.function.Function<ResourceId, String> typeOf) {

        Set<ProfessionType> offered = new java.util.HashSet<>();

        for (String type : types) {
            professionOfType.getOrDefault(type, Optional.empty()).ifPresent(offered::add);
        }

        for (ProfessionType profession : missingWorkshops(buildings, offered)) {
            for (String type : types) {
                if (professionOfType.getOrDefault(type, Optional.empty())
                        .filter(profession::equals)
                        .isPresent()) {
                    return Optional.of(type);
                }
            }
        }

        for (String type : types) {
            boolean built = buildings.stream()
                    .filter(Building::finished)
                    .anyMatch(building -> typeOf.apply(building.blueprint()).equals(type));

            if (!built) {
                return Optional.of(type);
            }
        }

        return types.stream().findFirst();
    }
}
