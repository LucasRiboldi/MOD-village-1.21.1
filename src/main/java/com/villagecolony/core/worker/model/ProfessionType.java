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

    /** Transforma matéria-prima em material de construção. */
    MANUFACTURER,

    /** Cuida das plantações. */
    FARMER,

    /** Constrói as expansões da vila. */
    BUILDER
}
