package com.villagecolony.core.resource.service;

import com.villagecolony.core.resource.model.ResourceTally;
import com.villagecolony.core.type.ResourceGroup;
import com.villagecolony.core.type.ResourceType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * O que satisfaz uma exigência além dela mesma — e nada mais.
 *
 * <p><b>Regra de arquitetura do autor, 2026-08-22:</b> {@code
 * ResourceGroup} <b>não significa equivalência</b>. Um grupo serve para
 * <b>classificar</b>. Se o mod quiser permitir substituição, ela precisa
 * ser <b>declarada</b>:
 *
 * <pre>
 * Exigência  →  Aceita
 * </pre>
 *
 * <p><b>O defeito que isso corrige, e ele foi visto em jogo.</b>
 * {@code COBBLESTONE} e {@code SANDSTONE} estão os dois em
 * {@link ResourceGroup#STONE}, e o déficit somava o grupo inteiro: uma
 * vila de deserto com pedregulho no baú concluía que a meta de arenito
 * estava cumprida, e o mineiro nunca ia cavar. Pedregulho não vira
 * arenito em receita nenhuma, e a soma dizia que sim.
 *
 * <p><b>O padrão é não substituir.</b> Uma exigência sem declaração se
 * satisfaz só com ela mesma. Acrescentar tipo novo ao {@code
 * ResourceType} não cria substituição por acidente de grupo, que é
 * exatamente como o arenito entrou.
 *
 * <h2>O que está declarado hoje, e por quê</h2>
 *
 * <p><b>Tronco por tronco, e tábua por tábua.</b> Quem tem o baú cheio
 * de abeto não precisa de carvalho para responder "esta colônia tem
 * madeira?", e mandar buscar seria trabalho para nada. É substituição de
 * <b>estoque</b>, e não de receita: a receita continua pedindo a espécie
 * pelo nome, e o estoque continua sabendo o tipo de cada tronco.
 *
 * <p><b>Pedra não está, e é o ponto.</b> Nem pedregulho por arenito, nem
 * o contrário. O dia em que alguma estrutura aceitar os dois, isso se
 * declara aqui, para aquela exigência, e por escrito.
 */
public final class ResourceSubstitution {

    /**
     * Os grupos cujos membros se substituem, e a lista é a declaração.
     *
     * <p>Estar num grupo não basta: é preciso estar <b>aqui</b>. {@code
     * STONE} é um grupo e não está nesta lista, e é por isso que
     * pedregulho deixou de responder por arenito.
     */
    private static final Set<ResourceGroup> INTERCHANGEABLE =
            EnumSet.of(ResourceGroup.WOOD, ResourceGroup.PLANKS);

    private static final Map<ResourceType, Set<ResourceType>> ACCEPTED = declare();

    private ResourceSubstitution() {
    }

    /**
     * O que serve para esta exigência, ela inclusive.
     *
     * <p>Nunca vazio: toda exigência aceita ao menos ela mesma.
     */
    public static Set<ResourceType> acceptedFor(ResourceType required) {
        Objects.requireNonNull(required, "required");

        return ACCEPTED.getOrDefault(required, Set.of(required));
    }

    /** Se o que a colônia tem serve para o que ela pediu. */
    public static boolean accepts(ResourceType required, ResourceType offered) {
        Objects.requireNonNull(offered, "offered");

        return acceptedFor(required).contains(offered);
    }

    /**
     * Quanto a colônia tem que sirva para esta exigência.
     *
     * <p>Soma <b>só o que foi declarado</b>. Era a soma do grupo até
     * 2026-08-22, e foi assim que o pedregulho passou por arenito.
     */
    public static int availableFor(ResourceType required, ResourceTally owned) {
        Objects.requireNonNull(owned, "owned");

        int total = 0;

        for (ResourceType accepted : acceptedFor(required)) {
            total += owned.amountOf(accepted);
        }

        return total;
    }

    private static Map<ResourceType, Set<ResourceType>> declare() {
        Map<ResourceType, Set<ResourceType>> table = new EnumMap<>(ResourceType.class);

        for (ResourceGroup group : INTERCHANGEABLE) {
            Set<ResourceType> members = EnumSet.noneOf(ResourceType.class);

            for (ResourceType type : ResourceType.values()) {
                if (type.group() == group) {
                    members.add(type);
                }
            }

            for (ResourceType type : members) {
                table.put(type, Collections.unmodifiableSet(EnumSet.copyOf(members)));
            }
        }

        return Collections.unmodifiableMap(table);
    }
}
