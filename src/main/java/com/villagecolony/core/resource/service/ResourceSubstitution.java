package com.villagecolony.core.resource.service;

import com.villagecolony.core.resource.model.ResourceTally;
import com.villagecolony.core.type.ResourceGroup;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.type.Substitution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
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
 *
 * <h2>Os quatro níveis — 2026-08-26</h2>
 *
 * <p>Decisão do autor: a resposta deixa de ser sim ou não e passa a ser
 * {@link Substitution}, os quatro níveis da ADR-009 §3.10. O padrão não
 * mudou — o que era "não" virou {@code FORBIDDEN}, e o que era "sim"
 * virou {@code ACCEPTABLE}. O que se ganhou foi <b>ordem</b>: sim e não
 * não sabem dizer "use este se não houver aquele".
 *
 * <h2>O que a Regra 27 impede, e por que nada está em ALTERNATIVE</h2>
 *
 * <p>A variedade que a ADR quer — <i>deserto prefere arenito, não quer
 * dizer que só possa arenito</i> — esbarra numa regra <b>imutável</b>: a
 * Regra 27 manda o construtor <i>aguardar a existência do específico
 * tipo de bloco que ele precisa</i>. Ele não troca bloco, e por decisão
 * do autor em 2026-08-20 não vai trocar.
 *
 * <p>E a substituição não é lida por ele: quem a lê é
 * {@code ResourceDemand.deficit}, que decide <b>quais tarefas abrir</b>.
 * Declarar aqui que pedregulho serve por arenito faria a colônia
 * concluir que a meta está cumprida e <b>não mandar o mineiro cavar</b>,
 * enquanto o construtor espera para sempre pelo arenito que ninguém foi
 * buscar. É exatamente o defeito de 2026-08-22, ressuscitado por outro
 * caminho.
 *
 * <p>Por isso a madeira é segura e a pedra não: a meta de madeira é
 * <b>genérica de propósito</b> — {@code OAK_LOG} responde pelo grupo
 * inteiro em {@code ColonyGoals}, e o construtor não pede espécie. A
 * meta de pedra nomeia o bloco que a casa daquele bioma usa.
 *
 * <p><b>Enquanto a Regra 27 valer, {@code ALTERNATIVE} fica vazio.</b> O
 * nível existe, a ordem funciona, e o dia em que o autor abrir mão da
 * Regra 27 — ou o dia em que a demanda de obra deixar de virar meta
 * nominal — a declaração é uma linha.
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

    /**
     * O que cada exigência aceita, e em que nível.
     *
     * <p>Ausente quer dizer {@link Substitution#FORBIDDEN}, e a exigência
     * comparada consigo mesma nunca chega aqui: ela é
     * {@link Substitution#PREFERRED} por definição.
     */
    private static final Map<ResourceType, Map<ResourceType, Substitution>> DECLARED =
            declare();

    private ResourceSubstitution() {
    }

    /**
     * O que serve para esta exigência, ela inclusive.
     *
     * <p>Nunca vazio: toda exigência aceita ao menos ela mesma.
     */
    public static Set<ResourceType> acceptedFor(ResourceType required) {
        Objects.requireNonNull(required, "required");

        Set<ResourceType> serving = EnumSet.of(required);

        DECLARED.getOrDefault(required, Map.of()).forEach((offered, level) -> {
            if (level.serves()) {
                serving.add(offered);
            }
        });

        return Collections.unmodifiableSet(serving);
    }

    /**
     * Em que nível o oferecido serve para o exigido — 2026-08-26.
     *
     * <p>A pergunta inteira mora aqui; o resto do arquivo são atalhos
     * sobre ela.
     */
    public static Substitution levelOf(ResourceType required, ResourceType offered) {
        Objects.requireNonNull(required, "required");
        Objects.requireNonNull(offered, "offered");

        if (required == offered) {
            return Substitution.PREFERRED;
        }

        return DECLARED
                .getOrDefault(required, Map.of())
                .getOrDefault(offered, Substitution.FORBIDDEN);
    }

    /**
     * O que serve, do melhor para o pior — a ordem da ADR-009 §3.10.
     *
     * <p>É a diferença entre aceitar e preferir, e é para isto que os
     * quatro níveis existem: quem for gastar um recurso gasta primeiro o
     * que sobra antes de tocar no que a colônia prefere guardar.
     *
     * <p>Empate dentro do mesmo nível se desfaz pela ordem do
     * {@code ResourceType}, e não por sorteio: vila que cresce diferente
     * a cada sessão é vila que ninguém consegue depurar.
     */
    public static List<ResourceType> byPreference(ResourceType required) {
        Objects.requireNonNull(required, "required");

        List<ResourceType> order = new ArrayList<>(acceptedFor(required));

        order.sort(Comparator
                .comparingInt((ResourceType type) -> levelOf(required, type).ordinal())
                .thenComparing(Enum::ordinal));

        return Collections.unmodifiableList(order);
    }

    /** Se o que a colônia tem serve para o que ela pediu. */
    public static boolean accepts(ResourceType required, ResourceType offered) {
        return levelOf(required, offered).serves();
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

    private static Map<ResourceType, Map<ResourceType, Substitution>> declare() {
        Map<ResourceType, Map<ResourceType, Substitution>> table =
                new EnumMap<>(ResourceType.class);

        for (ResourceGroup group : INTERCHANGEABLE) {
            Set<ResourceType> members = EnumSet.noneOf(ResourceType.class);

            for (ResourceType type : ResourceType.values()) {
                if (type.group() == group) {
                    members.add(type);
                }
            }

            for (ResourceType type : members) {
                Map<ResourceType, Substitution> accepted = new EnumMap<>(ResourceType.class);

                for (ResourceType other : members) {
                    if (other != type) {
                        // ACCEPTABLE, e nao PREFERRED: abeto serve tanto
                        // quanto carvalho para "tenho tronco?", mas quem
                        // se prefere e sempre o que se pediu.
                        accepted.put(other, Substitution.ACCEPTABLE);
                    }
                }

                table.put(type, Collections.unmodifiableMap(accepted));
            }
        }

        return Collections.unmodifiableMap(table);
    }
}
