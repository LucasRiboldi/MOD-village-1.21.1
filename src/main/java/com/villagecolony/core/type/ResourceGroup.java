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
     * Qualquer tábua.
     *
     * <p>Entrou na Fase 9, e corrige o que a versão anterior deste
     * arquivo dizia: que tábua não se substitui. A frase valia para
     * <b>receita</b> e continua valendo — quem pede tábua de carvalho
     * numa receita não aceita bétula, e o estoque continua contando cada
     * uma pelo nome.
     *
     * <p>O que mudou é a pergunta que o grupo responde, que é outra:
     * "esta colônia já tem material fabricado suficiente?". Aí a espécie
     * não importa, e sem o grupo o fabricante entraria no mesmo laço que
     * o E1: uma colônia de floresta de bétula transformaria tronco em
     * tábua para sempre, porque a meta de carvalho nunca seria atingida
     * por tábua nenhuma que ela conseguisse fazer.
     */
    PLANKS,

    /**
     * Recurso que só se satisfaz com ele mesmo.
     *
     * <p>Pedra não é substituída por nada.
     */
    NONE
}
