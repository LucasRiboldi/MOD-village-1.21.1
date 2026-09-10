package com.villagecolony.core.worker.model;

/**
 * Profissão de colônia de um trabalhador.
 *
 * <p>É uma camada adicional sobre a profissão Vanilla, não um
 * substituto: o aldeão mantém sua profissão, sua estação de trabalho e
 * sua rotina. Ver Profession-System.md.
 *
 * <p>Aqui existe apenas o tipo. Ferramenta exigida, capacidades e tarefas
 * permitidas são o sistema de profissões, que é TASK-013.
 */
public enum ProfessionType {

    /** Derruba árvores e replanta. Produz madeira. */
    LUMBERJACK,

    /**
     * Tira pedra do mundo — 2026-08-20.
     *
     * <p>Destrava duas coisas de uma vez: os 43 pedregulhos da casa de
     * planície, que o jogador tinha de guardar no baú, e a vila de
     * deserto, que nascia e nunca construía por não haver árvore. No
     * deserto o que ele tira é arenito, que ali é a parede.
     */
    MINER,

    /**
     * Tosquia ovelha — 2026-08-20.
     *
     * <p>A lã da cama. Sem cama não há aldeão novo, e sem aldeão novo a
     * vila não cresce: era o laço aberto que a Regra 21 deixou.
     */
    SHEPHERD,

    /**
     * Funde o que a fornalha funde — 2026-08-20.
     *
     * <p>Areia vira vidro, e vidro vira vidraça. É a exceção honesta que
     * a Regra 10 registrou em 08-18: a vidraça pedia fundir, e a colônia
     * não fundia.
     */
    SMELTER,

    /**
     * O carpinteiro: transforma madeira em material de construção.
     *
     * <p><b>Era o {@code MANUFACTURER}, e a divisão é de 2026-09-09</b>,
     * a pedido do autor. O fabricante fazia os dois ofícios — a tábua e a
     * pedra lavrada — em um arquivo de 639 linhas, acima do limite de
     * 500 que este projeto se impôs.
     *
     * <p><b>Save antigo não quebra.</b> {@code ColonySavedData}
     * devolve {@code null} para profissão que não reconhece, e o aldeão é
     * recontratado no ciclo seguinte: quem estava gravado como
     * {@code MANUFACTURER} volta sem função e ganha uma nova. Perde-se a
     * atribuição, não o mundo.
     */
    CARPENTER,

    /**
     * O pedreiro: transforma pedra bruta em material de construção.
     *
     * <p>A outra metade da divisão de 2026-09-09. <b>Ele não nasceu
     * decorativo</b>: profissão sem material declarado nunca recebe
     * pedido, e o aldeão ficaria com placa, baú e ferramenta sem nunca
     * trabalhar — que foi o estado do fazendeiro até 2026-08-27 e o que
     * {@code ProfessionResponsibilityTest} existe para impedir.
     *
     * <p>O material dele é o tijolo de pedra, e a cadeia inteira nasceu
     * junto: o mineiro traz pedregulho, o fundidor o assa em pedra, e o
     * pedreiro a lavra. Ver {@code ResourceType.STONE_BRICKS}.
     */
    MASON,

    /** Cuida das plantações. */
    FARMER,

    /** Constrói as expansões da vila. */
    BUILDER
}
