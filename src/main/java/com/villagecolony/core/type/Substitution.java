package com.villagecolony.core.type;

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
 * <p><b>A ordem do enum é a ordem da preferência</b>, e há código que
 * depende disso: quem escolhe entre dois recursos aceitos pega o de
 * menor {@code ordinal}. Reordenar aqui muda o que a colônia escolhe.
 */
public enum Substitution {

    /**
     * É o que se pediu, ou vale tanto quanto.
     *
     * <p>Todo recurso é {@code PREFERRED} para si mesmo, e isso não se
     * declara em lugar nenhum: sai de graça da comparação.
     */
    PREFERRED,

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
    ACCEPTABLE,

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
    ALTERNATIVE,

    /**
     * Não serve.
     *
     * <p><b>O padrão</b>, e é ele que segura a arquitetura de pé: estar
     * no mesmo {@link ResourceGroup} não basta, porque grupo classifica e
     * não equivale. Pedregulho e arenito moram os dois em
     * {@link ResourceGroup#STONE} e são proibidos um para o outro.
     */
    FORBIDDEN;

    /** Se este nível deixa o recurso passar. */
    public boolean serves() {
        return this != FORBIDDEN;
    }

    /** Se este é melhor que aquele — menor ordinal, maior preferência. */
    public boolean isBetterThan(Substitution other) {
        return ordinal() < other.ordinal();
    }
}
