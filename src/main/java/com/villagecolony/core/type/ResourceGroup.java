package com.villagecolony.core.type;

/**
 * Recursos que se substituem entre si numa meta.
 *
 * <p>A colônia conta cada madeira pelo nome — bétula é bétula, abeto é
 * abeto — porque as receitas Vanilla distinguem, e uma colônia que
 * esquecesse o tipo não saberia o que tem. Mas quando ela pergunta
 * "tenho madeira suficiente?", a resposta é a soma: sessenta e quatro
 * troncos de abeto satisfazem a mesma meta que sessenta e quatro de
 * carvalho, e mandar o lenhador buscar carvalho com o baú cheio de
 * abeto seria trabalho para nada.
 *
 * <p>É o que separa <b>o que a colônia tem</b> de <b>o que falta</b>. O
 * estoque é por tipo; o déficit é por grupo. Ver
 * {@code ResourceDemand.deficit}.
 */
public enum ResourceGroup {

    /** Qualquer tronco de árvore. */
    WOOD,

    /**
     * Recurso que só se satisfaz com ele mesmo.
     *
     * <p>Tábua de carvalho não é substituída por tábua de bétula numa
     * receita que peça carvalho, e pedra não é substituída por nada.
     */
    NONE
}
