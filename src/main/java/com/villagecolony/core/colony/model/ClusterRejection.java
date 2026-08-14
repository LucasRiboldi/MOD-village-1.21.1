package com.villagecolony.core.colony.model;

import com.villagecolony.core.type.ColonyPos;

import java.util.Objects;

/**
 * Um aglomerado de camas que <b>não</b> passou na validação de vila.
 *
 * <p>É o contrário de {@link VillageCandidate}, e existe pelo mesmo
 * motivo que ele: a detecção precisa dizer o que viu, não só o que
 * aprovou.
 *
 * <p><b>Por que a recusa precisa ser dita.</b> Até 2026-08-13 a detecção
 * devolvia apenas aglomerados aprovados, e com isso "a vila deixou de ser
 * viável" e "a vila não foi observada" chegavam ao mesmo lugar com a
 * mesma cara: uma lista vazia. A ADR-003 §6 pede distinguir as duas — a
 * primeira marca a colônia como {@link ColonyState#ABANDONED}, a segunda
 * não pode marcar nada. Sem esta classe, {@code ColonyState.ABANDONED}
 * era um valor que nada atribuía.
 *
 * <p>Recusa não é erro. Uma cama solta no meio do mundo produz uma
 * recusa a cada varredura, e é o comportamento certo.
 *
 * @param center onde o aglomerado está — a média das camas dele
 * @param bedCount camas encontradas
 * @param villagerCount aldeões vivos contados na área
 * @param reason qual das duas condições da ADR-003 §3 falhou
 */
public record ClusterRejection(
        ColonyPos center, int bedCount, int villagerCount, Reason reason) {

    /**
     * Aldeões não contados.
     *
     * <p>Quando as camas já não bastam, contar aldeões seria pagar uma
     * busca por entidades para descobrir algo que não muda a resposta.
     * O número existe no registro para quem lê o log, e mentir "0" ali
     * faria parecer vila esvaziada o que é acampamento.
     */
    public static final int VILLAGERS_NOT_COUNTED = -1;

    public ClusterRejection {
        Objects.requireNonNull(center, "center");
        Objects.requireNonNull(reason, "reason");
    }

    /** Como escrever a contagem de aldeões, inclusive quando não houve. */
    public String villagersAsText() {
        return villagerCount == VILLAGERS_NOT_COUNTED ? "not counted" : String.valueOf(villagerCount);
    }

    /**
     * Por que um aglomerado não é vila.
     *
     * <p>As duas condições do Passo 3 da ADR-003, separadas porque
     * significam coisas diferentes para quem lê o log: camas de menos é
     * acampamento ou vila demolida; aldeões de menos é vila que perdeu a
     * população e continua de pé.
     */
    public enum Reason {

        /** Menos de {@code MIN_BEDS} camas: acampamento, não vila. */
        TOO_FEW_BEDS,

        /** Camas bastam, mas não há gente: vila vazia. */
        TOO_FEW_VILLAGERS
    }
}
