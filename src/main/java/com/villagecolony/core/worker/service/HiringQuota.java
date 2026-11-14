package com.villagecolony.core.worker.service;

import org.jspecify.annotations.Nullable;

import com.villagecolony.core.worker.model.ProfessionPolicySet;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * As cotas de contratação de {@link ProfessionAssigner}: quantas cabeças
 * cada profissão recebe, o teto da política do mundo e a vaga que a demanda
 * da obra reclama.
 *
 * <p>Separada em 2026-10-02, ao juntar a demanda da obra (30-09) com as
 * políticas de profissão (ADR-030): as duas regras somadas passavam do teto
 * de linhas do assinador.
 */
final class HiringQuota {

    private HiringQuota() {
    }

    /**
     * A vaga que a demanda da obra reclama, se houver vaga na colônia.
     *
     * <p>Só com candidato: a pergunta sem dono é a contagem da colônia e
     * não sabe de qual colônia é a demanda.
     */
    static Optional<ProfessionType> demandedVacancy(
            @Nullable Worker candidate, Map<ProfessionType, Integer> counts, int adultPopulation,
            List<ProfessionType> growthOrder, ProfessionPolicySet policies) {

        if (candidate == null) {
            return Optional.empty();
        }

        boolean anySlotOpen = false;

        for (ProfessionType type : growthOrder) {
            if (!atMaximum(type, counts, policies)
                    && counts.getOrDefault(type, 0)
                            < targetCount(type, adultPopulation, growthOrder, policies)) {
                anySlotOpen = true;
                break;
            }
        }

        // Sem vaga aberta, quem está sem ofício ainda pega o machado se a obra espera madeira (Regra 189).
        boolean onlyWood = !anySlotOpen;

        if (onlyWood && candidate.profession().isPresent()) {
            return Optional.empty();
        }

        for (ProfessionType type : ProfessionDemand.of(candidate.colonyId())) {
            if ((onlyWood && type != ProfessionType.LUMBERJACK) || !counts.containsKey(type) || !growthOrder.contains(type)
                    || atMaximum(type, counts, policies) || candidate.isShunning(type)
                    || ProfessionDemand.atCap(candidate.colonyId(), type, counts.getOrDefault(type, 0))) {
                continue;
            }

            if (counts.getOrDefault(type, 0)
                    <= targetCount(type, adultPopulation, growthOrder, policies)) {
                return Optional.of(type);
            }
        }

        return Optional.empty();
    }

    /** Quantos construtores a vila contrata: um, porque a vila tem uma obra aberta por vez (Regra 273). */
    static final int BUILDERS = 1;

    static boolean atMaximum(ProfessionType type, Map<ProfessionType, Integer> counts,
            ProfessionPolicySet policies) {
        int maximum = policies.policyOf(type).maximumWorkers();

        // Uma obra aberta por vila, um construtor, também em mundo salvo com a política antiga (Regra 273).
        if (type == ProfessionType.BUILDER && (maximum == 0 || maximum > BUILDERS)) {
            maximum = BUILDERS;
        }

        return maximum != 0 && counts.getOrDefault(type, 0) >= maximum;
    }

    static int targetCount(ProfessionType type, int adults, List<ProfessionType> order,
            ProfessionPolicySet policies) {
        if (order.isEmpty()) {
            return 0;
        }
        int slots = adults < ProfessionAssigner.ADULTS_PER_BATCH ? Math.min(adults, ProfessionAssigner.GROWTH_ORDER.size())
                : (adults / ProfessionAssigner.ADULTS_PER_BATCH) * ProfessionAssigner.GROWTH_ORDER.size()
                + Math.min(adults % ProfessionAssigner.ADULTS_PER_BATCH, ProfessionAssigner.GROWTH_ORDER.size());
        Map<ProfessionType, Integer> targets = new EnumMap<>(ProfessionType.class);
        for (ProfessionType role : order) {
            targets.put(role, 0);
        }
        int cursor = 0;
        for (int assigned = 0; assigned < slots; assigned++) {
            int checked = 0;
            while (checked < order.size() && atMaximum(order.get(cursor), targets, policies)) {
                cursor = (cursor + 1) % order.size();
                checked++;
            }
            if (checked == order.size()) {
                break;
            }
            ProfessionType selected = order.get(cursor);
            targets.merge(selected, 1, Integer::sum);
            cursor = (cursor + 1) % order.size();
        }
        return targets.getOrDefault(type, 0);
    }
}
