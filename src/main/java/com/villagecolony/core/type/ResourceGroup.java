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
     * Qualquer lavoura — 2026-08-27.
     *
     * <p>Mesma razão do grupo da madeira, e a mesma pergunta: <i>"esta
     * colônia tem o que comer?"</i>. Batata responde tão bem quanto
     * trigo, e sem o grupo uma vila de cenoura plantaria para sempre
     * porque a meta de trigo nunca cairia — o E1 por outra porta.
     */
    CROPS,

    /**
     * Pedregulho e arenito: a parede que não é de madeira — 2026-08-20.
     *
     * <p>Um grupo, e não dois, pela mesma razão de {@link #PLANKS}: a
     * pergunta que o grupo responde é "a colônia já tem pedra bastante
     * para a obra?", e aí a espécie não importa. Qual pedra a vila usa é
     * decisão da {@code VillagePalette}, e essa continua por nome.
     */
    STONE,

    /** Areia — o que o fundidor recebe para dar vidro. */
    SAND,

    /** Lã, de onde a cama sai. */
    WOOL,

    /**
     * Carvão — 2026-08-21. A tocha sai daqui.
     *
     * <p>Grupo próprio, e não junto com a pedra, porque grupo é o
     * conjunto do que se <b>substitui</b>: cinco pedregulhos não fazem
     * uma tocha. Carvão e carvão vegetal, sim — a receita da tocha
     * aceita os dois, e a colônia não distingue o que o jogo não
     * distingue.
     */
    COAL,

    /**
     * Ferro cru — 2026-08-21. O lampião sai daqui, passando pela
     * fornalha.
     *
     * <p>Um grupo de um só, como {@link #SAND}: existe para dizer <b>de
     * onde</b> ele vem, que é a mina, e não para substituir nada. O
     * lingote não entra — ferro cru e lingote não se trocam, e pôr os
     * dois no mesmo grupo faria uma meta de lingote parecer satisfeita
     * por minério que ninguém fundiu.
     */
    IRON,

    /**
     * Recurso que só se satisfaz com ele mesmo.
     *
     * <p>Pedra não é substituída por nada.
     */
    NONE
}
