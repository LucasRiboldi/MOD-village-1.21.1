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
     * Serve sem ressalva quando o preferido não está à mão.
     *
     * <p>É o caso da madeira: quem tem o baú cheio de abeto não precisa
     * de carvalho para responder "esta colônia tem tronco?", e mandar
     * buscar seria trabalho para nada.
     */
    ACCEPTABLE,

    /**
     * Serve, e só quando não houver nada melhor.
     *
     * <p>O nível que a ADR criou para a variedade: o bloco entra, a
     * identidade da vila se mantém, e a colônia continua preferindo o
     * certo enquanto ele existir.
     *
     * <p><b>Nada está declarado neste nível hoje</b>, e não é
     * esquecimento — ver {@code ResourceSubstitution}, §"o que a Regra 27
     * impede".
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
