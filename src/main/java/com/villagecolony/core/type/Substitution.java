package com.villagecolony.core.type;

import java.util.Comparator;

/**
 * Quanto um recurso serve no lugar de outro — a ADR-009 §3.10.
 *
 * <p>A frase da ADR: <i>deserto <b>prefere</b> arenito; isso não quer
 * dizer que só possa arenito. Variedade sem perder identidade.</i>
 *
 * <p>Substitui a resposta de sim ou não que valeu de 2026-08-22 a
 * 2026-08-26. Ela não estava errada — o padrão continua sendo não
 * substituir, e a exigência sem declaração continua se satisfazendo só
 * com ela mesma. O que faltava era <b>ordem</b>: sim e não não sabem
 * dizer "use este se não houver aquele", e é essa a diferença entre
 * aceitar e preferir.
 *
 * <p><b>A preferência é o campo {@link #preference()}</b>, e não a ordem
 * de declaração: quem escolhe entre dois recursos aceitos pega o de
 * menor número. Até 2026-09-29 era o {@code ordinal()}, e reordenar as
 * constantes mudava o que a colônia escolhia. A folga de dez deixa
 * entrar um nível novo sem renumerar; {@code SubstitutionTest} fixa a
 * sequência.
 */
public enum Substitution {

    /**
     * É o que se pediu, ou vale tanto quanto.
     *
     * <p>Todo recurso é {@code PREFERRED} para si mesmo, e isso não se
     * declara em lugar nenhum: sai de graça da comparação.
     */
    PREFERRED(10),

    /**
     * Serve para a <b>meta</b> da colônia, e não para a parede.
     *
     * <p>É o caso da madeira: quem tem o baú cheio de abeto não precisa
     * de carvalho para responder "esta colônia tem tronco?", e mandar
     * buscar seria trabalho para nada.
     *
     * <p><b>O construtor continua exigindo o exato</b> neste nível — é a
     * Regra 27, e ela só abriu para pedra. Substituição de estoque não é
     * substituição de obra.
     */
    ACCEPTABLE(20),

    /**
     * Serve <b>até na parede</b>, e só quando não houver nada melhor.
     *
     * <p>O nível que a ADR criou para a variedade: o bloco entra na casa,
     * a identidade da vila se mantém, e a colônia continua preferindo o
     * certo enquanto ele existir.
     *
     * <p><b>É o que distingue este nível do de cima</b>, e é a diferença
     * que a Regra 27 desenhou em 2026-08-26: o que está aqui o construtor
     * pode assentar; o que está em {@link #ACCEPTABLE} ele conta e não
     * assenta.
     *
     * <p>Declarado hoje: a família da pedra, e só ela — pedregulho e
     * arenito, um pelo outro. Decisão do autor.
     */
    ALTERNATIVE(30),

    /**
     * Não serve.
     *
     * <p><b>O padrão</b>, e é ele que segura a arquitetura de pé: estar
     * no mesmo {@link ResourceGroup} não basta, porque grupo classifica e
     * não equivale. Pedregulho e arenito moram os dois em
     * {@link ResourceGroup#STONE} e são proibidos um para o outro.
     */
    FORBIDDEN(40);

    /** Do que mais serve para o que menos serve. */
    public static final Comparator<Substitution> BEST_FIRST =
            Comparator.comparingInt(Substitution::preference);

    private final int preference;

    Substitution(int preference) {
        this.preference = preference;
    }

    /** Menor é melhor. Só para ordenar — não é persistido. */
    public int preference() {
        return preference;
    }

    /** Se este nível deixa o recurso passar. */
    public boolean serves() {
        return this != FORBIDDEN;
    }

    /** Se este é melhor que aquele — menor {@link #preference()}. */
    public boolean isBetterThan(Substitution other) {
        return preference < other.preference;
    }
}
