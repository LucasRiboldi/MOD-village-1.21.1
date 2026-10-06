package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.VillagePalette;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.Side;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.StructureBlueprintReader;
import com.villagecolony.fabric.integration.VillageStructures;
import com.villagecolony.fabric.integration.VillageBiomes;
import net.minecraft.server.world.ServerWorld;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Random;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

/**
 * A ordem da lista de plantas: a menor primeiro enquanto não há casa, e as
 * recusadas fora sem esvaziar a lista — separado de {@link HousePlans} em
 * 2026-09-24, quando ele passou de 500 linhas.
 *
 * <p>São decisões puras sobre uma lista, e por isso testáveis sem mundo; ver
 * {@code HousePlansTest}. Os comentários vieram junto sem mudança.
 */
final class PlanOrdering {

    private PlanOrdering() {
    }

    /**
     * As plantas numa ordem que não favorece tamanho — ADR-036 item 5, que
     * desfez a Regra 25 (a maior que couber).
     *
     * <p>A varredura tenta as pegadas na ordem da lista, então a ordem é a
     * preferência. Embaralhar a cada chamada mudaria a pegada no meio de uma
     * varredura pausada; por isso a semente é estável (a colônia e quantas
     * construções ela tem) e muda só quando uma obra termina. A ordem de
     * entrada não importa: a lista é ordenada por id antes de embaralhar.
     */
    static List<Blueprint> mixed(List<Blueprint> plans, long seed) {
        List<Blueprint> ordered = new ArrayList<>(plans);

        ordered.sort(Comparator.comparing(plan -> plan.id().toString()));
        Collections.shuffle(ordered, new Random(seed));

        return List.copyOf(ordered);
    }

    /** A semente de {@link #mixed} para uma colônia com tantas construções. */
    static long seedFor(UUID colonyId, int buildings) {
        return colonyId.getMostSignificantBits() ^ colonyId.getLeastSignificantBits() ^ buildings;
    }

    /**
     * A menor planta na frente, enquanto a colônia não tem casa — decisão do
     * autor, 2026-09-15: a primeira casa é a que a colônia levanta sozinha.
     * O resto fica na ordem em que chegou. Reordena, não encurta.
     */
    static List<Blueprint> smallestFirst(List<Blueprint> plans, boolean hasNoHouseYet) {
        if (!hasNoHouseYet || plans.size() < 2) {
            return plans;
        }

        List<Blueprint> reordered = new ArrayList<>(plans);
        Blueprint smallest = smallestOf(reordered);

        reordered.remove(smallest);
        reordered.add(0, smallest);

        return List.copyOf(reordered);
    }

    private static Blueprint smallestOf(List<Blueprint> plans) {
        return plans.stream().min(Comparator.comparingInt(HousePlans::volumeOf)).orElseThrow();
    }

    /**
     * A lista sem as plantas marcadas — e nunca vazia.
     *
     * <p><b>Separada de {@link FarmPlans#plansFor} porque é a decisão, e decisão se
     * afirma sem mundo.</b> Perguntar ao {@code PlanRefusals} varre baú;
     * escolher o que fica da lista não precisa de nada. Com as duas juntas,
     * o único teste possível seria de jogo — e a base já registrou o preço
     * de um filtro que nada exercitava: quando a divisão do fabricante
     * entrou, removido o {@code continue}, <b>701 unitários e 275 testes de
     * jogo continuavam verdes</b>.
     *
     * <p><b>Nunca devolve vazio tendo planta no catálogo.</b> Se todas
     * estiverem marcadas, vale a menor por volume: a vila não para de
     * planejar, e a linha de desistência continua dizendo o que falta.
     *
     * <p><b>Visível ao pacote para o teste.</b> {@code HousePlansTest}
     * afirma as duas coisas: que a marcada sai, e que a lista não fica
     * vazia.
     */
    static List<Blueprint> without(List<Blueprint> plans, Set<ResourceId> skipped) {
        List<Blueprint> offered = new ArrayList<>();

        for (Blueprint plan : plans) {
            if (!skipped.contains(plan.id())) {
                offered.add(plan);
            }
        }

        if (offered.isEmpty() && !plans.isEmpty()) {
            return List.of(smallestOf(plans));
        }

        return List.copyOf(offered);
    }
}
