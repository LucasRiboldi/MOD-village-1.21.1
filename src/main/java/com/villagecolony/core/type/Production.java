package com.villagecolony.core.type;

/**
 * Como a colônia consegue um recurso.
 *
 * <p><b>Nasceu da ADR-009, em 2026-08-22.</b> A regra de ouro dela diz
 * que uma solução da forma {@code if (x == GLASS || x == IRON_INGOT)}
 * deve ser questionada — e era exatamente assim que
 * {@code ColonyCycle.typeFor} sabia que vidro e lingote saem da
 * fornalha. Uma <b>exceção nominal</b>, escrita à mão, que precisava de
 * mais um nome a cada material novo.
 *
 * <p>Aqui a resposta passa a ser <b>declarada pelo recurso</b>. Material
 * novo diz como é feito, e nenhuma linha de código precisa saber o nome
 * dele.
 *
 * <p>Não é o mesmo que {@link ResourceCategory}. A categoria diz o que o
 * recurso <b>é</b> — natural, processado, construção. Esta diz <b>de
 * onde ele vem</b>, que é a pergunta que decide qual profissão recebe a
 * tarefa. Areia e pedregulho são os dois naturais e os dois minerados;
 * tábua e vidro são os dois processados, e um sai da bancada e o outro
 * da fornalha.
 */
public enum Production {

    /** Do machado do lenhador. */
    HARVESTED,

    /**
     * Colhido de lavoura madura, e replantado — 2026-08-27.
     *
     * <p>Separado do {@link #HARVESTED} de propósito, e a diferença é
     * quem trabalha: madeira é do lenhador, lavoura é do fazendeiro. É a
     * produção declarada que manda a tarefa para a profissão certa — ver
     * {@code ColonyCycle.typeFor} —, e sem um valor próprio a colheita
     * viraria tarefa de madeira e o lenhador iria derrubar árvore para
     * atender fome.
     */
    FARMED,

    /** Da picareta do mineiro, na mina ou na superfície. */
    MINED,

    /** Da tesoura do pastor. */
    SHEARED,

    /** Da bancada do fabricante, por receita do jogo. */
    CRAFTED,

    /** Da fornalha do fundidor, por receita do jogo. */
    SMELTED
}
